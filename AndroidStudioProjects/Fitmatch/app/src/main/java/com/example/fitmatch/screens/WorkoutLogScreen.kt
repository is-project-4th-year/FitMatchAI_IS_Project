package com.example.fitmatch.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitmatch.data.ExerciseDTO
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.viewmodel.PlanViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import kotlin.collections.get
import kotlin.compareTo
import kotlin.text.get
import kotlin.text.toDouble
import kotlin.times


private fun String?.i() = this?.toIntOrNull() ?: 0
private fun Int.i(): Int = this

@Composable
fun WorkoutLogScreen(
    navigationManager: NavigationManager,
    planId :String,
    day: Int,
    dateMillis: Long,                        // pass today's date (or chosen date) in ms
    planVm: PlanViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ui by planVm.ui.collectAsState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp



    val exercises = remember(ui.latest, day) {
        ui.latest?.exercises?.filter { it.day == day } ?: emptyList()
    }
    val planId = ui.latest?.plan_id.orEmpty()

    // Local state per exercise (setsDone, repsDone, checked)
    data class ExLog(var sets: String = "", var reps: String = "", var done: Boolean = false)
    val logs = remember(exercises) { exercises.associate { it to mutableStateOf(ExLog()) } }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(Modifier.fillMaxSize()) {
            // Header (gradient, back, title)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp + topInset)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF19D27A), Color(0xFF4B84F6)))
                    )
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                ) {
                    IconButton(
                        onClick = {
                            runCatching { navigationManager.goBack() }
                                .onFailure { navigationManager.navigateToHomeScreen() }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Log workout • Day $day",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (planId.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active plan found. Generate a plan first.")
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    // Date label
                    val fmt = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
                    Text(fmt.format(Date(dateMillis)), style = MaterialTheme.typography.labelLarge)
                }

                // Exercise rows
                items(exercises.size) { idx ->
                    val ex = exercises[idx]
                    val st = logs[ex]!!.value

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE9ECF3), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                ex.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = st.sets,
                                    onValueChange = {
                                        logs[ex]!!.value = st.copy(sets = it.filter(Char::isDigit))
                                    },
                                    label = { Text("Sets done") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = st.reps,
                                    onValueChange = {
                                        logs[ex]!!.value = st.copy(reps = it.filter(Char::isDigit))
                                    },
                                    label = { Text("Reps done") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = st.done,
                                    onCheckedChange = { logs[ex]!!.value = st.copy(done = it) }
                                )
                                Text("Completed (meets intent)")
                            }
                            // Prescribed summary row
                            Text(
                                "Prescribed: ${ex.sets} × ${ex.reps}" +
                                        (if (!ex.tempo.isNullOrBlank() && ex.tempo != "-") " • tempo ${ex.tempo}" else "") +
                                        (if (ex.rest_sec > 0) " • rest ${ex.rest_sec}s" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF707A89)
                            )
                        }
                    }
                }

                // Save button
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF19D27A),
                            contentColor = Color.White
                        ),
                        onClick = {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button

                            // 1) Prescribed volume from the plan
                            val prescribedTotal: Int = exercises.sumOf { ex ->
                                val sets = ex.sets                           // Int
                                val reps =
                                    ex.reps.i()                      // Int (String-safe via helper)
                                sets * reps
                            }

                            // 2) Actual volume from user-entered logs
                            val actualTotal: Int = exercises.sumOf { ex ->
                                val entry =
                                    logs[ex]?.value                 // your MutableState<...> or similar
                                val s = entry?.sets?.toIntOrNull() ?: 0
                                val r = entry?.reps?.toIntOrNull() ?: 0
                                s * r
                            }

                            // 3) Count exercises considered "completed"
                            val exCompleted: Int = exercises.count { ex ->
                                val entry = logs[ex]?.value
                                when {
                                    entry?.done == true -> true
                                    else -> {
                                        val s = entry?.sets?.toIntOrNull() ?: 0
                                        val r = entry?.reps?.toIntOrNull() ?: 0
                                        val denom = ex.sets * ex.reps.i()
                                        val pct =
                                            if (denom > 0) (s * r).toDouble() / denom.toDouble() else 0.0
                                        pct >= 0.7
                                    }
                                }
                            }

                            val exCount = exercises.size
                            val volumeRatio = if (prescribedTotal > 0) {
                                actualTotal.toDouble() / prescribedTotal.toDouble()
                            } else 1.0

                            val intensityRatio = 1.0 // keep neutral if you don't track RPE/RIR

                            val dateKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                                .format(Date(dateMillis))

                            val payload = mapOf(
                                "plan_id" to planId,
                                "day" to day,
                                "date" to dateMillis,                 // Number (ms since epoch)
                                "date_key" to dateKey,                // "YYYYMMDD" for easy queries
                                "exercises_count" to exCount,
                                "exercises_completed" to exCompleted,
                                "volume_ratio" to volumeRatio,        // Double
                                "intensity_ratio" to intensityRatio   // Double
                            )

                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            db.collection("users").document(uid)
                                .collection("plan_logs").document(planId)
                                .collection("days").document(dateKey)
                                .set(payload)
                                .addOnSuccessListener {
                                    Log.d("WorkoutLog", "✅ Saved workout for $dateKey: $payload")
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Workout logged successfully!",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("WorkoutLog", "❌ Failed to save workout: ${e.localizedMessage}", e)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to save: ${e.localizedMessage}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }

                        }
                    ) {
                        Text("Save workout log")
                    }
                }
            }
        }
    }
}