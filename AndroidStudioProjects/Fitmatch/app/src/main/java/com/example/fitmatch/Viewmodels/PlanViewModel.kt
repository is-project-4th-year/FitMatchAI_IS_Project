package com.example.fitmatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.PlanDTO
import com.example.fitmatch.models.PlanRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.fitmatch.data.AdherenceSummary

data class PlanUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val latest: PlanDTO? = null,
    val history: List<PlanDTO> = emptyList()
)

@Suppress("UNCHECKED_CAST")
class PlanViewModel(
    private val repo: PlanRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _ui = MutableStateFlow(PlanUiState())
    val ui: StateFlow<PlanUiState> = _ui.asStateFlow()

    private fun uidOrThrow(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    fun startObservingHistory() {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }
            return
        }
        viewModelScope.launch {
            repo.observePlans(uid).collect { plans ->
                _ui.update { it.copy(history = plans, latest = plans.firstOrNull(), error = null) }
            }
        }
    }

    fun generateFromMetrics(
        age: Int,
        heightCm: Double,
        weightKg: Double,
        bmi: Double,
        goalTypeIndex: Int,
        workoutsPerWeek: Int,
        caloriesAvg: Double,
        equipment: String
    ) {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }; return
        }
        val features: Map<String, Any?> = mapOf(
            "age" to age,
            "height" to heightCm,
            "weight" to weightKg,
            "bmi" to bmi,
            "goal_type" to goalTypeIndex,
            "workouts_per_week" to workoutsPerWeek,
            "calories_avg" to caloriesAvg,
            "equipment" to equipment.lowercase()
        )
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val plan = repo.generateAndSavePlan(uid, features as Map<String, Any>)
                _ui.update { it.copy(loading = false, latest = plan) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Failed") }
            }
        }
    }

    fun clearError() { _ui.update { it.copy(error = null) } }

    /** For PlanScreen prefill */
    fun fetchLatestFeatures(onResult: (Map<String, Any?>) -> Unit, onError: (Throwable) -> Unit = {}) {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }; return
        }
        viewModelScope.launch {
            try {
                val map = repo.fetchLatestFeatures(uid)   // returns emptyMap() if none
                onResult(map)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /** Re-generate plan using adherence summary */
    fun regenWithAdherence(planId: String, summary: AdherenceSummary) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val uid = uidOrThrow()
                val base = repo.fetchLatestFeatures(uid)        // Map<String, Any?>
                val f = base.toMutableMap()

                f["volume_scale"] = summary.volume_scale
                f["intensity_scale"] = summary.intensity_scale
                f["progression_bias"] = when {
                    summary.completion_pct >= 0.90 -> 1
                    summary.completion_pct >= 0.75 -> 0
                    else -> -1
                }

                val plan = repo.generateAndSavePlan(uid, f as Map<String, Any>)
                _ui.update { it.copy(loading = false, latest = plan) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Regen failed") }
            }
        }
    }
    fun finalizeWeekAndRegenerate(weekStartMs: Long, weekEndMs: Long) {
        viewModelScope.launch {
            val uid = runCatching { uidOrThrow() }.getOrElse {
                _ui.update { it.copy(error = "Not signed in") }; return@launch
            }
            val planId = ui.value.latest?.plan_id ?: run {
                _ui.update { it.copy(error = "No active plan") }; return@launch
            }

            _ui.update { it.copy(loading = true, error = null) }
            try {
                val summary = repo.computeWeeklyAdherence(uid, planId, weekStartMs, weekEndMs)

                // Safety gate
                if (summary.completion_pct < 0.40) {
                    repo.saveAdherenceSummary(uid, summary)
                    _ui.update { it.copy(loading = false, error = "Low adherence this week; holding progression.") }
                    return@launch
                }

                repo.saveAdherenceSummary(uid, summary)
                // Reuse your existing path that already applies scales inside repo.generateAndSavePlan()
                regenWithAdherence(planId, summary)
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Weekly finalize failed") }
            }
        }
    }
}


