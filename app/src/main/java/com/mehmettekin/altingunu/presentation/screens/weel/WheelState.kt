package com.mehmettekin.altingunu.presentation.screens.weel

import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.utils.UiText


data class WheelState(
    val allParticipants: List<Participant> = emptyList(), // ✅ Başlangıçta yüklenen tüm katılımcılar
    val remainingParticipants: List<Participant> = emptyList(), // ✅ Henüz seçilmemiş katılımcılar
    val winners: List<String> = emptyList(),
    val winnerParticipants: List<Participant> = emptyList(), // ✅ Kazanan participant objeleri
    val rotation: Float = 0f, // ✅ Çark rotasyonu
    val isSpinning: Boolean = false, // ✅ Çark dönüyor mu?
    val currentWinner: String? = null,
    val drawSettings: ParticipantsScreenWholeInformation? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val resultsSaved: Boolean = false
)

