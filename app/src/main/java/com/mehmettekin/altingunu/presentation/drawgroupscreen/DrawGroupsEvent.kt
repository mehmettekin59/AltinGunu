package com.mehmettekin.altingunu.presentation.drawgroupscreen

import com.mehmettekin.altingunu.domain.model.DrawGroup

sealed class DrawGroupsEvent {
    data class OnTabChanged(val tab: DrawGroupTab) : DrawGroupsEvent()
    data class OnGroupClick(val group: DrawGroup) : DrawGroupsEvent()
    data class OnDeleteGroup(val group: DrawGroup) : DrawGroupsEvent()
    data class OnCreateNewGroup(val name: String, val description: String) : DrawGroupsEvent()
    data class OnGroupNameChanged(val name: String) : DrawGroupsEvent()
    data class OnGroupDescriptionChanged(val description: String) : DrawGroupsEvent()
    data object OnShowCreateDialog : DrawGroupsEvent()
    data object OnHideCreateDialog : DrawGroupsEvent()
    data object OnConfirmDelete : DrawGroupsEvent()
    data object OnCancelDelete : DrawGroupsEvent()
    data object OnErrorDismiss : DrawGroupsEvent()
    data object OnRefresh : DrawGroupsEvent()
}