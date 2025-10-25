package com.example.fitmatch.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitmatch.navigations.NavigationManager

@Composable
fun FitMatchHeader(
    title: String,
    subtitle: String,
    showBack: Boolean = false,
    navigationManager: NavigationManager? = null, // nullable to make it optional
    content: @Composable BoxScope.() -> Unit = {} // ✅ allows trailing custom content
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(
                brush = if (isDark)
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1A1A1D), Color(0xFF0F2027))
                    )
                else
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00C6FB), Color(0xFF0078FF))
                    )
            )
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        ) {
            // 🔹 Row for back + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBack && navigationManager != null) {
                    IconButton(onClick = { navigationManager.goBack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )
        }

        // ✅ Custom trailing composable (used in HomeScreen)
        content()
    }
}


@Composable
fun FitMatchCenteredHeader(
    title: String,
    subtitle: String,
    showBack: Boolean = false,
    navigationManager: NavigationManager? = null
) {
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
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp,
                    start = 16.dp,
                    end = 16.dp
                )
        ) {
            // 🔹 First row: Back arrow (left) + centered title
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showBack && navigationManager != null) {
                    IconButton(
                        onClick = { navigationManager.goBack() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 🔹 Subtitle: aligned with arrow, slightly below title
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                modifier = Modifier.padding(start = if (showBack) 48.dp else 0.dp)
            )
        }
    }
}



























//@Composable
//fun FitMatchHeader(
//    title: String,
//    subtitle: String? = null,
//    showBack: Boolean = false,
//    navigationManager: NavigationManager? = null,
//    content: @Composable BoxScope.() -> Unit = {}
//) {
//    val isDark = isSystemInDarkTheme()
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(220.dp)
//            .background(
//                brush = if (isDark)
//                // 🌙 Dark mode gradient (deep blue → charcoal)
//                    Brush.horizontalGradient(
//                        listOf(Color(0xFF1A1A1D), Color(0xFF0F2027))
//                    )
//                else
//                // ☀️ Light mode gradient (bright cyan → blue)
//                    Brush.horizontalGradient(
//                        listOf(Color(0xFF00C6FB), Color(0xFF0078FF))
//                    )
//            )
//            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
//    )
//    {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(
//                    WindowInsets.statusBars.asPaddingValues() // ✅ Proper inset handling
//                )
//                .padding(horizontal = 20.dp, vertical = 20.dp),
//            verticalArrangement = Arrangement.Top,
//            horizontalAlignment = Alignment.Start
//        ) {
//            if (showBack) {
//                IconButton(
//                    onClick = { navigationManager?.goBack() },
//                    modifier = Modifier.size(42.dp)
//                ) {
//                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
//                }
//                Spacer(Modifier.height(4.dp))
//            }
//
//            Text(
//                title,
//                color = Color.White,
//                fontWeight = FontWeight.Bold,
//                fontSize = 20.sp
//            )
//            subtitle?.let {
//                Text(
//                    it,
//                    color = Color.White.copy(alpha = 0.9f),
//                    fontSize = 13.sp
//                )
//            }
//        }
//
//        content()
//    }
//}
