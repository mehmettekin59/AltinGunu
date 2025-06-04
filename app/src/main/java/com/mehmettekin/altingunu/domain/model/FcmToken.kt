package com.mehmettekin.altingunu.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class FcmToken(
    val token: String,
    val participantName: String,
    val participantId: String,
    val deviceInfo: String = "", // Opsiyonel cihaz bilgisi
    val createdDate: Long = System.currentTimeMillis(),
    val lastActiveDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
