package com.mehmettekin.altingunu.domain.model



data class DrawInvitation(
    val id: String = "",
    val drawGroupId: String = "",
    val drawGroupName: String = "",
    val inviterName: String = "",
    val inviteCode: String = "",
    val expirationDate: Long = 0,
    val createdDate: Long = System.currentTimeMillis()
) {
    fun getRemainingDays(): Int {
        val remaining = expirationDate - System.currentTimeMillis()
        return (remaining / (24 * 60 * 60 * 1000)).toInt()
    }

    fun isExpired(): Boolean = System.currentTimeMillis() > expirationDate
}

