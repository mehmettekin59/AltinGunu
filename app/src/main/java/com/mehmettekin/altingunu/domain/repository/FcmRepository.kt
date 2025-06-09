package com.mehmettekin.altingunu.domain.repository


import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.utils.ResultState

interface FcmRepository {
    // ✅ ONLY TOKEN MANAGEMENT - APP SIDE
    suspend fun updateUserFcmToken(token: String): ResultState<Unit>
    suspend fun getUserFcmToken(): ResultState<String?>

    // ✅ SERVER FUNCTIONS - Only server calls
    suspend fun createInvitationOnServer(
        drawGroupId: String,
        drawGroupName: String,
        inviterName: String
    ): ResultState<String> // Returns invite code

    suspend fun validateInviteCodeOnServer(inviteCode: String): ResultState<DrawInvitation?>

    suspend fun submitParticipationRequestOnServer(
        inviteCode: String,
        participantName: String,
        fcmToken: String
    ): ResultState<Unit>

    suspend fun approveParticipationRequestOnServer(
        requestId: String,
        approve: Boolean
    ): ResultState<Unit>

    suspend fun sendGroupNotificationOnServer(
        groupId: String,
        title: String,
        message: String,
        extraData: Map<String, String>
    ): ResultState<Unit>
}