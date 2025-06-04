package com.mehmettekin.altingunu.presentation.drawgroupscreen

sealed class DrawGroupsNavigation {
    data class ToParticipants(val groupId: String) : DrawGroupsNavigation()
    data class ToCreateNewGroup(val name: String, val description: String) : DrawGroupsNavigation()
}