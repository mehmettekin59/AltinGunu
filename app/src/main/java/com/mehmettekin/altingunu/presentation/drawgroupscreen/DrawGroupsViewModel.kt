package com.mehmettekin.altingunu.presentation.drawgroupscreen

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.data.remote.FirestoreService
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.notification.FirestoreService
import com.mehmettekin.altingunu.presentation.drawgroupdetailscreen.DrawGroupDetailState
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
        if (groupId.isNotEmpty()) {
            listenToPendingRequests()
        }
    }

    fun loadDrawGroup(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            when (val result = drawGroupRepository.getDrawGroupById(id)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        drawGroup = result.data,
                        isLoading = false
                    )

                    // Davet kodunu al
                    result.data?.let { group ->
                        loadInviteCode(id, group.name)
                    }
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

    private fun loadInviteCode(groupId: String, groupName: String) {
        viewModelScope.launch {
            when (val result = fcmRepository.createInvitationOnServer(
                drawGroupId = groupId,
                drawGroupName = groupName,
                inviterName = "Grup Yöneticisi"
            )) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(inviteCode = result.data)
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


    fun approveRequest(requestId: String, approve: Boolean) {
        viewModelScope.launch {
            when (val result = fcmRepository.approveParticipationRequestOnServer(requestId, approve)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        message = if (approve) "Katılım talebi onaylandı" else "Katılım talebi reddedildi"
                    )

                    // Grubu yeniden yükle (katılımcı listesini güncellemek için)
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

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}