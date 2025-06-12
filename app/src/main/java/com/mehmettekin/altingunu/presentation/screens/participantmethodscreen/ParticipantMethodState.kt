package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen

import com.mehmettekin.altingunu.domain.model.InvitedParticipant
import com.mehmettekin.altingunu.utils.UiText

data class ParticipantMethodState(
    val groupName: String = "",
    val groupDescription: String = "",
    val inviteCode: String? = null,
    val invitedParticipants: List<InvitedParticipant> = emptyList(),
    val tempGroupId: String? = null,
    val showInviteDialog: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null
)
