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
    //member navigation
    fun navigateToMember(){}

    //member notification
    fun navigateToNotifications(){}

    //navigate to language
    fun navigateToLanguage(){}
    fun navigateToCountry(){}
    fun navigateToClearCache(){}
    fun navigateToEditProfile(){}
    fun navigateToLegal(){}
    fun navigateToHelp(){}
    fun navigateToAboutUs(){}


    //Navigate to Payment
    fun navigateToPayment(){
        navController.navigate("payment") {
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

    //navigate to downloads screen
    fun navigateToDownloadsScreen() {
        navController.navigate("downloads") {
            launchSingleTop = true
        }
    }

    // navigate to search screen
    fun navigateToSearchScreen() {
        navController.navigate("search") {
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
