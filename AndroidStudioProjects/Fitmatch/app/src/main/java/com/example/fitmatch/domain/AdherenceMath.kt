package com.example.fitmatch.domain


import com.example.fitmatch.data.DayLog
import kotlin.math.max
import kotlin.math.min

data class DayScore(
    val score: Double,           // 0..1
    val volume_ratio: Double,    // done/plan aggregated
    val hi_rpe: Boolean,
    val pain: Boolean
)

// per-day score
fun scoreDay(log: DayLog): DayScore {
    if (log.missed) return DayScore(0.0, 0.0, hi_rpe = false, pain = false)
    if (log.exercises.isEmpty()) return DayScore(0.0, 0.0, false, false)

    var sumScores = 0.0
    var sumVolumes = 0.0
    var hiRpe = false
    var painFlag = false

    log.exercises.forEach { e ->
        val planVol = max(1, e.planned_sets) * max(1, e.planned_reps)
        val doneVol = max(0, e.done_sets) * max(0, e.done_reps)
        val ratio = min(1.0, doneVol.toDouble() / planVol.toDouble())
        // RPE & pain reduce the contribution (conservative)
        val penalty = when {
            e.pain -> 0.5
            e.rpe >= 9.0 -> 0.8
            else -> 1.0
        }
        sumScores += ratio * penalty
        sumVolumes += min(1.0, doneVol.toDouble() / planVol.toDouble())
        if (e.rpe >= 9.0) hiRpe = true
        if (e.pain) painFlag = true
    }

    val n = log.exercises.size.coerceAtLeast(1)
    return DayScore(
        score = (sumScores / n).coerceIn(0.0, 1.0),
        volume_ratio = (sumVolumes / n).coerceIn(0.0, 1.0),
        hi_rpe = hiRpe,
        pain = painFlag
    )
}

// weekly roll-up -> knobs for next plan
data class WeekAdjust(
    val completion: Double,      // 0..1
    val missedDays: Int,
    val volumeScale: Double,     // e.g. 0.9, 1.0, 1.05
    val intensityScale: Double,  // e.g. 0.95, 1.0, 1.05
    val rec: String,
    val flags: Map<String, Boolean>
)

fun adjustFromWeek(dayScores: List<DayScore>): WeekAdjust {
    if (dayScores.isEmpty()) return WeekAdjust(0.0, 7, 1.0, 1.0, "No data.", mapOf("pain" to false, "hi_rpe" to false))

    val completion = dayScores.map { it.score }.average()
    val missedDays = dayScores.count { it.score == 0.0 }
    val anyPain = dayScores.any { it.pain }
    val anyHiRpe = dayScores.any { it.hi_rpe }

    // base knobs from completion
    var vol = when {
        completion >= 0.9 -> 1.05
        completion >= 0.75 -> 1.00
        completion >= 0.6 -> 0.95
        else -> 0.90
    }
    var intensity = when {
        completion >= 0.9 -> 1.05
        completion >= 0.75 -> 1.00
        completion >= 0.6 -> 0.98
        else -> 0.95
    }

    // safety rules
    if (anyPain) {
        vol = min(vol, 0.90)
        intensity = min(intensity, 0.95)
    }
    if (anyHiRpe) {
        intensity = min(intensity, 0.98)
    }
    // cap weekly volume change to ±10%
    vol = vol.coerceIn(0.90, 1.10)
    intensity = intensity.coerceIn(0.90, 1.10)

    val rec = buildString {
        when {
            anyPain -> append("Reduce load and address pain; ")
            anyHiRpe -> append("Watch fatigue (high RPE); ")
        }
        append(
            when {
                completion >= 0.9 -> "Great adherence—small progressive overload."
                completion >= 0.75 -> "Hold steady and build consistency."
                completion >= 0.6 -> "Slight deload to recover adherence."
                else -> "Deload and simplify sessions, re-establish routine."
            }
        )
    }

    return WeekAdjust(
        completion = completion,
        missedDays = missedDays,
        volumeScale = vol,
        intensityScale = intensity,
        rec = rec,
        flags = mapOf("pain" to anyPain, "hi_rpe" to anyHiRpe)
    )
}