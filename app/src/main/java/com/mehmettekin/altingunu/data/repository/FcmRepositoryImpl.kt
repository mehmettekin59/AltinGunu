package com.mehmettekin.altingunu.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.model.ParticipationRequest
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) : FcmRepository {

    override suspend fun createInvitation(invitation: DrawInvitation): ResultState<String> {
        return try {
            val inviteCode = generateInviteCode()
            val invitationWithCode = invitation.copy(
                id = firestore.collection("invitations").document().id,
                inviteCode = inviteCode
            )

            firestore.collection("invitations")
                .document(invitationWithCode.id)
                .set(invitationWithCode)
                .await()

            ResultState.Success(inviteCode)
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Davet oluşturulamadı"))
        }
    }

    override suspend fun getInvitationByCode(code: String): ResultState<DrawInvitation> {
        return try {
            val snapshot = firestore.collection("invitations")
                .whereEqualTo("inviteCode", code)
                .whereGreaterThan("expirationDate", System.currentTimeMillis())
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val invitation = snapshot.documents.first().toObject(DrawInvitation::class.java)
                invitation?.let {
                    ResultState.Success(it)
                } ?: ResultState.Error(UiText.directString("Davet bulunamadı"))
            } else {
                ResultState.Error(UiText.directString("Davet kodu geçersiz veya süresi dolmuş"))
            }
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Davet yüklenemedi"))
        }
    }

    override suspend fun submitParticipationRequest(request: ParticipationRequest): ResultState<Unit> {
        return try {
            firestore.collection("participation_requests")
                .document(request.id)
                .set(request)
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Katılım talebi gönderilemedi"))
        }
    }

    override suspend fun getPendingRequests(groupId: String): ResultState<List<ParticipationRequest>> {
        return try {
            val snapshot = firestore.collection("participation_requests")
                .whereEqualTo("drawGroupId", groupId)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val requests = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ParticipationRequest::class.java)
            }

            ResultState.Success(requests)
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Talepler yüklenemedi"))
        }
    }

    override suspend fun approveParticipationRequest(
        requestId: String,
        approve: Boolean
    ): ResultState<Unit> {
        return try {
            val requestDoc = firestore.collection("participation_requests")
                .document(requestId)

            val snapshot = requestDoc.get().await()
            val request = snapshot.toObject(ParticipationRequest::class.java)

            if (request != null) {
                // Talebi güncelle
                requestDoc.update(
                    mapOf(
                        "status" to if (approve) "approved" else "rejected",
                        "responseDate" to System.currentTimeMillis()
                    )
                ).await()

                // Onaylandıysa gruba ekle
                if (approve) {
                    val drawGroupDoc = firestore.collection("draw_groups")
                        .document(request.drawGroupId)

                    firestore.runTransaction { transaction ->
                        val groupSnapshot = transaction.get(drawGroupDoc)
                        val fcmTokens = groupSnapshot.get("fcmTokens") as? List<String> ?: emptyList()

                        transaction.update(
                            drawGroupDoc,
                            "fcmTokens", fcmTokens + request.fcmToken
                        )
                    }.await()
                }

                ResultState.Success(Unit)
            } else {
                ResultState.Error(UiText.directString("Talep bulunamadı"))
            }
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Talep işlenemedi"))
        }
    }

    override suspend fun sendGroupNotification(
        groupId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): ResultState<Unit> {
        return try {
            val groupDoc = firestore.collection("draw_groups").document(groupId).get().await()
            val fcmTokens = groupDoc.get("fcmTokens") as? List<String> ?: emptyList()

            // Her token'a bildirim gönder
            fcmTokens.forEach { token ->
                sendNotificationToToken(token, title, message, data)
            }

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Bildirim gönderilemedi"))
        }
    }

    override suspend fun sendNotificationToToken(
        token: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): ResultState<Unit> {
        return try {
            // Firebase Cloud Functions kullanarak bildirim gönder


            // Firestore'a bildirim kaydı ekle
            firestore.collection("notifications")
                .add(
                    mapOf(
                        "token" to token,
                        "title" to title,
                        "message" to message,
                        "data" to data,
                        "timestamp" to System.currentTimeMillis(),
                        "status" to "pending"
                    )
                )
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Bildirim gönderilemedi"))
        }
    }

    override suspend fun updateUserFcmToken(token: String): ResultState<Unit> {
        return try {
            // Kullanıcının FCM token'ını güncelle
            // Bu kısım kullanıcı sistemi eklendikten sonra implement edilecek
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Token güncellenemedi"))
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }
}