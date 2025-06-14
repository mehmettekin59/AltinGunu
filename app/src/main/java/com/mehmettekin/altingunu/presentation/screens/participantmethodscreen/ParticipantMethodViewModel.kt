package com.mehmettekin.altingunu.presentation.screens.participantmethodscreen


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.model.InviteStatus
import com.mehmettekin.altingunu.domain.model.InvitedParticipant
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.notification.FirestoreService
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.Job
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class ParticipantMethodViewModel @Inject constructor(
    private val drawGroupRepository: DrawGroupRepository,
    private val fcmRepository: FcmRepository,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _state = MutableStateFlow(ParticipantMethodState())
    val state: StateFlow<ParticipantMethodState> = _state.asStateFlow()

    fun updateGroupName(name: String) {
        _state.update { it.copy(groupName = name) }
    }

    fun updateGroupDescription(description: String) {
        _state.update { it.copy(groupDescription = description) }
    }

    // ✅ YENİ: Grup oluşturma fonksiyonu
    fun createGroup() {
        viewModelScope.launch {
            val groupName = _state.value.groupName.trim()

            if (groupName.isBlank()) {
                _state.update { it.copy(error = UiText.stringResource(R.string.error_empty_group_name)) }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            try {
                val currentDate = Calendar.getInstance()
                val groupId = UUID.randomUUID().toString()

                // Başlangıçta boş katılımcı listesi ile grup oluştur
                val newGroup = DrawGroup(
                    id = groupId,
                    name = groupName,
                    description = _state.value.groupDescription,
                    createdDate = System.currentTimeMillis(),
                    lastModifiedDate = System.currentTimeMillis(),
                    settings = ParticipantsScreenWholeInformation(
                        participantCount = 0,  // Başlangıçta 0
                        participants = emptyList(),
                        itemType = ItemType.TL,
                        specificItem = "",
                        monthlyAmount = 0.0,
                        durationMonths = 0,
                        startDay = currentDate.get(Calendar.DAY_OF_MONTH),
                        startMonth = currentDate.get(Calendar.MONTH) + 1,
                        startYear = currentDate.get(Calendar.YEAR)
                    ),
                    participants = emptyList(),
                    results = emptyList(),
                    isCompleted = false,
                    isActive = true,
                    fcmTokens = emptyList(),
                    currentPaymentIndex = 0
                )

                when (val result = drawGroupRepository.createDrawGroup(newGroup)) {
                    is ResultState.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                groupId = result.data,
                                isGroupCreated = true
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
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.dynamicString(e.message ?: "Beklenmeyen hata")
                    )
                }
            }
        }
    }


    fun generateInviteCode() {
        val groupId = _state.value.groupId ?: return  // Grup yoksa çık

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = fcmRepository.createInvitationOnServer(
                drawGroupId = groupId,  // ✅ Gerçek groupId
                drawGroupName = _state.value.groupName,
                inviterName = "Grup Yöneticisi"
            )) {
                is ResultState.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            inviteCode = result.data
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
            _state.value.groupId?.let { groupId ->
                when (val result = fcmRepository.getAcceptedParticipants(groupId)) {
                    is ResultState.Success -> {
                        val invitedParticipants = result.data.map { request ->
                            InvitedParticipant(
                                id = request.id,
                                name = request.name,
                                drawGroupId = groupId,
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

    // ✅ YENİ: createGroupWithAllParticipants yerine proceedWithParticipants
    fun proceedWithParticipants(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val groupId = _state.value.groupId ?: return@launch

            _state.update { it.copy(isLoading = true) }

            // Sadece davetli katılımcıları al
            val invitedParticipants = _state.value.invitedParticipants
                .filter { it.status == InviteStatus.ACCEPTED }

            if (invitedParticipants.size < 2) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.stringResource(R.string.error_min_participants)
                    )
                }
                return@launch
            }

            // Grubu katılımcılarla güncelle
            val participants = invitedParticipants.map { invited ->
                Participant(
                    id = invited.id,
                    name = invited.name
                )
            }

            val fcmTokens = invitedParticipants.mapNotNull { it.fcmToken }

            // Grup bilgilerini güncelle
            when (val getResult = drawGroupRepository.getDrawGroupById(groupId)) {
                is ResultState.Success -> {
                    getResult.data?.let { existingGroup ->
                        val updatedGroup = existingGroup.copy(
                            participants = participants,
                            fcmTokens = fcmTokens,
                            settings = existingGroup.settings.copy(
                                participantCount = participants.size,
                                participants = participants
                            ),
                            lastModifiedDate = System.currentTimeMillis()
                        )

                        when (val updateResult = drawGroupRepository.updateDrawGroup(updatedGroup)) {
                            is ResultState.Success -> {
                                _state.update { it.copy(isLoading = false) }
                                onSuccess(groupId)
                            }
                            is ResultState.Error -> {
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        error = updateResult.message
                                    )
                                }
                            }
                            else -> {
                                _state.update { it.copy(isLoading = false) }
                            }
                        }
                    }
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = getResult.message
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private var participantListenerJob: Job? = null

    fun startListeningToParticipants() {
        _state.value.groupId?.let { groupId ->
            // Önceki listener'ı iptal et
            participantListenerJob?.cancel()

            // Yeni listener başlat
            participantListenerJob = viewModelScope.launch {
                // Onaylanmış katılımcıları dinle
                firestoreService.listenToAcceptedParticipants(groupId).collect { participants ->
                    _state.update {
                        it.copy(invitedParticipants = participants)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        participantListenerJob?.cancel()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun getTotalParticipantCount(): Int {
        return _state.value.invitedParticipants.count { it.status == InviteStatus.ACCEPTED }
    }
}
