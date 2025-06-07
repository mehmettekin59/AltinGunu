package com.mehmettekin.altingunu.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.model.ParticipationRequest
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepositoryImpl @Inject constructor() : FcmRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

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

    override suspend fun sendGroupNotification(
        groupId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): ResultState<Unit> {
        return try {
            // Cloud Function'ı çağır
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
            // Firestore'a bildirim dökümanı ekle (Function otomatik gönderecek)
            val notification = hashMapOf(
                "token" to token,
                "title" to title,
                "message" to message,
                "data" to data,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            firestore.collection("notifications")
                .add(notification)
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.directString(e.message ?: "Bildirim gönderilemedi"))
        }
    }

    // ... diğer fonksiyonlar aynı kalacak ...

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }
}