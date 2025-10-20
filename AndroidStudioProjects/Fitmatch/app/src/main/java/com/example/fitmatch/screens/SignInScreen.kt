package com.example.fitmatch.screens

import android.R.attr.contentDescription
import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.fitmatch.R
import com.example.fitmatch.navigations.NavigationManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    navigationManager: NavigationManager,
    auth: FirebaseAuth,
    onGoogleSignInClick: () -> Unit = {},
    onFacebookSignInClick: () -> Unit = {},
    onAppleSignInClick: () -> Unit = {}
) {
    // --- FitMatch palette (matches your current UI) ---
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
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Logo (restored) ---
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
            Text("Welcome Back", fontSize = 22.sp, color = FMTitle, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Sign in to continue your fitness journey", fontSize = 13.sp, color = FMMuted)

            Spacer(Modifier.height(20.dp))

            // --- Tab switcher: Email | Phone (bold + green underline) ---
            TabSwitcher(
                titles = listOf("Email Login", "Phone Login"),
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

            // --- Pager (tap OR swipe) ---
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)            // give pager finite height
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()

                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (page) {
                        0 -> EmailLoginForm(
                            auth = auth,
                            navigationManager = navigationManager,
                            fieldBg = FMFieldBg,
                            navy = FMNavy,
                            muted = FMMuted
                        )
                        1 -> PhoneLoginFormWithOtpDialog(
                            auth = auth,
                            fieldBg = FMFieldBg,
                            navy = FMNavy,
                            muted = FMMuted,
                            green = FMGreen
                        )
                    }
                }
            }
//            Spacer(Modifier.height(10.dp))
            Text("or continue with", color = FMMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            SocialRow(
                onGoogle = onGoogleSignInClick,
                onFacebook = onFacebookSignInClick,
                onApple = onAppleSignInClick
            )
            // --- Bottom links (same place as your old sign-in) ---
            Spacer(Modifier.height(14.dp))

            }
        }
    }


@Composable
internal fun TabSwitcher(titles: List<String>, activeIndex: Int,
     onTabSelected: (Int) -> Unit,activeColor: Color,
    inactiveColor: Color,underlineColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            titles.forEachIndexed { i, t ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp)
                        .clickable { onTabSelected(i) }
                ) {
                    Text(
                        t,
                        color = if (i == activeIndex) activeColor else inactiveColor,
                        fontWeight = if (i == activeIndex) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth(0.5f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i == activeIndex) underlineColor else Color.Transparent)
                    )
                }
            }
        }
    }
}


@Composable
fun EmailLoginForm(auth: FirebaseAuth, navigationManager: NavigationManager, fieldBg: Color,
    navy: Color, muted: Color
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {   // <-- wrap everything
        OutlinedTextField(
            value = email, onValueChange = { email = it },
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
            value = password, onValueChange = { password = it },
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
                cursorColor = navy,
                focusedTextColor = navy,
                unfocusedTextColor = navy
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Forgot Password?",
                color = navy,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { navigationManager.navigateToResetPassword() }
            )
            Text(
                "Sign Up",
                color = navy,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { navigationManager.navigateToSignUp() }
            )
        }

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
            colors = ButtonDefaults.buttonColors(containerColor = navy),
            shape = RoundedCornerShape(10.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && !loading
        ) {
            if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            else Text("Sign In", color = Color.White)
        }

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color.Red, fontSize = 12.sp)
        }
    }
}


@Composable
fun PhoneLoginFormWithOtpDialog(auth: FirebaseAuth, fieldBg: Color, navy: Color, muted: Color,
    green: Color
) {
    val activity = LocalContext.current as Activity

    var phone by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var sending by remember { mutableStateOf(false) }
    var showOtp by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {   // <-- wrap everything
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
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) { /* manual entry: ignore auto */ }
                    override fun onVerificationFailed(e: FirebaseException) { sending = false; e.printStackTrace() }
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
                    if (t.isSuccessful) showOtp = false
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

@Composable
internal fun OtpDialog(
    phone: String,
    navy: Color,
    green: Color,
    muted: Color,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableStateOf(30) }
    var verifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // countdown
    LaunchedEffect(Unit) {
        secondsLeft = 30
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    // Native dialog dims background automatically; we style a dark card inside.
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {},
        text = {
            Surface(
                color = Color(0xFF1E1C27), // dark card
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // top bar with X
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x331C1C28))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Enter the 6-digit code sent to",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(phone, color = Color(0xFFBFC1C8), fontSize = 14.sp)

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = code, onValueChange = { code = it.take(6) },
                        label = { Text("OTP Code", color = Color(0xFFBFC1C8)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2A36),
                            unfocusedContainerColor = Color(0xFF2A2A36),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            verifying = true
                            scope.launch {
                                onVerify(code)
                                verifying = false
                            }
                        },
                        enabled = code.length == 6 && !verifying,
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (verifying) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Text("Verify", color = Color.White)
                    }

                    Spacer(Modifier.height(12.dp))
                    if (secondsLeft > 0) {
                        Text(
                            "Resend in ${secondsLeft}s",
                            color = Color(0xFF9EA1AE),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        TextButton(onClick = {
                            secondsLeft = 30
                            onResend()
                        }) {
                            Text("Resend OTP", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    )
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
