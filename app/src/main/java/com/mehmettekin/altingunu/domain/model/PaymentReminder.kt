package com.mehmettekin.altingunu.domain.model

data class PaymentReminder(
    val participantName: String,
    val amount: String,
    val paymentDate: String,
    val itemType: String,
    val specificItem: String
)
