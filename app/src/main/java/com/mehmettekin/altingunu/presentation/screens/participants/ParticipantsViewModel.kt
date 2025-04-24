package com.mehmettekin.altingunu.presentation.screens.participants

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.domain.usecase.ValidateDrawSettingsUseCase
import com.mehmettekin.altingunu.domain.usecase.ValidateParticipantsUseCase
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ParticipantsViewModel @Inject constructor(
    private val drawRepository: DrawRepository,
    private val validateDrawSettingsUseCase: ValidateDrawSettingsUseCase,
    private val validateParticipantsUseCase: ValidateParticipantsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ParticipantsState())
    val state: StateFlow<ParticipantsState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent: SharedFlow<Unit> = _navigationEvent.asSharedFlow()

    init {
        // Set current month and year as default
        val currentDate = YearMonth.now()
        _state.update { it.copy(
            startMonth = currentDate.monthValue,
            startYear = currentDate.year
        ) }

        // Set currency and gold options from Constants
        _state.update { it.copy(
            currencyOptions = Constraints.currencyCodeList,
            goldOptions = Constraints.goldCodeList
        ) }
    }

    fun onEvent(event: ParticipantsEvent) {
        when (event) {
            is ParticipantsEvent.OnParticipantCountChange -> handleParticipantCountChange(event.count)
            is ParticipantsEvent.OnAddParticipant -> handleAddParticipant(event.name)
            is ParticipantsEvent.OnRemoveParticipant -> handleRemoveParticipant(event.participant)
            is ParticipantsEvent.OnItemTypeSelect -> handleItemTypeSelect(event.type)
            is ParticipantsEvent.OnSpecificItemSelect -> handleSpecificItemSelect(event.item)
            is ParticipantsEvent.OnMonthlyAmountChange -> handleMonthlyAmountChange(event.amount)
            is ParticipantsEvent.OnDurationChange -> handleDurationChange(event.duration)
            is ParticipantsEvent.OnStartMonthSelect -> handleStartMonthSelect(event.month)
            is ParticipantsEvent.OnStartYearSelect -> handleStartYearSelect(event.year)
            is ParticipantsEvent.OnContinueClick -> handleContinueClick()
            is ParticipantsEvent.OnConfirmDialogConfirm -> handleConfirmDialogConfirm()
            is ParticipantsEvent.OnConfirmDialogDismiss -> handleConfirmDialogDismiss()
            is ParticipantsEvent.OnErrorDismiss -> handleErrorDismiss()
        }
    }

    private fun handleParticipantCountChange(count: String) {
        if (count.isEmpty() || count.toIntOrNull() != null) {
            _state.update { it.copy(participantCount = count) }
        }
    }

    private fun handleAddParticipant(name: String) {
        if (name.isNotBlank()) {
            val updatedParticipants = _state.value.participants.toMutableList()
            updatedParticipants.add(Participant(name = name))

            // Update participant count to match the actual number of participants
            val countStr = (updatedParticipants.size).toString()

            _state.update { it.copy(
                participants = updatedParticipants,
                participantCount = countStr
            ) }
        }
    }

    private fun handleRemoveParticipant(participant: Participant) {
        val updatedParticipants = _state.value.participants.toMutableList()
        updatedParticipants.remove(participant)

        // Update participant count
        val countStr = (updatedParticipants.size).toString()

        _state.update { it.copy(
            participants = updatedParticipants,
            participantCount = countStr
        ) }
    }

    private fun handleItemTypeSelect(type: ItemType) {
        Log.d("ParticipantsViewModel", "Selected ItemType: $type")
        _state.update { it.copy(
            selectedItemType = type,
            // Reset specific item when type changes
            selectedSpecificItem = ""
        ) }
    }

    private fun handleSpecificItemSelect(item: String) {
        _state.update { it.copy(selectedSpecificItem = item) }
    }

    private fun handleMonthlyAmountChange(amount: String) {
        // Only allow valid double format
        if (amount.isEmpty() || amount.toDoubleOrNull() != null) {
            _state.update { it.copy(monthlyAmount = amount) }
        }
    }

    private fun handleDurationChange(duration: String) {
        // Only allow valid integer format
        if (duration.isEmpty() || duration.toIntOrNull() != null) {
            _state.update { it.copy(durationMonths = duration) }
        }
    }

    private fun handleStartMonthSelect(month: Int) {
        _state.update { it.copy(startMonth = month) }
    }

    private fun handleStartYearSelect(year: Int) {
        _state.update { it.copy(startYear = year) }
    }

    private fun handleContinueClick() {
        viewModelScope.launch {
            // Show loading
            _state.update { it.copy(isLoading = true) }

            // Validate participants
            val participantsResult = validateParticipantsUseCase(_state.value.participants)

            if (participantsResult is ResultState.Error) {
                _state.update { it.copy(
                    isLoading = false,
                    error = participantsResult.message
                ) }
                return@launch
            }

            // Validate settings
            val participantCount = _state.value.participantCount.toIntOrNull() ?: 0
            val monthlyAmount = _state.value.monthlyAmount.toDoubleOrNull() ?: 0.0
            val durationMonths = _state.value.durationMonths.toIntOrNull() ?: 0

            val settings = ParticipantsScreenWholeInformation(
                participantCount = participantCount,
                participants = _state.value.participants,
                itemType = _state.value.selectedItemType,
                specificItem = _state.value.selectedSpecificItem,
                monthlyAmount = monthlyAmount,
                durationMonths = durationMonths,
                startMonth = _state.value.startMonth,
                startYear = _state.value.startYear
            )

            val settingsResult = validateDrawSettingsUseCase(settings)

            if (settingsResult is ResultState.Error) {
                _state.update { it.copy(
                    isLoading = false,
                    error = settingsResult.message
                ) }
                return@launch
            }

            // If validation successful, show confirmation dialog
            _state.update { it.copy(
                isLoading = false,
                isShowingConfirmDialog = true
            ) }
        }
    }

    private fun handleConfirmDialogConfirm() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Save settings
            val participantCount = _state.value.participantCount.toIntOrNull() ?: 0
            val monthlyAmount = _state.value.monthlyAmount.toDoubleOrNull() ?: 0.0
            val durationMonths = _state.value.durationMonths.toIntOrNull() ?: 0

            val settings = ParticipantsScreenWholeInformation(
                participantCount = participantCount,
                participants = _state.value.participants,
                itemType = _state.value.selectedItemType,
                specificItem = _state.value.selectedSpecificItem,
                monthlyAmount = monthlyAmount,
                durationMonths = durationMonths,
                startMonth = _state.value.startMonth,
                startYear = _state.value.startYear
            )

            val saveResult = drawRepository.saveDrawSettings(settings)

            if (saveResult is ResultState.Error) {
                _state.update { it.copy(
                    isLoading = false,
                    error = saveResult.message,
                    isShowingConfirmDialog = false
                ) }
                return@launch
            }

            // Save participants
            val saveParticipantsResult = drawRepository.saveParticipants(_state.value.participants)

            if (saveParticipantsResult is ResultState.Error) {
                _state.update { it.copy(
                    isLoading = false,
                    error = saveParticipantsResult.message,
                    isShowingConfirmDialog = false
                ) }
                return@launch
            }

            // Navigate to next screen
            _state.update { it.copy(
                isLoading = false,
                isShowingConfirmDialog = false
            ) }

            _navigationEvent.emit(Unit)
        }
    }

    private fun handleConfirmDialogDismiss() {
        _state.update { it.copy(isShowingConfirmDialog = false) }
    }

    private fun handleErrorDismiss() {
        _state.update { it.copy(error = null) }
    }
}