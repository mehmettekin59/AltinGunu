package com.mehmettekin.altingunu.presentation.screens.invitejoinscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.model.ParticipationRequest
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class InviteJoinViewModel @Inject constructor(
    private val fcmRepository: FcmRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InviteJoinState())
    val state: StateFlow<InviteJoinState> = _state.asStateFlow()


    fun loadInvitation(inviteCode: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            when (val result = fcmRepository.getInvitationByCode(inviteCode)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        invitation = result.data,
                        isLoading = false
                    )
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message.toString(),
                        isLoading = false
                    )
                }
                else -> {
                    _state.value = _state.value.copy(isLoading = false)
                }
            }
        }
    }

    fun onParticipantNameChange(name: String) {
        _state.value = _state.value.copy(participantName = name)
    }

    fun onJoinClick() {
        val invitation = _state.value.invitation ?: return
        val participantName = _state.value.participantName.trim()

        if (participantName.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen adınızı girin")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isJoining = true)

            // FCM token'ı al (gerçek uygulamada Firebase'den alınacak)
            val fcmToken = "mock_fcm_token_${UUID.randomUUID()}"

            val request = ParticipationRequest(
                id = UUID.randomUUID().toString(),
                drawGroupId = invitation.drawGroupId,
                participantName = participantName,
                fcmToken = fcmToken,
                inviteCode = invitation.inviteCode
            )

            when (val result = fcmRepository.submitParticipationRequest(request)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        isJoining = false,
                        joinSuccess = true
                    )
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message.toString(),
                        isJoining = false
                    )
                }
                else -> {
                    _state.value = _state.value.copy(isJoining = false)
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}