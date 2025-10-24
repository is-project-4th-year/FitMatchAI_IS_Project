package com.example.fitmatch.screens


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.fitmatch.data.WorkoutEntry
import com.example.fitmatch.data.WeeklyProgress
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitmatch.data.ProgressScope
import com.example.fitmatch.data.ProgressSummary
import com.example.fitmatch.models.ProgressUiState
import com.example.fitmatch.data.WeeklySeries
import com.example.fitmatch.models.ProgressViewModel
import com.example.fitmatch.navigations.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ---------------------------
// THEME
// ---------------------------
private val GreenStart = Color(0xFF1CC88A)
private val BlueEnd = Color(0xFF2F7CF6)
private val SurfaceCard = Color(0xFFF7F9FC)
private val Border = Color(0xFFE7ECF3)
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted = Color(0xFF6B7280)
private val ChipBg = Color(0xFFF0F4FF)


// ---------------------------
// SCREEN
// ---------------------------
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProgressScreen(
    navigationManager: NavigationManager,
    viewModel: ProgressViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.ui.collectAsState()

    Column(modifier.fillMaxSize()) {
        Header(navigationManager)
        when (state) {
            ProgressUiState.Loading -> LoadingState()
            is ProgressUiState.Error -> ErrorState((state as ProgressUiState.Error).message) { viewModel.refresh() }
            is ProgressUiState.Ready -> Content(
                data = (state as ProgressUiState.Ready).data,
                scope = viewModel.scope,
                onScope = viewModel::toggleScope
            )
        }
    }
}

@Composable
private fun Header(navigationManager: NavigationManager) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenStart, BlueEnd)))
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 18.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navigationManager.navigateToHomeScreen() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Your Progress", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("Track your fitness journey", color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Content(
    data: WeeklyProgress,
    scope: ProgressScope,
    onScope: (ProgressScope) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SummaryRow(data.summary) }
        item { ScopeSegment(scope, onScope) }
        item {
            CardWrap(title = "Workout Duration") {
                BarChart(
                    labels = data.series.labels,
                    values = data.series.durations,
                    barColor = Color(0xFF22C55E),
                    maxY = (maxOf(100, (data.series.durations.maxOrNull() ?: 0) + 10))
                )
            }
        }
        item {
            CardWrap(title = "Calories Burned") {
                AreaChart(
                    labels = data.series.labels,
                    values = data.series.calories,
                    lineColor = BlueEnd,
                    fillGradient = Brush.verticalGradient(listOf(BlueEnd.copy(alpha = .35f), Color.Transparent)),
                    maxY = (maxOf(900, (data.series.calories.maxOrNull() ?: 0) + 50))
                )
            }
        }
        item { RecentWorkouts(data.recent) }
    }
}

