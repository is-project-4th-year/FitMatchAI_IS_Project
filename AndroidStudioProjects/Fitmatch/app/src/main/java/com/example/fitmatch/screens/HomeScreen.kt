package com.example.fitmatch.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitmatch.R
import com.example.fitmatch.navigations.NavigationManager
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(navigationManager: NavigationManager, auth: FirebaseAuth) {
    val user = auth.currentUser
    if (user == null) {
        LaunchedEffect(Unit) { navigationManager.navigateToLogin() }
        return
    }

    val userName = user.displayName ?: "User"

    val FMNavy = Color(0xFF0B0D1A)
    val FMGreen = Color(0xFF1EC87C)
    val FMLightBlue = Color(0xFF4BA6F8)
    val FMLightOrange = Color(0xFFFEC544)
    val FMBackground = Color(0xFFFFFFFF)
    val FMMuted = Color(0xFF8E8E99)

    Scaffold(
        bottomBar = { FitMatchBottomNav() },
        containerColor = FMBackground
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
        ) {
            // ---------- HEADER ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00C6FB), Color(0xFF0078FF))
                        )
                    )
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                        }
                        IconButton(onClick = {
                            auth.signOut()
                            navigationManager.navigateToLogin()
                        }) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Welcome back, $userName!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ready for today’s workout?",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 26.dp, bottom = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = 0.68f,
                        color = FMGreen,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(70.dp)
                    )
                    Text(
                        "68%\nComplete",
                        color = Color.White,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // ---------- WEEKLY GOAL ----------
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("This Week’s Goal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FMNavy)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column {
                        Text("Lose 2 lbs", color = FMNavy, fontWeight = FontWeight.Bold)
                        Text("12", color = FMNavy, fontWeight = FontWeight.Bold)
                        Text("Day Streak", fontSize = 12.sp, color = FMMuted)
                    }
                    Column {
                        Spacer(Modifier.height(16.dp))
                        Text("89", color = FMNavy, fontWeight = FontWeight.Bold)
                        Text("Total Workouts", fontSize = 12.sp, color = FMMuted)
                    }
                }
            }

            // ---------- QUICK ACTIONS ----------
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Quick Actions", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FMNavy)
                Spacer(Modifier.height(12.dp))
                QuickActionCard("Add Metrics", "Log your latest workout", FMGreen)
                Spacer(Modifier.height(10.dp))
                QuickActionCard("View Recommendations", "AI-powered insights", FMLightBlue)
                Spacer(Modifier.height(10.dp))
                QuickActionCard("Track Progress", "View your analytics", FMLightOrange)
            }

            // ---------- LATEST RECOMMENDATION ----------
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Latest Recommendation", fontWeight = FontWeight.Bold, color = FMNavy)
                    Text("View All →", color = FMMuted, fontSize = 13.sp)
                }

                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFFCF8)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFFCF8))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Try High-Intensity Interval Training",
                            fontWeight = FontWeight.Bold,
                            color = FMNavy
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Workout Suggestion", fontSize = 12.sp, color = FMGreen, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Based on your recent cardio sessions, HIIT workouts could help you reach your weight loss goal 23% faster.",
                            color = FMMuted,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("89%", color = FMGreen, fontWeight = FontWeight.Bold)
                                Text("Confidence", fontSize = 12.sp, color = FMMuted)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("20–30 min", color = FMNavy, fontWeight = FontWeight.Bold)
                                Text("Duration", fontSize = 12.sp, color = FMMuted)
                            }
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = FMGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(35.dp)
                            ) {
                                Text("Try It", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ---------- SUMMARY ----------
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Today's Summary", fontWeight = FontWeight.Bold, color = FMNavy)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("2.4", "Miles Run", FMGreen, Modifier.weight(1f))
                    SummaryCard("420", "Calories", FMLightBlue, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable
fun SummaryCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(70.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun QuickActionCard(title: String, subtitle: String, background: Color, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable { onClick?.invoke() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
        }
    }
}

// ----------- BOTTOM NAV -----------
@Composable
fun FitMatchBottomNav() {
    val selectedIndex = remember { mutableStateOf(0) }
    val activeColor = Color(0xFF1EC87C)
    val inactiveColor = Color(0xFF8E8E99)
    val items = listOf("Home", "Goals", "Progress", "Profile")
    val icons = listOf(
        R.drawable.ic_home,
        R.drawable.ic_goals,
        R.drawable.ic_progress,
        R.drawable.ic_user
    )

    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
        items.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = selectedIndex.value == index,
                onClick = { selectedIndex.value = index },
                icon = {
                    Icon(
                        painter = painterResource(id = icons[index]),
                        contentDescription = label,
                        tint = if (selectedIndex.value == index) activeColor else inactiveColor,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        label,
                        color = if (selectedIndex.value == index) activeColor else inactiveColor,
                        fontSize = 11.sp
                    )
                }
            )
        }
    }
}
