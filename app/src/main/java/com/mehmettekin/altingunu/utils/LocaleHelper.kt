package com.mehmettekin.altingunu.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun updateLocale(context: Context, languageCode: String): ContextWrapper {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)

        // For all Android versions
        configuration.setLocale(locale)

        // For Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
        }

        val resources = context.resources
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Create a new context with the updated configuration
        val newContext = context.createConfigurationContext(configuration)
        return ContextWrapper(newContext)
    }

    fun applyLanguageToActivity(activity: Activity, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = activity.resources
        val configuration = Configuration(resources.configuration)

        // Set locale for all Android versions
        configuration.setLocale(locale)

        // For Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
        }

        // Update the configuration
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Recreate activity if language changed at runtime
        activity.recreate()
    }
}