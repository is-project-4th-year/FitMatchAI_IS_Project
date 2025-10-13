package com.example.fitmatch.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitmatch.R
import com.example.fitmatch.navigations.NavigationManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
fun LoginScreen(
    navigationManager: NavigationManager,
    auth: FirebaseAuth,
    onGoogleSignInClick: () -> Unit = {},
    onFacebookSignInClick: () -> Unit = {},
    onAppleSignInClick: () -> Unit = {}
) {
    // palette (same as your sign-in look)
    val FMBackground = Color(0xFFFFFFFF)
    val FMNavy = Color(0xFF0B0D1A)
    val FMFieldBg = Color(0xFFF2F3F5)
    val FMTitle = Color(0xFF15151E)
    val FMMuted = Color(0xFF8E8E99)
    val FMGreen = Color(0xFF1EC87C)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // phone auth (manual code entry)
    var phone by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var phoneLoading by remember { mutableStateOf(false) }
    val activity = LocalContext.current as Activity

    Scaffold(containerColor = FMBackground) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo box (restored)
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

            Spacer(Modifier.height(18.dp))
            Text("Welcome Back", fontSize = 22.sp, color = FMTitle)
            Spacer(Modifier.height(6.dp))
            Text("Sign in to continue your fitness journey", fontSize = 13.sp, color = FMMuted)

            Spacer(Modifier.height(22.dp))

            // ---------------- EMAIL LOGIN ----------------
            Text("Email Login", color = FMTitle)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email Address", color = FMMuted) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FMFieldBg,
                    unfocusedContainerColor = FMFieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = FMNavy
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password", color = FMMuted) },
                singleLine = true,
                visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (pwVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { pwVisible = !pwVisible }) { Icon(icon, contentDescription = null, tint = FMMuted) }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FMFieldBg,
                    unfocusedContainerColor = FMFieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = FMNavy
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    loading = true
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { t ->
                            loading = false
                            if (t.isSuccessful) navigationManager.navigateToHomeScreen()
                            else error = t.exception?.message ?: "Login failed"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FMNavy),
                shape = RoundedCornerShape(10.dp),
                enabled = email.isNotBlank() && password.isNotBlank() && !loading
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("Sign In", color = Color.White)
            }

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { navigationManager.navigateToResetPassword() }) { Text("Forgot Password?", color = FMNavy) }
                TextButton(onClick = { navigationManager.navigateToSignUp() }) { Text("Sign Up", color = FMNavy) }
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(error, color = Color.Red, fontSize = 12.sp)
            }

            // Divider
            Spacer(Modifier.height(18.dp))
            Divider()
            Spacer(Modifier.height(10.dp))
            Text("or continue with", color = FMMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            // Social icons
            SocialRow(
                onGoogle = onGoogleSignInClick,
                onFacebook = onFacebookSignInClick,
                onApple = onAppleSignInClick
            )

            // ---------------- PHONE LOGIN (MANUAL OTP) ----------------
            Spacer(Modifier.height(26.dp))
            Text("Phone Login", color = FMTitle)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone Number", color = FMMuted) },
                placeholder = { Text("+254712345678", color = FMMuted) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FMFieldBg,
                    unfocusedContainerColor = FMFieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = FMNavy
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (!codeSent) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        phoneLoading = true
                        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                // manual entry requested; don't auto sign-in here
                            }
                            override fun onVerificationFailed(e: FirebaseException) {
                                phoneLoading = false
                                e.printStackTrace()
                            }
                            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                                verificationId = id
                                codeSent = true
                                phoneLoading = false
                            }
                        }
                        val options = PhoneAuthOptions.newBuilder(auth)
                            .setPhoneNumber(phone)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity)
                            .setCallbacks(callbacks)
                            .build()
                        PhoneAuthProvider.verifyPhoneNumber(options)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FMNavy),
                    shape = RoundedCornerShape(10.dp),
                    enabled = phone.isNotBlank() && !phoneLoading
                ) {
                    if (phoneLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("Send OTP", color = Color.White)
                }
            } else {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = smsCode, onValueChange = { smsCode = it },
                    label = { Text("Enter OTP", color = FMMuted) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FMFieldBg,
                        unfocusedContainerColor = FMFieldBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = FMNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
                        phoneLoading = true
                        auth.signInWithCredential(credential).addOnCompleteListener { t ->
                            phoneLoading = false
                            if (t.isSuccessful) navigationManager.navigateToHomeScreen()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FMNavy),
                    shape = RoundedCornerShape(10.dp),
                    enabled = smsCode.length == 6 && !phoneLoading
                ) {
                    if (phoneLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("Verify & Sign In", color = Color.White)
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
internal fun SocialRow(
    onGoogle: () -> Unit,
    onFacebook: () -> Unit,
    onApple: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onGoogle) {
            Icon(painter = painterResource(id = R.drawable.google), contentDescription = "Google", tint = Color.Unspecified, modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = onFacebook) {
            Icon(painter = painterResource(id = R.drawable.facebookshare), contentDescription = "Facebook", tint = Color.Unspecified, modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = onApple) {
            Icon(painter = painterResource(id = R.drawable.apple), contentDescription = "Apple", tint = Color.Unspecified, modifier = Modifier.size(40.dp))
        }
    }
}
