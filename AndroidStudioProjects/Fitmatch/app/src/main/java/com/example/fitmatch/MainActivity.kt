package com.example.fitmatch

import android.os.Build
import android.util.Log
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitmatch.models.PlanRepositoryImpl
import com.example.fitmatch.navigations.NavigationManager
import com.example.fitmatch.screens.*
import com.example.fitmatch.ui.theme.FitMatchTheme
import com.example.fitmatch.viewmodel.PlanViewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.fitmatch.Viewmodels.GoalsViewModel
import com.example.fitmatch.models.GoalsRepository
import com.example.fitmatch.models.NutritionRepositoryImpl
import com.example.fitmatch.net.FitmatchApi
import com.example.fitmatch.net.NetworkModule
import com.example.fitmatch.viewmodel.NutritionViewModel
import com.example.fitmatch.viewmodel.PlanViewModelFactory

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
                        val u = auth.currentUser
                        val uid = u?.uid
                        if (uid != null) {
                            val db = FirebaseFirestore.getInstance()
                            val profile = mapOf(
                                "display_name" to (u.displayName ?: ""),
                                "email"       to (u.email ?: ""),
                                "photo_url"   to (u.photoUrl?.toString() ?: ""),
                                "created_at"  to com.google.firebase.Timestamp.now()
                            )
                            db.collection("users").document(uid).set(profile)
                                .addOnSuccessListener {
                                    navigationManager.navigateToHomeScreen()
                                }
                                .addOnFailureListener { e ->
                                    // optional: log or show a toast/snackbar
                                    navigationManager.navigateToHomeScreen()
                                }
                        } else {
                            navigationManager.navigateToHomeScreen()
                        }
                    } else {
                        Log.e("FitMatchAuth", "❌ Firebase sign-in failed", task.exception)
                    }
                }
            }
        } catch (e: ApiException) {
            Log.e("FitMatchAuth", "One Tap error: ${e.localizedMessage}")
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
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
            FitMatchTheme{

                val navController = rememberNavController()
                navigationManager = remember { NavigationManager(navController) }
                Surface {
                    val repo = remember {
                        PlanRepositoryImpl(
                            db = FirebaseFirestore.getInstance(),
                            api = NetworkModule.api
                        )
                    }
                    val planFactory = remember { PlanViewModelFactory(repo, auth) }

                    val planVm: PlanViewModel = viewModel(factory = planFactory)
                    NavHost(navController, startDestination = "splash") {

                        composable("splash") { SplashScreen(navController) }
                        composable("onboarding") {
                            OnboardingScreen(
                                navigationManager = navigationManager
                            )
                        }
                        composable("HomeScreen") {
                            HomeScreen(navigationManager = navigationManager, auth = auth)
                        }

                        composable("ProfileScreen") {
                            ProfileScreen(
                                navigationManager = navigationManager,
                                viewModel = viewModel()
                            )
                        }
                        composable(
                            route = "Progress/{planId}",
                            arguments = listOf(
                                navArgument("planId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val planId = backStackEntry.arguments?.getString("planId")!!
                            ProgressScreen(
                                navigationManager = navigationManager,
                                planId = planId
                            )
                        }
                        composable("signin") {
                            LoginScreen(
                                navigationManager = navigationManager,
                                auth = auth,
                                onGoogleSignInClick = { launchGoogleOneTap() },
                                onAppleSignInClick = { /* optional */ }
                            )
                        }

                        composable("PlanScreen") {
                            val goalsFactory = remember {
                                object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                                            ?: error("Not logged in")
                                        return GoalsViewModel(GoalsRepository(uid)) as T
                                    }
                                }
                            }
                            val goalsVm: GoalsViewModel = viewModel(factory = goalsFactory)
                            PlanScreen(
                                navigationManager = navigationManager, planVm = planVm,
                                goalsVm = goalsVm
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
                        composable("GoalScreen") {
                            GoalScreen(
                                auth = auth,
                                navigationManager = navigationManager
                            )
                        }
                        composable("ResetPassword") {
                            ResetPasswordScreen(navigationManager = navigationManager)
                        }
//                        composable("NutritionScreen"){
//                            val planRepo = PlanRepositoryImpl(
//                                    db = FirebaseFirestore.getInstance(),
//                                    api = NetworkModule.api
//                                )
//                            NutritionScreen(planRepo = planRepo, navigationManager = navigationManager)
//                        }
                        // In MainActivity (or wherever your NavHost is defined)


                            // ... other composables

                            composable("NutritionRoute") {
                                // Build deps (you likely already have singletons; keep it consistent)
                                val auth = FirebaseAuth.getInstance()
                                val db = FirebaseFirestore.getInstance()

                                // Your repos (replace impl constructors with yours)

                                val planRepo = remember {
                                    PlanRepositoryImpl(
                                        db = FirebaseFirestore.getInstance(),
                                        api = NetworkModule.api
                                    )
                                }
                                val nutritionRepo = remember { NutritionRepositoryImpl(db) } // your impl

                                val vm: NutritionViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return NutritionViewModel(
                                                auth = auth,
                                                nutritionRepo = nutritionRepo,
                                                planRepo = planRepo
                                            ) as T
                                        }
                                    }
                                )

                                // Kick off the stream once
                                LaunchedEffect(Unit) { vm.startObserving() }

                                NutritionRoute(viewModel = vm, navigationManager = navigationManager)
                            }


                        composable("otp/{vid}/{phone}") { backStack ->
                            val vid = backStack.arguments?.getString("vid") ?: ""
                            val phone = backStack.arguments?.getString("phone") ?: ""
                            OtpVerifyScreen(
                                auth = auth,
                                navigationManager = navigationManager,
                                verificationId = vid,
                                phoneNumber = phone
                            )
                        }
                        composable(
                            route = "workoutLog/{planId}/{day}/{dateMillis}",
                            arguments = listOf(
                                navArgument("planId") { type = NavType.StringType },
                                navArgument("day") { type = NavType.IntType },
                                navArgument("dateMillis") { type = NavType.LongType },
                            )
                        ) { backStackEntry ->
                            val planId = backStackEntry.arguments?.getString("planId")!!
                            val day = backStackEntry.arguments?.getInt("day")!!
                            val dateMillis = backStackEntry.arguments?.getLong("dateMillis")!!

                            WorkoutLogScreen(
                                planId = planId,
                                day = day,
                                dateMillis = dateMillis,
                                navigationManager = navigationManager,
                                 planVm = planVm
                            )
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