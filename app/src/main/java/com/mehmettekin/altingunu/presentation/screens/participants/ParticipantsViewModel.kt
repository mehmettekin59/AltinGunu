package com.mehmettekin.altingunu.presentation.screens.participants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.model.InvitedParticipant
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.presentation.screens.participantmethodscreen.ParticipantMethodState
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

    fun addManualParticipant(name: String) {
        if (name.isBlank()) return

        // Check for duplicate names only in manual participants
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

    fun generateInviteCode() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Geçici bir grup ID oluştur
            val tempGroupId = UUID.randomUUID().toString()

            when (val result = fcmRepository.createInvitationOnServer(
                drawGroupId = tempGroupId,
                drawGroupName = _state.value.groupName.ifEmpty { "Yeni Altın Günü Grubu-Today" },
                inviterName = "Grup Yöneticisi"
            )) {
                is ResultState.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            inviteCode = result.data,
                            tempGroupId = tempGroupId
                        )
                    }
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun shareInviteLink(onShare: (String) -> Unit) {
        _state.value.inviteCode?.let { code ->
            val inviteUrl = "https://altingunu.app/invite/$code"
            onShare(inviteUrl)
        }
    }

    fun copyInviteLink(onCopy: (String) -> Unit) {
        _state.value.inviteCode?.let { code ->
            val inviteUrl = "https://altingunu.app/invite/$code"
            onCopy(inviteUrl)
        }
    }

    // Sadece server'dan gelen onaylı katılımcıları göster
    fun refreshInvitedParticipants() {
        viewModelScope.launch {
            _state.value.tempGroupId?.let { groupId ->
                // Server'dan sadece ACCEPTED olan katılımcıları çek
                // Not: Bu endpoint implement edilmeli
                when (val result = fcmRepository.getAcceptedParticipants(groupId)) {
                    is ResultState.Success -> {
                        val invitedParticipants = result.data.map { request ->
                            InvitedParticipant(
                                id = request.id,
                                name = request.participantName,
                                status = InviteStatus.ACCEPTED, // Hepsi zaten onaylı
                                fcmToken = request.fcmToken,
                                joinedAt = request.joinedAt
                            )
                        }

                        _state.update {
                            it.copy(invitedParticipants = invitedParticipants)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // Tüm katılımcıları birleştir
    private fun combineAllParticipants(): Pair<List<Participant>, List<String>> {
        val allParticipants = mutableListOf<Participant>()
        val allFcmTokens = mutableListOf<String>()

        // Manuel katılımcıları ekle
        allParticipants.addAll(_state.value.manualParticipants)

        // Server'dan gelen (zaten onaylı) katılımcıları ekle
        _state.value.invitedParticipants.forEach { invited ->
            allParticipants.add(
                Participant(
                    id = invited.id,
                    name = invited.name
                )
            )
            invited.fcmToken?.let { allFcmTokens.add(it) }
        }

        return Pair(allParticipants, allFcmTokens)
    }

    // Grup oluştur
    fun createGroup(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val groupName = _state.value.groupName.trim()

            if (groupName.isBlank()) {
                _state.update { it.copy(error = UiText.stringResource(R.string.error_empty_group_name)) }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            // Tüm katılımcıları birleştir
            val (allParticipants, allFcmTokens) = combineAllParticipants()

            // Minimum 2 katılımcı kontrolü
            if (allParticipants.size < 2) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.stringResource(R.string.error_min_participants)
                    )
                }
                return@launch
            }

            val currentDate = Calendar.getInstance()

            // Yeni grup oluştur
            val newGroup = DrawGroup(
                id = _state.value.tempGroupId ?: UUID.randomUUID().toString(),
                name = groupName,
                description = _state.value.groupDescription,
                createdDate = System.currentTimeMillis(),
                lastModifiedDate = System.currentTimeMillis(),
                settings = ParticipantsScreenWholeInformation(
                    participantCount = allParticipants.size,
                    participants = allParticipants,
                    itemType = ItemType.TL,
                    specificItem = "",
                    monthlyAmount = 0.0,
                    durationMonths = 0,
                    startDay = currentDate.get(Calendar.DAY_OF_MONTH),
                    startMonth = currentDate.get(Calendar.MONTH) + 1,
                    startYear = currentDate.get(Calendar.YEAR)
                ),
                participants = allParticipants,
                results = emptyList(),
                isCompleted = false,
                isActive = true,
                fcmTokens = allFcmTokens,
                currentPaymentIndex = 0
            )

            // Grubu kaydet
            when (val result = drawGroupRepository.createDrawGroup(newGroup)) {
                is ResultState.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    onSuccess(result.data)
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun getTotalParticipantCount(): Int {
        return _state.value.manualParticipants.size + _state.value.invitedParticipants.size
    }
}