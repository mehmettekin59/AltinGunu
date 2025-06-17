package com.mehmettekin.altingunu.presentation.drawgroupscreen

import com.mehmettekin.altingunu.domain.model.DrawGroup

sealed class DrawGroupsEvent {
    data class OnTabChanged(val tab: DrawGroupTab) : DrawGroupsEvent()
    data object OnErrorDismiss : DrawGroupsEvent()
    data object OnRefresh : DrawGroupsEvent()
}