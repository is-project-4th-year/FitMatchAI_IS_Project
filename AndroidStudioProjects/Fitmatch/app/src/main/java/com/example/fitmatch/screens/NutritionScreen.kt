package com.example.fitmatch.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitmatch.data.SupplementRecDTO
import com.example.fitmatch.models.NutritionRepositoryImpl
import com.example.fitmatch.models.PlanRepository
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.viewmodel.NutritionUiState
import com.example.fitmatch.viewmodel.NutritionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun NutritionScreen(
    planRepo: PlanRepository,               // pass the same instance used in PlanScreen
    navigationManager: NavigationManager
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val vm: NutritionViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = NutritionRepositoryImpl(FirebaseFirestore.getInstance())
                return NutritionViewModel(auth, repo, planRepo) as T
            }
        }
    )

    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.startObserving() }

    Column(Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF00C6FB), Color(0xFF0078FF)))
                )
                .padding(horizontal = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
            ) {
                IconButton(onClick = {navigationManager.goBack()}, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text("Nutrition", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        if (ui.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        ui.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
        }

        val plan = ui.latest
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (plan == null) {
                item {
                    Column {
                        Text("No nutrition plan yet.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.regenerateFromLatestFeatures() }) {
                            Text("Generate from latest metrics")
                        }
                    }
                }
            } else {
                item {
                    // Macro card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(plan.goal.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                            Text("Calories: ${plan.macros.calories_target} kcal/day")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MacroChip("Protein", "${plan.macros.protein_g} g")
                                MacroChip("Fat", "${plan.macros.fat_g} g")
                                MacroChip("Carbs", "${plan.macros.carbs_g} g")
                                MacroChip("Fiber", "${plan.macros.fiber_g} g")
                            }
                            Text("Hydration: %.1f L/day".format(plan.hydration_liters))
                            if (plan.safety_flags.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text("Safety:", fontWeight = FontWeight.SemiBold)
                                plan.safety_flags.forEach { Text("• $it") }
                            }
                        }
                    }
                }

                item {
                    // Supplements
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Supplements", fontWeight = FontWeight.SemiBold)
                            plan.supplements.forEach { SuppRow(it) }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { vm.regenerateFromLatestFeatures() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Regenerate from latest metrics") }
                }
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF3F5F9),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) { Text("$label: $value", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
}

@Composable
private fun SuppRow(s: SupplementRecDTO) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(s.name, fontWeight = FontWeight.SemiBold)
        Text("Dose: ${s.dose}")
        if (s.notes.isNotBlank()) Text(s.notes, style = MaterialTheme.typography.bodySmall, color = Color(0xFF677181))
    }
}