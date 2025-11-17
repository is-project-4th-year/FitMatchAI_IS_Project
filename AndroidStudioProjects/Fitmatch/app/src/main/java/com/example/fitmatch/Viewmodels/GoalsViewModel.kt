package com.example.fitmatch.Viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.Goal
import com.example.fitmatch.models.GoalsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GoalsViewModel(
    private val repo: GoalsRepository = run {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: error("User not logged in")
        GoalsRepository(uid)
    }
) : ViewModel() {

    private val _activeGoal = MutableStateFlow<Goal?>(null)
    val activeGoal: StateFlow<Goal?> = _activeGoal.asStateFlow()

    // FIX: actually load from repository
    fun loadActiveGoal() = viewModelScope.launch {
        _activeGoal.value = repo.getActiveGoalOrNull()
    }

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    val activeGoals: StateFlow<List<Goal>> = goals
        .map { list -> list.filter { it.status == "active" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pastGoals: StateFlow<List<Goal>> = goals
        .map { list -> list.filter { it.status == "completed" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            repo.goalsFlow().collect { _goals.value = it }
        }
    }

    private fun computeEndDate(startMs: Long, durationWeeks: Int): Long {
        return startMs + durationWeeks * 7L * 24L * 60L * 60L * 1000L
    }

    fun add(goal: Goal) = viewModelScope.launch { repo.add(goal) }
    fun update(goal: Goal) = viewModelScope.launch { repo.update(goal) }
    fun updateProgress(id: String, newCurrent: Float) = viewModelScope.launch { repo.updateProgress(id, newCurrent) }
    fun markCompleted(id: String) = viewModelScope.launch { repo.markCompleted(id) }
    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }

    // If you call this helper elsewhere:
    fun saveGoal(
        goalType: String,
        targetValue: Float,
        unit: String,
        currentValue: Float,
        durationWeeks: Int,
        workoutsPerWeek: Int,
        startDate: Long
    ) = viewModelScope.launch {
        val end = computeEndDate(startDate, durationWeeks)
        val goal = Goal(
            id = "",
            goalType = goalType,
            targetValue = targetValue,
            unit = unit,
            currentValue = currentValue,
            durationWeeks = durationWeeks,
            workoutsPerWeek = workoutsPerWeek,
            startDate = startDate,
            endDate = end,
            status = "active"
        )
        // FIX: there is no repository.save(...). Use repo.add(...)
        repo.add(goal)
        _activeGoal.value = goal
    }
}
