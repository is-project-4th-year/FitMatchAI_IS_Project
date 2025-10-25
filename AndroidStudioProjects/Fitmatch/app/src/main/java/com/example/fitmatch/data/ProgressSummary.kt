package com.example.fitmatch.data

import java.time.LocalDate


data class ProgressSummary(
    val totalWorkouts: Int,
    val workoutsChangePct: Int, // +5 for +5%
    val caloriesBurned: Int,
    val caloriesSpanDays: Int,
    val activeTimeMinutes: Int,
    val weightLbs: Double,
    val weightChangeLbs: Double
)

data class WorkoutEntry(
    val id: String,
    val date: LocalDate,
    val title: String,
    val tag: String,        // strength | cardio | hiit | flexibility
    val durationMin: Int,
    val calories: Int
)

data class WeeklySeries(
    val labels: List<String>, // Mon .. Sun
    val durations: List<Int>,
    val calories: List<Int>
)

data class WeeklyProgress(
    val summary: ProgressSummary,
    val series: WeeklySeries,
    val recent: List<WorkoutEntry>
)