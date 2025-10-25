package com.example.fitmatch.models

import com.example.fitmatch.data.PlanDTO
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    suspend fun submitMetrics(
        age: Int,
        height: Double,
        weight: Double,
        bmi: Double,
        goalTypeIndex: Int,      // 0=fatloss, 1=hypertrophy, 2=endurance
        workoutsPerWeek: Int,
        caloriesAvg: Double,
        equipment: String        // "none" | "basic" | "gym"
    ): String

    /** Emits all plans; you can take the latest in the UI. */
    fun observeRecommendations(): Flow<List<PlanDTO>>
}
