package com.example.fitmatch.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fitmatch.data.ProgressUi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.map
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.models.ProgressDayLog
import com.example.fitmatch.models.ProgressRepository
import kotlin.math.max

class ProgressViewModel(
    private val repo: ProgressRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _ui = MutableStateFlow(ProgressUi())
    val ui = _ui.asStateFlow()

    fun load(planId: String) {
        val uid = auth.currentUser?.uid ?: run {
            _ui.value = _ui.value.copy(error = "Not signed in")
            return
        }
        _ui.value = _ui.value.copy(loading = true, error = null)

        viewModelScope.launch {
            runCatching { repo.fetchLogs(uid, planId, 90) }
                .onSuccess { logs ->
                    _ui.value = derive(logs)
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Failed")
                }
        }
    }

    private fun derive(logs: List<ProgressDayLog>): ProgressUi {
        if (logs.isEmpty()) {
            return ProgressUi(
                loading = false,
                logs = emptyList()
            )
        }

        // --- Sort once (by day) ---
        val sorted = logs.sortedBy { it.date }

        // --- Date formatters ---
        val dayFmt   = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        val weekFmt  = java.text.SimpleDateFormat("YYYY-'W'ww", java.util.Locale.US)
        val keyFmt   = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())

        // --- Daily series: adherence %, labels, volume & intensity ---
        val dailyAdherencePct = sorted.map { log ->
            val total = log.exercises_count.coerceAtLeast(1)
            (log.exercises_completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
        val dailyLabels = sorted.map { dayFmt.format(java.util.Date(it.date)) }

        val dailyVolumeRatio    = sorted.map { it.volume_ratio.toFloat() }
        val dailyIntensityRatio = sorted.map { it.intensity_ratio.toFloat() }

        // --- Weekly aggregates: volume bars + completion % ---
        val groupedByWeek = sorted.groupBy { weekFmt.format(java.util.Date(it.date)) }
        val weekKeysSorted = groupedByWeek.keys.sorted()

        val weeklyVolumeBars = weekKeysSorted.map { weekKey ->
            val weekLogs = groupedByWeek[weekKey]!!
            val avgVol = weekLogs.map { it.volume_ratio }.average()
            avgVol.toFloat()
        }

        val weeklyCompletionPct = weekKeysSorted.map { weekKey ->
            val weekLogs = groupedByWeek[weekKey]!!
            val done  = weekLogs.sumOf { it.exercises_completed }
            val total = weekLogs.sumOf { it.exercises_count }.coerceAtLeast(1)
            (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }

        val weeklyLabels = weekKeysSorted.map { it.substringAfter('-') } // "W##"

        // --- Session tiles: done vs missed (rule: >=70% completed counts as "done") ---
        var doneSessions = 0
        var missedSessions = 0
        sorted.forEach { log ->
            val total = log.exercises_count
            val completed = log.exercises_completed
            if (total > 0 && completed >= (0.7f * total)) doneSessions++ else missedSessions++
        }

        // --- This week % (use device "current week") ---
        val now = System.currentTimeMillis()
        val currentWeekKey = weekFmt.format(java.util.Date(now))
        val thisWeekLogs = groupedByWeek[currentWeekKey].orEmpty()
        val thisWeekPct = if (thisWeekLogs.isEmpty()) 0f else {
            val c = thisWeekLogs.sumOf { it.exercises_completed }
            val t = thisWeekLogs.sumOf { it.exercises_count }.coerceAtLeast(1)
            (c.toFloat() / t.toFloat()).coerceIn(0f, 1f)
        }

        // --- Streak: longest consecutive-day run where a log exists ---
        val byKey = sorted.associateBy { it.date_key } // assumes date_key == "yyyyMMdd"
        var bestStreak = 0
        var currentStreak = 0
        val firstDay = sorted.first().date
        val lastDay  = sorted.last().date
        var cursor = firstDay
        while (cursor <= lastDay) {
            val key = keyFmt.format(java.util.Date(cursor))
            if (byKey.containsKey(key)) {
                currentStreak += 1
                if (currentStreak > bestStreak) bestStreak = currentStreak
            } else {
                currentStreak = 0
            }
            cursor += 24L * 60L * 60L * 1000L
        }

        // --- Lifetime counters ---
        val cumulativeSessionsDone  = sorted.sumOf { it.exercises_completed }
        val cumulativeSessionsTotal = sorted.sumOf { it.exercises_count }.coerceAtLeast(1)

        // --- Return full UI model (preserve your existing fields + new ones) ---
        return ProgressUi(
            loading = false,
            logs = sorted,

            // existing
            dailyAdherencePct = dailyAdherencePct,
            dailyLabels = dailyLabels,
            weeklyVolumeBars = weeklyVolumeBars,
            weeklyLabels = weeklyLabels,
            doneSessions = doneSessions,
            missedSessions = missedSessions,
            thisWeekPct = thisWeekPct,
            streakDays = bestStreak,

            // new
            dailyVolumeRatio = dailyVolumeRatio,
            dailyIntensityRatio = dailyIntensityRatio,
            weeklyCompletionPct = weeklyCompletionPct,
            weeklyCompletionLabels = weeklyLabels,
            cumulativeSessionsDone = cumulativeSessionsDone,
            cumulativeSessionsTotal = cumulativeSessionsTotal,
            bestStreakDays = bestStreak
        )
    }

}