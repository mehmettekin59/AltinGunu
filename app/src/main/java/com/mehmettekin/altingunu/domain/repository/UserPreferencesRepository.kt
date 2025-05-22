package com.mehmettekin.altingunu.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    suspend fun setLanguage(languageCode: String)
    fun getLanguage(): Flow<String>
    suspend fun setFirstLaunchCompleted()
    fun isFirstLaunch(): Flow<Boolean>
}