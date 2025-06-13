package com.mehmettekin.altingunu.presentation.screens.weel



import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.model.ReminderData
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.ValueFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WheelViewModel @Inject constructor(
    private val drawRepository: DrawRepository,
    private val settingsDataStore: SettingsDataStore,
    private val fcmRepository: FcmRepository,
    private val groupId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(WheelState())
    val state = _state.asStateFlow()

    val participants: List<Participant> get() = _state.value.remainingParticipants

    init {
        loadParticipants()
        loadDrawSettings()
    }

    private fun loadParticipants() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = drawRepository.getParticipants()) {
                is ResultState.Success -> {
                    _state.update { currentState ->
                        currentState.copy(
                            allParticipants = result.data,
                            remainingParticipants = result.data,
                            isLoading = false
                        )
                    }
                }
                is ResultState.Error -> {
                    _state.update { currentState ->
                        currentState.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                }
                ResultState.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
                ResultState.Idle -> {
                    // No action needed
                }
            }
        }
    }

    private fun loadDrawSettings() {
        viewModelScope.launch {
            when (val result = drawRepository.getDrawSettings()) {
                is ResultState.Success -> {
                    _state.update { currentState ->
                        currentState.copy(drawSettings = result.data)
                    }
                }
                is ResultState.Error -> {
                    _state.update { currentState ->
                        currentState.copy(error = result.message)
                    }
                }
                ResultState.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
                ResultState.Idle -> {
                    // No action needed
                }
            }
        }
    }

    fun spinWheel() {
        val currentRemainingParticipants = _state.value.remainingParticipants
        if (_state.value.isSpinning || currentRemainingParticipants.isEmpty() || currentRemainingParticipants.size <= 1) return

        _state.update { currentState ->
            currentState.copy(
                rotation = 0f,
                isSpinning = true,
                currentWinner = null
            )
        }
    }

    fun updateRotation(newRotation: Float) {
        _state.update { currentState ->
            currentState.copy(rotation = newRotation)
        }
    }

    fun finishSpin(finalRotation: Float) {
        val currentRemainingParticipants = _state.value.remainingParticipants

        if (currentRemainingParticipants.isNotEmpty()) {
            val sliceAngle = 360f / currentRemainingParticipants.size
            val pointerAngle = (finalRotation % 360f)
            val normalizedAngle = (360f - pointerAngle + 90f) % 360f
            val winnerIndex = (normalizedAngle / sliceAngle).toInt()
            val selectedWinner = currentRemainingParticipants[winnerIndex % currentRemainingParticipants.size]

            addWinner(selectedWinner)
        }

        _state.update { currentState ->
            currentState.copy(
                rotation = finalRotation,
                isSpinning = false
            )
        }
    }

    private fun addWinner(winnerParticipant: Participant) {
        _state.update { currentState ->
            val updatedWinners = currentState.winners + winnerParticipant.name
            val updatedWinnerParticipants = currentState.winnerParticipants + winnerParticipant
            val updatedRemainingParticipants = currentState.remainingParticipants.filter { it.id != winnerParticipant.id }

            currentState.copy(
                winners = updatedWinners,
                winnerParticipants = updatedWinnerParticipants,
                remainingParticipants = updatedRemainingParticipants,
                currentWinner = winnerParticipant.name
            )
        }
    }

    fun handleLastParticipant() {
        val currentRemainingParticipants = _state.value.remainingParticipants
        if (currentRemainingParticipants.size == 1 && !_state.value.isSpinning) {
            currentRemainingParticipants.firstOrNull()?.let { lastParticipant ->
                addWinner(lastParticipant)
            }
        }
    }

    fun reset() {
        _state.update { currentState ->
            currentState.copy(
                remainingParticipants = currentState.allParticipants,
                winners = emptyList(),
                winnerParticipants = emptyList(),
                rotation = 0f,
                isSpinning = false,
                currentWinner = null
            )
        }
    }

    fun saveResults() {
        viewModelScope.launch {
            val currentWinnerParticipants = _state.value.winnerParticipants
            val currentDrawSettings = _state.value.drawSettings

            if (currentWinnerParticipants.isEmpty() || currentDrawSettings == null) return@launch

            val results = createDrawResults(currentWinnerParticipants, currentDrawSettings)

            when (val saveResult = drawRepository.saveDrawResults(results)) {
                is ResultState.Success -> {
                    // Hatırlatıcıları Firebase'e kaydet
                    saveRemindersToFirebase(results, currentDrawSettings)

                    _state.update { currentState ->
                        currentState.copy(resultsSaved = true)
                    }
                }
                is ResultState.Error -> {
                    _state.update { currentState ->
                        currentState.copy(error = saveResult.message)
                    }
                }
                ResultState.Loading, ResultState.Idle -> {
                    // No action needed
                }
            }
        }
    }
    private suspend fun saveRemindersToFirebase(
        results: List<DrawResult>,
        settings: ParticipantsScreenWholeInformation,
    ) {
        try {

            val reminders = results.map { result ->
                ReminderData(
                    participantName = result.participantName,
                    amount = result.amount,
                    paymentDate = result.month,
                    itemType = settings.itemType.name,
                    specificItem = settings.specificItem
                )
            }

            firestoreService.saveScheduledReminders(groupId, reminders)

            Log.d("WheelViewModel", "Hatırlatıcılar Firebase'e kaydedildi: $groupId")
        } catch (e: Exception) {
            Log.e("WheelViewModel", "Hatırlatıcı kaydetme hatası", e)
        }
    }
}


    private fun createDrawResults(
        winners: List<Participant>,
        settings: ParticipantsScreenWholeInformation
    ): List<DrawResult> {
        val results = mutableListOf<DrawResult>()

        val amountPerPerson = settings.calculateAmountPerPerson()

        val formattedAmount = settings.currentFormattedPrice ?:
        ValueFormatter.formatWithSymbol(
            amountPerPerson.toString(),
            settings.itemType,
            settings.specificItem
        )

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        val peoplePerMonth = settings.participantCount / settings.durationMonths

        winners.forEachIndexed { index, participant ->
            val monthIndex = index / peoplePerMonth
            val (day, month, year) = settings.getNextPaymentDate(monthIndex)

            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, day)
            val dateString = dateFormat.format(calendar.time)

            results.add(
                DrawResult(
                    participantId = participant.id,
                    participantName = participant.name,
                    month = dateString,
                    amount = formattedAmount
                )
            )
        }

        return results
    }
}



