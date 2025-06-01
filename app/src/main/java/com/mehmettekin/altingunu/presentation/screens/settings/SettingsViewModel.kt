package com.mehmettekin.altingunu.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.AltinGunuApplication
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import com.mehmettekin.altingunu.notification.GoldDayNotificationManager
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val settingsDataStore: SettingsDataStore,
    private val application: AltinGunuApplication,
    private val notificationManager: GoldDayNotificationManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load language setting
                val language = userPreferencesRepository.getLanguage().first()

                // Load API update interval
                val interval = settingsDataStore.getApiUpdateInterval()
                val isReminderEnabled = settingsDataStore.isReminderEnabled()
                val reminderDaysBefore = settingsDataStore.getReminderDaysBefore()

                _state.value = _state.value.copy(
                    selectedLanguage = language,
                    apiUpdateInterval = interval,
                    isReminderEnabled = isReminderEnabled,
                    reminderDaysBefore = reminderDaysBefore,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = UiText.stringResource(R.string.an_error_occurred_while_loading_settings, e.message ?: ""),
                    isLoading = false
                )
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnLanguageChange -> {
                if (event.languageCode == _state.value.selectedLanguage) return

                _state.value = _state.value.copy(isLoading = true)
                viewModelScope.launch {
                    try {
                        // Update language in preferences
                        userPreferencesRepository.setLanguage(event.languageCode)

                        // Update application language immediately
                        application.setCurrentLanguage(event.languageCode)

                        // Set language flag to trigger activity recreation
                        _state.value = _state.value.copy(
                            selectedLanguage = event.languageCode,
                            isLoading = false,
                            languageChanged = true
                        )

                        // Force locale update at application level
                        updateApplicationLocale(event.languageCode)
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(
                            error = UiText.stringResource(R.string.language_change_error, e.message ?: ""),
                            isLoading = false
                        )
                    }
                }
            }

            is SettingsEvent.OnApiUpdateIntervalChange -> {
                if (event.seconds == _state.value.apiUpdateInterval) return

                _state.value = _state.value.copy(isLoading = true)
                viewModelScope.launch {
                    try {
                        settingsDataStore.setApiUpdateInterval(event.seconds)
                        _state.value = _state.value.copy(
                            apiUpdateInterval = event.seconds,
                            isLoading = false
                        )
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(
                            error = UiText.stringResource(R.string.error_changing_update_interval, e.message ?: ""),
                            isLoading = false
                        )
                    }
                }
            }

            is SettingsEvent.OnReminderToggle -> {
                viewModelScope.launch {
                    try {
                        settingsDataStore.setReminderEnabled(event.enabled)
                        _state.update { it.copy(isReminderEnabled = event.enabled) }
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(
                            error = UiText.stringResource(R.string.error_changing_update_interval, e.message ?: "")
                        )
                    }
                }
            }

            is SettingsEvent.OnReminderDaysChange -> {
                viewModelScope.launch {
                    try {
                        settingsDataStore.setReminderDaysBefore(event.days)
                        _state.update { it.copy(reminderDaysBefore = event.days) }
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(
                            error = UiText.stringResource(R.string.error_changing_update_interval, e.message ?: "")
                        )
                    }
                }
            }

            is SettingsEvent.OnTestNotification -> {
                viewModelScope.launch {
                    if (checkNotificationPermission()) {
                        try {
                            // ✅ DÜZELTME: Sadece bir test bildirimi gönder
                            notificationManager.showTestNotification(
                                winnerName = "Test Kullanıcı",
                                amount = "1.000 TL",
                                paymentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            )

                            // ✅ BAŞARI MESAJI EKLE
                            _state.value = _state.value.copy(
                                error = UiText.stringResource(R.string.test_notification_sent)
                            )
                        } catch (e: Exception) {
                            _state.value = _state.value.copy(
                                error = UiText.stringResource(R.string.error_changing_update_interval, e.message ?: "")
                            )
                        }
                    } else {
                        _state.value = _state.value.copy(
                            error = UiText.stringResource(R.string.notification_permission_required)
                        )
                    }
                }
            }

            is SettingsEvent.OnDefaultsReset -> {
                _state.value = _state.value.copy(isLoading = true)
                viewModelScope.launch {
                    try {
                        // Reset language to default
                        userPreferencesRepository.setLanguage(Constraints.DefaultSettings.DEFAULT_LANGUAGE)
                        application.setCurrentLanguage(Constraints.DefaultSettings.DEFAULT_LANGUAGE)

                        // Force locale update at application level
                        updateApplicationLocale(Constraints.DefaultSettings.DEFAULT_LANGUAGE)

                        // Reset API update interval to default
                        settingsDataStore.setApiUpdateInterval(Constraints.DefaultSettings.DEFAULT_API_UPDATE_INTERVAL)
                        settingsDataStore.setReminderEnabled(Constraints.DefaultSettings.DEFAULT_REMINDER_ENABLED)
                        settingsDataStore.setReminderDaysBefore(Constraints.DefaultSettings.DEFAULT_REMINDER_DAYS_BEFORE)

                        _state.value = _state.value.copy(
                            selectedLanguage = Constraints.DefaultSettings.DEFAULT_LANGUAGE,
                            apiUpdateInterval = Constraints.DefaultSettings.DEFAULT_API_UPDATE_INTERVAL,
                            isReminderEnabled = Constraints.DefaultSettings.DEFAULT_REMINDER_ENABLED,
                            reminderDaysBefore = Constraints.DefaultSettings.DEFAULT_REMINDER_DAYS_BEFORE,
                            isLoading = false,
                            languageChanged = true
                        )
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(
                            error = UiText.stringResource(R.string.error_returning_to_default_settings, e.message ?: ""),
                            isLoading = false
                        )
                    }
                }
            }

            is SettingsEvent.OnErrorDismiss -> {
                _state.value = _state.value.copy(error = null)
            }
        }
    }

    private fun checkNotificationPermission(): Boolean {
        val hasPostNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 öncesinde izin gerekmiyor
        }

        val hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationManager.hasExactAlarmPermission()
        } else {
            true // Android 12 öncesinde izin gerekmiyor
        }

        return hasPostNotificationPermission && hasExactAlarmPermission
    }

    private fun updateApplicationLocale(languageCode: String) {
        // This will force the application to apply the language change at the system level
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        // Using application context to update resources globally
        val resources = application.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun resetLanguageChanged() {
        _state.value = _state.value.copy(languageChanged = false)
    }

    // ✅ YENİ: Eksik olan setError fonksiyonu
    fun setError(error: UiText) {
        _state.update { it.copy(error = error) }
    }
}



