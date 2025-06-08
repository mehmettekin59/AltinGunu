package com.mehmettekin.altingunu.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mehmettekin.altingunu.presentation.drawgroupscreen.DrawGroupsScreen
import com.mehmettekin.altingunu.presentation.screens.enter.EnterScreen
import com.mehmettekin.altingunu.presentation.screens.participants.ParticipantsScreen
import com.mehmettekin.altingunu.presentation.screens.result.ResultScreen
import com.mehmettekin.altingunu.presentation.screens.settings.SettingsScreen
import com.mehmettekin.altingunu.presentation.screens.splash.SplashScreen
import com.mehmettekin.altingunu.presentation.screens.weel.WheelScreen
import com.mehmettekin.altingunu.presentation.screens.invitejoinscreen.InviteJoinScreen


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

        // ✅ YENİ: DrawGroups Ana Ekranı
        composable(route = Screen.DrawGroups.route) {
            DrawGroupsScreen(navController = navController)
        }

        // ✅ GÜNCELLENDİ: GroupId parametresi eklendi
        composable(
            route = Screen.Participants.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: "new"
            ParticipantsScreen(
                navController = navController,
                groupId = groupId
            )
        }

        // ✅ GÜNCELLENDİ: GroupId parametresi eklendi
        composable(
            route = Screen.Wheel.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            WheelScreen(
                navController = navController,
                groupId = groupId
            )
        }

        // ✅ GÜNCELLENDİ: GroupId parametresi eklendi
        composable(
            route = Screen.Results.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ResultScreen(
                navController = navController,
                groupId = groupId
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }


        // ✅ YENİ: Davet linki ekranı
        composable(
            route = Screen.InviteJoin.route,
            arguments = listOf(navArgument("inviteCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val inviteCode = backStackEntry.arguments?.getString("inviteCode") ?: ""
            InviteJoinScreen(
                navController = navController,
            )
        }
    }
}