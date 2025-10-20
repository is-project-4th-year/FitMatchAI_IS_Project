package com.example.fitmatch.Viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.GoalsRepository
import com.example.fitmatch.models.Goal

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GoalsViewModel(private val repo: GoalsRepository) : ViewModel() {
    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    val activeGoals = goals.map { it.filter { g -> g.status == "active" } }.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    val pastGoals = goals.map { it.filter { g -> g.status == "completed" } }.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    init {
        viewModelScope.launch {
            repo.goalsFlow().collect { _goals.value = it }
        }
    }

    fun add(goal: Goal) = viewModelScope.launch { repo.add(goal) }
    fun update(goal: Goal) = viewModelScope.launch { repo.update(goal) }
    fun updateProgress(id: String, newCurrent: Float) = viewModelScope.launch { repo.updateProgress(id, newCurrent) }
    fun markCompleted(id: String) = viewModelScope.launch { repo.markCompleted(id) }
    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }

    companion object {
        fun factory(auth: FirebaseAuth): GoalsViewModel {
            val uid = auth.currentUser?.uid ?: error("User not logged in")
            return GoalsViewModel(GoalsRepository(uid))
        }
    }
}