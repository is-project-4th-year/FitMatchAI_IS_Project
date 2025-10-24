package com.example.fitmatch.models

import com.example.fitmatch.data.WeeklyProgress
import com.example.fitmatch.data.WorkoutEntry
import kotlinx.coroutines.flow.Flow

interface FitMatchMLRepository {
    /**
     * Fetches computed aggregates + series from your ML pipeline (e.g.,
     * predictions + last-week actuals). Do network/Firestore/TF Lite here.
     */
    suspend fun getWeeklyProgress(scope: String): WeeklyProgress
    fun observeRecentWorkouts(): Flow<List<WorkoutEntry>>
}
