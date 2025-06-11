package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen

import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.utils.UiText

data class ParticipantMethodState(
    val selectedMethod: ParticipantMethod = ParticipantMethod.MANUAL,
    val groupName: String = "",
    val groupDescription: String = "",
    val manualParticipants: List<Participant> = emptyList(),
    val inviteCode: String? = null,
    val showInviteDialog: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null
)
