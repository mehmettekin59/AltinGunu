package com.mehmettekin.altingunu.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ParticipationRequest(
    val id: String,
    val drawGroupId: String,
    val participantName: String,
    val fcmToken: String,
    val inviteCode: String,
    val requestDate: Long = System.currentTimeMillis(),
    val status: ParticipationStatus = ParticipationStatus.PENDING,
    val deviceInfo: String = ""
)
