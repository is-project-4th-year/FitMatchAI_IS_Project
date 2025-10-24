package com.example.fitmatch.models

import com.example.fitmatch.data.Goal
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue

class GoalsRepository(private val uid: String) {
    private val col = Firebase.firestore
        .collection("users").document(uid)
        .collection("goals")

    fun goalsFlow() = callbackFlow<List<Goal>> {
        val reg = col.addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { it.toObject(Goal::class.java)?.copy(id = it.id) } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun add(goal: Goal) {
        col.add(goal).await()
    }

    suspend fun update(goal: Goal) {
        col.document(goal.id).set(goal).await()
    }

    suspend fun updateProgress(id: String, newCurrent: Float) {
        col.document(id).update(mapOf("currentValue" to newCurrent)).await()
    }

    suspend fun markCompleted(id: String) {
        col.document(id).update(mapOf("status" to "completed", "currentValue" to FieldValue.delete()))
            .await()
    }

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }
}