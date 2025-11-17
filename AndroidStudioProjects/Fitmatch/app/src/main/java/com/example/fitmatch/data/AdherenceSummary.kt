package com.example.fitmatch.data

data class AdherenceSummary(
    val plan_id: String,
    val year_week: String,        // "2025-W44"
    val completion_pct: Double,   // 0.0..1.0
    val volume_scale: Double,     // ~0.85..1.15
    val intensity_scale: Double,  // ~0.90..1.10
    val missed_days: Int,
    val soreness_flag: Boolean,
    val notes: String = ""
)
