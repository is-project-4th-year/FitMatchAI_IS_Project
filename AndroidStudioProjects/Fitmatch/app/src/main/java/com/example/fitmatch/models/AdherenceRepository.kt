package com.example.fitmatch.models

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.fitmatch.data.AdherenceSummary
import com.example.fitmatch.data.DayLog
import com.example.fitmatch.domain.adjustFromWeek
import com.example.fitmatch.domain.scoreDay
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

class AdherenceRepository(private val uid: String) {
    private val db = Firebase.firestore

    private fun daysColl(planId: String) =
        db.collection("users").document(uid)
            .collection("plan_logs").document(planId)
            .collection("days")

    private fun adherenceDoc(planId: String, yearWeek: String) =
        db.collection("users").document(uid)
            .collection("adherence").document("${planId}_$yearWeek")

    suspend fun saveDayLog(planId: String, log: DayLog) {
        // log.date should be "YYYY-MM-DD"
        daysColl(planId).document(log.date).set(log).await()
    }
    private fun normalizeFlags(flags: Any?): List<String> = when (flags) {
        is Collection<*> -> flags.map { it?.toString().orEmpty() }
        is Map<*, *>     -> flags.values.map { it?.toString().orEmpty() }
        is String        -> listOf(flags)
        null             -> emptyList()
        else             -> listOf(flags.toString())
    }

    private fun hasSoreness(flags: List<String>): Boolean =
        flags.any { s: String -> s.contains("sore", ignoreCase = true) }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recomputeWeek(planId: String): AdherenceSummary {
        // last 7 day logs (ordered)
        val logs = daysColl(planId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(DayLog::class.java) }
            .sortedBy { it.date }
            .takeLast(7)

        val scores = logs.map { scoreDay(it) }
        val adj = adjustFromWeek(scores)

        // ISO week string "YYYY-Www"
        val now = LocalDate.now()
        val wf = WeekFields.ISO
        val year = now.get(wf.weekBasedYear())
        val week = now.get(wf.weekOfWeekBasedYear())
        val yearWeek = "%04d-W%02d".format(now.get(wf.weekBasedYear()),
            now.get(wf.weekOfWeekBasedYear()))

        val flagsList: List<String> = normalizeFlags(adj.flags)
        val sorenessFlag: Boolean = hasSoreness(flagsList)
        val notes: String = flagsList.joinToString(", ")


        val summary = AdherenceSummary(
            plan_id = planId,
            year_week = yearWeek,
            completion_pct = adj.completion,
            volume_scale = adj.volumeScale,
            intensity_scale = adj.intensityScale,
            missed_days = adj.missedDays,
            soreness_flag = sorenessFlag,
            notes = notes
        )

        adherenceDoc(planId, yearWeek).set(summary).await()
        return summary
    }
}