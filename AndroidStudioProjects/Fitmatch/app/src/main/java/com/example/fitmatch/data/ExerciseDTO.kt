package com.example.fitmatch.data

data class ExerciseDTO(
    val day: Int = 0,
    val block: String = "",
    val name: String = "",
    val sets: Int = 0,
    val reps: String = "",
    val tempo: String = "",
    val rest_sec: Int = 0
)

data class PlanDTO(
    val prediction: Double = 0.0,
    val plan_id: String = "",
    val microcycle_days: Int = 0,
    val exercises: List<ExerciseDTO> = emptyList(),
    val notes: String = "",
    val model_version: String = ""
)


