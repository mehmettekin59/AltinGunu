package com.mehmettekin.altingunu.presentation.screens.settings

data class SettingsState(
    val selectedLanguage: String = "tr",
    val isLoading: Boolean = false,
    val error: UiText? = null
)
