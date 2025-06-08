package com.mehmettekin.altingunu.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.model.ParticipationRequest
import com.mehmettekin.altingunu.domain.model.ParticipationStatus
import com.mehmettekin.altingunu.domain.model.PaymentReminder
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepositoryImpl @Inject constructor() : FcmRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val messaging = FirebaseMessaging.getInstance()

    // Token Yönetimi
    override suspend fun updateUserFcmToken(token: String): ResultState<Unit> {
        return try {
            // FCM token'ı Firestore'da güncelle
            val tokenData = hashMapOf(
                "token" to token,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "isActive" to true
            )

            firestore.collection("fcm_tokens")
                .document(token)
                .set(tokenData)
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Token güncelleme hatası"))
        }
    }

    override suspend fun getUserFcmToken(): ResultState<String?> {
        return try {
            val token = messaging.token.await()
            ResultState.Success(token)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Token alma hatası"))
        }
    }

    // Davet Sistemi
    override suspend fun createInvitation(invitation: DrawInvitation): ResultState<String> {
        return try {
            val inviteCode = generateInviteCode()
            val invitationWithCode = invitation.copy(
                id = UUID.randomUUID().toString(),
                inviteCode = inviteCode,
                expirationDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
            )

            firestore.collection("invitations")
                .document(invitationWithCode.id)
                .set(invitationWithCode)
                .await()

            ResultState.Success(inviteCode)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Davet oluşturulamadı"))
        }
    }

    override suspend fun getInvitationByCode(code: String): ResultState<DrawInvitation?> {
        return try {
            val querySnapshot = firestore.collection("invitations")
                .whereEqualTo("inviteCode", code)
                .whereGreaterThan("expirationDate", System.currentTimeMillis())
                .limit(1)
                .get()
                .await()

            val invitation = if (!querySnapshot.isEmpty) {
                querySnapshot.documents.first().toObject(DrawInvitation::class.java)
            } else null

            ResultState.Success(invitation)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Davet bulunamadı"))
        }
    }

    override suspend fun submitParticipationRequest(request: ParticipationRequest): ResultState<Unit> {
        return try {
            val requestWithId = request.copy(
                id = UUID.randomUUID().toString(),
                requestDate = System.currentTimeMillis(),
                status = ParticipationStatus.PENDING.name
            )

            firestore.collection("participation_requests")
                .document(requestWithId.id)
                .set(requestWithId)
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Katılım talebi gönderilemedi"))
        }
    }

    override suspend fun getPendingRequests(groupId: String): ResultState<List<ParticipationRequest>> {
        return try {
            val querySnapshot = firestore.collection("participation_requests")
                .whereEqualTo("drawGroupId", groupId)
                .whereEqualTo("status", ParticipationStatus.PENDING.name)
                .get()
                .await()

            val requests = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(ParticipationRequest::class.java)
            }

            ResultState.Success(requests)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Bekleyen talepler alınamadı"))
        }
    }

    override suspend fun approveParticipationRequest(requestId: String, approve: Boolean): ResultState<Unit> {
        return try {
            val status = if (approve) ParticipationStatus.APPROVED else ParticipationStatus.REJECTED

            firestore.collection("participation_requests")
                .document(requestId)
                .update(
                    "status", status.name,
                    "responseDate", com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Talep işlenemedi"))
        }
    }

    // Bildirim Gönderimi
    override suspend fun sendGroupNotification(
        groupId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): ResultState<Unit> {
        return try {
            val result = functions
                .getHttpsCallable("sendGroupNotification")
                .call(hashMapOf(
                    "groupId" to groupId,
                    "title" to title,
                    "message" to message,
                    "extraData" to data
                ))
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Bildirim gönderilemedi"))
        }
    }

    override suspend fun sendPaymentReminder(
        groupId: String,
        payerName: String,
        amount: String,
        paymentDate: String,
        itemInfo: String
    ): ResultState<Unit> {
        val title = "🪙 Altın Günü Hatırlatması"
        val message = "$payerName kişisi için $amount tutarında ödeme tarihi: $paymentDate"

        val data = mapOf(
            "type" to "payment_reminder",
            "group_id" to groupId,
            "payer_name" to payerName,
            "amount" to amount,
            "payment_date" to paymentDate,
            "item_info" to itemInfo
        )

        return sendGroupNotification(groupId, title, message, data)
    }

    override suspend fun schedulePaymentReminders(
        groupId: String,
        reminders: List<PaymentReminder>
    ): ResultState<Unit> {
        return try {
            // Firebase Cloud Scheduler ile zamanlanmış bildirimler
            val scheduledData = hashMapOf(
                "groupId" to groupId,
                "reminders" to reminders,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            firestore.collection("scheduled_reminders")
                .document(groupId)
                .set(scheduledData)
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Hatırlatıcılar zamanlanamadı"))
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }
}