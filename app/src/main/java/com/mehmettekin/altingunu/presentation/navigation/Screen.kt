package com.mehmettekin.altingunu.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Enter : Screen("enter_screen")
    object DrawGroups : Screen("draw_groups_screen")

    object DrawGroupDetail : Screen("draw_group_detail/{groupId}") {
        fun createRoute(groupId: String) = "draw_group_detail/$groupId"
    }

    object ParticipantMethod : Screen("participant_method/{groupName}/{groupDescription}") {
        fun createRoute(groupName: String, groupDescription: String) =
            "participant_method/${Uri.encode(groupName)}/${Uri.encode(groupDescription)}"
    }

    object Participants : Screen("participants_screen/{groupId}") {
        fun createRoute(groupId: String = "new") = "participants_screen/$groupId"
    }

    object Wheel : Screen("wheel_screen/{groupId}") {
        fun createRoute(groupId: String) = "wheel_screen/$groupId"
    }

    object Results : Screen("results_screen/{groupId}") {
        fun createRoute(groupId: String) = "results_screen/$groupId"
    }

    object Settings : Screen("settings_screen")
}