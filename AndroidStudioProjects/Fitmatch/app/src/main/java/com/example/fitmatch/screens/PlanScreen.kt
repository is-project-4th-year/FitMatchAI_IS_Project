package com.example.fitmatch.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitmatch.Viewmodels.GoalsViewModel
import com.example.fitmatch.data.ExerciseDTO
import com.example.fitmatch.models.GoalsRepository
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.viewmodel.PlanViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.input.KeyboardType
import com.example.fitmatch.data.Goal
import com.example.fitmatch.util.estimateCalorieTarget

@Composable
fun PlanScreen(
    navigationManager: NavigationManager,
    planVm: PlanViewModel = viewModel(),
    goalsVm: GoalsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Not logged in")
                return GoalsViewModel(GoalsRepository(uid)) as T
            }
        }
    )
) {
    val ui by planVm.ui.collectAsState()
    val activeGoals: List<Goal>
            by goalsVm.activeGoals.collectAsState(initial = emptyList())
    LaunchedEffect(Unit) { planVm.startObservingHistory() }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp

    // --- top header (gradient bar + back + title) ---
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp + topInset)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF19D27A), Color(0xFF4B84F6))
                    )
                )
                .padding(start = 12.dp, end = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
//                    .padding(top  = topInset)

            ) {
                IconButton(
                    onClick = {
                        try {
                            navigationManager.goBack()
                        } catch (_: Throwable) {
                            navigationManager.navigateToHomeScreen()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Plan Workout",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // --- segmented toggle like the screenshots ---
        var tab by rememberSaveable { mutableStateOf(0) } // 0=Quick Select, 1=Custom Entry
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF3F5F9))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegPill(
                label = "Quick Select",
                selected = tab == 0,
                onClick = { tab = 0 },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            SegPill(
                label = "Custom Entry",
                selected = tab == 1,
                onClick = { tab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        when (tab) {
            0 -> QuickSelectSection(ui = ui)
            1 -> CustomEntrySectionStyled(planVm = planVm,
                uiLoading = ui.loading,
                uiError = ui.error,
                activeGoals = activeGoals)
        }
    }
}

/* ------------ Quick Select (cards feed) ------------ */

@Composable
private fun QuickSelectSection(ui: com.example.fitmatch.viewmodel.PlanUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        if (ui.error != null) {
            item {
                Text(ui.error, color = MaterialTheme.colorScheme.error)
            }
        }

        val latest = ui.latest
        if (latest == null) {
            item {
                Text(
                    "No plan yet. Switch to Custom Entry to create one.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val byDay = latest.exercises.groupBy { it.day }.toSortedMap()
            byDay.forEach { (day, list) ->
                item(key = "day_$day") {
                    WorkoutDayCard(day = day, items = list)
                }
            }
            item {
                if (latest.notes.isNotBlank()) {
                    Text(
                        latest.notes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutDayCard(day: Int, items: List<ExerciseDTO>) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Day $day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            items.forEach { ex ->
                ExerciseRowCard(ex)
            }
        }
    }
}

@Composable
private fun ExerciseRowCard(ex: ExerciseDTO) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE9ECF3), RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Title + tag chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                if (ex.block.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFF5FF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(ex.block.lowercase(), color = Color(0xFF5A7BEF), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            // Meta rows styled similar to "Duration / Calories" layout
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Sets × Reps", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AA3B2))
                    Text("${ex.sets} × ${ex.reps}", style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Rest", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AA3B2))
                    Text("${ex.rest_sec}s", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Tempo: ${ex.tempo.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF707A89))
            }
        }
    }
}

/* ------------ Custom Entry (single card form, same fields/backend) ------------ */

@Composable
fun CustomEntrySectionStyled(planVm: PlanViewModel, uiLoading: Boolean,
                                     uiError: String?, activeGoals:List<Goal>
) {
    var age by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var bmi by rememberSaveable { mutableStateOf("") }
    var goalIndex by rememberSaveable { mutableStateOf(1) }   // 0/1/2
    var workouts by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var equipment by rememberSaveable { mutableStateOf("gym") }

    fun computeBmi() {
        val h = height.toDoubleOrNull()
        val w = weight.toDoubleOrNull()
        if (h != null && h > 0 && w != null && w > 0) {
            val m = h / 100.0
            bmi = String.format("%.1f", w / (m * m))
        }
    }
    fun estimateAndFillCalories() {
        val a = age.toIntOrNull()
        val h = height.toDoubleOrNull()
        val w = weight.toDoubleOrNull()
        val wpw = workouts.toIntOrNull()
        val tgt = estimateCalorieTarget(a, h, w, wpw)
        if (tgt != null) {
            calories = tgt.targetIntake.toString()
            // If you want a toast/snackbar later, you can show tgt.notes
        }
    }
    // (A) Prefill from latest active Goal (goal type + workouts/week)
    LaunchedEffect(activeGoals) {
        val g = activeGoals.firstOrNull()
        if (g != null) {
            goalIndex = when (g.goalType.trim().lowercase()) {
                "fatloss", "fat loss", "cut" -> 0
                "endurance" -> 2
                else -> 1
            }
            if (workouts.isBlank() && g.workoutsPerWeek > 0) {
                workouts = g.workoutsPerWeek.toString()
            }
            // Optional display-only label (if you want to show it in UI later)
//            val label = "${g.durationWeeks} weeks • ${dateLabel(g.startDate)} - ${dateLabel(g.endDate)}"
        }
    }

// (B) Prefill from last submitted Features (age/height/weight/bmi/calories/equipment)
    LaunchedEffect(Unit) {
        planVm.fetchLatestFeatures(
            onResult = { f ->
                age      = (f["age"] as? Number)?.toInt()?.toString() ?: age
                height   = (f["height"] as? Number)?.toDouble()?.toString() ?: height
                weight   = (f["weight"] as? Number)?.toDouble()?.toString() ?: weight
                bmi      = (f["bmi"] as? Number)?.toDouble()?.let { String.format("%.1f", it) } ?: bmi
                calories = (f["calories_avg"] as? Number)?.toDouble()?.toString() ?: calories
                (f["equipment"] as? String)?.let { equipment = it }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add Custom Workout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    // Row 1
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = age, onValueChange = { age = it.filter(Char::isDigit) },
                            label = { Text("Age") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = workouts, onValueChange = { workouts = it.filter(Char::isDigit) },
                            label = { Text("Workouts/Week") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Row 2
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = height, onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Height (cm)") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Row 3
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = bmi, onValueChange = { bmi = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("BMI") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = calories, onValueChange = { calories = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Avg Calories") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Calculate BMI button (light, full width)
                    OutlinedButton(onClick = {
                        computeBmi()
                        estimateAndFillCalories() },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Calculate BMI and Calories")
                    }

                    // Goal chips row (pill look)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChipPill("Fat loss", selected = goalIndex == 0) { goalIndex = 0 }
                        ChipPill("Hypertrophy", selected = goalIndex == 1) { goalIndex = 1 }
                        ChipPill("Endurance", selected = goalIndex == 2) { goalIndex = 2 }
                    }

                    // Equipment chips row
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChipPill("None", selected = equipment.equals("none", true)) { equipment = "none" }
                        ChipPill("Basic", selected = equipment.equals("basic", true)) { equipment = "basic" }
                        ChipPill("Gym", selected = equipment.equals("gym", true)) { equipment = "gym" }
                    }

                    if (uiError != null) {
                        Text(uiError, color = MaterialTheme.colorScheme.error)
                    }

                    // Big primary button like the screenshot
                    Button(
                        enabled = !uiLoading,
                        onClick = {
                            val a = age.toIntOrNull() ?: 0
                            val h = height.toDoubleOrNull() ?: 0.0
                            val w = weight.toDoubleOrNull() ?: 0.0
                            val b = bmi.toDoubleOrNull() ?: 0.0
                            val wpw = workouts.toIntOrNull() ?: 3
                            val cal = calories.toDoubleOrNull() ?: 0.0

                            planVm.generateFromMetrics(
                                age = a,
                                heightCm = h,
                                weightKg = w,
                                bmi = b,
                                goalTypeIndex = goalIndex,
                                workoutsPerWeek = wpw,
                                caloriesAvg = cal,
                                equipment = equipment
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF19D27A),
                            contentColor = Color.White
                        )
                    ) {
                        if (uiLoading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                        else Text("Save Workout Plan")
                    }

                    // Subtle cancel (outlined) to mirror the mock
                    OutlinedButton(
                        onClick = { /* no-op or reset */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

/* ------------ tiny reusable UI helpers (purely visual) ------------ */

@Composable
private fun SegPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color.White else Color(0x00000000),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Box(
            Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.Black else Color(0xFF7C8AA0)
            )
        }
    }
}

@Composable
private fun ChipPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0xFFE8F6EE) else Color(0xFFF3F5F9),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color(0xFF16A05F) else Color(0xFF606D80)
            )
        }
    }
}