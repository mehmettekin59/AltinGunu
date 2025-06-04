package com.mehmettekin.altingunu.domain.repository

import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.model.FcmToken
import com.mehmettekin.altingunu.domain.model.ParticipationRequest
import com.mehmettekin.altingunu.utils.ResultState

interface FcmRepository {

    // ============ TOKEN YÖNETİMİ ============

    /**
     * Cihazın FCM token'ını kaydeder
     */
    suspend fun saveDeviceToken(token: String, participantName: String, participantId: String): ResultState<Unit>

    /**
     * Cihazın FCM token'ını getirir
     */
    suspend fun getDeviceToken(): ResultState<String?>

    /**
     * Token'ı günceller
     */
    suspend fun updateToken(oldToken: String, newToken: String): ResultState<Unit>

    /**
     * Belirli bir grubun tüm FCM token'larını getirir
     */
    suspend fun getGroupTokens(groupId: String): ResultState<List<FcmToken>>

    // ============ DAVET YÖNETİMİ ============

    /**
     * Yeni davet oluşturur
     */
    suspend fun createInvitation(invitation: DrawInvitation): ResultState<String>

    /**
     * Davet kodunu getirir
     */
    suspend fun getInvitationByCode(inviteCode: String): ResultState<DrawInvitation?>

    /**
     * Katılım talebini gönderir
     */
    suspend fun submitParticipationRequest(request: ParticipationRequest): ResultState<Unit>

    /**
     * Bekleyen katılım taleplerini getirir
     */
    suspend fun getPendingRequests(groupId: String): ResultState<List<ParticipationRequest>>

    /**
     * Katılım talebini onaylar/reddeder
     */
    suspend fun approveParticipationRequest(requestId: String, approved: Boolean): ResultState<Unit>

    // ============ BİLDİRİM YÖNETİMİ ============

    /**
     * Grup üyelerine bildirim gönderir (Server'a istek atar)
     */
    suspend fun sendGroupNotification(
        groupId: String,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ): ResultState<Unit>

    /**
     * Ödeme hatırlatma bildirimi gönderir
     */
    suspend fun sendPaymentReminder(
        groupId: String,
        payerName: String,
        amount: String,
        paymentDate: String,
        itemInfo: String
    ): ResultState<Unit>
}