package com.mehmettekin.altingunu.domain.repository


import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.model.ParticipationRequest
import com.mehmettekin.altingunu.domain.model.PaymentReminder
import com.mehmettekin.altingunu.utils.ResultState

interface FcmRepository {
    // Token Yönetimi
    suspend fun updateUserFcmToken(token: String): ResultState<Unit>
    suspend fun getUserFcmToken(): ResultState<String?>

    // Davet Sistemi
    suspend fun createInvitation(invitation: DrawInvitation): ResultState<String>
    suspend fun getInvitationByCode(code: String): ResultState<DrawInvitation?>
    suspend fun submitParticipationRequest(request: ParticipationRequest): ResultState<Unit>
    suspend fun getPendingRequests(groupId: String): ResultState<List<ParticipationRequest>>
    suspend fun approveParticipationRequest(requestId: String, approve: Boolean): ResultState<Unit>

    // Bildirim Gönderimi
    suspend fun sendGroupNotification(
        groupId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): ResultState<Unit>

    suspend fun sendPaymentReminder(
        groupId: String,
        payerName: String,
        amount: String,
        paymentDate: String,
        itemInfo: String
    ): ResultState<Unit>

    suspend fun schedulePaymentReminders(
        groupId: String,
        reminders: List<PaymentReminder>
    ): ResultState<Unit>
}