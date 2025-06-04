package com.mehmettekin.altingunu.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Enter : Screen("enter_screen")
    object DrawGroups : Screen("draw_groups_screen") // ✅ YENİ: Ana çekiliş listesi
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
    object InviteJoin : Screen("invite_join_screen/{inviteCode}") { // ✅ YENİ: Davet ekranı
        fun createRoute(inviteCode: String) = "invite_join_screen/$inviteCode"
    }
}