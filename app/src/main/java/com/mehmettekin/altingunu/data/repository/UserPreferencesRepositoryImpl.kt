package com.mehmettekin.altingunu.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mehmettekin.altingunu.AltinGunuApplication
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import com.mehmettekin.altingunu.utils.Constraints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val application: AltinGunuApplication
) : UserPreferencesRepository {

    private val languageCodeKey = stringPreferencesKey(Constraints.DataStoreKeys.LANGUAGE_CODE)
    private val isFirstLaunchKey = booleanPreferencesKey(Constraints.DataStoreKeys.IS_FIRST_LAUNCH)

    override suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[languageCodeKey] = languageCode
        }
        // Uygulama seviyesinde dil ayarını da güncelle
        application.setCurrentLanguage(languageCode)
    }

    override fun getLanguage(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[languageCodeKey] ?: Constraints.DefaultSettings.DEFAULT_LANGUAGE
        }
    }

    override suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[isFirstLaunchKey] = false
        }
    }

    override fun isFirstLaunch(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[isFirstLaunchKey] != false
        }
    }
}