package com.mehmettekin.altingunu.domain.model

data class ReminderData(
    val participantName: String,
    val amount: String,
    val paymentDate: String,
    val itemType: String,
    val specificItem: String
)
