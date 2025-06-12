package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.model.InviteStatus
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

    fun generateInviteCode() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Geçici bir grup ID oluştur
            val tempGroupId = UUID.randomUUID().toString()

            when (val result = fcmRepository.createInvitationOnServer(
                drawGroupId = tempGroupId,
                drawGroupName = _state.value.groupName.ifEmpty { "Yeni Altın Günü Grubu" },
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

    fun refreshInvitedParticipants() {
        viewModelScope.launch {
            _state.value.tempGroupId?.let { groupId ->
                // Server'dan sadece ACCEPTED olan katılımcıları çek
                when (val result = fcmRepository.getAcceptedParticipants(groupId)) {
                    is ResultState.Success -> {
                        val invitedParticipants = result.data.map { request ->
                            InvitedParticipant(
                                id = request.id,
                                name = request.participantName,
                                status = InviteStatus.ACCEPTED,
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

    fun approveInvitedParticipant(participantId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = fcmRepository.approveParticipationRequestOnServer(participantId, true)) {
                is ResultState.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    refreshInvitedParticipants()
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

    fun rejectInvitedParticipant(participantId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = fcmRepository.approveParticipationRequestOnServer(participantId, false)) {
                is ResultState.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    refreshInvitedParticipants()
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

    fun createGroupWithAllParticipants(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val groupName = _state.value.groupName.trim()

            if (groupName.isBlank()) {
                _state.update { it.copy(error = UiText.stringResource(R.string.error_empty_group_name)) }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            // Sadece davetli katılımcıları al
            val allParticipants = _state.value.invitedParticipants
                .filter { it.status == InviteStatus.ACCEPTED }
                .map { invited ->
                    Participant(
                        id = invited.id,
                        name = invited.name
                    )
                }

            val allFcmTokens = _state.value.invitedParticipants
                .filter { it.status == InviteStatus.ACCEPTED }
                .mapNotNull { it.fcmToken }

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
        return _state.value.invitedParticipants.count { it.status == InviteStatus.ACCEPTED }
    }
}
