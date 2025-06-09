package com.mehmettekin.altingunu.domain.model

data class InvitedParticipant(
    val id: String,
    val name: String,
    val status: InviteStatus,
    val fcmToken: String? = null,
    val joinedAt: Long? = null
)
