package com.example.fitmatch.screens

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitmatch.Viewmodels.GoalsViewModel
import com.example.fitmatch.data.Goal
import com.example.fitmatch.models.GoalsRepository
import com.example.fitmatch.navigations.NavigationManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun GoalScreen(
    navigationManager: NavigationManager,
    auth: FirebaseAuth,
    goalsVm: GoalsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val uid = auth.currentUser?.uid ?: error("User not logged in")
                return GoalsViewModel(GoalsRepository(uid)) as T
            }
        }
    )
) {
    val active by goalsVm.activeGoals.collectAsState(initial = emptyList())
    val past by goalsVm.pastGoals.collectAsState(initial = emptyList())
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp


    Column(Modifier.fillMaxSize()) {
        // Gradient header + back + title + subtitle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp+topInset)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF19D27A), Color(0xFF4B84F6))
                    )
                )
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)

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
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        "Goal Management",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Set new fitness goals and track your progress",
                        color = Color(0xE6FFFFFF),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Create new goal card (styled)
            item {
                CreateGoalCardStyled(onSave = { goalsVm.add(it) })
            }

            // Active goals header with count pill
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Goals", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8FFF3))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text(active.size.toString(), color = Color(0xFF17B269)) }
                }
            }

            items(active, key = { it.id }) { g ->
                GoalCardStyled(
                    goal = g,
                    showActiveBadge = true,
                    onEdit = { goalsVm.update(it) },
                    onUpdateProgress = { nv -> goalsVm.updateProgress(g.id, nv) },
                    onComplete = { goalsVm.markCompleted(g.id) },
                    onDelete = { goalsVm.delete(g.id) }
                )
            }

            if (past.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Past Goals", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF4F6FA))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text(past.size.toString(), color = Color(0xFF6B7688)) }
                    }
                }
                items(past, key = { it.id }) { g ->
                    GoalCardStyled(
                        goal = g,
                        showActiveBadge = false,
                        onEdit = { goalsVm.update(it) },
                        onUpdateProgress = { },
                        onComplete = { },
                        onDelete = { goalsVm.delete(g.id) }
                    )
                }
            }
        }
    }
}

/* ---------- Create Goal (styled like mock) ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGoalCardStyled(onSave: (Goal) -> Unit) {
    val context = LocalContext.current
    var goalType by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("lbs") }
    var weeks by remember { mutableStateOf("12") }
    var freq by remember { mutableStateOf("4") }
    var startMillis by remember { mutableStateOf(0L) }
    var endMillis by remember { mutableStateOf(0L) }

    fun pickDate(ctx: Context, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            ctx,
            { _, y, m, d -> cal.set(y, m, d); onPicked(cal.timeInMillis) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14E06E))
                )
                Spacer(Modifier.width(8.dp))
                Text("Create New Goal", fontWeight = FontWeight.SemiBold)
            }

            // Goal type (simple text keeps backend same; looks like dropdown)
            OutlinedTextField(
                value = goalType,
                onValueChange = { goalType = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Goal Type") },
                placeholder = { Text("Choose your goal type") }
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Target Value") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weeks,
                    onValueChange = { weeks = it.filter(Char::isDigit) },
                    label = { Text("Duration (weeks)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = freq,
                    onValueChange = { freq = it.filter(Char::isDigit) },
                    label = { Text("Workouts/week") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { pickDate(context) { startMillis = it } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (startMillis == 0L) "Pick date" else dateLabel(startMillis)) }

                OutlinedButton(
                    onClick = { pickDate(context) { endMillis = it } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (endMillis == 0L) "Pick date" else dateLabel(endMillis)) }
            }

            // Gradient primary button
            Surface(
                onClick = {
                    val g = Goal(
                        goalType = goalType.trim(),
                        targetValue = target.toFloatOrNull() ?: 0f,
                        unit = unit.trim(),
                        currentValue = 0f,
                        durationWeeks = weeks.toIntOrNull() ?: 0,
                        workoutsPerWeek = freq.toIntOrNull() ?: 0,
                        startDate = startMillis,
                        endDate = endMillis,
                        status = "active"
                    )
                    onSave(g)
                    goalType = ""; target = ""; unit = "lbs"; weeks = "12"; freq = "4"
                    startMillis = 0L; endMillis = 0L
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1EC87C), Color(0xFF4B84F6))
                        )
                    )
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Save Goal", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* ---------- Goal Card (styled) ---------- */

@Composable
private fun GoalCardStyled(
    goal: Goal,
    showActiveBadge: Boolean,
    onEdit: (Goal) -> Unit,
    onUpdateProgress: (Float) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5E8F1), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFE8FFF3)))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(goal.goalType, fontWeight = FontWeight.SemiBold)
                        val sub = "${goal.currentValue.toInt()}/${goal.targetValue.toInt()} ${goal.unit}"
                        Text(sub, color = Color(0xFF7C8191), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showActiveBadge) {
                        AssistChip(
                            onClick = {},
                            label = { Text("active") },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFE8FFF3))
                        )
                    }
                    IconButton(onClick = { onEdit(goal) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }

            Text("Progress", color = Color(0xFF7C8191), fontSize = 12.sp)
            LinearProgressIndicator(
                progress = goal.progress / 100f,
                trackColor = Color(0xFFE9EBF1),
                color = Color(0xFF2DCA73),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${dateLabel(goal.startDate)} - ${dateLabel(goal.endDate)}", color = Color(0xFF7C8191), fontSize = 12.sp)
                Text("${goal.workoutsPerWeek}x/week", color = Color(0xFF7C8191), fontSize = 12.sp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var newVal by remember { mutableStateOf(goal.currentValue.toString()) }
                OutlinedTextField(
                    value = newVal,
                    onValueChange = { newVal = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    singleLine = true,
                    label = { Text("Update progress") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextButton(onClick = { onUpdateProgress(newVal.toFloatOrNull() ?: goal.currentValue) }) {
                    Text("Update")
                }
                if (goal.progress >= 100) {
                    TextButton(onClick = onComplete) { Text("Complete") }
                }
            }
        }
    }
}

private fun dateLabel(ms: Long): String {
    if (ms == 0L) return "Pick date"
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return fmt.format(Date(ms))
}