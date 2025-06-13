package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen

import com.mehmettekin.altingunu.domain.model.InvitedParticipant
import com.mehmettekin.altingunu.utils.UiText

data class ParticipantMethodState(
    val groupName: String = "",
    val groupDescription: String = "",
    val inviteCode: String? = null,
    val invitedParticipants: List<InvitedParticipant> = emptyList(),
    val groupId: String? = null,
    val isGroupCreated: Boolean = false,
    val showInviteDialog: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null
)
