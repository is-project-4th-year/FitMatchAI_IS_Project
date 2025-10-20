package com.example.fitmatch.screens


import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.fitmatch.models.Goal
import com.example.fitmatch.Viewmodels.GoalsViewModel
import com.example.fitmatch.navigations.NavigationManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GoalScreen(
    auth: FirebaseAuth,
    navigationManager: NavigationManager,
    vm: GoalsViewModel = remember { GoalsViewModel.factory(auth) }
) {
    val active by vm.activeGoals.collectAsState()
    val past by vm.pastGoals.collectAsState()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // slightly taller for softer gradient blend
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF14E06E), Color(0xFF4285F4)) // green → blue
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(WindowInsets.statusBars.asPaddingValues()) // 👈 makes it fit under camera/notch
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navigationManager.goBack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Goal Management",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Set new fitness goals and track your progress",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CreateGoalCard(onSave = { vm.add(it) }) }

            // Active Goals header
            item {
                SectionHeader(title = "Active Goals", count = active.size)
            }
            items(active, key = { it.id }) { g ->
                GoalCard(
                    goal = g,
                    onEdit = { vm.update(it) },
                    onUpdateProgress = { value -> vm.updateProgress(g.id, value) },
                    onComplete = { vm.markCompleted(g.id) },
                    onDelete = { vm.delete(g.id) }
                )
            }

            // Past Goals header
            item {
                SectionHeader(title = "Past Goals", count = past.size)
            }
            items(past, key = { it.id }) { g ->
                GoalCard(
                    goal = g,
                    showActiveBadge = false,
                    onEdit = { vm.update(it) },
                    onUpdateProgress = {},
                    onComplete = {},
                    onDelete = { vm.delete(g.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        AssistChip(
            onClick = {},
            label = { Text("$count") },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGoalCard(onSave: (Goal) -> Unit) {
    val context = LocalContext.current
    var goalType by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var weeks by remember { mutableStateOf("12") }
    var freq by remember { mutableStateOf("4") }
    var startMillis by remember { mutableStateOf(0L) }
    var endMillis by remember { mutableStateOf(0L) }

    fun pickDate(ctx: Context,  onPicked: (Long) -> Unit) {

        val cal = Calendar.getInstance()
        DatePickerDialog(
            ctx,
            { _, y, m, d -> cal.set(y, m, d); onPicked(cal.timeInMillis) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF14E06E)),
                )
                Spacer(Modifier.width(8.dp))
                Text("Create New Goal", fontWeight = FontWeight.SemiBold)
            }

            // Goal Type dropdown (simple text field to keep it compact)
            OutlinedTextField(
                value = goalType,
                onValueChange = { goalType = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Goal Type") },
                placeholder = { Text("Choose your goal type") }
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = target, onValueChange = { target = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Target Value") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = unit, onValueChange = { unit = it },
                    label = { Text("Unit(kg)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weeks, onValueChange = { weeks = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (weeks)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = freq, onValueChange = { freq = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Workouts/week") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { pickDate(context) { startMillis = it } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (startMillis == 0L) "start date" else dateLabel(startMillis)) }

                OutlinedButton(
                    onClick = { pickDate(context) { endMillis = it } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (endMillis == 0L) "end date" else dateLabel(endMillis)) }
            }

            Button(
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

                    // clear fields
                    goalType = ""; target = ""; unit = ""; weeks = "12"; freq = "4"
                    startMillis = 0L; endMillis = 0L
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1EC87C),      // FitMatch green
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Save Goal",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

//// gradient button helper
//@Composable private fun BrushButton(): Color = Color.Transparent
//@Composable
//private fun GradientButton(
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier,
//    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
//    content: @Composable RowScope.() -> Unit
//) {
//    Box(
//        modifier = modifier
//            .clip(shape)
//            .background(
//                brush = Brush.horizontalGradient(
//                    listOf(Color(0xFF14E06E), Color(0xFF4285F4))
//                )
//            )
//    ) {
//        Button(
//            onClick = onClick,
//            modifier = Modifier.fillMaxWidth().height(48.dp),
//            shape = shape,
//            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
//            content = content
//        )
//    }
//}

@Composable
private fun GoalCard(
    goal: Goal,
    showActiveBadge: Boolean = true,
    onEdit: (Goal) -> Unit,
    onUpdateProgress: (Float) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    if (showActiveBadge)
                        AssistChip(onClick = {}, label = { Text("active") }, colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFE8FFF3)))
                    IconButton(onClick = { /* open edit dialog */ }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
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

            // Edit progress inline (optional quick control)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var newVal by remember { mutableStateOf(goal.currentValue.toString()) }
                OutlinedTextField(
                    value = newVal,
                    onValueChange = { newVal = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    singleLine = true,
                    label = { Text("Update progress") },
                    modifier = Modifier.weight(1f)
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