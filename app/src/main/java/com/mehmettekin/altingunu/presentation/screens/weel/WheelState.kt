package com.mehmettekin.altingunu.presentation.screens.weel

import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.utils.UiText

data class WheelState(
    val participants: List<String> = emptyList(),
    val remainingParticipants: List<String> = emptyList(),
    val winners: List<String> = emptyList(),
    val currentWinner: String? = null,
    val drawSettings: ParticipantsScreenWholeInformation? = null,
    val isLoading: Boolean = false,
    val isSpinning: Boolean = false,
    val rotation: Float = 0f,
    val error: UiText? = null,
    val resultsSaved: Boolean = false
)
