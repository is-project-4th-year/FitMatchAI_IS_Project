package com.example.fitmatch.models

import com.example.fitmatch.data.AdherenceSummary
import com.example.fitmatch.net.FitmatchApi
import com.example.fitmatch.net.PredictBody
import com.example.fitmatch.data.PlanDTO
import com.google.firebase.Timestamp
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
                    PlanDTO(
                        prediction = (d.get("prediction") as? Number)?.toDouble() ?: 0.0,
                        plan_id = d.getString("plan_id").orEmpty(),
                        microcycle_days = (d.get("microcycle_days") as? Number)?.toInt() ?: 0,
                        exercises = exercises,
                        notes = d.getString("notes").orEmpty(),
                        model_version = d.getString("model_version").orEmpty()
                    )
                } catch (_: Exception) { null }
            } ?: emptyList()
            trySend(plans).isSuccess
        }
        awaitClose { reg.remove() }
    }

    override suspend fun generateAndSavePlan(
        userId: String,
        features: Map<String, Any>
    ): PlanDTO {
        // 1) call Cloud Run
        val plan = api.predict(PredictBody(user_id = userId, features = features))

        // 2) persist to Firestore
        val doc = mapOf(
            "prediction" to plan.prediction,
            "plan_id" to plan.plan_id,
            "microcycle_days" to plan.microcycle_days,
            "exercises" to plan.exercises.map {
                mapOf(
                    "day" to it.day, "block" to it.block, "name" to it.name,
                    "sets" to it.sets, "reps" to it.reps, "tempo" to it.tempo, "rest_sec" to it.rest_sec
                )
            },
            "notes" to plan.notes,
            "model_version" to (plan.model_version ?: ""),
            "created_at" to Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("workoutplan")
            .add(doc)
            .await()

        return plan
    }


    override suspend fun fetchLatestFeatures(uid: String): Map<String, Any?> {
        val snap = db.collection("users").document(uid)
            .collection("features")
            .orderBy("submitted_at", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        return snap.documents.firstOrNull()?.data ?: emptyMap()
    }

    override suspend fun computeWeeklyAdherence(
        uid: String, planId: String, weekStartMs: Long, weekEndMs: Long
    ): AdherenceSummary {
        val logs = db.collection("users").document(uid)
            .collection("workout_logs")
            .whereEqualTo("plan_id", planId)
            .whereGreaterThanOrEqualTo("date", weekStartMs) // date = epoch millis (number)
            .whereLessThan("date", weekEndMs)
            .get().await()

        var totalDays = 0
        var doneDays = 0
        var volAccum = 0.0
        var intAccum = 0.0

        for (d in logs.documents) {
            totalDays++
            val ex = (d.getLong("exercises_count") ?: 0L).toInt()
            val exDone = (d.getLong("exercises_completed") ?: 0L).toInt()
            val vol = d.getDouble("volume_ratio") ?: 1.0   // client: actual/prescribed
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
            soreness_flag = false
        )
    }

    override suspend fun saveAdherenceSummary(uid: String, s: AdherenceSummary) {
        db.collection("users").document(uid)
            .collection("adherence_summaries")
            .document("${s.plan_id}_${s.year_week}")
            .set(s).await()
    }

    private fun yearWeekLabel(ms: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        val y = cal.get(java.util.Calendar.YEAR)
        val w = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        return "%04d-W%02d".format(y, w)
    }

}