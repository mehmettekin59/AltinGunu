package com.mehmettekin.altingunu.presentation.screens.result

import android.net.Uri
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.utils.UiText

data class ResultsState(
    val results: List<DrawResult> = emptyList(),
    val drawSettings: ParticipantsScreenWholeInformation? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val pdfUri: Uri? = null,
    val message: UiText? = null
)
