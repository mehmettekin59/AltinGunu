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

    var currentLanguage: String = Constraints.DefaultSettings.DEFAULT_LANGUAGE
        private set

    override fun attachBaseContext(base: Context) {
        // Sync olarak SharedPreferences'dan yükle
        currentLanguage = loadLanguageSync(base)
        super.attachBaseContext(LocaleHelper.updateLocale(base, currentLanguage))
    }

    private fun loadLanguageSync(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("language_code",
            detectUserLanguage(context)) ?: Constraints.DefaultSettings.DEFAULT_LANGUAGE
    }
}


