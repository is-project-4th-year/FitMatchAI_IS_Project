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
    val model_version: String = "",
    val created_at_ms: Long = 0L,
    val week_index : Int = 1,
    val day_offset: Int= 0
)


