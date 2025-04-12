package com.mehmettekin.altingunu.presentation.screens.participants

import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant

sealed class ParticipantsEvent {
    data class OnParticipantCountChange(val count: String) : ParticipantsEvent()
    data class OnItemTypeSelect(val type: ItemType) : ParticipantsEvent()
    data class OnSpecificItemSelect(val item: String) : ParticipantsEvent()
    data class OnMonthlyAmountChange(val amount: String) : ParticipantsEvent()
    data class OnDurationChange(val duration: String) : ParticipantsEvent()
    data class OnStartMonthSelect(val month: Int) : ParticipantsEvent()
    data class OnStartYearSelect(val year: Int) : ParticipantsEvent()
    data class OnAddParticipant(val name: String) : ParticipantsEvent()
    data class OnRemoveParticipant(val participant: Participant) : ParticipantsEvent()
    data object OnContinueClick : ParticipantsEvent()
    data object OnConfirmDialogConfirm : ParticipantsEvent()
    data object OnConfirmDialogDismiss : ParticipantsEvent()
    data object OnErrorDismiss : ParticipantsEvent()
}