package com.mehmettekin.altingunu.presentation.drawgroupdetailscreen

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.notification.FirestoreService
import com.mehmettekin.altingunu.utils.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class DrawGroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val drawGroupRepository: DrawGroupRepository,
    private val fcmRepository: FcmRepository,
    private val firestoreService: FirestoreService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""

    private val _state = MutableStateFlow(DrawGroupDetailState())
    val state: StateFlow<DrawGroupDetailState> = _state.asStateFlow()

    init {
        loadDrawGroup(groupId)
        listenToPendingRequests()
    }

    fun loadDrawGroup(id: String) {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            when (val result = drawGroupRepository.getDrawGroupById(id)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        drawGroup = result.data,
                        isLoading = false
                    )

                    // Davet kodunu al
                    loadInviteCode(id)
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        message = result.message.asString(context),
                        isLoading = false
                    )
                }
                else -> {
                    _state.value = _state.value.copy(isLoading = false)
                }
            }
        }
    }

    private fun loadInviteCode(groupId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            // ✅ Önce var olan kodu kontrol et
            when (val existingResult = fcmRepository.getExistingInviteCode(groupId)) {
                is ResultState.Success -> {

                    if (existingResult.data != null) {
                        // Var olan kodu kullan
                        _state.value = _state.value.copy(inviteCode = existingResult.data)
                        _state.value = _state.value.copy(isLoading = false)

                    } else {
                        // Yoksa yeni oluştur
                        createNewInviteCode(groupId)
                    }
                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            message = existingResult.message.asString(context),
                            isLoading = false
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun generateInviteCode() {
        val currentGroupId = groupId
        if (currentGroupId.isEmpty()) return

        loadInviteCode(currentGroupId)
    }
    private fun createNewInviteCode(groupId: String) {
        viewModelScope.launch {
            val drawGroup = _state.value.drawGroup ?: return@launch
            _state.value = _state.value.copy(isLoading = true)

            when (val result = fcmRepository.createInvitationOnServer(
                drawGroupId = groupId,
                drawGroupName = drawGroup.name,
                inviterName = "Grup Yöneticisi"
            )) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        inviteCode = result.data,
                        isLoading = false
                    )

                }
                is ResultState.Error -> {
                    _state.update {
                        it.copy(
                            message = result.message.asString(context),
                            isLoading = false
                        )
                    }
                }
                else -> {
                    _state.value = _state.value.copy(isLoading = false)
                }
            }
        }
    }


    fun approveRequest(requestId: String, approve: Boolean) {
        viewModelScope.launch {
            when (val result = fcmRepository.approveParticipationRequestOnServer(requestId, approve)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        message = if (approve) "Katılım talebi onaylandı" else "Katılım talebi reddedildi"
                    )
                    listenToPendingRequests()
                    loadDrawGroup(groupId)
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        message = result.message.asString(context)
                    )
                }
                else -> {}
            }
        }
    }

    fun markAsCompleted() {
        viewModelScope.launch {
            when (val result = drawGroupRepository.markGroupAsCompleted(groupId)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        message = "Çekiliş tamamlandı"
                    )
                    loadDrawGroup(groupId)
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        message = result.message.asString(context)
                    )
                }
                else -> {}
            }
        }
    }

    private fun listenToPendingRequests() {
        viewModelScope.launch {
            firestoreService.listenToPendingRequests(groupId).collect { pendingRequests ->
                _state.update {
                    it.copy(pendingRequests = pendingRequests)
                }
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
