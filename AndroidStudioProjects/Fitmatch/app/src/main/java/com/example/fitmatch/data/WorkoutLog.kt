package com.example.fitmatch.data

data class ExerciseLog(
    val name: String = "",
    val planned_sets: Int = 0,
    val planned_reps: Int = 0,
    val done_sets: Int = 0,
    val done_reps: Int = 0,
    val rpe: Double = 0.0,
    val pain: Boolean = false
)

data class DayLog(
    val date: String = "",
    val missed: Boolean = false,
    val exercises: List<ExerciseLog> = emptyList(),
    val notes: String = "",
    val created_at: Long = System.currentTimeMillis()
)

