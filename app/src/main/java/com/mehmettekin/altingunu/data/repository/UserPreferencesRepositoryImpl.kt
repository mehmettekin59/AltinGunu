package com.mehmettekin.altingunu.data.repository

import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.mehmettekin.altingunu.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : UserPreferencesRepository {
    override fun getLanguage(): Flow<String> = settingsDataStore.getLanguage()
    override suspend fun setLanguage(languageCode: String) = settingsDataStore.setLanguage(languageCode)
}