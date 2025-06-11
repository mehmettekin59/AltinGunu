package com.mehmettekin.altingunu.presentation.screens.participants

import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.utils.UiText
import java.util.Calendar

data class ParticipantsState(
    val participantCount: String = "",
    val selectedItemType: ItemType = ItemType.TL,
    val selectedSpecificItem: String = "",
    val monthlyAmount: String = "",
    val durationMonths: String = "",
    val startDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    val startMonth: Int = 1,
    val startYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val participants: List<Participant> = emptyList(),

    val isShowingConfirmDialog: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val currencyOptions: List<String> = emptyList(),
    val goldOptions: List<String> = emptyList(),
    val groupId: String = "",
    val groupName: String = "",
    val groupDescription: String = ""
)
