package com.example.fitmatch.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitmatch.R
import com.example.fitmatch.navigations.NavigationManager
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ResetPasswordScreen(navigationManager: NavigationManager) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- FitMatch brand palette
    val FMBackground = Color(0xFFFFFFFF)
    val FMNavy = Color(0xFF0B0D1A)
    val FMFieldBg = Color(0xFFF2F3F5)
    val FMTitle = Color(0xFF15151E)
    val FMMuted = Color(0xFF8E8E99)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button (top-left)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigationManager.goBack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = FMNavy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Top icon (same style as sign-in)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(FMNavy, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dumbell),
                    contentDescription = "FitMatch Logo",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Reset Password",
                color = FMTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Recover your account password",
                color = FMMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Email field
            Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                Text(
                    text = "Email Address",
                    color = FMTitle,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Enter your email", color = FMMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = TextStyle(color = FMTitle, fontSize = 14.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FMFieldBg,
                        unfocusedContainerColor = FMFieldBg,
                        disabledContainerColor = FMFieldBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = FMNavy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Reset Button
            Button(
                onClick = {
                    isLoading = true
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Reset link sent to your email",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FMNavy),
                shape = RoundedCornerShape(10.dp),
                enabled = email.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "Send Reset Email",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Back to login
            Text(
                text = "Back to Sign In",
                color = FMNavy,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    navigationManager.goBack()
                }
            )
        }
    }
}
