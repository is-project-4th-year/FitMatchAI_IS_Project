package com.example.fitmatch.models


import com.example.fitmatch.data.PlanDTO
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    /** Stream user’s saved plans (newest first) */
    fun observePlans(userId: String): Flow<List<PlanDTO>>

    /**
     * Calls Cloud Run /predict with [features],
     * saves the returned plan to Firestore under the user,
     * and returns it.
     */
    suspend fun generateAndSavePlan(userId: String, features: Map<String, Any>): PlanDTO
}



