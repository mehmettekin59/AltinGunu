package com.mehmettekin.altingunu.data.repository

import android.app.DownloadManager.Query
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.messaging.FirebaseMessaging
import com.mehmettekin.altingunu.domain.model.DrawInvitation
import com.mehmettekin.altingunu.domain.model.InviteStatus
import com.mehmettekin.altingunu.domain.model.InvitedParticipant
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepositoryImpl @Inject constructor() : FcmRepository {

    // ✅ DÜZELTME: Region eklendi
    private val functions = FirebaseFunctions.getInstance("us-central1")
    private val messaging = FirebaseMessaging.getInstance()

    override suspend fun updateUserFcmToken(token: String): ResultState<Unit> {
        return try {
            // Server'a token güncelleme isteği gönder
            val data = hashMapOf(
                "participantId" to "current_user",
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

   /* // ✅ SERVER ÇAĞRILARI - Firebase Functions
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

            val response = result.getData() as Map<String, Any>
            val inviteCode = response["inviteCode"] as String

            ResultState.Success(inviteCode)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Davet oluşturulamadı"))
        }
    }
    */
   override suspend fun createInvitationOnServer(
       drawGroupId: String,
       drawGroupName: String,
       inviterName: String
   ): ResultState<String> {
       return try {
           Log.d("TEST", "=== BAŞLADI ===")
           Log.d("TEST", "drawGroupId: '$drawGroupId'")
           Log.d("TEST", "drawGroupName: '$drawGroupName'")
           Log.d("TEST", "inviterName: '$inviterName'")

           // Basit test data
           val data = hashMapOf(
               "drawGroupId" to "test-123",
               "drawGroupName" to "Test Group",
               "inviterName" to "Test User"
           )

           Log.d("TEST", "Test data hazırlandı: $data")

           val result = functions.getHttpsCallable("createInvitation")
               .call(data)
               .await()

           Log.d("TEST", "Function çağrısı başarılı!")
           Log.d("TEST", "Result: ${result.getData()}")

           // Geçici olarak sabit değer dönelim
           ResultState.Success("TEST-INVITE-CODE")

       } catch (e: Exception) {
           Log.e("TEST", "HATA: ${e.javaClass.simpleName}")
           Log.e("TEST", "Message: ${e.message}")
           if (e is FirebaseFunctionsException) {
               Log.e("TEST", "Firebase Code: ${e.code}")
               Log.e("TEST", "Firebase Details: ${e.details}")
           }
           ResultState.Error(UiText.dynamicString("TEST HATA: ${e.message}"))
       }
   }

    override suspend fun validateInviteCodeOnServer(inviteCode: String): ResultState<DrawInvitation?> {
        return try {
            val data = hashMapOf("inviteCode" to inviteCode)

            val result = functions.getHttpsCallable("validateInviteCode")
                .call(data)
                .await()

            val response = result.getData() as Map<String, Any>
            val isValid = response["valid"] as Boolean

            if (isValid) {
                val invitationMap = response["invitation"] as Map<String, Any>
                val invitation = DrawInvitation(
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

    override suspend fun getExistingInviteCode(groupId: String): ResultState<String?> {
        return try {
            val inviteQuery = functions.getHttpsCallable("getExistingInviteCode")
                .call(hashMapOf("groupId" to groupId))
                .await()

            val response = inviteQuery.getData() as Map<String, Any>
            val inviteCode = response["inviteCode"] as? String

            ResultState.Success(inviteCode)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Davet kodu alınamadı"))
        }
    }

    override suspend fun getAcceptedParticipants(groupId: String): ResultState<List<InvitedParticipant>> {
        return try {
            val data = hashMapOf("groupId" to groupId)

            val result = functions.getHttpsCallable("getAcceptedParticipants")
                .call(data)
                .await()

            val response = result.getData() as Map<String, Any>
            val participantsData = response["participants"] as List<Map<String, Any>>

            val participants = participantsData.map { participantMap ->
                InvitedParticipant(
                    id = participantMap["id"] as String,
                    drawGroupId = participantMap["drawGroupId"] as String,
                    name = participantMap["participantName"] as String,
                    fcmToken = participantMap["fcmToken"] as? String,
                    status = when(participantMap["status"] as String) {
                        "ACCEPTED" -> InviteStatus.ACCEPTED
                        "PENDING" -> InviteStatus.PENDING
                        "REJECTED" -> InviteStatus.REJECTED
                        else -> InviteStatus.PENDING
                    },
                    joinedAt = (participantMap["responseDate"] as? Number)?.toLong()
                )
            }

            ResultState.Success(participants)
        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Katılımcılar alınamadı"))
        }
    }
}