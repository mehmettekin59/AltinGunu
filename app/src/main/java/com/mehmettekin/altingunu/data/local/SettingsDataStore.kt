package com.mehmettekin.altingunu.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mehmettekin.altingunu.utils.Constraints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // Keys
    private val apiUpdateIntervalKey = intPreferencesKey(Constraints.DataStoreKeys.API_UPDATE_INTERVAL)
    private val languageCodeKey = stringPreferencesKey(Constraints.DataStoreKeys.LANGUAGE_CODE)

    // API Update Interval
    suspend fun setApiUpdateInterval(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[apiUpdateIntervalKey] = seconds
        }
    }

    fun getApiUpdateInterval(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[apiUpdateIntervalKey] ?: Constraints.DefaultSettings.DEFAULT_API_UPDATE_INTERVAL
        }
    }

    // Language
    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[languageCodeKey] = languageCode
        }
    }

    fun getLanguage(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[languageCodeKey] ?: Constraints.DefaultSettings.DEFAULT_LANGUAGE
        }
    }
}