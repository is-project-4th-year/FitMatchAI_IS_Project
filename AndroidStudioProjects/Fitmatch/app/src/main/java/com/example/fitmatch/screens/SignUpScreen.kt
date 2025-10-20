package com.example.fitmatch.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitmatch.R
import com.example.fitmatch.navigations.NavigationManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SignUpScreen(
    navigationManager: NavigationManager,
    auth: FirebaseAuth,
    onGoogleSignInClick: () -> Unit = {},
    onFacebookSignInClick: () -> Unit = {},
    onAppleSignInClick: () -> Unit = {}
) {
    // --- FitMatch palette ---
    val FMBackground = Color(0xFFFFFFFF)
    val FMNavy = Color(0xFF0B0D1A)
    val FMFieldBg = Color(0xFFF2F3F5)
    val FMTitle = Color(0xFF15151E)
    val FMMuted = Color(0xFF8E8E99)
    val FMGreen = Color(0xFF1EC87C)

    val pagerScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    Scaffold(containerColor = FMBackground) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Logo ---
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
            Text("Join FitMatch", fontSize = 22.sp, color = FMTitle, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Create your account", fontSize = 13.sp, color = FMMuted)

            Spacer(Modifier.height(20.dp))

            // --- Tab switcher ---
            TabSwitcher(
                titles = listOf("Email Sign Up", "Phone Sign Up"),
                activeIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    pagerScope.launch {
                        pagerState.animateScrollToPage(index, animationSpec = tween(200))
                    }
                },
                activeColor = FMNavy,
                inactiveColor = FMMuted,
                underlineColor = FMGreen
            )

            Spacer(Modifier.height(16.dp))

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                when (page) {
                    0 -> EmailSignUpForm(auth, navigationManager, FMFieldBg, FMNavy, FMMuted)
                    1 -> PhoneSignUpFormWithOtpDialog(auth, FMFieldBg, FMNavy, FMMuted, FMGreen, navigationManager)
                }
            }

            // --- Divider + Social row ---
            Spacer(Modifier.height(22.dp))
            Divider()
            Spacer(Modifier.height(10.dp))
            Text("or continue with", color = FMMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            SocialRow(
                onGoogle = onGoogleSignInClick,
                onFacebook = onFacebookSignInClick,
                onApple = onAppleSignInClick
            )

            Spacer(Modifier.height(14.dp))
            TextButton(onClick = { navigationManager.navigateToLogin() }) {
                Text("Already have an account? Sign In", color = FMNavy)
            }
        }
    }
}

@Composable
private fun EmailSignUpForm(
    auth: FirebaseAuth,
    navigationManager: NavigationManager,
    fieldBg: Color,
    navy: Color,
    muted: Color
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email Address", color = muted) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = navy
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password", color = muted) },
        singleLine = true,
        visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { pwVisible = !pwVisible }) {
                Icon(
                    imageVector = if (pwVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = muted
                )
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = navy
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = confirm,
        onValueChange = { confirm = it },
        label = { Text("Confirm Password", color = muted) },
        singleLine = true,
        visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = navy
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
        colors = ButtonDefaults.buttonColors(containerColor = navy),
        shape = RoundedCornerShape(10.dp),
        enabled = email.isNotBlank() && password.isNotBlank() && confirm.isNotBlank() && !loading
    ) {
        if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        else Text("Create Account", color = Color.White)
    }

    if (message.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color.Red, fontSize = 12.sp)
    }
}

@Composable
private fun PhoneSignUpFormWithOtpDialog(
    auth: FirebaseAuth,
    fieldBg: Color,
    navy: Color,
    muted: Color,
    green: Color,
    navigationManager: NavigationManager
) {
    val activity = LocalContext.current as Activity

    var phone by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var sending by remember { mutableStateOf(false) }
    var showOtp by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = phone, onValueChange = { phone = it },
        label = { Text("Phone Number", color = muted) },
        placeholder = { Text("+254712345678", color = muted) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = navy
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            sending = true
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {}
                override fun onVerificationFailed(e: FirebaseException) {
                    sending = false
                    e.printStackTrace()
                }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    resendToken = token
                    sending = false
                    showOtp = true
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
        colors = ButtonDefaults.buttonColors(containerColor = navy),
        shape = RoundedCornerShape(10.dp),
        enabled = phone.isNotBlank() && !sending
    ) {
        if (sending) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        else Text("Send OTP", color = Color.White)
    }

    if (showOtp) {
        OtpDialog(
            phone = phone,
            navy = navy,
            green = green,
            muted = muted,
            onDismiss = { showOtp = false },
            onVerify = { code ->
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                auth.signInWithCredential(credential).addOnCompleteListener { t ->
                    if (t.isSuccessful) {
                        showOtp = false
                        navigationManager.navigateToHomeScreen()
                    }
                }
            },
            onResend = {
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {}
                    override fun onVerificationFailed(e: FirebaseException) { e.printStackTrace() }
                    override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                        verificationId = id
                        resendToken = token
                    }
                }
                val builder = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                resendToken?.let { builder.setForceResendingToken(it) }
                PhoneAuthProvider.verifyPhoneNumber(builder.build())
            }
        )
    }
}
