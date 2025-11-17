package com.example.fitmatch.data

data class Goal(
    val id: String = "",
    val goalType: String = "",      // e.g., "Lose weight", "Run 5K"
    val targetValue: Float = 0f,    // 15 (lbs) or 25 (minutes)
    val unit: String = "",          // "lbs", "minutes", etc.
    val currentValue: Float = 0f,   // progress value
    val durationWeeks: Int = 0,
    val workoutsPerWeek: Int = 0,
    val startDate: Long = 0L,       // epoch millis
    val endDate: Long = 0L,
    val status: String = "active"   // "active" | "completed"
) {
    val progress: Int get() = if (targetValue <= 0f) 0
    else ((currentValue / targetValue) * 100f).coerceIn(0f, 100f).toInt()
}