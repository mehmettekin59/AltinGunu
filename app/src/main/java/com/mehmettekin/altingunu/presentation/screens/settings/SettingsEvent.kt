package com.mehmettekin.altingunu.presentation.screens.settings

sealed class SettingsEvent {
    data class OnLanguageChange(val languageCode: String) : SettingsEvent()
    data class OnApiUpdateIntervalChange(val seconds: Int) : SettingsEvent()
    data object OnErrorDismiss : SettingsEvent()
    data object OnDefaultsReset : SettingsEvent()
}