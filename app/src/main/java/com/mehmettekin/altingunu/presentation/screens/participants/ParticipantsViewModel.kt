package com.mehmettekin.altingunu.presentation.screens.participants



import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
import com.mehmettekin.altingunu.domain.usecase.ValidateDrawSettingsUseCase
import com.mehmettekin.altingunu.domain.usecase.ValidateParticipantsUseCase
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import com.mehmettekin.altingunu.utils.ValueFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParticipantsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val drawRepository: DrawRepository,
    private val drawGroupRepository: DrawGroupRepository,
    private val kapaliCarsiRepository: KapaliCarsiRepository,
    private val validateParticipantsUseCase: ValidateParticipantsUseCase,
    private val validateDrawSettingsUseCase: ValidateDrawSettingsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ParticipantsState())
    val state: StateFlow<ParticipantsState> = _state.asStateFlow()

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""

    init {
        loadCurrencyAndGoldOptions()
        loadExistingGroup()
    }

    private fun loadCurrencyAndGoldOptions() {
        _state.update { currentState ->
            currentState.copy(
                currencyOptions = Constraints.currencyCodeList,
                goldOptions = Constraints.goldCodeList
            )
        }
    }

    private fun loadExistingGroup() {
        if (groupId.isEmpty() || groupId == "new") return

        viewModelScope.launch {
            when (val result = drawGroupRepository.getDrawGroupById(groupId)) {
                is ResultState.Success -> {
                    result.data?.let { group ->
                        _state.update { currentState ->
                            currentState.copy(
                                groupId = groupId,
                                groupName = group.name,
                                groupDescription = group.description,
                                participants = group.participants,
                                participantCount = group.participants.size.toString()
                            )
                        }
                    }
                }
                is ResultState.Error -> {
                    _state.update { currentState ->
                        currentState.copy(error = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun onEvent(event: ParticipantsEvent) {
        when (event) {
            is ParticipantsEvent.OnParticipantCountChange -> {
                _state.update { currentState ->
                    currentState.copy(participantCount = event.count)
                }
            }
            is ParticipantsEvent.OnItemTypeSelect -> {
                _state.update { currentState ->
                    currentState.copy(
                        selectedItemType = event.type,
                        selectedSpecificItem = if (event.type == ItemType.TL) "" else currentState.selectedSpecificItem
                    )
                }
            }
            is ParticipantsEvent.OnSpecificItemSelect -> {
                _state.update { currentState ->
                    currentState.copy(selectedSpecificItem = event.item)
                }
            }
            is ParticipantsEvent.OnMonthlyAmountChange -> {
                _state.update { currentState ->
                    currentState.copy(monthlyAmount = event.amount)
                }
            }
            is ParticipantsEvent.OnDurationChange -> {
                _state.update { currentState ->
                    currentState.copy(durationMonths = event.duration)
                }
            }
            is ParticipantsEvent.OnStartMonthSelect -> {
                _state.update { currentState ->
                    currentState.copy(startMonth = event.month)
                }
            }
            is ParticipantsEvent.OnStartYearSelect -> {
                _state.update { currentState ->
                    currentState.copy(startYear = event.year)
                }
            }
            is ParticipantsEvent.OnStartDaySelect -> {
                _state.update { currentState ->
                    currentState.copy(startDay = event.day)
                }
            }
            is ParticipantsEvent.OnContinueClick -> {
                _state.update { it.copy(isShowingConfirmDialog = true) }
            }
            is ParticipantsEvent.OnConfirmDialogConfirm -> {
                saveParticipantsAndSettings()
            }
            is ParticipantsEvent.OnConfirmDialogDismiss -> {
                _state.update { currentState ->
                    currentState.copy(isShowingConfirmDialog = false)
                }
            }
            is ParticipantsEvent.OnErrorDismiss -> {
                _state.update { currentState ->
                    currentState.copy(error = null)
                }
            }
        }
    }

    private fun saveParticipantsAndSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val currentState = _state.value
            val participantCount = currentState.participantCount.toIntOrNull() ?: 0
            val monthlyAmount = currentState.monthlyAmount.toDoubleOrNull() ?: 0.0
            val durationMonths = currentState.durationMonths.toIntOrNull() ?: 0
            val participants = currentState.participants

            // Get current price for currency/gold
            var currentFormattedPrice: String? = null
            if (currentState.selectedItemType != ItemType.TL && currentState.selectedSpecificItem.isNotBlank()) {
                kapaliCarsiRepository.getExchangeRates().collect { ratesResult ->
                    if (ratesResult is ResultState.Success) {
                        val rate = ratesResult.data.find { it.code == currentState.selectedSpecificItem }
                        rate?.let {
                            currentFormattedPrice = ValueFormatter.formatWithSymbol(
                                it.satis,
                                currentState.selectedItemType,
                                currentState.selectedSpecificItem
                            )
                        }
                    }
                }
            }

            val settings = ParticipantsScreenWholeInformation(
                participantCount = participantCount,
                participants = participants,
                itemType = currentState.selectedItemType,
                specificItem = currentState.selectedSpecificItem,
                monthlyAmount = monthlyAmount,
                durationMonths = durationMonths,
                startMonth = currentState.startMonth,
                startYear = currentState.startYear,
                startDay = currentState.startDay,
                currentFormattedPrice = currentFormattedPrice
            )

            // Validate settings
            val validationResult = validateDrawSettingsUseCase(settings)
            if (validationResult is ResultState.Error) {
                _state.update {
                    it.copy(
                        error = validationResult.message,
                        isLoading = false,
                        isShowingConfirmDialog = false
                    )
                }
                return@launch
            }

            // Update existing group
            if (groupId.isNotEmpty() && groupId != "new") {
                when (val getResult = drawGroupRepository.getDrawGroupById(groupId)) {
                    is ResultState.Success -> {
                        getResult.data?.let { existingGroup ->
                            val updatedGroup = existingGroup.copy(
                                settings = settings,
                                participants = participants,
                                lastModifiedDate = System.currentTimeMillis()
                            )

                            when (val updateResult = drawGroupRepository.updateDrawGroup(updatedGroup)) {
                                is ResultState.Success -> {
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            isShowingConfirmDialog = false,
                                        )
                                    }
                                }
                                is ResultState.Error -> {
                                    _state.update {
                                        it.copy(
                                            error = updateResult.message,
                                            isLoading = false,
                                            isShowingConfirmDialog = false
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    is ResultState.Error -> {
                        _state.update {
                            it.copy(
                                error = getResult.message,
                                isLoading = false,
                                isShowingConfirmDialog = false
                            )
                        }
                    }
                    else -> {}
                }
            } else {
                // Create new group
                return@launch
            }
        }
    }
}