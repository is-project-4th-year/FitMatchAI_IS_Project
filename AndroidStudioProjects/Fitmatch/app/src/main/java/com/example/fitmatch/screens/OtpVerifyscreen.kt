package com.example.fitmatch.screens



import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitmatch.navigations.NavigationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider

@Composable
fun OtpVerifyScreen(
    auth: FirebaseAuth,
    navigationManager: NavigationManager,
    verificationId: String,
    phoneNumber: String
) {
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Verify $phoneNumber", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = otpCode,
            onValueChange = { otpCode = it },
            placeholder = { Text("Enter 6-digit code") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                isLoading = true
                val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        navigationManager.navigateToHomeScreen()
                    } else {
                        task.exception?.printStackTrace()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = otpCode.length == 6 && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            else Text("Verify Code")
        }
    }
}
