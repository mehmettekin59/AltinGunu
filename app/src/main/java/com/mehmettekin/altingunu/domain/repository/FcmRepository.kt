package com.mehmettekin.altingunu.domain.repository

import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.model.FcmToken
import com.mehmettekin.altingunu.domain.model.ParticipationRequest
import com.mehmettekin.altingunu.utils.ResultState

import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.model.ParticipationRequest
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText

interface FcmRepository {
    suspend fun createInvitation(invitation: DrawInvitation): ResultState<String>
    suspend fun getInvitationByCode(code: String): ResultState<DrawInvitation>
    suspend fun submitParticipationRequest(request: ParticipationRequest): ResultState<Unit>
    suspend fun getPendingRequests(groupId: String): ResultState<List<ParticipationRequest>>
    suspend fun approveParticipationRequest(requestId: String, approve: Boolean): ResultState<Unit>
    suspend fun sendGroupNotification(
        groupId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): ResultState<Unit>
    suspend fun sendNotificationToToken(
        token: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): ResultState<Unit>
    suspend fun updateUserFcmToken(token: String): ResultState<Unit>
}