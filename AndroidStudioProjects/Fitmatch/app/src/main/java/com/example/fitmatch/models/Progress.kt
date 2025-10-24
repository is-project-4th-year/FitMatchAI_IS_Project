package com.example.fitmatch.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.ProgressScope
import com.example.fitmatch.models.ProgressUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val repo: FitMatchMLRepository = DemoRepo() // inject your real impl
) : ViewModel()
{
    private val _ui = MutableStateFlow<ProgressUiState>(ProgressUiState.Loading)
    val ui = _ui.asStateFlow()

    var scope by mutableStateOf(ProgressScope.WEEK)
        private set

    init { refresh() }

    fun toggleScope(newScope: ProgressScope) {
        if (newScope != scope) {
            scope = newScope
            refresh()
        }
    }

    fun refresh() = viewModelScope.launch {
        _ui.value = ProgressUiState.Loading
        runCatching { repo.getWeeklyProgress(scope.name.lowercase()) }
            .onSuccess { _ui.value = ProgressUiState.Ready(it) }
            .onFailure { _ui.value = ProgressUiState.Error(it.message ?: "Unknown error") }
    }
}