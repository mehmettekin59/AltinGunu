package com.mehmettekin.altingunu.domain.model

import androidx.compose.runtime.Immutable
import java.util.UUID


@Immutable
data class DrawGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String, // "Aile Altın Günü", "İş Arkadaşları" vs
    val description: String = "", // Opsiyonel açıklama
    val createdDate: Long = System.currentTimeMillis(),
    val lastModifiedDate: Long = System.currentTimeMillis(),
    val settings: ParticipantsScreenWholeInformation,
    val participants: List<Participant>,
    val results: List<DrawResult>,
    val isCompleted: Boolean = false, // Çekiliş tamamlandı mı?
    val isActive: Boolean = true, // Aktif mi? (Silinmek üzere vs)
    val fcmTokens: List<String> = emptyList(), // Katılımcıların FCM token'ları
    val currentPaymentIndex: Int = 0 // Şu anda hangi ödeme sırasında
) {

     // Çekilişin tamamlanma yüzdesini hesaplar

    fun getCompletionPercentage(): Float {
        if (settings.durationMonths == 0) return 0f
        return (currentPaymentIndex.toFloat() / settings.durationMonths.toFloat()) * 100f
    }

    // Bir sonraki ödeme yapacak kişiyi döndürür
    fun getNextPaymentPerson(): String? {
        return if (currentPaymentIndex < results.size) {
            results[currentPaymentIndex].participantName
        } else null
    }


     //Bir sonraki ödeme tarihini döndürür

    fun getNextPaymentDate(): Triple<Int, Int, Int>? {
        return if (currentPaymentIndex < settings.durationMonths) {
            settings.getNextPaymentDate(currentPaymentIndex)
        } else null
    }


     // Çekilişin durumunu string olarak döndürür

    fun getStatusText(): String {
        return when {
            isCompleted -> "Tamamlandı"
            !isActive -> "Pasif"
            currentPaymentIndex == 0 -> "Başlamadı"
            currentPaymentIndex >= settings.durationMonths -> "Tamamlandı"
            else -> "Devam Ediyor"
        }
    }

    //Aktif katılımcı sayısını döndürür (FCM token'ı olan)
    fun getActiveParticipantCount(): Int {
        return fcmTokens.size
    }


    // Çekilişin kısa özet bilgisini döndürür
    fun getSummary(): String {
        val itemTypeText = when (settings.itemType) {
            ItemType.TL -> "TL"
            ItemType.CURRENCY -> "Döviz"
            ItemType.GOLD -> "Altın"
        }
        return "$itemTypeText - ${settings.participantCount} kişi - ${settings.durationMonths} ay"
    }
}
