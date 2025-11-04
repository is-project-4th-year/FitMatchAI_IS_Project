package com.example.fitmatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.DietPlanDTO
import com.example.fitmatch.models.NutritionRepository
import com.example.fitmatch.models.PlanRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NutritionUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val latest: DietPlanDTO? = null
)

class NutritionViewModel(
    private val auth: FirebaseAuth,
    private val nutritionRepo: NutritionRepository,
    private val planRepo: PlanRepository // we reuse it to pull latest base features
) : ViewModel() {

    private val _ui = MutableStateFlow(NutritionUiState())
    val ui: StateFlow<NutritionUiState> = _ui.asStateFlow()

    private fun uidOrThrow(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    fun startObserving() {
        val uid = runCatching { uidOrThrow() }.getOrElse {
            _ui.update { it.copy(error = "Not signed in") }; return
        }
        viewModelScope.launch {
            nutritionRepo.observeLatest(uid).collect { plan ->
                _ui.update { it.copy(latest = plan, error = null) }
            }
        }
    }

    /** Generate from the most recent base features you already save under /features */
    fun regenerateFromLatestFeatures() {
        viewModelScope.launch {
            val uid = runCatching { uidOrThrow() }.getOrElse {
                _ui.update { it.copy(error = "Not signed in") }; return@launch
            }
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val base = planRepo.fetchLatestFeatures(uid)
                if (base.isEmpty()) {
                    _ui.update { it.copy(loading = false, error = "No saved metrics. Create a workout plan first.") }
                    return@launch
                }
                val plan = nutritionRepo.generateAndSaveFromFeatures(uid, base)
                _ui.update { it.copy(loading = false, latest = plan) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Failed") }
            }
        }
    }
}