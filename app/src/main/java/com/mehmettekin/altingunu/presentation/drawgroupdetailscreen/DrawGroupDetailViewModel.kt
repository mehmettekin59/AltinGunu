package com.mehmettekin.altingunu.presentation.drawgroupdetailscreen

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class DrawGroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val drawGroupRepository: DrawGroupRepository,
    private val fcmRepository: FcmRepository,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""

    private val _state = MutableStateFlow(DrawGroupDetailState())
    val state: StateFlow<DrawGroupDetailState> = _state.asStateFlow()

    init {
        loadDrawGroup(groupId)
        loadPendingRequests()
        loadReminderSettings()
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
            // Mevcut davet kodunu kontrol et veya yeni oluştur
            val invitation = DrawInvitation(
                id = "",
                drawGroupId = groupId,
                drawGroupName = _state.value.drawGroup?.name ?: "",
                inviterName = "Grup Yöneticisi",
                inviteCode = "",
                expirationDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
            )

            when (val result = fcmRepository.createInvitation(invitation)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(inviteCode = result.data)
                }
                else -> {}
            }
        }
    }

    private fun loadPendingRequests() {
        viewModelScope.launch {
            when (val result = fcmRepository.getPendingRequests(groupId)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(pendingRequests = result.data)
                }
                else -> {}
            }
        }
    }


    fun approveRequest(requestId: String, approve: Boolean) {
        viewModelScope.launch {
            when (val result = fcmRepository.approveParticipationRequest(requestId, approve)) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        message = if (approve) "Katılım talebi onaylandı" else "Katılım talebi reddedildi"
                    )
                    loadPendingRequests()
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
            when (val result = drawGroupRepository.markDrawGroupAsCompleted(groupId)) {
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

    fun onInviteClick() {
        shareInviteLink()
    }

    fun shareInviteLink() {
        _state.value.inviteCode?.let { code ->
            val inviteUrl = "https://altingunu.app/invite/$code"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Altın Günü çekilişimize katılın! $inviteUrl")
            }

            val chooser = Intent.createChooser(shareIntent, "Davet linkini paylaş")
            if (chooser.resolveActivity(context.packageManager) != null) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        }
    }



    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
