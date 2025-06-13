package com.mehmettekin.altingunu.notification

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.mehmettekin.altingunu.domain.model.InviteStatus
import com.mehmettekin.altingunu.domain.model.InvitedParticipant
import com.mehmettekin.altingunu.domain.model.ReminderData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    // Hatırlatıcıları kaydet
    suspend fun saveScheduledReminders(groupId: String, reminders: List<ReminderData>) {
        val reminderDoc = hashMapOf(
            "groupId" to groupId,
            "reminders" to reminders.map { reminder ->
                hashMapOf(
                    "participantName" to reminder.participantName,
                    "amount" to reminder.amount,
                    "paymentDate" to reminder.paymentDate,
                    "itemType" to reminder.itemType,
                    "specificItem" to reminder.specificItem
                )
            },
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("scheduled_reminders")
            .document(groupId)
            .set(reminderDoc)
            .await()
    }
    fun listenToPendingRequests(groupId: String): Flow<List<InvitedParticipant>> = callbackFlow {
        val listener = db.collection("participation_requests")
            .whereEqualTo("drawGroupId", groupId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val pendingRequests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        InvitedParticipant(
                            id = doc.id,
                            name = doc.getString("participantName") ?: "",
                            drawGroupId = groupId,
                            status = InviteStatus.PENDING,
                            fcmToken = doc.getString("fcmToken"),
                            joinedAt = doc.getLong("requestDate")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(pendingRequests)
            }

        awaitClose { listener.remove() }
    }

    fun listenToAcceptedParticipants(groupId: String): Flow<List<InvitedParticipant>> = callbackFlow {
        val listener = db.collection("participation_requests")
            .whereEqualTo("drawGroupId", groupId)
            .whereEqualTo("status", "ACCEPTED")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val acceptedParticipants = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        InvitedParticipant(
                            id = doc.id,
                            name = doc.getString("participantName") ?: "",
                            drawGroupId = groupId,
                            status = InviteStatus.ACCEPTED,
                            fcmToken = doc.getString("fcmToken"),
                            joinedAt = doc.getLong("responseDate")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(acceptedParticipants)
            }

        awaitClose { listener.remove() }
    }
}