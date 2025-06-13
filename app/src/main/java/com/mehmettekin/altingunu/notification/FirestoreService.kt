package com.mehmettekin.altingunu.notification

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.mehmettekin.altingunu.domain.model.ReminderData
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
}