package com.mehmettekin.altingunu

import android.app.Application
import android.content.Context
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.LocaleHelper
import com.mehmettekin.altingunu.utils.NumeralHelper
import com.mehmettekin.altingunu.utils.RTLHelper
import dagger.hilt.android.HiltAndroidApp



@HiltAndroidApp
class AltinGunuApplication: Application() {

    var currentLanguage: String = Constraints.DefaultSettings.DEFAULT_LANGUAGE
        private set

    override fun attachBaseContext(base: Context) {
        // Sync olarak dil ayarını yükle
        currentLanguage = loadLanguageSync(base)
        super.attachBaseContext(LocaleHelper.updateLocale(base, currentLanguage))
    }

    override fun onCreate() {
        super.onCreate()
        // ✅ WorkManager kaldırıldı - sorun yok!
        android.util.Log.d("AltinGunuApp", "Application started successfully")
    }

    private fun loadLanguageSync(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("language_code", detectUserLanguage())
            ?: Constraints.DefaultSettings.DEFAULT_LANGUAGE
    }

    private fun detectUserLanguage(): String {
        val deviceLanguage = java.util.Locale.getDefault().language
        return when {
            deviceLanguage in Constraints.SUPPORTED_LANGUAGES -> deviceLanguage
            "en" in Constraints.SUPPORTED_LANGUAGES -> "en"
            else -> Constraints.DefaultSettings.DEFAULT_LANGUAGE
        }
    }

    fun setCurrentLanguage(languageCode: String) {
        currentLanguage = languageCode

        // NumeralHelper'a da dili bildir
        NumeralHelper.setLanguage(languageCode)

        // RTLHelper'a da dili bildir
        RTLHelper.setLanguage(languageCode)

        // SharedPreferences'a da kaydet
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("language_code", languageCode).apply()
    }

}


