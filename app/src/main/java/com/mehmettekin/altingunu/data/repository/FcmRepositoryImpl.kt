package com.mehmettekin.altingunu.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.domain.repository.InvitationData
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepositoryImpl @Inject constructor() : FcmRepository {

    private val functions = FirebaseFunctions.getInstance()
    private val messaging = FirebaseMessaging.getInstance()

    // ✅ TOKEN YÖNETİMİ - APP TARAFINDA
    override suspend fun updateUserFcmToken(token: String): ResultState<Unit> {
        return try {
            // Server'a token güncelleme isteği gönder
            val data = hashMapOf(
                "participantId" to "current_user", // Gerçek kullanıcı ID'si kullanılacak
                "token" to token
            )

            functions.getHttpsCallable("updateFcmToken")
                .call(data)
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

    // ✅ SERVER ÇAĞRILARI - Firebase Functions
    override suspend fun createInvitationOnServer(
        drawGroupId: String,
        drawGroupName: String,
        inviterName: String
    ): ResultState<String> {
        return try {
            val data = hashMapOf(
                "drawGroupId" to drawGroupId,
                "drawGroupName" to drawGroupName,
                "inviterName" to inviterName
            )

            val result = functions.getHttpsCallable("createInvitation")
                .call(data)
                .await()

            val response = result.data as Map<String, Any>
            val inviteCode = response["inviteCode"] as String

            ResultState.Success(inviteCode)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Davet oluşturulamadı"))
        }
    }

    override suspend fun validateInviteCodeOnServer(inviteCode: String): ResultState<InvitationData?> {
        return try {
            val data = hashMapOf("inviteCode" to inviteCode)

            val result = functions.getHttpsCallable("validateInviteCode")
                .call(data)
                .await()

            val response = result.data as Map<String, Any>
            val isValid = response["valid"] as Boolean

            if (isValid) {
                val invitationMap = response["invitation"] as Map<String, Any>
                val invitation = InvitationData(
                    id = invitationMap["id"] as String,
                    drawGroupId = invitationMap["drawGroupId"] as String,
                    drawGroupName = invitationMap["drawGroupName"] as String,
                    inviterName = invitationMap["inviterName"] as String,
                    inviteCode = invitationMap["inviteCode"] as String,
                    expirationDate = (invitationMap["expirationDate"] as Number).toLong(),
                    createdDate = (invitationMap["createdDate"] as Number).toLong()
                )
                ResultState.Success(invitation)
            } else {
                ResultState.Success(null)
            }
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Davet bulunamadı"))
        }
    }

    override suspend fun submitParticipationRequestOnServer(
        inviteCode: String,
        participantName: String,
        fcmToken: String
    ): ResultState<Unit> {
        return try {
            val data = hashMapOf(
                "inviteCode" to inviteCode,
                "participantName" to participantName,
                "fcmToken" to fcmToken
            )

            functions.getHttpsCallable("submitParticipationRequest")
                .call(data)
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Katılım talebi gönderilemedi"))
        }
    }

    override suspend fun approveParticipationRequestOnServer(
        requestId: String,
        approve: Boolean
    ): ResultState<Unit> {
        return try {
            val data = hashMapOf(
                "requestId" to requestId,
                "approve" to approve
            )

            functions.getHttpsCallable("approveParticipationRequest")
                .call(data)
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Talep işlenemedi"))
        }
    }

    override suspend fun sendGroupNotificationOnServer(
        groupId: String,
        title: String,
        message: String,
        extraData: Map<String, String>
    ): ResultState<Unit> {
        return try {
            val data = hashMapOf(
                "groupId" to groupId,
                "title" to title,
                "message" to message,
                "extraData" to extraData
            )

            functions.getHttpsCallable("sendGroupNotification")
                .call(data)
                .await()

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Bildirim gönderilemedi"))
        }
    }
}