package com.mehmettekin.altingunu.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun updateLocale(context: Context, languageCode: String): ContextWrapper {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)

        // Android 8.0+ için (min SDK 26)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)

        val newContext = context.createConfigurationContext(configuration)
        return ContextWrapper(newContext)
    }
}