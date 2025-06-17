package com.mehmettekin.altingunu.presentation.drawgroupscreen

import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.utils.UiText

data class DrawGroupsState(
    val allGroups: List<DrawGroup> = emptyList(),
    val activeGroups: List<DrawGroup> = emptyList(),
    val completedGroups: List<DrawGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val selectedTab: DrawGroupTab = DrawGroupTab.ACTIVE,
)
enum class DrawGroupTab {
    ACTIVE,     // Aktif çekilişler
    COMPLETED,  // Tamamlanan çekilişler
    ALL         // Tüm çekilişler
}

