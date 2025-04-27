package com.mehmettekin.altingunu.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ParticipantsScreenWholeInformation(
    val participantCount: Int,
    val participants: List<Participant>,
    val itemType: ItemType,
    val specificItem: String,
    val monthlyAmount: Double,
    val durationMonths: Int,
    val startMonth: Int,
    val startYear: Int,
    val currentFormattedPrice: String? = null
){
    fun calculateAmountPerPerson(): Double {
        return if (participantCount <= durationMonths) {
            monthlyAmount
        } else {
            monthlyAmount * durationMonths / participantCount
        }
    }
}
