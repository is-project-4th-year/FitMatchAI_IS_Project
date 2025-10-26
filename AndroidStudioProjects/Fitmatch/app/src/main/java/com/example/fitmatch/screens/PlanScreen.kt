package com.example.fitmatch.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitmatch.Viewmodels.PlanUiState
import com.example.fitmatch.Viewmodels.PlanViewModel
import com.example.fitmatch.data.ExerciseDTO
import com.example.fitmatch.navigations.NavigationManager
import com.google.android.play.integrity.internal.v

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(viewModel:PlanViewModel, navigationManager: NavigationManager
) {
    val ui = viewModel.ui.collectAsState().value
    var selectedTab by remember { mutableIntStateOf(1) } // 0 = Quick Select, 1 = Custom Entry
    // Auto-switch to Quick Select when a plan arrives
    LaunchedEffect(ui.latestPlan) {
        if (ui.latestPlan != null) selectedTab = 0
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Workout") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // --- Tabs ---
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Your Plan") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Custom Entry") }
                )
            }

            when (selectedTab) {
                0 -> QuickSelectSection(ui = ui)     // shows ML output if present
                1 -> CustomEntrySection(viewModel = viewModel, ui = ui) // form to send metrics
            }



        }
    }
}
@Composable
fun QuickSelectSection(ui:PlanUiState){
    val plan = ui.latestPlan

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        if (ui.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        ui.error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        if (plan == null) {
            Text(
                "No plan yet. Go to Custom Entry, submit your metrics, and your ML plan will appear here.",
                style = MaterialTheme.typography.bodyMedium
            )
            return
        }

        // Header
        Text(
            "Plan: ${plan.plan_id}   •   Days: ${plan.microcycle_days}",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))

        // Group exercises by day into cards (like your screenshot)
        val byDay = plan.exercises.groupBy { it.day }.toSortedMap()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            byDay.forEach { (day, items) ->
                item {
                    Text("Day $day", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }
                items(items.size) { idx ->
                    val ex = items[idx]
                    ExerciseCard(ex)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(plan.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ExerciseCard(ex: ExerciseDTO) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            // Title + optional tag (block)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ex.name, style = MaterialTheme.typography.titleMedium)
                if (ex.block.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(ex.block.lowercase()) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Prescriptions in two columns
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Sets × Reps", style = MaterialTheme.typography.labelSmall)
                    Text("${ex.sets} × ${ex.reps}", style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Rest", style = MaterialTheme.typography.labelSmall)
                    Text("${ex.rest_sec} sec", style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Tempo", style = MaterialTheme.typography.labelSmall)
                    Text(ex.tempo.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun CustomEntrySection(
    viewModel: PlanViewModel,
    ui: PlanUiState
) {
    // === your original local state (unchanged) ===
    var age by remember { mutableStateOf("22") }
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("75") }
    var bmi by remember { mutableStateOf("24.5") }
    var goalType by remember { mutableStateOf("1") }        // 0=fatloss,1=hypertrophy,2=endurance
    var workouts by remember { mutableStateOf("4") }
    var calories by remember { mutableStateOf("2200") }
    var equipment by remember { mutableStateOf("gym") }     // none|basic|gym

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Minimal metrics form (UNCHANGED) ----
        OutlinedTextField(age, { age = it.filter(Char::isDigit) }, label = { Text("Age") })
        OutlinedTextField(height, { height = it.filter { c -> c.isDigit() || c=='.' } }, label = { Text("Height (cm)") })
        OutlinedTextField(weight, { weight = it.filter { c -> c.isDigit() || c=='.' } }, label = { Text("Weight (kg)") })
        OutlinedTextField(bmi, { bmi = it.filter { c -> c.isDigit() || c=='.' } }, label = { Text("BMI") })
        OutlinedTextField(goalType, { goalType = it.filter(Char::isDigit) }, label = { Text("Goal Type (0/1/2)") })
        OutlinedTextField(workouts, { workouts = it.filter(Char::isDigit) }, label = { Text("Workouts/week") })
        OutlinedTextField(calories, { calories = it.filter { c -> c.isDigit() || c=='.' } }, label = { Text("Avg Calories") })
        OutlinedTextField(equipment, { equipment = it }, label = { Text("Equipment (none/basic/gym)") })

        Button(
            onClick = {
                viewModel.submitMetrics(
                    age.toIntOrNull() ?: 22,
                    height.toDoubleOrNull() ?: 175.0,
                    weight.toDoubleOrNull() ?: 75.0,
                    bmi.toDoubleOrNull() ?: 24.5,
                    goalType.toIntOrNull() ?: 1,
                    workouts.toIntOrNull() ?: 4,
                    calories.toDoubleOrNull() ?: 2200.0,
                    equipment.lowercase()
                )
            },
            enabled = !ui.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(if (ui.loading) "Submitting…" else "Generate Plan")
        }

        // ⛔️ We DO NOT render the plan here — it appears on the Quick Select tab.
        ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun LabeledNumberField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
}




