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
        if (logs.isEmpty()) return ProgressUi(loading = false, logs = emptyList())

        // Daily adherence trend (ordered)
        val dailyPct = logs.map { l ->
            val denom = max(1, l.exercises_count)
            (100f * l.exercises_completed.toFloat() / denom.toFloat()).coerceIn(0f, 100f)
        }

        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val dayLabels = logs.map { l -> dayFmt.format(Date(l.date)).uppercase(Locale.getDefault()) }

        // Weekly volume bars (avg volume ratio per ISO week)
        val weekFmt = SimpleDateFormat("YYYY-'W'ww", Locale.US) // "2025-W44"
        val grouped = logs.groupBy { weekFmt.format(Date(it.date)) }
        val weeklyLabels = grouped.keys.sorted()
        val weeklyBars = weeklyLabels.map { wk ->
            val vals = grouped[wk]!!.map { it.volume_ratio.toFloat() }
            vals.average().toFloat()
        }

        // Done / missed sessions for pie
        val done = logs.count { it.exercises_completed >= max(1, it.exercises_count * 7 / 10) }
        val missed = logs.size - done

        // This week % (over last 7 days)
        val last7 = logs.takeLast(7)
        val twPct = if (last7.isNotEmpty()) {
            val c = last7.sumOf { it.exercises_completed }
            val d = last7.sumOf { it.exercises_count }.coerceAtLeast(1)
            (c.toFloat() / d.toFloat()).coerceIn(0f, 1f)
        } else 0f

        // Streak: consecutive days with any log from the end
        val keyFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        var streak = 0
        if (logs.isNotEmpty()) {
            val byKey = logs.associateBy { it.date_key }
            var cursor = logs.last().date
            while (true) {
                val key = keyFmt.format(Date(cursor))
                if (byKey.containsKey(key)) {
                    streak += 1
                    cursor -= 24L * 60L * 60L * 1000L
                } else break
            }
        }

        return ProgressUi(
            loading = false,
            logs = logs,
            dailyAdherencePct = dailyPct,
            dailyLabels = dayLabels,
            weeklyVolumeBars = weeklyBars,
            weeklyLabels = weeklyLabels.map { it.substringAfter('-') }, // "W44"
            doneSessions = done,
            missedSessions = missed,
            thisWeekPct = twPct,
            streakDays = streak
        )
    }
}