package com.mehmettekin.altingunu.utils

import java.util.Locale

object LanguageDetectionUtil {
    fun detectDeviceLanguage(): String {
        val deviceLocale = Locale.getDefault()
        val deviceLanguage = deviceLocale.language

        return if (deviceLanguage in Constraints.SUPPORTED_LANGUAGES) {
            deviceLanguage
        } else {
            if ("en" in Constraints.SUPPORTED_LANGUAGES) "en"
            else Constraints.DefaultSettings.DEFAULT_LANGUAGE
        }
    }
}