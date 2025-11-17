package com.example.fitmatch.models

import com.example.fitmatch.data.AdherenceSummary
import com.example.fitmatch.data.ExerciseDTO
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
import java.util.Calendar

class PlanRepositoryImpl(
    private val db: FirebaseFirestore,
    private val api: FitmatchApi
) : PlanRepository {

    /** Stream user’s saved plans (newest first). */
    override fun observePlans(userId: String): Flow<List<PlanDTO>> = callbackFlow {
        val ref = db.collection("users").document(userId)
            .collection("workoutplan")
            .orderBy("created_at_ms", Query.Direction.DESCENDING)

        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList()).isSuccess
                return@addSnapshotListener
            }
            val plans = snap?.documents?.mapNotNull { d ->
                try {
                    val exercisesRaw = d.get("exercises") as? List<Map<String, Any?>> ?: emptyList()
                    val createdMs: Long =
                        (d.getLong("created_at_ms") ?: 0L).takeIf { it > 0L }
                            ?: d.getTimestamp("created_at")?.toDate()?.time
                            ?: 0L

                    val exercises = exercisesRaw.map {
                        ExerciseDTO(
                            day       = (it["day"] as? Number)?.toInt() ?: 0,
                            block     = it["block"]?.toString().orEmpty(),
                            name      = it["name"]?.toString().orEmpty(),
                            sets      = (it["sets"] as? Number)?.toInt() ?: 0,
                            reps      = it["reps"]?.toString().orEmpty(),
                            tempo     = it["tempo"]?.toString().orEmpty(),
                            rest_sec  = (it["rest_sec"] as? Number)?.toInt() ?: 0
                        )
                    }

                    PlanDTO(
                        prediction       = (d.get("prediction") as? Number)?.toDouble() ?: 0.0,
                        plan_id          = d.getString("plan_id").orEmpty(),
                        microcycle_days  = (d.get("microcycle_days") as? Number)?.toInt() ?: 0,
                        exercises        = exercises,
                        notes            = d.getString("notes").orEmpty(),
                        model_version    = d.getString("model_version").orEmpty(),
                        created_at_ms    = createdMs,
                        week_index       = (d.get("week_index") as? Number)?.toInt(),
                        day_offset       = (d.get("day_offset") as? Number)?.toInt()
                    )
                } catch (_: Exception) { null }
            } ?: emptyList()

            trySend(plans).isSuccess
        }
        awaitClose { reg.remove() }
    }

    // Only the base 8 features are snapshotted for future regenerations.
    private val baseKeys = setOf(
        "age","height","weight","bmi",
        "goal_type","workouts_per_week","calories_avg","equipment"
    )

    override suspend fun fetchLatestFeatures(uid: String): Map<String, Any?> {
        val snap = db.collection("users").document(uid)
            .collection("features")
            .orderBy("submitted_at", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val data = snap.documents.firstOrNull()?.data ?: emptyMap()
        // Drop submitted_at/status and keep only the 8 base keys
        return data.filterKeys { it in baseKeys }
    }

    private suspend fun saveFeaturesSnapshot(uid: String, features: Map<String, Any>) {
        val payload = HashMap(features)
        payload["submitted_at"] = FieldValue.serverTimestamp()
        payload["status"]       = "pending"
        db.collection("users").document(uid)
            .collection("features")
            .add(payload)
            .await()
    }

     override suspend fun generateAndSavePlan(
        userId: String,
        features: Map<String, Any>,
        dayOffset: Int,
        weekIndex: Int,
        variantSeed:Int?
     ): PlanDTO {
        // Persist a clean features snapshot (base fields only)
        runCatching { saveFeaturesSnapshot(userId, features.filterKeys { it in baseKeys }) }

         val body = PredictBody(
             userId = userId,
             features = features,
             weekIndex = weekIndex,
             variantSeed = variantSeed
         )
         val plan = api.predict(body) // returns PredictResponse


         // Shift days so the next microcycle continues at the correct day number
        val shifted = plan.exercises.map {
            mapOf(
                "day"      to (it.day + dayOffset),
                "block"    to it.block,
                "name"     to it.name,
                "sets"     to it.sets,
                "reps"     to it.reps,
                "tempo"    to it.tempo,
                "rest_sec" to it.rest_sec
            )
        }

         val clientNowMs = System.currentTimeMillis()
         val doc = mapOf(
             "prediction"       to plan.prediction,
             "plan_id"          to plan.plan_id,
             "microcycle_days"  to plan.microcycle_days,
             "exercises"        to shifted,                     // keep shifted
             "notes"            to plan.notes,
             "model_version"    to (plan.model_version ?: ""),
             "created_at"       to FieldValue.serverTimestamp(),// server authoritative
             "created_at_ms"    to clientNowMs,                 // used for ordering in listener
             "week_index"       to weekIndex,
             "day_offset"       to dayOffset
         )

        db.collection("users").document(userId)
            .collection("workoutplan")
            .add(doc)
            .await()

        // Return DTO with shifted days + local ms for immediate UI
        return PlanDTO(
            prediction       = plan.prediction,
            plan_id          = plan.plan_id,
            microcycle_days  = plan.microcycle_days,
            exercises        = plan.exercises.map { it.copy(day = it.day + dayOffset) },
            notes            = plan.notes,
            model_version    = plan.model_version,
            created_at_ms    = clientNowMs,
            week_index       = weekIndex,
            day_offset       = dayOffset
        )
    }

    override suspend fun computeWeeklyAdherence(
        uid: String,
        planId: String,
        weekStartMs: Long,
        weekEndMs: Long
    ): AdherenceSummary {
        val days = db.collection("users").document(uid)
            .collection("plan_logs").document(planId)
            .collection("days")
            .whereGreaterThanOrEqualTo("date", weekStartMs) // epoch millis
            .whereLessThan("date", weekEndMs)
            .get()
            .await()

        var totalDays = 0
        var doneDays  = 0
        var volAccum  = 0.0
        var intAccum  = 0.0

        for (d in days.documents) {
            totalDays++
            val ex    = (d.getLong("exercises_count") ?: 0L).toInt()
            val exDone= (d.getLong("exercises_completed") ?: 0L).toInt()
            val vol   = d.getDouble("volume_ratio") ?: 1.0
            val intr  = d.getDouble("intensity_ratio") ?: 1.0

            if (ex > 0 && exDone >= (0.7 * ex)) doneDays++
            volAccum += vol
            intAccum += intr
        }

        val completion = if (totalDays == 0) 0.0 else doneDays.toDouble() / totalDays
        val volScale   = (if (totalDays == 0) 1.0 else volAccum / totalDays).coerceIn(0.85, 1.15)
        val intScale   = (if (totalDays == 0) 1.0 else intAccum / totalDays).coerceIn(0.90, 1.10)
        val missed     = (7 - doneDays).coerceAtLeast(0)

        return AdherenceSummary(
            plan_id         = planId,
            year_week       = yearWeekLabel(weekStartMs),
            completion_pct  = completion,
            volume_scale    = volScale,
            intensity_scale = intScale,
            missed_days     = missed,
            soreness_flag   = false,
            notes           = ""
        )
    }
    private fun isTooSimilar(a: PlanDTO?, b: PlanDTO?): Boolean {
        if (a == null || b == null) return false
        val an = a.exercises.map { it.name }.toSet()
        val bn = b.exercises.map { it.name }.toSet()
        if (an.isEmpty() || bn.isEmpty()) return false
        val overlap = an.intersect(bn).size.toDouble() / an.union(bn).size.toDouble()
        return overlap >= 0.75  // ▶ same 75%+ of names -> treat as “too similar”
    }
    override suspend fun generateWithAntiRepeat(
        userId: String,
        cleanFeatures: Map<String, Any>,
        dayOffset: Int,
        weekIndex: Int,
        lastPlan: PlanDTO?
    ): PlanDTO {
        val first = generateAndSavePlan(userId, cleanFeatures, dayOffset, weekIndex, variantSeed = 0)
        if (!isTooSimilar(lastPlan, first)) return first
        // one forced retry with a different seed
        return generateAndSavePlan(userId, cleanFeatures, dayOffset, weekIndex, variantSeed = 1)
    }

    override suspend fun saveAdherenceSummary(uid: String, summary: AdherenceSummary) {
        db.collection("users").document(uid)
            .collection("adherence_summaries")
            .document("${summary.plan_id}_${summary.year_week}")
            .set(summary)
            .await()
    }

    private fun yearWeekLabel(ms: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        val y = cal.get(Calendar.YEAR)
        val w = cal.get(Calendar.WEEK_OF_YEAR)
        return "%04d-W%02d".format(y, w)
    }
    }