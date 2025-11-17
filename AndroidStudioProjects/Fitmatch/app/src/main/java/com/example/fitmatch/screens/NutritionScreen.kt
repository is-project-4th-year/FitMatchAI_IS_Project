package com.example.fitmatch.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.viewmodel.NutritionViewModel

/* ──────────────────────────────────────────────────────────────────────────────
   Public entry (use this in your NavHost)
   - Owns VM, collects state, handles loading/error, and calls content UI
   ────────────────────────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionRoute(viewModel: NutritionViewModel = viewModel(),
    navigationManager: NavigationManager) {
    val ui by viewModel.ui.collectAsState()

    // Start listening once
    LaunchedEffect(Unit) { viewModel.startObserving() }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Nutrition") },
                navigationIcon = {
                    IconButton(onClick = {navigationManager.goBack()}) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.regenerateFromLatestFeatures() },
                        enabled = !ui.loading
                    ) { Text("Regenerate") }
                }
            )
        }
    ) { inner ->
        when {
            ui.loading -> LoadingBox(Modifier.padding(inner))
            ui.error != null -> ErrorBox(ui.error!!, Modifier.padding(inner))
            ui.latest == null -> EmptyBox(
                text = "No nutrition plan yet.",
                modifier = Modifier.padding(inner)
            )
            else -> NutritionScreenContent(
                modifier = Modifier.padding(inner),
                goal = ui.latest!!.goal,
                calories = ui.latest!!.macros.calories_target,
                protein = ui.latest!!.macros.protein_g,
                fatG = ui.latest!!.macros.fat_g,
                carbsG = ui.latest!!.macros.carbs_g,
                fiberG = ui.latest!!.macros.fiber_g,
                hydrationLiters = ui.latest!!.hydration_liters,
                supplements = ui.latest!!.supplements.map {
                    SuppItemUi(name = it.name, dose = it.dose, notes = it.notes)
                }
            )
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   UI-only data for supplements
   ────────────────────────────────────────────────────────────────────────────── */
data class SuppItemUi(val name: String, val dose: String, val notes: String?)

/* ──────────────────────────────────────────────────────────────────────────────
   Pure UI content (no VM work here)
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
private fun NutritionScreenContent(
    modifier: Modifier = Modifier,
    goal: String,
    calories: Int,
    protein: Int,
    fatG: Int,
    carbsG: Int,
    fiberG: Int,
    hydrationLiters: Double,
    supplements: List<SuppItemUi>
) {
    val prettyGoal = when (goal.lowercase()) {
        "fatloss", "fat loss", "cut" -> "Fat Loss"
        "hypertrophy", "muscle"      -> "Hypertrophy"
        "endurance"                  -> "Endurance"
        else                         -> goal.ifBlank { "Nutrition" }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverviewCard(
            title = prettyGoal,
            calories = calories,
            protein = protein,
            fatG = fatG,
            carbsG = carbsG,
            fiberG = fiberG,
            hydrationLiters = hydrationLiters
        )
        SupplementsCard(supplements)
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   Cards & rows
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
private fun OverviewCard(
    title: String,
    calories: Int,
    protein: Int,
    fatG: Int,
    carbsG: Int,
    fiberG: Int,
    hydrationLiters: Double
) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Calories: $calories kcal/day", style = MaterialTheme.typography.bodyMedium)

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroChip("Protein", "$protein g")
                MacroChip("Fat", "$fatG g")
                MacroChip("Carbs", "$carbsG g")
                MacroChip("Fiber", "$fiberG g")
            }

            Text(
                "Hydration: ${"%.1f".format(hydrationLiters)} L/day",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SupplementsCard(items: List<SuppItemUi>) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Supplements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (items.isEmpty()) {
                Text("None recommended.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.forEachIndexed { idx, it ->
                    SupplementRow(it)
                    if (idx != items.lastIndex) Divider(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplementRow(s: SuppItemUi) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(s.name, fontWeight = FontWeight.SemiBold)
        Text("Dose: ${s.dose}", style = MaterialTheme.typography.bodyMedium)
        if (!s.notes.isNullOrBlank()) {
            Text(
                s.notes!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   Small reusable pieces
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
private fun MacroChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}