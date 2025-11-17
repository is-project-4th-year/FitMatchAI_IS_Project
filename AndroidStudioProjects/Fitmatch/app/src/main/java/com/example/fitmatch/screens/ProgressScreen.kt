package com.example.fitmatch.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitmatch.models.ProgressRepository
import com.example.fitmatch.models.ProgressViewModelFactory
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.screens.Charts.BarChartMP
import com.example.fitmatch.screens.Charts.LineChartMP
import com.example.fitmatch.screens.Charts.PieChartMP
import com.example.fitmatch.viewmodel.ProgressViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun ProgressScreen(
    navigationManager: NavigationManager,
    planId: String,
    vm: ProgressViewModel = viewModel(
        factory = ProgressViewModelFactory(
            repo = ProgressRepository(FirebaseFirestore.getInstance()),
            auth = FirebaseAuth.getInstance()
        )
    )
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(planId) { vm.load(planId) }
    val scroll = rememberScrollState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
    ) {
        // Gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp + topInset)

                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF19D27A), Color(0xFF4B84F6))
                    )
                )
                .padding(start = 12.dp, end = 12.dp, top = topInset)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { runCatching { navigationManager.goBack() }
                    .onFailure { navigationManager.navigateToHomeScreen() } }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Progress", color = Color.White,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Track adherence and volume over time",
                        color = Color(0xE6FFFFFF), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (ui.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        ui.error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(12.dp))
        }

        // Content
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // This Week Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("This Week", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = ui.thisWeekPct,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = Color(0xFF2DCA73),
                        trackColor = Color(0xFFE9EBF1)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                        StatPill("${(ui.thisWeekPct * 100).toInt()}%", "Completion")
                        StatPill("${ui.streakDays}d", "Streak")
                        StatPill("${ui.cumulativeSessionsDone}/${ui.cumulativeSessionsTotal}", "Exercises Done")
                        StatPill("${ui.doneSessions}/${ui.doneSessions + ui.missedSessions}", "Sessions")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Daily adherence trend (Line)
            ChartCard(title = "Daily Adherence") {
                if (ui.dailyAdherencePct.isNotEmpty())
                    LineChartMP(
                        values = ui.dailyAdherencePct,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                Spacer(Modifier.height(6.dp))
                Text("Last ${ui.dailyAdherencePct.size} days • % of completed prescribed work",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF707A89))
            }

            // Weekly volume ratio (Bar)
            ChartCard(title = "Weekly Volume Ratio") {
                if (ui.weeklyVolumeBars.isNotEmpty())
                    BarChartMP(
                        values = ui.weeklyVolumeBars,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                Spacer(Modifier.height(6.dp))
                Text("Average actual/prescribed volume per week",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF707A89))
            }

            // Done vs Missed (Pie)
            ChartCard(title = "Session Completion") {
                PieChartMP(
                    slices = listOf(
                        "Done" to ui.doneSessions.toFloat(),
                        "Missed" to ui.missedSessions.toFloat()
                    ),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
            // --- NEW: Daily Volume vs Intensity (two lines) ---
            ChartCard(title = "Daily Volume & Intensity") {
                if (ui.dailyVolumeRatio.isNotEmpty() && ui.dailyIntensityRatio.isNotEmpty()) {
                    // First line: volume
                    LineChartMP(
                        values = ui.dailyVolumeRatio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    // Second line: intensity
                    LineChartMP(
                        values = ui.dailyIntensityRatio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Volume ratio & intensity ratio by day",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF707A89)
                    )
                } else {
                    Text("No data yet", style = MaterialTheme.typography.bodySmall)
                }
            }

// --- NEW: Weekly Completion % (bar) ---
            ChartCard(title = "Weekly Completion %") {
                if (ui.weeklyCompletionPct.isNotEmpty()) {
                    // MP bar takes Float list; values already in 0..1
                    BarChartMP(
                        values = ui.weeklyCompletionPct,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Percent of prescribed work completed per week",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF707A89)
                    )
                } else {
                    Text("No weekly data yet", style = MaterialTheme.typography.bodySmall)
                }
            }

        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4F6FA))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7688))
    }
}