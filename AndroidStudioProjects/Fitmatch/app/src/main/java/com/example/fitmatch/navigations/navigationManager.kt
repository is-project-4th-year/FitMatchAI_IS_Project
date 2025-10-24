package com.example.fitmatch.navigations

import androidx.navigation.NavHostController


class NavigationManager(private val navController: NavHostController) {

    // Navigate to Onboarding Screen
    fun navigateToOnboarding() {
        navController.navigate("onboarding") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
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


    fun navigateToProgress(){
        navController.navigate("ProgressScreen") {
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
