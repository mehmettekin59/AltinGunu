package com.mehmettekin.altingunu.presentation.screens.settings

import com.mehmettekin.altingunu.utils.UiText

data class SettingsState(
    val selectedLanguage: String = "tr",
    val isLoading: Boolean = false,
    val error: UiText? = null
)
