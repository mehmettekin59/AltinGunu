package com.mehmettekin.altingunu.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ParticipationRequest(
    val id: String = "",
    val drawGroupId: String = "",
    val participantName: String = "",
    val fcmToken: String = "",
    val inviteCode: String = "",
    val status: String = "pending",
    val requestDate: Long = System.currentTimeMillis(),
    val responseDate: Long? = null
)
