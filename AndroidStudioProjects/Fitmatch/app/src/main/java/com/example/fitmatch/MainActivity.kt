package com.example.fitmatch

import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.screens.*
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private lateinit var navigationManager: NavigationManager

    // ✅ Google One Tap result handler
    private val oneTapLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCred).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FitMatchAuth", "✅ Google/Firebase sign-in success")
                        // 🔥 Automatically navigate to Home after Google sign-in
                        navigationManager.navigateToHomeScreen()
                    } else {
                        Log.e("FitMatchAuth", "❌ Firebase sign-in failed", task.exception)
                    }
                }
            }
        } catch (e: ApiException) {
            Log.e("FitMatchAuth", "One Tap error: ${e.localizedMessage}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                navigationManager = remember { NavigationManager(navController) }

                Surface {
                    NavHost(navController, startDestination = "splash") {

                        composable("splash") { SplashScreen(navController) }
                        composable("onboarding") {
                            OnboardingScreen(
                                navigationManager = navigationManager
                            )
                        }
                        composable("HomeScreen") {
                            HomeScreen(navigationManager = navigationManager)
                        }

                        composable("signin") {
                            LoginScreen(
                                navigationManager = navigationManager,
                                auth = auth,
                                onGoogleSignInClick = { launchGoogleOneTap() },
                                onAppleSignInClick = { /* optional */ }
                            )
                        }
                        composable("SignUp") {
                            SignUpScreen(
                                navigationManager = navigationManager,
                                auth = auth,
                                onGoogleSignInClick = { launchGoogleOneTap() },
                                onAppleSignInClick = { /* optional */ }
                            )
                        }



                        composable("ResetPassword") {
                            ResetPasswordScreen(navigationManager = navigationManager)
                        }


                        composable("GoalScreen") {
                            GoalScreen(auth = auth, navigationManager = navigationManager)
                        }
                    }
                }
            }
        }
    }
    // ✅ Launch Google One Tap flow
    private fun launchGoogleOneTap() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                val request = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                oneTapLauncher.launch(request)
            }
            .addOnFailureListener { e ->
                Log.e("FitMatchAuth", "Sign-in failed: ${e.localizedMessage}")
                // 🔁 Fallback to Sign Up flow if user has no saved credentials
                val signUpRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(getString(R.string.default_web_client_id))
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .build()
                oneTapClient.beginSignIn(signUpRequest)
                    .addOnSuccessListener { result ->
                        val request =
                            IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        oneTapLauncher.launch(request)
                    }
                    .addOnFailureListener { ex ->
                        Log.e("FitMatchAuth", "Sign-up failed: ${ex.localizedMessage}")
                    }
            }
    }

    // --- SplashScreen (unchanged) ---
    @Composable
    fun SplashScreen(navController: NavHostController) {
        LaunchedEffect(Unit) {
            delay(1500)
            navController.navigate("onboarding") {
                popUpTo(0)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dumbell),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("FitMatch", fontWeight = FontWeight.Bold, fontSize = (24.sp))
                Text("Your Personalized Fitness Journey", fontSize = (12.sp))
            }
        }
    }
}