package com.example.fitmatch.screens

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.text.input.KeyboardType
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
import java.time.Instant
import java.time.ZoneId
import java.util.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import kotlinx.coroutines.launch

/* ---------- helpers ---------- */

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun computeEndDateMs(startMs: Long?, weeks: Int): Long? {
    if (startMs == null || weeks <= 0) return null
    return startMs + (weeks * 7L) * DAY_MS
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Long.toDateText(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toString()

private fun dateLabel(ms: Long): String {
    if (ms == 0L) return "Pick date"
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return fmt.format(Date(ms))
}

/* ---------- screen ---------- */

@Composable
fun GoalScreen(navigationManager: NavigationManager, auth: FirebaseAuth,
    goalsVm: GoalsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
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

    val snackbar = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) },
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ){scaffoldPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ){
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp + topInset)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF19D27A), Color(0xFF4B84F6))
                        )
                    )
                    .padding(top = 12.dp+ topInset, start = 16.dp, end = 16.dp)
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
                // Create goal
                item {
                    CreateGoalCardStyled(
                        onSave = { goalsVm.add(it)},
                        onGoToPlan = {navigationManager.navigateToPlan()},
                        snackbar = snackbar
                    )
                }

                // Active
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

                // Past
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
}
/* ---------- Create Goal (styled) ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGoalCardStyled(
    onSave: (Goal) -> Unit,
    onGoToPlan:() -> Unit,
    snackbar: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var goalType by remember { mutableStateOf("") }
    val goalOptions = listOf("Fat loss", "Hypertrophy", "Endurance")
    var goalExpanded by remember { mutableStateOf(false) }

    var target by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kgs") }
    var weeks by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(0L) }
    var endMillis by remember { mutableStateOf<Long?>(null) }

    // Auto recompute end when start or weeks change
    LaunchedEffect(weeks, startMillis) {
        val w = weeks.toIntOrNull() ?: 0
        endMillis = computeEndDateMs(startMillis.takeIf { it > 0 }, w)
    }

    fun pickDate(ctx: Context, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                cal.set(y, m, d)
                // normalize to midnight for neatness
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onPicked(cal.timeInMillis)
            },
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) {
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

            ExposedDropdownMenuBox(
                expanded = goalExpanded,
                onExpandedChange = { goalExpanded = !goalExpanded }
            ) {
                OutlinedTextField(
                    value = goalType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Goal Type") },
                    placeholder = { Text("Choose your goal type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = goalExpanded,
                    onDismissRequest = { goalExpanded = false }
                ) {
                    goalOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                goalType = option
                                goalExpanded = false
                            }
                        )
                    }
                }
            }

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
                ) {
                    Text(if (startMillis == 0L) "Pick date" else dateLabel(startMillis))
                }

                // Read-only end date (auto)
                OutlinedTextField(
                    value = endMillis?.let { dateLabel(it) } ?: "—",
                    onValueChange = { /* read-only */ },
                    label = { Text("End Date (auto)") },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    singleLine = true
                )
            }

            // Gradient primary button
            Surface(
                onClick = {
                    val g = Goal(
                        id = "",
                        goalType = goalType,
                        targetValue = target.toFloatOrNull() ?: 0f,
                        unit = unit.trim(),
                        currentValue = 0f,
                        durationWeeks = weeks.toIntOrNull() ?: 0,
                        workoutsPerWeek = freq.toIntOrNull() ?: 0,
                        startDate = startMillis,
                        endDate = endMillis ?: computeEndDateMs(
                            startMillis.takeIf { it > 0 }, weeks.toIntOrNull() ?: 0
                        ) ?: 0L,
                        status = "active"
                    )
                    onSave(g)

                    goalType = ""; target = ""; unit = "kg"; weeks = ""; freq = ""
                    startMillis = 0L; endMillis = null

                    scope.launch {
                        val res = snackbar.showSnackbar(
                            message = "Goal saved. Create a workout plan now?",
                            actionLabel = "Open Plan"
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            onGoToPlan()
                        }
                    }

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
    var editing by remember { mutableStateOf(false) }

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
                    // OPEN THE DIALOG INSTEAD OF DIRECT UPDATE
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }

            Text("Progress", color = Color(0xFF7C8191), fontSize = 12.sp)
            LinearProgressIndicator(
                progress = (goal.progress / 100f).coerceIn(0f, 1f),
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

    if (editing) {
        EditGoalDialog(
            initial = goal,
            onDismiss = { editing = false },
            onSave = { updated ->
                editing = false
                onEdit(updated)        // <- uses your existing goalsVm.update(...)
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGoalDialog(
    initial: Goal,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit
) {
    val ctx = LocalContext.current

    // fields
    var goalType by remember { mutableStateOf(
        when (initial.goalType.lowercase()) {
            "fatloss", "fat loss", "cut" -> "Fat loss"
            "endurance" -> "Endurance"
            else -> "Hypertrophy"
        }
    ) }
    val goalOptions = listOf("Fat loss", "Hypertrophy", "Endurance")
    var expanded by remember { mutableStateOf(false) }

    var target by remember { mutableStateOf(
        (if (initial.targetValue == 0f) "" else initial.targetValue.toString())
    ) }
    var unit by remember { mutableStateOf(if (initial.unit.isBlank()) "kg" else initial.unit) }
    var weeks by remember { mutableStateOf(
        (if (initial.durationWeeks <= 0) "" else initial.durationWeeks.toString())
    ) }
    var freq by remember { mutableStateOf(
        (if (initial.workoutsPerWeek <= 0) "" else initial.workoutsPerWeek.toString())
    ) }

    var startMs by remember { mutableStateOf(initial.startDate) }
    var endMs by remember { mutableStateOf<Long?>(initial.endDate.takeIf { it > 0 }) }

    // auto-compute end date
    LaunchedEffect(startMs, weeks) {
        val w = weeks.toIntOrNull() ?: 0
        endMs = computeEndDateMs(startMs.takeIf { it > 0 }, w)
    }

    fun pickDate(onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = if (startMs > 0) startMs else System.currentTimeMillis() }
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                cal.set(y, m, d)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onPicked(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val normalized = when (goalType.lowercase()) {
                    "fat loss" -> "fatloss"
                    "endurance" -> "endurance"
                    else -> "hypertrophy"
                }
                val updated = initial.copy(
                    goalType = normalized,
                    targetValue = target.toFloatOrNull() ?: initial.targetValue,
                    unit = unit.trim(),
                    durationWeeks = weeks.toIntOrNull() ?: initial.durationWeeks,
                    workoutsPerWeek = freq.toIntOrNull() ?: initial.workoutsPerWeek,
                    startDate = startMs,
                    endDate = endMs ?: initial.endDate
                )
                onSave(updated)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Edit Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Goal type (dropdown)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = goalType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Goal Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        goalOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = { goalType = opt; expanded = false }
                            )
                        }
                    }
                }

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
                        onClick = { pickDate { startMs = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (startMs == 0L) "Pick date" else dateLabel(startMs))
                    }

                    OutlinedTextField(
                        value = endMs?.let { dateLabel(it) } ?: "—",
                        onValueChange = {},
                        label = { Text("End Date (auto)") },
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        singleLine = true
                    )
                }
            }
        }
    )
}
