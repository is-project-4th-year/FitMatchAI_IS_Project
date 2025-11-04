package com.example.fitmatch.models

import com.example.fitmatch.data.AdherenceSummary
import com.example.fitmatch.data.PlanDTO
import com.example.fitmatch.net.FitmatchApi
import com.example.fitmatch.net.PredictBody
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PlanRepositoryImpl(
    private val db: FirebaseFirestore,
    private val api: FitmatchApi
) : PlanRepository {

    /** Live stream of user's plans (newest first). */
    override fun observePlans(userId: String): Flow<List<PlanDTO>> = callbackFlow {
        val ref = db.collection("users").document(userId)
            .collection("workoutplan")
            .orderBy("created_at", Query.Direction.DESCENDING)

        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList()).isSuccess
                return@addSnapshotListener
            }
            val plans = snap?.documents?.mapNotNull { d ->
                try {
                    val exercisesRaw = d.get("exercises") as? List<Map<String, Any?>> ?: emptyList()
                    val exercises = exercisesRaw.map {
                        com.example.fitmatch.data.ExerciseDTO(
                            day = (it["day"] as? Number)?.toInt() ?: 0,
                            block = it["block"]?.toString().orEmpty(),
                            name = it["name"]?.toString().orEmpty(),
                            sets = (it["sets"] as? Number)?.toInt() ?: 0,
                            reps = it["reps"]?.toString().orEmpty(),
                            tempo = it["tempo"]?.toString().orEmpty(),
                            rest_sec = (it["rest_sec"] as? Number)?.toInt() ?: 0
                        )
                    }
                    val tsMs = d.getTimestamp("created_at")?.toDate()?.time ?: 0L
                    val wkIdx = (d.get("week_index") as? Number)?.toInt()
                    val dayOff = (d.get("day_offset") as? Number)?.toInt()
                    PlanDTO(
                        prediction = (d.get("prediction") as? Number)?.toDouble() ?: 0.0,
                        plan_id = d.getString("plan_id").orEmpty(),
                        microcycle_days = (d.get("microcycle_days") as? Number)?.toInt() ?: 0,
                        exercises = exercises,
                        notes = d.getString("notes").orEmpty(),
                        model_version = d.getString("model_version").orEmpty(),
                        created_at_ms = tsMs,
                        week_index = wkIdx,
                        day_offset = dayOff
                    )
                } catch (_: Exception) { null }
            } ?: emptyList()
            trySend(plans).isSuccess
        }
        awaitClose { reg.remove() }
    }

    // --- Internal helpers ----------------------------------------------------

    // Only the base 8 fields should be snapshotted for future regenerations
    private val baseKeys = setOf(
        "age","height","weight","bmi",
        "goal_type","workouts_per_week","calories_avg","equipment"
    )

    private fun baseOnly(m: Map<String, Any>) = m.filterKeys { it in baseKeys }

    private suspend fun saveFeaturesSnapshot(uid: String, features: Map<String, Any>) {
        val payload = HashMap(features)
        payload["submitted_at"] = FieldValue.serverTimestamp()
        payload["status"] = "pending"
        db.collection("users").document(uid)
            .collection("features")
            .add(payload)
            .await()
    }

    // --- API + Firestore -----------------------------------------------------

    override suspend fun generateAndSavePlan(
        userId: String,
        features: Map<String, Any>,
        dayOffset: Int,
        weekIndex: Int
    ): PlanDTO {
        // keep a clean copy of base fields for future regen
        runCatching { saveFeaturesSnapshot(userId, baseOnly(features)) }

        // Call Cloud Run with only primitives/strings in features
        val plan = api.predict(PredictBody(user_id = userId, features = features))

        // Shift the days so the new microcycle continues where the user left off
        val shifted = plan.exercises.map {
            mapOf(
                "day" to (it.day + dayOffset),
                "block" to it.block,
                "name" to it.name,
                "sets" to it.sets,
                "reps" to it.reps,
                "tempo" to it.tempo,
                "rest_sec" to it.rest_sec
            )
        }

        val createdAt = Timestamp.now()

        val doc = mapOf(
            "prediction" to plan.prediction,
            "plan_id" to plan.plan_id,
            "microcycle_days" to plan.microcycle_days,
            "exercises" to shifted,                      // ✅ store shifted days
            "notes" to plan.notes,
            "model_version" to (plan.model_version ?: ""),
            "created_at" to createdAt,
            "week_index" to weekIndex,                   // ✅ persist week index
            "day_offset" to dayOffset                    // ✅ persist day offset
        )

        db.collection("users").document(userId)
            .collection("workoutplan")
            .add(doc)
            .await()

        // return shifted plan immediately for UI
        return PlanDTO(
            prediction = plan.prediction,
            plan_id = plan.plan_id,
            microcycle_days = plan.microcycle_days,
            exercises = plan.exercises.map { it.copy(day = it.day + dayOffset) },
            notes = plan.notes,
            model_version = plan.model_version,
            created_at_ms = createdAt.toDate().time,
            week_index = weekIndex,
            day_offset = dayOffset
        )
    }

    /** Latest features snapshot (base-only) or emptyMap() if none. */
    override suspend fun fetchLatestFeatures(uid: String): Map<String, Any?> {
        val snap = db.collection("users").document(uid)
            .collection("features")
            .orderBy("submitted_at", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        // We only ever saved base fields here; safe to return directly
        return snap.documents.firstOrNull()?.data ?: emptyMap()
    }

    /** Compute adherence from nested day logs: users/{uid}/plan_logs/{planId}/days. */
    override suspend fun computeWeeklyAdherence(
        uid: String,
        planId: String,
        weekStartMs: Long,
        weekEndMs: Long
    ): AdherenceSummary {
        val days = db.collection("users").document(uid)
            .collection("plan_logs").document(planId)
            .collection("days")
            .whereGreaterThanOrEqualTo("date", weekStartMs) // Long (epoch millis)
            .whereLessThan("date", weekEndMs)
            .get()
            .await()

        var totalDays = 0
        var doneDays = 0
        var volAccum = 0.0
        var intAccum = 0.0

        for (d in days.documents) {
            totalDays++
            val ex = (d.getLong("exercises_count") ?: 0L).toInt()
            val exDone = (d.getLong("exercises_completed") ?: 0L).toInt()
            val vol = d.getDouble("volume_ratio") ?: 1.0
            val intr = d.getDouble("intensity_ratio") ?: 1.0

            if (ex > 0 && exDone >= (0.7 * ex)) doneDays++
            volAccum += vol
            intAccum += intr
        }

        val completion = if (totalDays == 0) 0.0 else doneDays.toDouble() / totalDays
        val volScale = (if (totalDays == 0) 1.0 else volAccum / totalDays).coerceIn(0.85, 1.15)
        val intScale = (if (totalDays == 0) 1.0 else intAccum / totalDays).coerceIn(0.90, 1.10)
        val missed = (7 - doneDays).coerceAtLeast(0)

        return AdherenceSummary(
            plan_id = planId,
            year_week = yearWeekLabel(weekStartMs),
            completion_pct = completion,
            volume_scale = volScale,
            intensity_scale = intScale,
            missed_days = missed,
            soreness_flag = false,
            notes = ""
        )
    }

    override suspend fun saveAdherenceSummary(uid: String, summary: AdherenceSummary) {
        db.collection("users").document(uid)
            .collection("adherence_summaries")
            .document("${summary.plan_id}_${summary.year_week}")
            .set(summary)
            .await()
    }

    private fun yearWeekLabel(ms: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        val y = cal.get(java.util.Calendar.YEAR)
        val w = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        return "%04d-W%02d".format(y, w)
    }
}