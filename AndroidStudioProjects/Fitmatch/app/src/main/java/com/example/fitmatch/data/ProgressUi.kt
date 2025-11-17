package com.example.fitmatch.data

import com.example.fitmatch.models.ProgressDayLog


data class ProgressUi(
    val loading: Boolean = false,
    val error: String? = null,
    val logs: List<ProgressDayLog> = emptyList(),
    // derived
    val dailyAdherencePct: List<Float> = emptyList(), // 0..100
    val dailyLabels: List<String> = emptyList(),      // e.g. "Mon", "Tue"
    val weeklyVolumeBars: List<Float> = emptyList(),  // avg volume ratio per week
    val weeklyLabels: List<String> = emptyList(),     // e.g. "W44", "W45"
    val doneSessions: Int = 0,
    val missedSessions: Int = 0,
    val thisWeekPct: Float = 0f,                     // 0..1
    val streakDays: Int = 0,
    val dailyVolumeRatio: List<Float> = emptyList(),
    val dailyIntensityRatio: List<Float> = emptyList(),
    // NEW — weekly aggregates
    val weeklyCompletionPct: List<Float> = emptyList(),  // 0..1
    val weeklyCompletionLabels: List<String> = emptyList(), // W##
    // NEW — cumulated context
    val cumulativeSessionsDone: Int = 0,
    val cumulativeSessionsTotal: Int = 0,
    val bestStreakDays: Int = 0
)