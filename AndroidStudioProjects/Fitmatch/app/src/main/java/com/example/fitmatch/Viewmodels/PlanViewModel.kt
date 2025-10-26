package com.example.fitmatch.Viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.ExerciseDTO
import com.example.fitmatch.data.PlanDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PlanUiState(
    val loading: Boolean = false,
    val lastRequestId: String? = null,
    val latestPlan: PlanDTO? = null,
    val error: String? = null
)

class PlanViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
): ViewModel() {

    private val _ui = MutableStateFlow(PlanUiState())
    val ui: StateFlow<PlanUiState> = _ui

    init {
        observeRecommendations()
        

    }

    fun submitMetrics(
        age: Int, height: Double, weight: Double, bmi: Double,
        goalType: Int, workoutsPerWeek: Int, caloriesAvg: Double,
        equipment: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, error = null, latestPlan = null)
                val ref = db.collection("users").document(uid)
                    .collection("features").document()
                val payload = mapOf(
                        "age" to age,
                        "height" to height,
                        "weight" to weight,
                        "bmi" to bmi,
                        "goal_type" to goalType,
                        "workouts_per_week" to workoutsPerWeek,
                        "calories_avg" to caloriesAvg,
                        "equipment" to equipment,
                        "status" to "pending",
                        "submitted_at" to com.google.firebase.Timestamp.now()
                )
                ref.set(payload).addOnSuccessListener {
                    _ui.value = _ui.value.copy(lastRequestId = ref.id)
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(25_000)
                        val stall = ui.value.loading && ui.value.latestPlan == null
                        if (stall) _ui.value = _ui.value.copy(
                            loading = false,
                            error = "Timed out waiting for workout plan."
                        )
                    }
                }.addOnFailureListener { e ->
                    _ui.value = _ui.value.copy(loading = false, error = e.message)
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
        }
    }

    private fun observeRecommendations() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("workoutplan")
            .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    _ui.value = _ui.value.copy(loading = false, error = err.message)
                    return@addSnapshotListener
                }
                val d = snap?.documents?.firstOrNull()
                if (d == null) {
                    // No plan yet
                    _ui.value = _ui.value.copy(loading = false)
                    return@addSnapshotListener
                }
                val ex = (d.get("exercises") as? List<Map<String, Any?>>)?.map {
                    ExerciseDTO(
                        day = (it["day"] as? Number)?.toInt() ?: 0,
                        block = it["block"]?.toString() ?: "",
                        name = it["name"]?.toString() ?: "",
                        sets = (it["sets"] as? Number)?.toInt() ?: 0,
                        reps = it["reps"]?.toString() ?: "",
                        tempo = it["tempo"]?.toString() ?: "",
                        rest_sec = (it["rest_sec"] as? Number)?.toInt() ?: 0
                    )
                } ?: emptyList()
                val plan = PlanDTO(
                    prediction = d.getDouble("prediction") ?: 0.0,
                    plan_id = d.getString("plan_id") ?: "",
                    microcycle_days = (d.getLong("microcycle_days") ?: 0L).toInt(),
                    exercises = ex,
                    notes = d.getString("notes") ?: "",
                    model_version = d.getString("model_version") ?: ""
                )
                _ui.value = _ui.value.copy(loading = false, latestPlan = plan)
            }
    }


    companion object {
        fun factory(
            auth: FirebaseAuth,
            db: FirebaseFirestore
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlanViewModel(auth, db) as T
            }
        }
    }
}
