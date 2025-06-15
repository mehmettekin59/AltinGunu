package com.mehmettekin.altingunu.presentation.drawgroupdetailscreen

import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.model.InvitedParticipant

data class DrawGroupDetailState(
    val drawGroup: DrawGroup? = null,
    val pendingRequests: List<InvitedParticipant> = emptyList(),
    val inviteCode: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val showDeleteDialog: Boolean = false
)
