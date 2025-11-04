package com.example.fitmatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.AdherenceSummary
import com.example.fitmatch.data.PlanDTO
import com.example.fitmatch.models.PlanRepository
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

@Suppress("UNCHECKED_CAST")
class PlanViewModel(
    private val repo: PlanRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // ---- Features that /predict expects ----
    private val requiredBaseKeys = setOf(
        "age","height","weight","bmi",
        "goal_type","workouts_per_week","calories_avg","equipment"
    )
    private val adherenceKeys = setOf("volume_scale","intensity_scale","progression_bias")
    private val allowedKeys = requiredBaseKeys + adherenceKeys

    private fun sanitizeFeatures(raw: Map<String, Any?>): Map<String, Any> =
        raw.filterKeys { it in allowedKeys }
            .mapNotNull { (k, v) ->
                val clean: Any? = when (v) {
                    is Number, is String, is Boolean -> v
                    is com.google.firebase.Timestamp -> v.seconds   // or v.toDate().time
                    else -> null
                }
                if (clean != null) k to clean else null
            }.toMap()

    private fun hasBaseFeatures(m: Map<String, Any?>): Boolean =
        requiredBaseKeys.all { it in m && m[it] != null }

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

    /** Prefill for PlanScreen (runs in coroutine, uses repository). */
    fun fetchLatestFeatures(
        onResult: (Map<String, Any?>) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }; return
        }
        viewModelScope.launch {
            try {
                val map = repo.fetchLatestFeatures(uid)  // already sanitized to base fields in repo
                onResult(map)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /** Re-generate plan using adherence summary (merges base features + adherence scales). */
    fun regenWithAdherence(planId: String, summary: AdherenceSummary) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val uid = uidOrThrow()

                val base = repo.fetchLatestFeatures(uid) // Map<String, Any?>
                if (!hasBaseFeatures(base)) {
                    _ui.update {
                        it.copy(loading = false,
                            error = "No saved base features. Create a plan from Custom Entry first.")
                    }
                    return@launch
                }

                val merged = base.toMutableMap().apply {
                    this["volume_scale"] = summary.volume_scale
                    this["intensity_scale"] = summary.intensity_scale
                    this["progression_bias"] = when {
                        summary.completion_pct >= 0.90 -> 1
                        summary.completion_pct >= 0.75 -> 0
                        else -> -1
                    }
                }

                val clean = sanitizeFeatures(merged)
                val plan = repo.generateAndSavePlan(uid, clean)
                _ui.update { it.copy(loading = false, latest = plan, error = null) }
            } catch (e: Exception) {
                val msg = e.message ?: "Regen failed"
                val friendly = if (msg.contains("Missing feature", ignoreCase = true))
                    "Could not regenerate: missing base features. Create a plan from Custom Entry first."
                else msg
                _ui.update { it.copy(loading = false, error = friendly) }
            }
        }
    }

    fun finalizeWeekAndRegenerate(
        weekStartMs: Long,
        weekEndMs: Long,
        workoutsPerWeek: Int? = null,  // from active goal (optional)
        goalEndMs: Long? = null        // from active goal (optional)
    ) {
        viewModelScope.launch {
            val uid = runCatching { uidOrThrow() }.getOrElse {
                _ui.update { it.copy(error = "Not signed in") }; return@launch
            }
            val latest = ui.value.latest ?: run {
                _ui.update { it.copy(error = "No active plan") }; return@launch
            }

            // Optional: stop if the goal window is over
            if (goalEndMs != null && weekStartMs >= goalEndMs) {
                _ui.update { it.copy(error = "Goal period ended. Create a new goal to continue.") }
                return@launch
            }

            _ui.update { it.copy(loading = true, error = null) }
            try {
                // 1) summarize adherence for this week (your existing repo logic)
                val summary = repo.computeWeeklyAdherence(uid, latest.plan_id, weekStartMs, weekEndMs)
                repo.saveAdherenceSummary(uid, summary)

                // 2) If adherence is too low, hold progression
                if (summary.completion_pct < 0.40) {
                    _ui.update { it.copy(loading = false, error = "Low adherence this week; holding progression.") }
                    return@launch
                }

                // 3) pull base features and merge adherence adjustments
                val base = repo.fetchLatestFeatures(uid)
                val merged = base.toMutableMap().apply {
                    this["volume_scale"] = summary.volume_scale
                    this["intensity_scale"] = summary.intensity_scale
                    this["progression_bias"] = when {
                        summary.completion_pct >= 0.90 -> 1
                        summary.completion_pct >= 0.75 -> 0
                        else -> -1
                    }
                }

                // 4) Compute next day offset + week index
                //    We use the highest day number we've ever shown, then continue.
                val nextOffset = latest.exercises.maxOfOrNull { it.day } ?: 0
                val nextWeek   = (latest.week_index) + 1

                // 5) Generate next week's plan with shifted days
                val plan = repo.generateAndSavePlan(
                    userId = uid,
                    features = merged as Map<String, Any>,
                    dayOffset = nextOffset,
                    weekIndex = nextWeek
                )

                _ui.update { it.copy(loading = false, latest = plan, error = null) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Weekly finalize failed") }
            }
        }
    }
}