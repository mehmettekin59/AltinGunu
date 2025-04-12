package com.mehmettekin.altingunu.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(route = Screen.Enter.route) {
            EnterScreen(navController = navController)
        }

        composable(route = Screen.Participants.route) {
            ParticipantsScreen(navController = navController)
        }

        composable(route = Screen.Wheel.route) {
            WheelScreen(navController = navController)
        }

        composable(route = Screen.Results.route) {
            ResultScreen(navController = navController)
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}