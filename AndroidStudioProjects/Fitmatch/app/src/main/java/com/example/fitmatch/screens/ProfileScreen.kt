package com.example.fitmatch.screens


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.fitmatch.Viewmodels.ProfileViewModel
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.ui.theme.FitMatchTheme

@Composable
fun ProfileScreen(
    navigationManager: NavigationManager,
    viewModel: ProfileViewModel = viewModel()
) {
    val profile by viewModel.userProfile.collectAsState()
    var darkMode by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var fieldToEdit by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfileImage(it) }
    }

    LaunchedEffect(Unit) { viewModel.ensureUserDoc() }

    FitMatchTheme(darkTheme = darkMode) {
        val colors = MaterialTheme.colorScheme

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                // ====== Top Gradient Bar (Dynamic for Dark Mode) ======
                val gradientBrush = if (darkMode)
                    Brush.horizontalGradient(listOf(Color(0xFF1A1A1D), Color(0xFF0F2027)))
                else
                    Brush.horizontalGradient(listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)))

                // ====== Top Gradient Bar (Themed + Height 220dp) ======
                val isDark = isSystemInDarkTheme()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            brush = if (isDark)
                            // 🌙 Dark Mode Gradient
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1A1A1D), Color(0xFF0F2027))
                                )
                            else
                            // ☀️ Light Mode Gradient
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00C6FB), Color(0xFF0078FF))
                                )
                        )
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                ) {
                    // Keep all your inside content the same ↓
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                                bottom = 8.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigationManager.goBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text("Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        IconButton(onClick = { /* Settings optional */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }


                // ====== Profile Card ======
                Card(
                    modifier = Modifier
                        .offset(y = (-50).dp)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Profile Image
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = profile.profileImageUrl
                                        ?: "https://via.placeholder.com/150"
                                ),
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePicker.launch("image/*") },
                                contentScale = ContentScale.Crop
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier
                                    .offset(x = (-6).dp, y = (-6).dp)
                                    .size(22.dp)
                                    .background(colors.background, CircleShape)
                                    .padding(2.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(profile.fullName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onSurface)
                        Text(profile.email, color = colors.onSurface.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text("Member since January 2024", color = colors.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)

                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            InfoCard("Workouts", profile.workouts.toString(), colors)
                            InfoCard("Day Streak", profile.streak.toString(), colors)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            InfoCard("Goals Done", profile.goals.toString(), colors)
                            InfoCard("Total Time", profile.totalTime, colors)
                        }
                    }
                }

                // ====== Personal Info Section ======
                Text(
                    "Personal Information",
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.onBackground
                )
                Spacer(Modifier.height(8.dp))
                PersonalInfoItem("Full Name", profile.fullName, colors) {
                    fieldToEdit = "fullName"
                    newValue = profile.fullName
                    showEditDialog = true
                }
                PersonalInfoItem("Email", profile.email, colors) {
                    fieldToEdit = "email"
                    newValue = profile.email
                    showEditDialog = true
                }
                PersonalInfoItem("Phone", profile.phone, colors) {
                    fieldToEdit = "phone"
                    newValue = profile.phone
                    showEditDialog = true
                }

                // ====== Settings Section ======
                SettingsSection(
                    darkMode = darkMode,
                    onDarkModeToggle = { darkMode = it },
                    onLogout = { viewModel.logout { navigationManager.navigateToLogin() } },
                    colors = colors
                )
            }

            if (showEditDialog) {
                EditFieldDialog(
                    title = "Edit ${fieldToEdit.replaceFirstChar { it.uppercase() }}",
                    value = newValue,
                    onDismiss = { showEditDialog = false },
                    onSave = {
                        viewModel.updateField(fieldToEdit, newValue)
                        showEditDialog = false
                    },
                    onValueChange = { newValue = it }
                )
            }
        }
    }
}

// --- InfoCard now uses theme colors ---
@Composable
fun InfoCard(label: String, value: String, colors: ColorScheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = colors.primary, fontSize = 16.sp)
        Text(label, color = colors.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

// --- Personal Info Item with theme support ---
@Composable
fun PersonalInfoItem(label: String, value: String, colors: ColorScheme, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, color = colors.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(value, color = colors.onSurface, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.Edit, contentDescription = null, tint = colors.primary)
        }
    }
}

// --- Settings section also themed ---
@Composable
fun SettingsSection(
    darkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onLogout: () -> Unit,
    colors: ColorScheme
) {
    Column(Modifier.padding(16.dp)) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dark Mode", color = colors.onSurface)
            Switch(
                checked = darkMode,
                onCheckedChange = onDarkModeToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.primary,
                    uncheckedThumbColor = colors.onSurface.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = colors.errorContainer),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Logout", color = colors.onErrorContainer)
        }
    }
}


@Composable
fun EditFieldDialog(title: String, value: String, onDismiss: () -> Unit, onSave: () -> Unit,
                    onValueChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun SettingItem(title: String, switchValue: Boolean? = null, onToggle: ((Boolean) -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        if (switchValue != null && onToggle != null) {
            Switch(checked = switchValue, onCheckedChange = onToggle)
        }
    }
}




