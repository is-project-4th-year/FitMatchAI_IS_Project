package com.example.fitmatch.navigations

import android.net.Uri
import androidx.navigation.NavHostController


class NavigationManager(private val navController: NavHostController) {

    // Navigate to Onboarding Screen
    fun navigateToOnboarding() {
        navController.navigate("onboarding") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToOtp(verificationId: String, phone: String) {
        val vid = Uri.encode(verificationId)
        val ph  = Uri.encode(phone)
        navController.navigate("otp/$vid/$ph") {
            launchSingleTop = true
        }
    }
    fun openWorkoutLog(planId: String, day: Int, dateMillis: Long = System.currentTimeMillis()) {
        navController.navigate("workoutLog/$planId/$day/$dateMillis")
    }

    fun navigateToGoals(){
        navController.navigate("GoalScreen") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }
    fun navigateToPlan() {
        navController.navigate("PlanScreen") {
            launchSingleTop = true
        }
    }


    fun navigateToProgress(planId: String){
        navController.navigate("Progress/$planId") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToProfile(){
        navController.navigate("ProfileScreen") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }


    // Navigate to Home Screen
    fun navigateToHomeScreen() {
        navController.navigate("HomeScreen") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    //navigate to ProfileScreen
    fun navigateToProfileScreen() {
        navController.navigate("profileScreen") {
            launchSingleTop = true
        }
    }

    // Navigate to SignUp Screen
    fun navigateToSignUp() {
        navController.navigate("SignUp") {
            launchSingleTop = true
        }
    }

    // Navigate to ForgotPassword Screen
    fun navigateToResetPassword() {
        navController.navigate("ResetPassword") {
            launchSingleTop = true
        }
    }

    // Navigate to Login Screen
    fun navigateToLogin() {
        navController.navigate("signIn") {
            launchSingleTop = true
        }
    }


    // Go back to the previous screen
    fun goBack() {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }
}