// ---------------------------
// SUMMARY CARDS
// ---------------------------
@Composable
private fun SummaryRow(s: ProgressSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total Workouts",
                value = s.totalWorkouts.toString(),
                badge = "+${s.workoutsChangePct}%"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Calories Burned",
                value = s.caloriesBurned.formatComma(),
                badge = "${s.caloriesSpanDays} days"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Active Time",
                value = s.activeTimeMinutes.minutesToHrsMins()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Current Weight (lbs)",
                value = String.format(Locale.US, "%.1f", s.weightLbs),
                badge = "${if (s.weightChangeLbs>0) "+" else ""}${s.weightChangeLbs} lbs"
            )
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier,title: String, value: String, badge: String? = null) {
    Surface(
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border1px()
    ) {
        Column(Modifier.padding(16.dp)) {
            if (badge != null) Pill(badge)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(title, fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ChipBg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { Text(text, fontSize = 11.sp, color = BlueEnd, fontWeight = FontWeight.SemiBold) }
}

// ---------------------------
// SEGMENT CONTROL
// ---------------------------
@Composable
private fun ScopeSegment(scope: ProgressScope, onScope: (ProgressScope) -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border1px()
    ) {
        Row(Modifier.padding(4.dp)) {
            SegBtn(modifier = Modifier.weight(1f),"This Week", scope == ProgressScope.WEEK) { onScope(ProgressScope.WEEK) }
            Spacer(Modifier.width(6.dp))
            SegBtn(modifier = Modifier.weight(1f),"This Month", scope == ProgressScope.MONTH) { onScope(ProgressScope.MONTH) }
        }
    }
}

@Composable
private fun SegBtn(modifier: Modifier = Modifier, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color.White else Color.Transparent
    val content = if (selected) TextPrimary else TextMuted
    val shape = RoundedCornerShape(999.dp)
    Surface(
        onClick = onClick,
        color = bg,
        shape = shape,
        tonalElevation = 0.dp
    ) {
        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = content, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

// ---------------------------
// CHART CARDS
// ---------------------------
@Composable
private fun CardWrap(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border1px()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun BarChart(
    labels: List<String>,
    values: List<Int>,
    barColor: Color,
    maxY: Int
) {
    ChartCanvas(height = 170.dp, labels = labels, maxY = maxY) { w, h, xStep, yScale ->
        val barW = xStep * .48f
        values.forEachIndexed { i, v ->
            val x = xStep * (i + 1) - barW/2
            val barH = v * yScale
            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, h - barH),
                size = androidx.compose.ui.geometry.Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
        }
    }
}

@Composable
private fun AreaChart(
    labels: List<String>,
    values: List<Int>,
    lineColor: Color,
    fillGradient: Brush,
    maxY: Int
) {
    ChartCanvas(height = 180.dp, labels = labels, maxY = maxY) { w, h, xStep, yScale ->
        val path = Path()
        val linePath = Path()
        values.forEachIndexed { i, v ->
            val x = xStep * (i + 1)
            val y = h - (v * yScale)
            if (i == 0) {
                path.moveTo(x, h)
                path.lineTo(x, y)
                linePath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                linePath.lineTo(x, y)
            }
        }
        path.lineTo(xStep * values.size, h)
        path.close()
        drawPath(path, brush = fillGradient)
        drawPath(linePath, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun ChartCanvas(
    height: Dp,
    labels: List<String>,
    maxY: Int,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.(w: Float, h: Float, xStep: Float, yScale: Float) -> Unit
) {
    val labelColor = TextMuted
    Column(Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(top = 8.dp, bottom = 8.dp)
                .background(SurfaceCard, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val w = size.width
            val h = size.height
            // grid lines (4)
            val grid = 4
            val yStep = h / grid
            repeat(grid + 1) { i ->
                val y = h - i * yStep
                drawLine(Color(0xFFEFF3F8), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(w, y))
            }
            val xStep = w / (labels.size + 1)
            val yScale = h / maxY
            content(w, h, xStep, yScale)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { l -> Text(l, color = labelColor, fontSize = 11.sp) }
        }
    }
}

// ---------------------------
// RECENT WORKOUTS
// ---------------------------
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun RecentWorkouts(list: List<WorkoutEntry>) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border1px()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recent Workouts", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            list.forEach { WorkoutRow(it) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WorkoutRow(w: WorkoutEntry) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(w.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.width(8.dp))
                Tag(w.tag)
            }
            Spacer(Modifier.height(2.dp))
            Text(w.date.humanize(), fontSize = 12.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${w.durationMin} min", fontWeight = FontWeight.Medium, color = TextPrimary)
            Text("${w.calories} cal", fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
private fun Tag(kind: String) {
    val (bg, fg) = when (kind.lowercase()) {
        "strength" -> Color(0xFFEFF3FF) to Color(0xFF3B82F6)
        "cardio" -> Color(0xFFE8FFF1) to Color(0xFF10B981)
        "hiit" -> Color(0xFFFFF1E7) to Color(0xFFF59E0B)
        else -> Color(0xFFF3E8FF) to Color(0xFF8B5CF6)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) { Text(kind.lowercase(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

// ---------------------------
// STATES & HELPERS
// ---------------------------
@Composable
private fun LoadingState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(8.dp))
        Text("Loading progress…", color = TextMuted)
    }
}

@Composable
private fun ErrorState(msg: String, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Failed to load: $msg", color = Color(0xFFDC2626))
        Spacer(Modifier.height(8.dp))
        Button(onClick = retry, shape = RoundedCornerShape(10.dp)) { Text("Retry") }
    }
}

private fun Int.minutesToHrsMins(): String {
    val h = this / 60
    val m = this % 60
    return String.format(Locale.US, "%dh %02dm", h, m)
}

private fun Int.formatComma(): String = String.format(Locale.US, "%,d", this)

@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.humanize(): String {
    val now = LocalDate.now()
    return when (this) {
        now -> "Today"
        now.minusDays(1) -> "Yesterday"
        else -> this.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) } +
                " • ${this.dayOfMonth}"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun sampleWorkouts(): List<WorkoutEntry> {
    val today = LocalDate.now()
    return listOf(
        WorkoutEntry("1", today, "Full Body Strength", "strength", 70, 520),
        WorkoutEntry("2", today.minusDays(1), "Morning Run", "cardio", 45, 410),
        WorkoutEntry("3", today.minusDays(2), "HIIT Training", "hiit", 40, 480),
        WorkoutEntry("4", today.minusDays(3), "Yoga Flow", "flexibility", 60, 280),
        WorkoutEntry("5", today.minusDays(4), "Upper Body", "strength", 55, 380)
    )
}

// Simple 1px border utility to match the soft card look
private fun Modifier.border1px(): Modifier = this.then(
    androidx.compose.ui.Modifier.drawBehind {
        val strokeColor = Border
        val strokeWidth = 1f
        drawRoundRect(
            color = strokeColor,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
    }
)
