package com.mehmettekin.altingunu.data.local

import android.content.Context
import android.util.Log
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.domain.model.*
import com.mehmettekin.altingunu.domain.repository.FcmRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepositoryImpl @Inject constructor(
    private val drawGroupsDataStore: DrawGroupsDataStore,
    @ApplicationContext private val context: Context
) : FcmRepository {

    companion object {
        private const val TAG = "FcmRepository"
        private const val INVITE_EXPIRATION_DAYS = 7L // Davet 7 gün geçerli
    }

    // ============ TOKEN YÖNETİMİ ============

    override suspend fun saveDeviceToken(
        token: String,
        participantName: String,
        participantId: String
    ): ResultState<Unit> {
        return try {
            val fcmToken = FcmToken(
                token = token,
                participantName = participantName,
                participantId = participantId,
                deviceInfo = getDeviceInfo(),
                createdDate = System.currentTimeMillis(),
                lastActiveDate = System.currentTimeMillis(),
                isActive = true
            )

            drawGroupsDataStore.addFcmToken(fcmToken)
            Log.d(TAG, "FCM token saved for participant: $participantName")
            ResultState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving FCM token", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_participants,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun getDeviceToken(): ResultState<String?> {
        return try {
            // Bu implementasyon local storage'dan token alır
            // Gerçek uygulamada FirebaseMessaging.getInstance().token kullanılır
            val tokens = drawGroupsDataStore.getFcmTokens()
            val deviceToken = tokens.firstOrNull { it.isActive }?.token
            ResultState.Success(deviceToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device token", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_retrieving_participants,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun updateToken(oldToken: String, newToken: String): ResultState<Unit> {
        return try {
            val tokens = drawGroupsDataStore.getFcmTokens().toMutableList()
            val index = tokens.indexOfFirst { it.token == oldToken }

            if (index >= 0) {
                tokens[index] = tokens[index].copy(
                    token = newToken,
                    lastActiveDate = System.currentTimeMillis()
                )
                drawGroupsDataStore.saveFcmTokens(tokens)
                Log.d(TAG, "FCM token updated successfully")
            }

            ResultState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_participants,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun getGroupTokens(groupId: String): ResultState<List<FcmToken>> {
        return try {
            val tokens = drawGroupsDataStore.getGroupFcmTokens(groupId)
            ResultState.Success(tokens.filter { it.isActive })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting group tokens", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_retrieving_participants,
                    e.message ?: ""
                )
            )
        }
    }

    // ============ DAVET YÖNETİMİ ============

    override suspend fun createInvitation(invitation: DrawInvitation): ResultState<String> {
        return try {
            val invitationWithId = if (invitation.id.isEmpty()) {
                invitation.copy(
                    id = UUID.randomUUID().toString(),
                    inviteCode = generateInviteCode()
                )
            } else {
                invitation
            }

            drawGroupsDataStore.addInvitation(invitationWithId)
            Log.d(TAG, "Invitation created with code: ${invitationWithId.inviteCode}")
            ResultState.Success(invitationWithId.inviteCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating invitation", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun getInvitationByCode(inviteCode: String): ResultState<DrawInvitation?> {
        return try {
            val invitation = drawGroupsDataStore.getInvitationByCode(inviteCode)
            ResultState.Success(invitation)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting invitation by code", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_retrieving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun submitParticipationRequest(request: ParticipationRequest): ResultState<Unit> {
        return try {
            val requestWithId = if (request.id.isEmpty()) {
                request.copy(
                    id = UUID.randomUUID().toString(),
                    requestDate = System.currentTimeMillis(),
                    deviceInfo = getDeviceInfo()
                )
            } else {
                request
            }

            drawGroupsDataStore.addParticipationRequest(requestWithId)
            Log.d(TAG, "Participation request submitted for: ${requestWithId.participantName}")
            ResultState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting participation request", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_participants,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun getPendingRequests(groupId: String): ResultState<List<ParticipationRequest>> {
        return try {
            val requests = drawGroupsDataStore.getPendingRequests(groupId)
            ResultState.Success(requests)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pending requests", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_retrieving_participants,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun approveParticipationRequest(
        requestId: String,
        approved: Boolean
    ): ResultState<Unit> {
        return try {
            val status = if (approved) ParticipationStatus.APPROVED else ParticipationStatus.REJECTED
            drawGroupsDataStore.updateParticipationRequest(requestId, status)

            // Eğer onaylandıysa, katılımcıyı gruba ekle
            if (approved) {
                addApprovedParticipantToGroup(requestId)
            }

            Log.d(TAG, "Participation request $requestId ${if (approved) "approved" else "rejected"}")
            ResultState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error approving participation request", e)
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_participants,
                    e.message ?: ""
                )
            )
        }
    }

    // ============ BİLDİRİM YÖNETİMİ ============

    override suspend fun sendGroupNotification(
        groupId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): ResultState<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = drawGroupsDataStore.getGroupFcmTokens(groupId)
                val activeTokens = tokens.filter { it.isActive }.map { it.token }

                if (activeTokens.isEmpty()) {
                    return@withContext ResultState.Error(
                        UiText.stringResource(R.string.error_retrieving_participants)
                    )
                }

                // Burada gerçek FCM server API çağrısı yapılacak
                // Şimdilik log olarak simüle ediyoruz
                Log.d(TAG, "Sending notification to ${activeTokens.size} devices")
                Log.d(TAG, "Title: $title, Message: $message")
                Log.d(TAG, "Data: $data")

                // TODO: Gerçek FCM server API çağrısı
                // sendFcmNotificationToTokens(activeTokens, title, message, data)

                ResultState.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending group notification", e)
                ResultState.Error(
                    UiText.stringResource(
                        R.string.error_saving_draw_results,
                        e.message ?: ""
                    )
                )
            }
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

    // ============ HELPER METHODS ============

    private fun generateInviteCode(): String {
        // 8 karakterlik random kod oluştur
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }

    private fun getDeviceInfo(): String {
        return try {
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        } catch (e: Exception) {
            "Unknown Device"
        }
    }

    private suspend fun addApprovedParticipantToGroup(requestId: String) {
        try {
            val requests = drawGroupsDataStore.getParticipationRequests()
            val request = requests.find { it.id == requestId } ?: return

            val group = drawGroupsDataStore.getDrawGroupById(request.drawGroupId) ?: return

            // Katılımcıyı gruba ekle
            val newParticipant = Participant(
                id = UUID.randomUUID().toString(),
                name = request.participantName
            )

            val updatedParticipants = group.participants.toMutableList()
            updatedParticipants.add(newParticipant)

            // FCM token'ı gruba ekle
            val updatedTokens = group.fcmTokens.toMutableList()
            updatedTokens.add(request.fcmToken)

            val updatedGroup = group.copy(
                participants = updatedParticipants,
                fcmTokens = updatedTokens,
                lastModifiedDate = System.currentTimeMillis()
            )

            drawGroupsDataStore.updateDrawGroup(updatedGroup)

            // FCM token'ı kaydet
            saveDeviceToken(request.fcmToken, request.participantName, newParticipant.id)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding approved participant to group", e)
        }
    }
}