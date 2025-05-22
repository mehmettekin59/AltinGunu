package com.mehmettekin.altingunu

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject


@HiltAndroidApp
class AltinGunuApplication: Application() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    // Uygulama için CoroutineScope oluşturun
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var currentLanguage: String = Constraints.DefaultSettings.DEFAULT_LANGUAGE
        private set

    override fun attachBaseContext(base: Context) {
        // Depolanan dil tercihini yükle veya belirle
        currentLanguage = runBlocking {
            try {
                settingsDataStore.getLanguageCode()
            } catch (e: Exception) {
                Constraints.DefaultSettings.DEFAULT_LANGUAGE
            }
        }

        super.attachBaseContext(LocaleHelper.updateLocale(base, currentLanguage))
    }

    override fun onCreate() {
        super.onCreate()

        // İlk çalıştırma kontrolü ve dil tespiti
        applicationScope.launch {
            try {
                val isFirstLaunch = settingsDataStore.isFirstLaunch()

                if (isFirstLaunch) {
                    val detectedLanguage = detectUserLanguage(applicationContext)
                    settingsDataStore.saveLanguagePreferences(detectedLanguage, false)

                    // Eğer dil değiştiyse locale'i güncelle
                    if (detectedLanguage != currentLanguage) {
                        setCurrentLanguage(detectedLanguage)
                    }
                }
            } catch (e: Exception) {
                Log.e("AltinGunuApplication", "Dil tercihi kontrolü sırasında hata", e)
            }
        }
    }

    // Uygulamanın yaşam döngüsü boyunca CoroutineScope'u düzgün şekilde kapatın
    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }

    private fun detectUserLanguage(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            context.resources.configuration.locale
        }

        val countryCode = locale.country.lowercase(Locale.ROOT)
        val languageCode = locale.language.lowercase(Locale.ROOT)

        return when {
            countryCode in Constraints.ARABIC_COUNTRIES || languageCode == "ar" -> "ar"
            countryCode in Constraints.ENGLISH_COUNTRIES || languageCode == "en" -> "en"
            countryCode == "tr" || languageCode == "tr" -> "tr"
            else -> Constraints.DefaultSettings.DEFAULT_LANGUAGE
        }
    }

    fun setCurrentLanguage(languageCode: String) {
        currentLanguage = languageCode
        // Uygulama konfigürasyonunu güncelle
        applicationContext.resources.configuration.setLocale(Locale(languageCode))
        applicationContext.resources.updateConfiguration(
            applicationContext.resources.configuration,
            applicationContext.resources.displayMetrics
        )
    }
}


