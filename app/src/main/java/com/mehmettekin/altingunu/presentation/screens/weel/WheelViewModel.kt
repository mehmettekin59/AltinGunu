package com.mehmettekin.altingunu.presentation.screens.weel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.formatDecimalValue

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WheelViewModel @Inject constructor(
    private val drawRepository: DrawRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WheelState())
    val state = _state.asStateFlow()

    // UI states
    var participants by mutableStateOf<List<Participant>>(emptyList())
        private set
    var winners by mutableStateOf<List<String>>(emptyList())
        private set
    var winnerParticipants by mutableStateOf<List<Participant>>(emptyList())
        private set
    var rotation by mutableFloatStateOf(0f)
        private set
    var isSpinning by mutableStateOf(false)
        private set
    var winner by mutableStateOf<String?>(null)
        private set

    // Settings related to the draw
    private var drawSettings: ParticipantsScreenWholeInformation? = null

    init {
        loadParticipants()
        loadDrawSettings()
    }

    private fun loadParticipants() {
        viewModelScope.launch {
            when (val result = drawRepository.getParticipants()) {
                is ResultState.Success -> {
                    val participantsList = result.data
                    participants = participantsList
                    _state.value = _state.value.copy(
                        participants = participantsList.map { it.name },
                        isLoading = false
                    )
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                ResultState.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
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
                    drawSettings = result.data
                    _state.value = _state.value.copy(
                        drawSettings = result.data,
                        isLoading = false
                    )
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                ResultState.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
                ResultState.Idle -> {
                    // No action needed
                }
            }
        }
    }

    fun spinWheel() {
        if (isSpinning || participants.isEmpty() || participants.size <= 1) return
        isSpinning = true
        winner = null
    }

    fun updateRotation(newRotation: Float) {
        rotation = newRotation
    }

    fun finishSpin(finalRotation: Float) {
        rotation = finalRotation
        val remainingParticipantNames = _state.value.participants

        if (remainingParticipantNames.isNotEmpty()) {
            val sliceAngle = 360f / remainingParticipantNames.size
            val pointerAngle = (finalRotation % 360f)
            val normalizedAngle = (360f - pointerAngle + 90f) % 360f
            val winnerIndex = (normalizedAngle / sliceAngle).toInt()
            val selectedWinner = remainingParticipantNames[winnerIndex % remainingParticipantNames.size]

            addWinner(selectedWinner)

            // Update remaining participants
            val updatedParticipants = remainingParticipantNames.toMutableList()
            updatedParticipants.remove(selectedWinner)

            _state.value = _state.value.copy(
                participants = updatedParticipants
            )
        }

        isSpinning = false
    }

    private fun addWinner(winnerName: String) {
        val updatedWinners = winners.toMutableList()
        updatedWinners.add(winnerName)
        winners = updatedWinners

        // Find the corresponding participant object
        val participant = participants.find { it.name == winnerName }
        participant?.let {
            val updatedWinnerParticipants = winnerParticipants.toMutableList()
            updatedWinnerParticipants.add(it)
            winnerParticipants = updatedWinnerParticipants
        }

        winner = winnerName

        // State'i de güncelleyin
        _state.value = _state.value.copy(
            winners = winners,  // State'deki winners listesini güncelle
            participants = _state.value.participants.filter { it != winnerName },// Bu da eklendi silebilirsin
            currentWinner = winnerName
        )
    }

    fun handleLastParticipant() {
        val remainingParticipants = _state.value.participants
        if (remainingParticipants.size == 1 && !isSpinning) {
            remainingParticipants.firstOrNull()?.let {
                addWinner(it)
                _state.value = _state.value.copy(
                    participants = emptyList()
                )
            }
        }
    }

    fun reset() {
        loadParticipants() // Reload from DataStore
        winners = emptyList()
        winnerParticipants = emptyList()
        rotation = 0f
        isSpinning = false
        winner = null
    }

    fun saveResults() {
        viewModelScope.launch {
            if (winnerParticipants.isEmpty() || drawSettings == null) return@launch

            val settings = drawSettings!!
            val results = createDrawResults(winnerParticipants, settings)

            when (val saveResult = drawRepository.saveDrawResults(results)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        resultsSaved = true
                    )
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        error = saveResult.message
                    )
                }
                ResultState.Loading, ResultState.Idle -> {
                    // No action needed
                }
            }
        }
    }

    private fun createDrawResults(
        winners: List<Participant>,
        settings: ParticipantsScreenWholeInformation
    ): List<DrawResult> {
        val results = mutableListOf<DrawResult>()

        // Calculate the payment amount based on settings
        val amountPerPerson = if (settings.participantCount <= settings.durationMonths) {
            // Each person gets the full monthly amount
            settings.monthlyAmount
        } else {
            // Each person gets a share of the monthly amount
            settings.monthlyAmount * settings.durationMonths / settings.participantCount
        }

        // Format for currency and gold

        val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.getDefault()))

// Format amount based on type - formatDecimalValue kullanarak
        val formattedAmount = when (settings.itemType) {
            ItemType.TL -> {
                val formattedValue = formatDecimalValue(amountPerPerson.toString(), decimalFormat)
                "$formattedValue ₺"
            }
            ItemType.CURRENCY, ItemType.GOLD -> {
                val format = if (settings.itemType == ItemType.GOLD) {
                    DecimalFormat("#,##", DecimalFormatSymbols.getInstance(Locale.getDefault()))
                } else {
                    decimalFormat
                }

                val formattedValue = formatDecimalValue(amountPerPerson.toString(), format)
                "$formattedValue ${settings.specificItem}"
            }
        }


        // Starting month and year
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, settings.startYear)
        calendar.set(Calendar.MONTH, settings.startMonth - 1) // 0-based month

        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // Generate results for each winner
        winners.forEachIndexed { index, participant ->
            // Move calendar to the correct month for this participant
            if (index > 0) {
                calendar.add(Calendar.MONTH, 1)
            }

            val monthName = dateFormat.format(calendar.time)

            results.add(
                DrawResult(
                    participantId = participant.id,
                    participantName = participant.name,
                    month = monthName,
                    amount = formattedAmount
                )
            )
        }

        return results
    }
}


