package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ParticipantMethodViewModel @Inject constructor(
    private val drawGroupRepository: DrawGroupRepository,
    private val fcmRepository: FcmRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ParticipantMethodState())
    val state: StateFlow<ParticipantMethodState> = _state.asStateFlow()

    fun updateGroupName(name: String) {
        _state.update { it.copy(groupName = name) }
    }

    fun updateGroupDescription(description: String) {
        _state.update { it.copy(groupDescription = description) }
    }

    fun selectMethod(method: ParticipantMethod) {
        _state.update { it.copy(selectedMethod = method) }
    }

    fun addManualParticipant(name: String) {
        if (name.isBlank()) return

        val isDuplicate = _state.value.manualParticipants.any {
            it.name.lowercase() == name.trim().lowercase()
        }

        if (isDuplicate) {
            _state.update { it.copy(error = UiText.stringResource(R.string.error_duplicate_names)) }
            return
        }

        val newParticipant = Participant(
            id = UUID.randomUUID().toString(),
            name = name.trim()
        )

        _state.update { currentState ->
            currentState.copy(
                manualParticipants = currentState.manualParticipants + newParticipant
            )
        }
    }

    fun removeManualParticipant(participant: Participant) {
        _state.update { currentState ->
            currentState.copy(
                manualParticipants = currentState.manualParticipants.filter { it.id != participant.id }
            )
        }
    }

    fun createGroup() {
        // Yeni grup oluşturuluyor
        val newGroup = DrawGroup(
            id = UUID.randomUUID().toString(),
            name = _state.value.groupName,
            description = _state.value.groupDescription,
            // ... diğer alanlar
        )

        // Grup repository'ye kaydediliyor
        drawGroupRepository.createDrawGroup(newGroup)
    }


    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}