package com.mehmettekin.altingunu.presentation.drawgroupscreen

import com.mehmettekin.altingunu.domain.model.DrawGroup

sealed class DrawGroupsEvent {
    data class OnTabChanged(val tab: DrawGroupTab) : DrawGroupsEvent()
    data class OnGroupClick(val group: DrawGroup) : DrawGroupsEvent()
    data class OnDeleteGroup(val group: DrawGroup) : DrawGroupsEvent()
    data object OnConfirmDelete : DrawGroupsEvent()
    data object OnCancelDelete : DrawGroupsEvent()
    data object OnErrorDismiss : DrawGroupsEvent()
    data object OnRefresh : DrawGroupsEvent()
}