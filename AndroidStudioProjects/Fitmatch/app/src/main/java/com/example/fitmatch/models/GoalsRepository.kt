package com.example.fitmatch.models

import com.example.fitmatch.data.Goal
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date


class GoalsRepository(private val uid: String) {
    private val col = Firebase.firestore
        .collection("users").document(uid)
        .collection("goals")

    /* ---- helpers ---- */
    private fun Any?.asLongMillis(): Long = when (this) {
        is Number -> this.toLong()
        is Timestamp -> this.toDate().time
        is Date -> this.time
        else -> 0L
    }

    suspend fun getActiveGoalOrNull(): Goal? {
        return try {
            val snap = col
                .whereEqualTo("status", "active")
                .orderBy("startDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val doc = snap.documents.firstOrNull() ?: return null
            runCatching { doc.toGoalSafe() }.getOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toGoalSafe(): Goal {
        val m = data ?: emptyMap<String, Any?>()
        return Goal(
            id = id,
            goalType = (m["goalType"] as? String).orEmpty(),
            targetValue = (m["targetValue"] as? Number)?.toFloat() ?: 0f,
            unit = (m["unit"] as? String).orEmpty(),
            currentValue = (m["currentValue"] as? Number)?.toFloat() ?: 0f,
            durationWeeks = (m["durationWeeks"] as? Number)?.toInt() ?: 0,
            workoutsPerWeek = (m["workoutsPerWeek"] as? Number)?.toInt() ?: 0,
            startDate = m["startDate"].asLongMillis(),
            endDate = m["endDate"].asLongMillis(),
            status = (m["status"] as? String) ?: "active"
        )
    }

    private fun Goal.toMapNormalized(): Map<String, Any?> = mapOf(
        "goalType" to goalType,
        "targetValue" to targetValue,
        "unit" to unit,
        "currentValue" to currentValue,
        "durationWeeks" to durationWeeks,
        "workoutsPerWeek" to workoutsPerWeek,
        "startDate" to (startDate.takeIf { it > 0 } ?: 0L),
        "endDate" to (endDate.takeIf { it > 0 } ?: 0L),
        "status" to status
    )

    /* ---- streaming ---- */
    fun goalsFlow() = callbackFlow<List<Goal>> {
        val reg = col.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents
                ?.mapNotNull { runCatching { it.toGoalSafe() }.getOrNull() }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    /* ---- CRUD ---- */
    suspend fun add(goal: Goal) {
        col.add(goal.toMapNormalized()).await()
    }

    suspend fun update(goal: Goal) {
        col.document(goal.id).set(goal.toMapNormalized()).await()
    }

    suspend fun updateProgress(id: String, newCurrent: Float) {
        col.document(id).update(mapOf("currentValue" to newCurrent)).await()
    }

    suspend fun markCompleted(id: String) {
        col.document(id).update(
            mapOf(
                "status" to "completed",
                "currentValue" to FieldValue.delete()
            )
        ).await()
    }

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }
}
