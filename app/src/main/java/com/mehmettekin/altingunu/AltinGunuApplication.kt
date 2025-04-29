package com.mehmettekin.altingunu

import android.app.Application
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class AltinGunuApplication: Application() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    var currentLanguage: String = "tr" // Varsayılan dil
        private set

    override fun onCreate() {
        super.onCreate()

        // Dil tercihini senkron olarak yükle
        currentLanguage = runBlocking {
            try {
                settingsDataStore.getLanguage().first()
            } catch (e: Exception) {
                "tr" // Varsayılan dil
            }
        }
    }

    fun setCurrentLanguage(languageCode: String) {
        currentLanguage = languageCode
    }
}
