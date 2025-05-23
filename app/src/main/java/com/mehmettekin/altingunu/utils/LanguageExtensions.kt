package com.mehmettekin.altingunu.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import com.mehmettekin.altingunu.MainActivity
import java.util.Locale

object LanguageExtensions {

    /**
     * Updates the app locale without recreating the activity
     */
    fun Context.updateLocale(languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = resources.configuration
        configuration.setLocale(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
        }

        return createConfigurationContext(configuration)
    }

    /**
     * Creates an intent to restart the application with new language settings
     */
    fun Context.restartApp() {
        val packageManager = packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    /**
     * Helper function to recreate all activities when language changes
     */
    fun Activity.recreateAllActivities() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }
}