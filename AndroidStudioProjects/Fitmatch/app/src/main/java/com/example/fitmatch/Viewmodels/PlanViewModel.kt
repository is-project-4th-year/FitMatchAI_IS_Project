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
import kotlin.math.max

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface UiEvent {
    data object WorkoutCreated : UiEvent
    data class Error(val msg: String) : UiEvent
}

private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
val events = _events.asSharedFlow()

data class PlanUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val latest: PlanDTO? = null,
    val history: List<PlanDTO> = emptyList()
)

class PlanViewModel(
    private val repo: PlanRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    // — features sanitation —
    private val requiredBaseKeys = setOf(
        "age","height","weight","bmi",
        "goal_type","workouts_per_week","calories_avg","equipment"
    )
    private val adherenceKeys = setOf("volume_scale","intensity_scale","progression_bias")
    private val allowedKeys   = requiredBaseKeys + adherenceKeys

    private fun sanitizeFeatures(raw: Map<String, Any?>): Map<String, Any> =
        raw.filterKeys { it in allowedKeys }
            .mapNotNull { (k, v) ->
                val clean: Any? = when (v) {
                    is Number, is String, is Boolean -> v
                    else -> null // drop Timestamp, maps, lists
                }
                if (clean != null) k to clean else null
            }.toMap()

    private val _ui = MutableStateFlow(PlanUiState())
    val ui: StateFlow<PlanUiState> = _ui.asStateFlow()

    private fun uidOrThrow(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    // ===== Snapshot “flip-back” guard =====
    // After we generate a plan locally, hold off applying older snapshots for a short window.
    private var stickyCreatedAtMs: Long? = null
    private var lastLocalCreatedAtMs: Long = 0L
    private var holdStartedAtMs: Long = 0L
    private val holdWindowMs: Long = 6000L // keep as you set it

    private fun shouldApplySnapshotCandidate(snapshotLatestMs: Long): Boolean {
        if (lastLocalCreatedAtMs == 0L) return true
        if (snapshotLatestMs >= lastLocalCreatedAtMs) return true
        val elapsed = System.currentTimeMillis() - holdStartedAtMs
        return elapsed > holdWindowMs
    }

    /** Live history + latest plan (guarded). */
    fun startObservingHistory() {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }
            _events.tryEmit(UiEvent.Error("Not signed in"))
            return
        }
        viewModelScope.launch {
            repo.observePlans(uid).collect { plans ->
                if (plans.isEmpty()) {
                    _ui.update { it.copy(history = emptyList(), latest = null, error = null) }
                    return@collect
                }

                val sortedDefault = plans.sortedWith(
                    compareByDescending<PlanDTO> { it.week_index ?: 0 }
                        .thenByDescending { it.day_offset ?: 0 }
                        .thenByDescending { it.created_at_ms }
                )

                val sticky = stickyCreatedAtMs?.let { ms ->
                    plans.firstOrNull { it.created_at_ms == ms }
                }

                val candidate = sticky ?: sortedDefault.first()
                val candidateMs = candidate.created_at_ms
                val accept = when {
                    lastLocalCreatedAtMs == 0L -> true
                    candidateMs >= lastLocalCreatedAtMs -> true
                    else -> (System.currentTimeMillis() - holdStartedAtMs) > holdWindowMs
                }

                if (accept) {
                    _ui.update { it.copy(history = sortedDefault, latest = candidate, error = null) }
                } else {
                    _ui.update { it.copy(history = sortedDefault) }
                }
            }
        }
    }

    /** Create initial plan (week_index=0 / day_offset=0). */
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
            _ui.update { it.copy(error = "Not signed in") }
            _events.tryEmit(UiEvent.Error("Not signed in"))
            return
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
                stickyCreatedAtMs = plan.created_at_ms
                lastLocalCreatedAtMs = max(lastLocalCreatedAtMs, plan.created_at_ms)
                holdStartedAtMs = System.currentTimeMillis()
                _ui.update { it.copy(loading = false, latest = plan, error = null) }

                // <<< ADDED: success event for snackbar >>>
                _events.tryEmit(UiEvent.WorkoutCreated)

            } catch (e: Exception) {
                val msg = e.message ?: "Failed"
                _ui.update { it.copy(loading = false, error = msg) }
                // <<< ADDED: error event >>>
                _events.tryEmit(UiEvent.Error(msg))
            }
        }
    }

    /** Provide latest features to the screen for prefill. */
    fun fetchLatestFeatures(
        onResult: (Map<String, Any?>) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }
            _events.tryEmit(UiEvent.Error("Not signed in"))
            return
        }
        viewModelScope.launch {
            try {
                val map = repo.fetchLatestFeatures(uid)
                onResult(map)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /** Re-generate plan using adherence (merges base features + scalers, shifts days). */
    fun regenWithAdherence(planId: String, summary: AdherenceSummary) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val uid  = uidOrThrow()
                val base = repo.fetchLatestFeatures(uid)
                if (base.isEmpty()) {
                    val msg = "No saved base features. Create a plan first."
                    _ui.update { it.copy(loading = false, error = msg) }
                    _events.tryEmit(UiEvent.Error(msg))
                    return@launch
                }

                val merged = base.toMutableMap().apply {
                    this["volume_scale"]    = summary.volume_scale
                    this["intensity_scale"] = summary.intensity_scale
                    this["progression_bias"] = when {
                        summary.completion_pct >= 0.90 -> 1
                        summary.completion_pct >= 0.75 -> 0
                        else -> -1
                    }
                }.filterValues { it!=null } as Map<String, Any>

                val clean = sanitizeFeatures(merged)
                val last = ui.value.latest
                val lastMaxDay = last?.exercises?.maxOfOrNull { it.day } ?: 0
                val nextOffset = lastMaxDay
                val nextWeek   = (last?.week_index ?: 0) + 1

                val newPlan = repo.generateWithAntiRepeat(
                    userId   = uid,
                    cleanFeatures = clean,
                    dayOffset = nextOffset,
                    weekIndex = nextWeek,
                    lastPlan = last
                )
                stickyCreatedAtMs = newPlan.created_at_ms
                lastLocalCreatedAtMs = max(lastLocalCreatedAtMs, newPlan.created_at_ms)
                holdStartedAtMs = System.currentTimeMillis()

                _ui.update { it.copy(loading = false, latest = newPlan, error = null) }
                // (No success toast requested here; keeping exactly to your ask.)

            } catch (e: Exception) {
                val msg = e.message ?: "Regen failed"
                _ui.update { it.copy(loading = false, error = msg) }
                _events.tryEmit(UiEvent.Error(msg))
            }
        }
    }

    /** Finalize week => compute adherence => gate => save => regenerate next week. */
    fun finalizeWeekAndRegenerate(
        weekStartMs: Long,
        weekEndMs: Long,
        workoutsPerWeek: Int?,
        goalEndMs: Long?
    ) {
        viewModelScope.launch {
            val uid = runCatching { uidOrThrow() }.getOrElse {
                _ui.update { it.copy(error = "Not signed in") }
                _events.tryEmit(UiEvent.Error("Not signed in"))
                return@launch
            }
            val latest = ui.value.latest ?: run {
                _ui.update { it.copy(error = "No active plan") }
                _events.tryEmit(UiEvent.Error("No active plan"))
                return@launch
            }

            if (goalEndMs != null && System.currentTimeMillis() > goalEndMs) {
                val msg = "Goal period ended. Create a new goal to continue."
                _ui.update { it.copy(error = msg) }
                _events.tryEmit(UiEvent.Error(msg))
                return@launch
            }

            _ui.update { it.copy(loading = true, error = null) }
            try {
                val summary = repo.computeWeeklyAdherence(uid, latest.plan_id, weekStartMs, weekEndMs)

                if (summary.completion_pct < 0.40) {
                    repo.saveAdherenceSummary(uid, summary)
                    val msg = "Low adherence this week; holding progression."
                    _ui.update { it.copy(loading = false, error = msg) }
                    _events.tryEmit(UiEvent.Error(msg))
                    return@launch
                }

                repo.saveAdherenceSummary(uid, summary)
                // regen path handles offsets/guards
                regenWithAdherence(latest.plan_id, summary)
            } catch (e: Exception) {
                val msg = e.message ?: "Weekly finalize failed"
                _ui.update { it.copy(loading = false, error = msg) }
                _events.tryEmit(UiEvent.Error(msg))
            }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }

    private fun isNewer(current: PlanDTO?, candidate: PlanDTO): Boolean {
        if (current == null) return true
        val cw = current.week_index ?: 0
        val ww = candidate.week_index ?: 0
        if (ww != cw) return ww > cw

        val co = current.day_offset ?: 0
        val wo = candidate.day_offset ?: 0
        if (wo != co) return wo > co

        return candidate.created_at_ms > current.created_at_ms
    }
}
