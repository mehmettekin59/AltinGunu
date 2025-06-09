package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen

import com.mehmettekin.altingunu.utils.UiText

data class ParticipantMethodState(
    val selectedMethod: ParticipantMethod = ParticipantMethod.INVITE,
    val isLoading: Boolean = false,
    val error: UiText? = null
)
