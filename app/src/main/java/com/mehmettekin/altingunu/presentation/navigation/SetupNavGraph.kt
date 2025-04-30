package com.mehmettekin.altingunu.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mehmettekin.altingunu.presentation.screens.enter.EnterScreen
import com.mehmettekin.altingunu.presentation.screens.participants.ParticipantsScreen
import com.mehmettekin.altingunu.presentation.screens.result.ResultScreen
import com.mehmettekin.altingunu.presentation.screens.settings.SettingsScreen
import com.mehmettekin.altingunu.presentation.screens.splash.SplashScreen
import com.mehmettekin.altingunu.presentation.screens.weel.WheelScreen


@Composable
fun SetupNavGraph(modifier: Modifier,navController: NavHostController) {
    NavHost(
        modifier = modifier,
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