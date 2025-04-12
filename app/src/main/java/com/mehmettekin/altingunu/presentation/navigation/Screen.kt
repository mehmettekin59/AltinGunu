package com.mehmettekin.altingunu.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Enter : Screen("enter_screen")
    object Participants : Screen("participants_screen")
    object Wheel : Screen("wheel_screen")
    object Results : Screen("results_screen")
    object Settings : Screen("settings_screen")
}