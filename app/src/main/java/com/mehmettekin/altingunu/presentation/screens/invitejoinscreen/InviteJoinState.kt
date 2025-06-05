package com.mehmettekin.altingunu.presentation.screens.invitejoinscreen

import com.mehmettekin.altingunu.domain.model.DrawInvitation


data class InviteJoinState(
    val invitation: DrawInvitation? = null,
    val participantName: String = "",
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val joinSuccess: Boolean = false,
    val error: String? = null
)
