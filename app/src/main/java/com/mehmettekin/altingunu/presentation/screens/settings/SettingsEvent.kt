package com.mehmettekin.altingunu.presentation.screens.settings

sealed class SettingsEvent {
    data class OnLanguageChange(val languageCode: String) : SettingsEvent()
    data class OnApiUpdateIntervalChange(val seconds: Int) : SettingsEvent()
    data class OnReminderToggle(val enabled: Boolean) : SettingsEvent()        // ✅ YENİ
    data class OnReminderDaysChange(val days: Int) : SettingsEvent()           // ✅ YENİ
    data object OnTestNotification : SettingsEvent()
    data object OnErrorDismiss : SettingsEvent()
    data object OnDefaultsReset : SettingsEvent()
}