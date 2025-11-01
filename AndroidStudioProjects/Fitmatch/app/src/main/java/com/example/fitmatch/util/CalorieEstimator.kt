package com.example.fitmatch.util


data class CalorieTarget(
    val tdee: Int,
    val dailyDeficit: Int,
    val targetIntake: Int,
    val notes: String
)

fun estimateCalorieTarget(
    age: Int?,             // not used in neutral BMR but kept for future sex/bodyfat support
    heightCm: Double?,     // not used in neutral BMR but kept for future upgrade
    weightKg: Double?,
    workoutsPerWeek: Int?,
    weeklyLossPct: Double = 0.005 // 0.5% bw/week
): CalorieTarget? {
    val w = weightKg ?: return null
    val wpw = workoutsPerWeek ?: 0

    val activity = when {
        wpw <= 0 -> 1.2
        wpw <= 3 -> 1.375
        wpw <= 5 -> 1.55
        else     -> 1.725
    }

    // Neutral BMR when sex/body-fat unknown
    val bmrNeutral = 22.0 * w
    val tdee = (bmrNeutral * activity).toInt()

    val weeklyLossKg = w * weeklyLossPct
    var dailyDeficit = ((weeklyLossKg * 7700.0) / 7.0).toInt()
    dailyDeficit = dailyDeficit.coerceIn(300, 500) // safety cap

    val target = (tdee - dailyDeficit).coerceAtLeast(1300) // sensible floor
    val note = "TDEE≈$tdee, deficit≈$dailyDeficit, target≈$target kcal/day"

    return CalorieTarget(tdee, dailyDeficit, target, note)
}