package com.mehmettekin.altingunu

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
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

        // ✅ FIREBASE INITIALIZATION EKLENDI
        initializeFirebase()

        NumeralHelper.setLanguage(currentLanguage)
        RTLHelper.setLanguage(currentLanguage)
        Constraints.setLanguage(currentLanguage)
    }

    // ✅ YENİ: Firebase initialization
    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d("AltinGunu", "Firebase başarıyla başlatıldı")

            // FCM token'ı al (opsiyonel - debug için)
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("AltinGunu", "FCM token alma başarısız", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("AltinGunu", "FCM Registration Token: $token")
            }

        } catch (e: Exception) {
            Log.e("AltinGunu", "Firebase başlatma hatası", e)
        }
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

        // Constraints'e dili bildir
        Constraints.setLanguage(languageCode)

        // SharedPreferences'a da kaydet
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("language_code", languageCode).apply()
    }
}


