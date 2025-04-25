package com.mehmettekin.altingunu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mehmettekin.altingunu.data.local.DrawResultsDataStore
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Extension functions for DataStore instances
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings_datastore"
)

val Context.drawResultsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "draw_results_datastore"
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {


    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideDrawResultsDataStore(
        @ApplicationContext context: Context,
        moshi: Moshi
    ): DrawResultsDataStore {
        return DrawResultsDataStore(context, moshi)
    }
}