package com.example.fitmatch.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.viewmodel.NutritionViewModel

/* ──────────────────────────────────────────────────────────────────────────────
   Public entry (use this in your NavHost)
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun NutritionRoute(
    viewModel: NutritionViewModel = viewModel(),
    navigationManager: NavigationManager
) {
    val ui by viewModel.ui.collectAsState()
    LaunchedEffect(Unit) { viewModel.startObserving() }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp
    var tab by rememberSaveable { mutableStateOf(0) } // 0 = Plan, 1 = Guides

    Column(Modifier.fillMaxSize()) {
        // Gradient header to match Plan screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp + topInset)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF19D27A), Color(0xFF4B84F6))
                    )
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
                    onClick = { navigationManager.goBack() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Nutrition",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { viewModel.regenerateFromLatestFeatures() },
                    enabled = !ui.loading
                ) { Text("Regenerate", color = Color.White) }
            }
        }

        if (ui.loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }

        ui.error?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Segmented pills like your Plan screen
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF3F5F9))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegPill("Supplement", selected = tab == 0, onClick = { tab = 0 }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            SegPill("Guides", selected = tab == 1, onClick = { tab = 1 }, modifier = Modifier.weight(1f))
        }

        when {
            ui.latest == null && !ui.loading && ui.error == null -> {
                EmptyBox(
                    text = "No nutrition plan yet. Tap Regenerate to create one.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                )
            }

            tab == 0 -> {
                // PLAN tab
                ui.latest?.let { latest ->
                    NutritionScreenContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        goal = latest.goal,
                        calories = latest.macros.calories_target,
                        protein = latest.macros.protein_g,
                        fatG = latest.macros.fat_g,
                        carbsG = latest.macros.carbs_g,
                        fiberG = latest.macros.fiber_g,
                        hydrationLiters = latest.hydration_liters,
                        supplements = latest.supplements.map {
                            SuppItemUi(name = it.name, dose = it.dose, notes = it.notes)
                        }
                    )
                }
            }

            tab == 1 -> {
                // GUIDES tab (light tips; mirrors second-tab pattern)
                GuidesContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

data class SuppItemUi(val name: String, val dose: String, val notes: String?)

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
        modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overview
        BorderedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(prettyGoal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                KeyValueRow("Calories", "$calories kcal/day")

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

                KeyValueRow("Hydration", "${"%.1f".format(hydrationLiters)} L/day")
            }
        }

        // Macro breakdown bars (visual pop)
        BorderedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Macro Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val total = (protein + fatG + carbsG).coerceAtLeast(1)
                MacroBarRow(label = "Protein", value = protein, max = total)
                MacroBarRow(label = "Fat", value = fatG, max = total)
                MacroBarRow(label = "Carbs", value = carbsG, max = total)
            }
        }

        // Supplements
        BorderedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Supplements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (supplements.isEmpty()) {
                    Text("None recommended.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    supplements.forEachIndexed { idx, it ->
                        SupplementRow(it)
                        if (idx != supplements.lastIndex) Divider(Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   Guides (second tab)
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
private fun GuidesContent(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BorderedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hydration Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Bullet("Aim ~30–35 ml/kg daily (more with heat & long sessions).")
                Bullet("Sip through the day; use electrolytes when training long or in heat.")
            }
        }
        BorderedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Protein Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Bullet("Distribute 20–40 g across 3–4 meals.")
                Bullet("Prioritize whole foods; supplement only to fill gaps.")
            }
        }
        BorderedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Carb Timing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Bullet("Center carbs around workouts for performance & recovery.")
                Bullet("Use slower carbs on rest days for satiety and steady energy.")
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   Reusable pieces (match your Plan look)
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
private fun SegPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color.White else Color.Transparent,
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
private fun BorderedCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
    ) { content() }
}

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
private fun MacroBarRow(label: String, value: Int, max: Int) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("$value g", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = (value / max.toFloat()).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
