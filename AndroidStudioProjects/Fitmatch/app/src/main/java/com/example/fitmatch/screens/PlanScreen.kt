package com.example.fitmatch.screens

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
import com.example.fitmatch.Viewmodels.PlanViewModel
import com.example.fitmatch.navigations.NavigationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(navigationManager: NavigationManager, viewModel: PlanViewModel) {
    val ui by viewModel.ui.collectAsState()

    var age by remember { mutableStateOf("22") }
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("75") }
    var bmi by remember { mutableStateOf("24.5") }
    var goalType by remember { mutableStateOf("1") }        // 0=fatloss,1=hypertrophy,2=endurance
    var workouts by remember { mutableStateOf("4") }
    var calories by remember { mutableStateOf("2200") }
    var equipment by remember { mutableStateOf("gym") }     // none|basic|gym

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Plan") },
                navigationIcon = {
                    IconButton(onClick = { navigationManager.goBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Minimal metrics form ----
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
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (ui.loading) "Submitting…" else "Generate Plan") }

            ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }

            // ---- Render latest plan ----
            ui.latestPlan?.let { plan ->
                Spacer(Modifier.height(12.dp))
                Text("Plan: ${plan.plan_id}  •  Days: ${plan.microcycle_days}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val grouped = plan.exercises.groupBy { it.day }.toSortedMap()
                grouped.forEach { (day, items) ->
                    Text("Day $day", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    items.forEach { ex ->
                        Text("• ${ex.block}: ${ex.name} — ${ex.sets}×${ex.reps} • Rest ${ex.rest_sec}s • Tempo ${ex.tempo}")
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (plan.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Notes: ${plan.notes}")
                }
            }
        }
    }
}
