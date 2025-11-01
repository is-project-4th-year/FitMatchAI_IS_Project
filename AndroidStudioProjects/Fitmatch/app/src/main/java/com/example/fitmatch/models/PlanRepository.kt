package com.example.fitmatch.models


import com.example.fitmatch.data.AdherenceSummary
import com.example.fitmatch.data.PlanDTO
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    /** Stream user’s saved plans (newest first) */
    fun observePlans(userId: String): Flow<List<PlanDTO>>

    suspend fun fetchLatestFeatures(uid:String): Map<String, Any?>
    suspend fun generateAndSavePlan(userId: String, features: Map<String, Any>): PlanDTO
    suspend fun computeWeeklyAdherence(uid: String, planId: String, weekStartMs: Long, weekEndMs: Long): AdherenceSummary
    suspend fun saveAdherenceSummary(uid: String, summary: AdherenceSummary)

}



