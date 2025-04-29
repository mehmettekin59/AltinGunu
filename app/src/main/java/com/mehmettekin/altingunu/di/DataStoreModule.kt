package com.mehmettekin.altingunu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mehmettekin.altingunu.data.local.DrawResultsDataStore
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

// Qualifier'lar
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDataStoreQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DrawResultsDataStoreQualifier

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Singleton
    @Provides
    @SettingsDataStoreQualifier
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return createDataStore(context, "settings_datastore")
    }

    @Singleton
    @Provides
    @DrawResultsDataStoreQualifier
    fun provideDrawResultsDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return createDataStore(context, "draw_results_datastore")
    }

    private fun createDataStore(context: Context, fileName: String): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            produceFile = { context.preferencesDataStoreFile(fileName) },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        )
    }

    @Singleton
    @Provides
    fun provideSettingsDataStoreImpl(
        @SettingsDataStoreQualifier dataStore: DataStore<Preferences>
    ): SettingsDataStore {
        return SettingsDataStore(dataStore)
    }

    @Singleton
    @Provides
    fun provideDrawResultsDataStoreImpl(
        @DrawResultsDataStoreQualifier dataStore: DataStore<Preferences>,
        moshi: Moshi
    ): DrawResultsDataStore {
        return DrawResultsDataStore(dataStore, moshi)
    }
}