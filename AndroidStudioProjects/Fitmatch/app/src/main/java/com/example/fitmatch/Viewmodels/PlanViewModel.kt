package com.example.fitmatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.PlanDTO
import com.example.fitmatch.models.PlanRepository
import com.example.fitmatch.data.AdherenceSummary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val latest: PlanDTO? = null,
    val history: List<PlanDTO> = emptyList()
)

class PlanViewModel(
    private val repo: PlanRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // The 8 required base features your /predict expects
    private val requiredBaseKeys = setOf(
        "age","height","weight","bmi",
        "goal_type","workouts_per_week","calories_avg","equipment"
    )
    private val adherenceKeys = setOf("volume_scale","intensity_scale","progression_bias")
    private val allowedKeys = requiredBaseKeys + adherenceKeys

    private val _ui = MutableStateFlow(PlanUiState())
    val ui: StateFlow<PlanUiState> = _ui.asStateFlow()

    private fun uidOrThrow(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    /** Live history + latest plan */
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

    /** Entry point for Custom Entry -> generates fresh plan (week_index=0 / day_offset=0). */
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
        val features: Map<String, Any> = mapOf(
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
                val plan = repo.generateAndSavePlan(
                    userId = uid,
                    features = features,
                    dayOffset = 0,
                    weekIndex = 0
                )
                _ui.update { it.copy(loading = false, latest = plan) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Failed") }
            }
        }
    }

    /** Provide latest features to the PlanScreen for prefill. */
    fun fetchLatestFeatures(
        onResult: (Map<String, Any?>) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }; return
        }
        viewModelScope.launch {
            try {
                val map = repo.fetchLatestFeatures(uid)          // already base-only in repo
                onResult(map)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /** Re-generate plan using adherence; shifts days to continue where the user left off. */
    fun regenWithAdherence(planId: String, summary: AdherenceSummary) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val uid = uidOrThrow()

                // pull most recent base features
                val base = repo.fetchLatestFeatures(uid)
                if (!requiredBaseKeys.all { base[it] != null }) {
                    _ui.update {
                        it.copy(loading = false, error = "No saved base features. Create a plan first.")
                    }
                    return@launch
                }

                val latest = ui.value.latest
                val wpw = latest?.microcycle_days?.coerceAtLeast(1) ?: 3
                val lastDay = latest?.exercises?.maxOfOrNull { ex -> ex.day } ?: 0
                val currentWeekIndex = latest?.week_index ?: (lastDay / wpw)
                val nextWeekIndex = currentWeekIndex + 1
                val dayOffset = lastDay // continue counting days globally

                val features = base.toMutableMap().apply {
                    this["volume_scale"] = summary.volume_scale
                    this["intensity_scale"] = summary.intensity_scale
                    this["progression_bias"] = when {
                        summary.completion_pct >= 0.90 -> 1
                        summary.completion_pct >= 0.75 -> 0
                        else -> -1
                    }
                } // map contains only primitives/strings

                val plan = repo.generateAndSavePlan(
                    userId = uid,
                    features = features as Map<String, Any>,
                    dayOffset = dayOffset,
                    weekIndex = nextWeekIndex
                )
                _ui.update { it.copy(loading = false, latest = plan) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Regen failed") }
            }
        }
    }

    /** Finalize a week => compute adherence => (gate) => persist summary => regenerate next week. */
    fun finalizeWeekAndRegenerate(
        weekStartMs: Long,
        weekEndMs: Long,
        workoutsPerWeek: Int?,  // optional (used only for fallback calc)
        goalEndMs: Long?        // optional (block if goal is over)
    ) {
        viewModelScope.launch {
            val uid = runCatching { uidOrThrow() }.getOrElse {
                _ui.update { it.copy(error = "Not signed in") }; return@launch
            }
            val latest = ui.value.latest ?: run {
                _ui.update { it.copy(error = "No active plan") }; return@launch
            }

            if (goalEndMs != null && System.currentTimeMillis() > goalEndMs) {
                _ui.update { it.copy(error = "Goal period ended. Create a new goal to continue.") }
                return@launch
            }

            _ui.update { it.copy(loading = true, error = null) }
            try {
                val summary = repo.computeWeeklyAdherence(uid, latest.plan_id, weekStartMs, weekEndMs)

                if (summary.completion_pct < 0.40) {
                    repo.saveAdherenceSummary(uid, summary)
                    _ui.update { it.copy(loading = false, error = "Low adherence this week; holding progression.") }
                    return@launch
                }

                repo.saveAdherenceSummary(uid, summary)

                // Delegate to the regen function (ensures single code path)
                regenWithAdherence(latest.plan_id, summary)

            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Weekly finalize failed") }
            }
        }
    }
}