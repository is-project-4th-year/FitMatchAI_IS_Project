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
fun SignUpScreen(
    navigationManager: NavigationManager,
    auth: FirebaseAuth,
    onGoogleSignInClick: () -> Unit = {},
    onFacebookSignInClick: () -> Unit = {},
    onAppleSignInClick: () -> Unit = {}
) {
    // palette
    val FMBackground = Color(0xFFFFFFFF)
    val FMNavy = Color(0xFF0B0D1A)
    val FMFieldBg = Color(0xFFF2F3F5)
    val FMTitle = Color(0xFF15151E)
    val FMMuted = Color(0xFF8E8E99)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    // phone signup with manual OTP
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
            // Logo + title (signup)
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
            Text("Join FitMatch", fontSize = 22.sp, color = FMTitle)
            Spacer(Modifier.height(6.dp))
            Text("Create your account", fontSize = 13.sp, color = FMMuted)

            Spacer(Modifier.height(22.dp))

            // ---------------- EMAIL SIGN UP ----------------
            Text("Email Sign Up", color = FMTitle)
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
                value = confirm, onValueChange = { confirm = it },
                label = { Text("Confirm Password", color = FMMuted) },
                singleLine = true,
                visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                    if (password != confirm) {
                        message = "Passwords do not match"
                        return@Button
                    }
                    loading = true
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { t ->
                            loading = false
                            if (t.isSuccessful) navigationManager.navigateToHomeScreen()
                            else message = t.exception?.message ?: "Sign-up failed"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FMNavy),
                shape = RoundedCornerShape(10.dp),
                enabled = email.isNotBlank() && password.isNotBlank() && confirm.isNotBlank() && !loading
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("Create Account", color = Color.White)
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { navigationManager.navigateToLogin() }) {
                Text("Already have an account? Sign In", color = FMNavy)
            }

            if (message.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(message, color = Color.Red, fontSize = 12.sp)
            }

            // Divider
            Spacer(Modifier.height(18.dp))
            Divider()
            Spacer(Modifier.height(10.dp))
            Text("or continue with", color = FMMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            SocialRow(
                onGoogle = onGoogleSignInClick,
                onFacebook = onFacebookSignInClick,
                onApple = onAppleSignInClick
            )

            // ---------------- PHONE SIGN UP (MANUAL OTP) ----------------
            Spacer(Modifier.height(26.dp))
            Text("Phone Sign Up", color = FMTitle)
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
                                // manual entry desired; do not auto-complete
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
                            if (t.isSuccessful) {
                                // phone auth = account exists now (signup semantics)
                                navigationManager.navigateToHomeScreen()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FMNavy),
                    shape = RoundedCornerShape(10.dp),
                    enabled = smsCode.length == 6 && !phoneLoading
                ) {
                    if (phoneLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("Verify & Create Account", color = Color.White)
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}
