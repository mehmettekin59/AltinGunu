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
    val startDay: Int, // ✅ YENİ: Gün bilgisi eklendi
    val currentFormattedPrice: String? = null
){
    fun calculateAmountPerPerson(): Double {
        val numberOfPeopleToDistributeInOneMonth = participantCount / durationMonths
        return if (participantCount == durationMonths) {
            monthlyAmount
        } else {
            participantCount * monthlyAmount / numberOfPeopleToDistributeInOneMonth
        }
    }

    // ✅ YENİ: Tam tarih string'i döndüren helper
    fun getStartDateString(): String {
        return String.format("%02d/%02d/%d", startDay, startMonth, startYear)
    }

    // ✅ YENİ: Bir sonraki ödeme tarihini hesaplayan helper
    fun getNextPaymentDate(monthOffset: Int = 0): Triple<Int, Int, Int> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(startYear, startMonth - 1, startDay) // 0-based month
        calendar.add(java.util.Calendar.MONTH, monthOffset)

        return Triple(
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1, // 1-based month
            calendar.get(java.util.Calendar.YEAR)
        )
    }
}

