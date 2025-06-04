package com.mehmettekin.altingunu.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class DrawInvitation(
    val id: String,
    val drawGroupId: String,
    val drawGroupName: String,
    val inviterName: String, // Davet eden kişi
    val inviteCode: String, // Davet kodu (link için)
    val expirationDate: Long, // Davet geçerlilik süresi
    val createdDate: Long = System.currentTimeMillis(),
    val maxParticipants: Int = -1, // -1 = sınırsız
    val currentParticipants: Int = 0,
    val isActive: Boolean = true
) {

    fun isValid(): Boolean {
        return isActive &&
                System.currentTimeMillis() < expirationDate &&
                (maxParticipants == -1 || currentParticipants < maxParticipants)
    }

    fun getRemainingDays(): Long {
        val remaining = expirationDate - System.currentTimeMillis()
        return if (remaining > 0) remaining / (24 * 60 * 60 * 1000) else 0
    }
}
