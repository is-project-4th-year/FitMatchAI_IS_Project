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
        age: Int, heightCm: Double, weightKg: Double, bmi: Double,
        goalTypeIndex: Int, workoutsPerWeek: Int, caloriesAvg: Double, equipment: String
    ) {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }; return
        }
        val features = mapOf(
            "age" to age, "height" to heightCm, "weight" to weightKg, "bmi" to bmi,
            "goal_type" to goalTypeIndex, "workouts_per_week" to workoutsPerWeek,
            "calories_avg" to caloriesAvg, "equipment" to equipment.lowercase()
        )
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            runCatching { repo.generateAndSavePlan(uid, features) }
                .onSuccess { plan -> _ui.update { it.copy(loading = false, latest = plan) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Failed") } }
        }
    }

    fun clearError() { _ui.update { it.copy(error = null) } }
}