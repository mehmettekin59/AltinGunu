package com.mehmettekin.altingunu.presentation.screens.result

data class ResultsState(
    val results: List<DrawResult> = emptyList(),
    val drawSettings: ParticipantsScreenWholeInformation? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val pdfUri: Uri? = null
)
