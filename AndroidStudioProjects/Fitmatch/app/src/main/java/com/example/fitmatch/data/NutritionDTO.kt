package com.example.fitmatch.data

data class MacroSplitDTO(
    val calories_target: Int = 0,
    val protein_g: Int = 0,
    val fat_g: Int = 0,
    val carbs_g: Int = 0,
    val fiber_g: Int = 0
)

data class SupplementRecDTO(
    val name: String = "",
    val dose: String = "",
    val notes: String = ""
)

data class DietPlanDTO(
    val goal: String = "",
    val safety_flags: List<String> = emptyList(),
    val macros: MacroSplitDTO = MacroSplitDTO(),
    val supplements: List<SupplementRecDTO> = emptyList(),
    val hydration_liters: Double = 0.0,
    val created_at_ms: Long = 0L
)




















