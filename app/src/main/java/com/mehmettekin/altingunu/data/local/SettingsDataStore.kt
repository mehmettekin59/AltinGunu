package com.mehmettekin.altingunu.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mehmettekin.altingunu.utils.Constraints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) {
    private val languageKey = stringPreferencesKey(Constraints.DataStoreKeys.LANGUAGE_CODE)
    private val isFirstLaunchKey = booleanPreferencesKey(Constraints.DataStoreKeys.IS_FIRST_LAUNCH)
    private val apiUpdateIntervalKey = intPreferencesKey(Constraints.DataStoreKeys.API_UPDATE_INTERVAL)

    suspend fun getLanguageCode(): String {
        return dataStore.data.map { preferences ->
            preferences[languageKey] ?: Constraints.DefaultSettings.DEFAULT_LANGUAGE
        }.first()
    }

    suspend fun isFirstLaunch(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[isFirstLaunchKey] ?: true
        }.first()
    }

    // Modified to return a Flow<Int> instead of Int
    fun getApiUpdateIntervalFlow(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[apiUpdateIntervalKey] ?: Constraints.DefaultSettings.DEFAULT_API_UPDATE_INTERVAL
        }
    }

    // Keep the original method for compatibility
    suspend fun getApiUpdateInterval(): Int {
        return dataStore.data.map { preferences ->
            preferences[apiUpdateIntervalKey] ?: Constraints.DefaultSettings.DEFAULT_API_UPDATE_INTERVAL
        }.first()
    }

    suspend fun saveLanguagePreferences(languageCode: String, isFirstLaunch: Boolean = false) {
        dataStore.edit { preferences ->
            preferences[languageKey] = languageCode
            preferences[isFirstLaunchKey] = isFirstLaunch
        }
    }

    suspend fun saveApiUpdateInterval(intervalSeconds: Int) {
        dataStore.edit { preferences ->
            preferences[apiUpdateIntervalKey] = intervalSeconds
        }
    }

    // Add this alias method to match what's being called in SettingsViewModel
    suspend fun setApiUpdateInterval(intervalSeconds: Int) {
        saveApiUpdateInterval(intervalSeconds)
    }
}