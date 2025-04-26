package com.mehmettekin.altingunu.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val settingsDataStore: SettingsDataStore
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
                val interval = settingsDataStore.getApiUpdateInterval().first()

                _state.value = _state.value.copy(
                    selectedLanguage = language,
                    apiUpdateInterval = interval,
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
                        userPreferencesRepository.setLanguage(event.languageCode)
                        _state.value = _state.value.copy(
                            selectedLanguage = event.languageCode,
                            isLoading = false
                        )
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

            is SettingsEvent.OnDefaultsReset -> {
                _state.value = _state.value.copy(isLoading = true)
                viewModelScope.launch {
                    try {
                        // Reset language to default
                        userPreferencesRepository.setLanguage(Constraints.DefaultSettings.DEFAULT_LANGUAGE)

                        // Reset API update interval to default
                        settingsDataStore.setApiUpdateInterval(Constraints.DefaultSettings.DEFAULT_API_UPDATE_INTERVAL)

                        _state.value = _state.value.copy(
                            selectedLanguage = Constraints.DefaultSettings.DEFAULT_LANGUAGE,
                            apiUpdateInterval = Constraints.DefaultSettings.DEFAULT_API_UPDATE_INTERVAL,
                            isLoading = false
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
}