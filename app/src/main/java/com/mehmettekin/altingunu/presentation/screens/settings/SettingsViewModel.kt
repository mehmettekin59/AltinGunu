package com.mehmettekin.altingunu.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Load language setting
            userPreferencesRepository.getLanguage().collectLatest { language ->
                _state.value = _state.value.copy(
                    selectedLanguage = language
                )
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnLanguageChange -> {
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
                            error = UiText.dynamicString("Dil değiştirme hatası: ${e.message}"),
                            isLoading = false
                        )
                    }
                }
            }
            is SettingsEvent.OnErrorDismiss -> {
                _state.value = _state.value.copy(error = null)
            }
            else -> {}
        }
    }
}