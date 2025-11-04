package com.example.fitmatch.models


import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

data class ProgressDayLog(
    val plan_id: String = "",
    val day: Int = 0,
    val date: Long = 0L,                // millis
    val date_key: String = "",          // "yyyyMMdd"
    val exercises_count: Int = 0,
    val exercises_completed: Int = 0,
    val volume_ratio: Double = 1.0,     // actual/prescribed (0..n)
    val intensity_ratio: Double = 1.0   // neutral=1
)

class ProgressRepository(
    private val db: FirebaseFirestore
) {
    suspend fun fetchLogs(uid: String, planId: String, limit: Int = 60): List<ProgressDayLog> {
        val snap = db.collection("users").document(uid)
            .collection("plan_logs").document(planId)
            .collection("days")
            .orderBy("date_key", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await()
        return snap.documents.mapNotNull { it.toObject(ProgressDayLog::class.java) }
            .sortedBy { it.date_key }
    }
}


//class ProgressRepository(
//    private val db: FirebaseFirestore
//) {
//    fun observeDaily(uid: String, planId: String, lastNDays: Int = 30): Flow<List<DayProgress>> = callbackFlow {
//        val ref = db.collection("users").document(uid)
//            .collection("plan_logs").document(planId)
//            .collection("days")
//            .orderBy("date", Query.Direction.DESCENDING)
//            .limit(lastNDays.toLong())
//
//        val reg = ref.addSnapshotListener { snap, err ->
//            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
//            val items = snap?.documents?.mapNotNull { d ->
//                val date = d.getLong("date") ?: return@mapNotNull null
//                val dateKey = d.getString("date_key") ?: ""
//                val completed = (d.getLong("exercises_completed") ?: 0L).toInt()
//                val planned = (d.getLong("exercises_count") ?: 0L).toInt()
//                val volume = (d.getDouble("volume_ratio") ?: 1.0)
//                val intensity = (d.getDouble("intensity_ratio") ?: 1.0)
//                DayProgress(date, dateKey, completed, planned, volume, intensity)
//            }?.sortedBy { it.date } ?: emptyList()
//            trySend(items)
//        }
//        awaitClose { reg.remove() }
//    }
//}