package com.mehmettekin.altingunu.presentation.screens.participants

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.domain.usecase.ValidateDrawSettingsUseCase
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
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
    savedStateHandle: SavedStateHandle,
    private val drawRepository: DrawRepository,
    private val validateDrawSettingsUseCase: ValidateDrawSettingsUseCase,
    private val drawGroupRepository: DrawGroupRepository
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""

    private val _state = MutableStateFlow(ParticipantsState())
    val state: StateFlow<ParticipantsState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent: SharedFlow<Unit> = _navigationEvent.asSharedFlow()

    private var currentDrawGroup: DrawGroup? = null

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

        // Load group data
        loadDrawGroup()
    }

    private fun loadDrawGroup() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = drawGroupRepository.getDrawGroupById(groupId)) {
                is ResultState.Success -> {
                    result.data?.let { group ->
                        currentDrawGroup = group
                        _state.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                participants = group.participants,
                                participantCount = group.participants.size.toString(),
                                groupName = group.name,
                                groupDescription = group.description,
                                // Load existing settings if available
                                selectedItemType = group.settings.itemType,
                                selectedSpecificItem = group.settings.specificItem,
                                monthlyAmount = group.settings.monthlyAmount.toString().ifEmpty { "" },
                                durationMonths = group.settings.durationMonths.toString().ifEmpty { "" },
                                startDay = group.settings.startDay,
                                startMonth = group.settings.startMonth,
                                startYear = group.settings.startYear
                            )
                        }
                    }
                }
                is ResultState.Error -> {
                    _state.update { it.copy(
                        isLoading = false,
                        error = result.message
                    ) }
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onEvent(event: ParticipantsEvent) {
        when (event) {
            is ParticipantsEvent.OnParticipantCountChange -> handleParticipantCountChange(event.count)
            is ParticipantsEvent.OnItemTypeSelect -> handleItemTypeSelect(event.type)
            is ParticipantsEvent.OnSpecificItemSelect -> handleSpecificItemSelect(event.item)
            is ParticipantsEvent.OnMonthlyAmountChange -> handleMonthlyAmountChange(event.amount)
            is ParticipantsEvent.OnDurationChange -> handleDurationChange(event.duration)
            is ParticipantsEvent.OnStartDaySelect -> handleStartDaySelect(event.day)
            is ParticipantsEvent.OnStartMonthSelect -> handleStartMonthSelect(event.month)
            is ParticipantsEvent.OnStartYearSelect -> handleStartYearSelect(event.year)
            is ParticipantsEvent.OnContinueClick -> handleContinueClick()
            is ParticipantsEvent.OnConfirmDialogConfirm -> handleConfirmDialogConfirm()
            is ParticipantsEvent.OnConfirmDialogDismiss -> handleConfirmDialogDismiss()
            is ParticipantsEvent.OnErrorDismiss -> handleErrorDismiss()
            // Removed: OnAddParticipant and OnRemoveParticipant events
        }
    }

    private fun handleParticipantCountChange(count: String) {
        // This is now read-only, participants are managed in ParticipantMethodScreen
        // Do nothing or show a message that participants can't be changed here
    }

    private fun handleItemTypeSelect(type: ItemType) {
        _state.update { it.copy(
            selectedItemType = type,
            selectedSpecificItem = ""
        ) }
    }

    private fun handleSpecificItemSelect(item: String) {
        _state.update { it.copy(selectedSpecificItem = item) }
    }

    private fun handleMonthlyAmountChange(amount: String) {
        if (amount.isEmpty() || amount.toDoubleOrNull() != null) {
            _state.update { it.copy(monthlyAmount = amount) }
        }
    }

    private fun handleDurationChange(duration: String) {
        if (duration.isEmpty() || duration.toIntOrNull() != null) {
            _state.update { it.copy(durationMonths = duration) }
        }
    }

    private fun handleStartDaySelect(day: Int) {
        _state.update { it.copy(startDay = day) }
    }

    private fun handleStartMonthSelect(month: Int) {
        _state.update { it.copy(startMonth = month) }
    }

    private fun handleStartYearSelect(year: Int) {
        _state.update { it.copy(startYear = year) }
    }

    private fun handleContinueClick() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Validate settings
            val participantCount = _state.value.participants.size
            val monthlyAmount = _state.value.monthlyAmount.toDoubleOrNull() ?: 0.0
            val durationMonths = _state.value.durationMonths.toIntOrNull() ?: 0

            val settings = ParticipantsScreenWholeInformation(
                participantCount = participantCount,
                participants = _state.value.participants,
                itemType = _state.value.selectedItemType,
                specificItem = _state.value.selectedSpecificItem,
                monthlyAmount = monthlyAmount,
                durationMonths = durationMonths,
                startDay = _state.value.startDay,
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

            currentDrawGroup?.let { group ->
                // Update group with new settings
                val monthlyAmount = _state.value.monthlyAmount.toDoubleOrNull() ?: 0.0
                val durationMonths = _state.value.durationMonths.toIntOrNull() ?: 0

                val updatedSettings = ParticipantsScreenWholeInformation(
                    participantCount = group.participants.size,
                    participants = group.participants,
                    itemType = _state.value.selectedItemType,
                    specificItem = _state.value.selectedSpecificItem,
                    monthlyAmount = monthlyAmount,
                    durationMonths = durationMonths,
                    startDay = _state.value.startDay,
                    startMonth = _state.value.startMonth,
                    startYear = _state.value.startYear
                )

                val updatedGroup = group.copy(
                    settings = updatedSettings,
                    lastModifiedDate = System.currentTimeMillis()
                )

                when (val updateResult = drawGroupRepository.updateDrawGroup(updatedGroup)) {
                    is ResultState.Success -> {
                        // Also save to legacy draw repository for compatibility
                        drawRepository.saveDrawSettings(updatedSettings)
                        drawRepository.saveParticipants(group.participants)

                        _state.update { it.copy(
                            isLoading = false,
                            isShowingConfirmDialog = false
                        ) }

                        _navigationEvent.emit(Unit)
                    }
                    is ResultState.Error -> {
                        _state.update { it.copy(
                            isLoading = false,
                            error = updateResult.message,
                            isShowingConfirmDialog = false
                        ) }
                    }
                    else -> {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun handleConfirmDialogDismiss() {
        _state.update { it.copy(isShowingConfirmDialog = false) }
    }

    private fun handleErrorDismiss() {
        _state.update { it.copy(error = null) }
    }
}