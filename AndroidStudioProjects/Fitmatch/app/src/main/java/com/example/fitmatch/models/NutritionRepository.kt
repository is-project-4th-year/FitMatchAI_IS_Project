package com.example.fitmatch.models

import com.example.fitmatch.data.DietPlanDTO
import kotlinx.coroutines.flow.Flow

interface NutritionRepository {
    fun observeLatest(uid: String): Flow<DietPlanDTO?>
    suspend fun generateAndSaveFromFeatures(uid: String, features: Map<String, Any?>): DietPlanDTO
}