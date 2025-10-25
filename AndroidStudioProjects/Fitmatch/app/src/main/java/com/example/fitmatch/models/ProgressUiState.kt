package com.example.fitmatch.models

import com.example.fitmatch.data.WeeklyProgress

sealed interface ProgressUiState {
    data object Loading : ProgressUiState
    data class Ready(val data: WeeklyProgress) : ProgressUiState
    data class Error(val message: String) : ProgressUiState
}