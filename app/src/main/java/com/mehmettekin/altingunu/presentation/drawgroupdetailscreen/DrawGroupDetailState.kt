package com.mehmettekin.altingunu.presentation.drawgroupdetailscreen

import com.mehmettekin.altingunu.domain.model.DrawGroup

data class DrawGroupDetailState(
    val drawGroup: DrawGroup? = null,
        val pendingRequests: List<ParticipationRequest> = emptyList(),
    val inviteCode: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
)
