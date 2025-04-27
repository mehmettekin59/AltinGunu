package com.mehmettekin.altingunu

import android.app.Application
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.mehmettekin.altingunu.data.repository.UserPreferencesRepositoryImpl
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltAndroidApp
class AltinGunuApplication: Application(){

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    var currentLanguage: String = "tr" // Varsayılan dil
        private set

    override fun onCreate() {
        super.onCreate()
        // Uygulama başlatıldığında dil tercihini yükle
        loadLanguagePreference()
    }

    private fun loadLanguagePreference() {
        // Arka planda yükle, UI thread'i bloke etmeden
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userPrefsRepo = UserPreferencesRepositoryImpl(settingsDataStore)
                currentLanguage = userPrefsRepo.getLanguage().first()
            } catch (e: Exception) {
                // Hata durumunda varsayılan dili kullan
                currentLanguage = "tr"
            }
        }
    }

    fun setCurrentLanguage(languageCode: String) {
        currentLanguage = languageCode
    }
}
