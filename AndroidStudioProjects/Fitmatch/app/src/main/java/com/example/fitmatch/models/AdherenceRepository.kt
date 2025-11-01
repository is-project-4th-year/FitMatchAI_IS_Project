package com.example.fitmatch.models

import android.os.Build
import androidx.annotation.RequiresApi
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

    private fun adherenceDoc(planId: String) =
        db.collection("users").document(uid)
            .collection("adherence").document(planId)

    suspend fun saveDayLog(planId: String, log: DayLog) {
        daysColl(planId).document(log.date).set(log).await()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun recomputeWeek(planId: String): AdherenceSummary {
        // pull last 7 days
        val snap = daysColl(planId).get().await()
        val logs = snap.documents.mapNotNull { it.toObject(DayLog::class.java) }
            .sortedBy { it.date }
            .takeLast(7)

        val scores = logs.map { scoreDay(it) }
        val adj = adjustFromWeek(scores)

        val now = LocalDate.now()
        val wf = WeekFields.of(Locale.getDefault())
        val start = now.with(wf.dayOfWeek(), 1)
        val end = now.with(wf.dayOfWeek(), 7)

        val summary = AdherenceSummary(
            week_start = start.toString(),
            week_end = end.toString(),
            completion_pct = adj.completion,
            missed_days = adj.missedDays,
            flags = adj.flags,
            volume_scale = adj.volumeScale,
            intensity_scale = adj.intensityScale,
            recommendation = adj.rec,
            updated_at = System.currentTimeMillis()
        )
        adherenceDoc(planId).set(summary).await()
        return summary
    }
}