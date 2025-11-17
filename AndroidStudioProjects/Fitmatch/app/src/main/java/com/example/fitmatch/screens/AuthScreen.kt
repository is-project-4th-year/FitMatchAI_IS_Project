//package com.example.fitmatch.screens
//
//
//import android.app.Activity
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Visibility
//import androidx.compose.material.icons.filled.VisibilityOff
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.VisualTransformation
//import com.example.fitmatch.R
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.fitmatch.navigations.NavigationManager
//import com.google.firebase.FirebaseException
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.PhoneAuthCredential
//import com.google.firebase.auth.PhoneAuthOptions
//import com.google.firebase.auth.PhoneAuthProvider
//import java.util.concurrent.TimeUnit
//
//@Composable
//fun AuthScreen(navigationManager: NavigationManager, auth: FirebaseAuth,
//    onGoogleSignInClick: () -> Unit = {},
//    onPhoneSignInClick: () -> Unit = {})
//{
//    val tabs = listOf("Email Login", "Email Sign Up", "Phone Login", "Phone Sign Up")
//    var selectedTab by remember { mutableStateOf(0) }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(horizontal = 24.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("FitMatch", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
//        Spacer(Modifier.height(24.dp))
//
//        // Tabs
//        TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
//            tabs.forEachIndexed { index, title ->
//                Tab(
//                    selected = selectedTab == index,
//                    onClick = { selectedTab = index },
//                    text = { Text(title, fontSize = 13.sp) }
//                )
//            }
//        }
//
//        Spacer(Modifier.height(24.dp))
//
//        when (selectedTab) {
//            0 -> EmailLoginSection(auth, navigationManager, onGoogleSignInClick)
//            1 -> EmailSignUpSection(auth, navigationManager, onGoogleSignInClick)
//            2 -> PhoneLoginSection(auth, navigationManager)
//            3 -> PhoneSignUpSection(auth, navigationManager)
//
//        }
//    }
//}
//
//@Composable
//fun EmailLoginSection(
//    auth: FirebaseAuth,
//    navigationManager: NavigationManager,
//    onGoogleSignInClick: () -> Unit = {}
//    ) {
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var passwordVisible by remember { mutableStateOf(false) }
//    var errorMessage by remember { mutableStateOf("") }
//    var isLoading by remember { mutableStateOf(false) }
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
//        Spacer(Modifier.height(12.dp))
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            singleLine = true,
//            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//            trailingIcon = {
//                val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
//                IconButton(onClick = { passwordVisible = !passwordVisible }) {
//                    Icon(icon, contentDescription = null)
//                }
//            },
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(Modifier.height(20.dp))
//        Button(
//            onClick = {
//                isLoading = true
//                auth.signInWithEmailAndPassword(email, password)
//                    .addOnCompleteListener { task ->
//                        isLoading = false
//                        if (task.isSuccessful) navigationManager.navigateToHomeScreen()
//                        else errorMessage = task.exception?.message ?: "Login failed"
//                    }
//            },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
//        ) {
//            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
//            else Text("Login")
//        }
//
//        if (errorMessage.isNotEmpty()) {
//            Spacer(Modifier.height(8.dp))
//            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
//        }
//
//        Spacer(Modifier.height(16.dp))
//        TextButton(onClick = { navigationManager.navigateToResetPassword() }) {
//            Text("Forgot Password?")
//            Spacer(Modifier.height(24.dp))
//            Divider(thickness = 1.dp)
//            Spacer(Modifier.height(12.dp))
//            Text("or continue with", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//            Spacer(Modifier.height(12.dp))
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                IconButton(onClick = onGoogleSignInClick ) {
//                    Image(
//                        painter = painterResource(id = R.drawable.google),
//                        contentDescription = "Google",
//                        modifier = Modifier.size(45.dp)
//                    )
//                }
//
//                IconButton(onClick = { /* Facebook */ }) {
//                    Image(
//                        painter = painterResource(id = R.drawable.facebookshare),
//                        contentDescription = "Facebook",
//                        modifier = Modifier.size(45.dp)
//                    )
//                }
//
//                IconButton(onClick = { /* Apple Sign-in */ }) {
//                    Image(painter = painterResource(
//                        id = R.drawable.apple),
//                        contentDescription = "Apple",
//                        modifier = Modifier.size(45.dp))
//                }
//            }
//        }
//
//   }
//}
//
//
//
//
//@Composable
//fun EmailSignUpSection(
//    auth: FirebaseAuth,
//    navigationManager: NavigationManager,
//    onGoogleSignInClick: () -> Unit = {})
//{
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var confirmPassword by remember { mutableStateOf("") }
//    var passwordVisible by remember { mutableStateOf(false) }
//    var isLoading by remember { mutableStateOf(false) }
//    var message by remember { mutableStateOf("") }
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
//        Spacer(Modifier.height(12.dp))
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//            trailingIcon = {
//                val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
//                IconButton(onClick = { passwordVisible = !passwordVisible }) {
//                    Icon(icon, contentDescription = null)
//                }
//            },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(Modifier.height(12.dp))
//        OutlinedTextField(
//            value = confirmPassword,
//            onValueChange = { confirmPassword = it },
//            label = { Text("Confirm Password") },
//            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(Modifier.height(20.dp))
//        Button(
//            onClick = {
//                if (password != confirmPassword) {
//                    message = "Passwords do not match"
//                    return@Button
//                }
//                isLoading = true
//                auth.createUserWithEmailAndPassword(email, password)
//                    .addOnCompleteListener { task ->
//                        isLoading = false
//                        if (task.isSuccessful) navigationManager.navigateToHomeScreen()
//                        else message = task.exception?.message ?: "Sign-up failed"
//                    }
//            },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
//        ) {
//            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
//            else Text("Create Account")
//        }
//
//        if (message.isNotEmpty()) {
//            Spacer(Modifier.height(8.dp))
//            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
//        }
//        Spacer(Modifier.height(24.dp))
//        Divider(thickness = 1.dp)
//        Spacer(Modifier.height(12.dp))
//        Text("or continue with", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//        Spacer(Modifier.height(12.dp))
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceEvenly,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = onGoogleSignInClick ) {
//                    Image(
//                        painter = painterResource(id = R.drawable.google),
//                        contentDescription = "Google",
//                        modifier = Modifier.size(45.dp)
//                    )
//                }
//                IconButton(onClick = { /* Facebook */ }) {
//                    Image(
//                        painter = painterResource(id = R.drawable.facebookshare),
//                        contentDescription = "Facebook",
//                        modifier = Modifier.size(45.dp)
//                    )
//                }
//                IconButton(onClick = { /* Apple Sign-in */ }) {
//                    Image(painter = painterResource(
//                        id = R.drawable.apple),
//                        contentDescription = "Apple",
//                        modifier = Modifier.size(45.dp))
//                }
//            }
//        }
//
//
//    }
//
//
//@Composable
//fun PhoneLoginSection(auth: FirebaseAuth, navigationManager: NavigationManager) {
//    var phoneNumber by remember { mutableStateOf("") }
//    var verificationId by remember { mutableStateOf("") }
//    var smsCode by remember { mutableStateOf("") }
//    var codeSent by remember { mutableStateOf(false) }
//    var isLoading by remember { mutableStateOf(false) }
//    val activity = LocalContext.current as Activity
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        if (!codeSent) {
//            OutlinedTextField(
//                value = phoneNumber,
//                onValueChange = { phoneNumber = it },
//                label = { Text("Phone Number") },
//                placeholder = { Text("+254712345678") },
//                singleLine = true,
//                modifier = Modifier.fillMaxWidth()
//            )
//            Spacer(Modifier.height(20.dp))
//            Button(
//                onClick = {
//                    isLoading = true
//                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
//                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
//                            auth.signInWithCredential(credential)
//                            navigationManager.navigateToHomeScreen()
//                        }
//                        override fun onVerificationFailed(e: FirebaseException) {
//                            isLoading = false
//                            e.printStackTrace()
//                        }
//                        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
//                            verificationId = id
//                            codeSent = true
//                            isLoading = false
//                        }
//                    }
//                    val options = PhoneAuthOptions.newBuilder(auth)
//                        .setPhoneNumber(phoneNumber)
//                        .setTimeout(60L, TimeUnit.SECONDS)
//                        .setActivity(activity)
//                        .setCallbacks(callbacks)
//                        .build()
//                    PhoneAuthProvider.verifyPhoneNumber(options)
//                },
//                modifier = Modifier.fillMaxWidth(),
//                enabled = phoneNumber.isNotBlank() && !isLoading
//            ) {
//                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
//                else Text("Send Code")
//            }
//        } else {
//            OutlinedTextField(value = smsCode, onValueChange = { smsCode = it }, label = { Text("Enter Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
//            Spacer(Modifier.height(20.dp))
//            Button(
//                onClick = {
//                    val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
//                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
//                        if (task.isSuccessful) navigationManager.navigateToHomeScreen()
//                    }
//                },
//                modifier = Modifier.fillMaxWidth(),
//                enabled = smsCode.length == 6
//            ) {
//                Text("Verify Code")
//            }
//        }
//    }
//}
//
//@Composable
//fun PhoneSignUpSection(auth: FirebaseAuth, navigationManager: NavigationManager) {
//    // identical to PhoneLoginSection for now (Firebase treats phone verification as sign-in)
//    PhoneLoginSection(auth, navigationManager)
//}
