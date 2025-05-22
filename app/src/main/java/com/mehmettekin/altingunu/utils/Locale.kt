package com.mehmettekin.altingunu.utils

import android.content.Context
import com.mehmettekin.altingunu.AltinGunuApplication
import java.util.Locale

enum class NumeralSystem {
    LATIN, EASTERN_ARABIC
}

object NumeralHelper {
    fun getSystemForLocale(locale: Locale): NumeralSystem {
        val language = locale.language
        val country = locale.country.uppercase(Locale.ROOT)

        return when {
            language == "ar" -> NumeralSystem.EASTERN_ARABIC
            country in Constraints.ARABIC_COUNTRIES.map { it.uppercase(Locale.ROOT) } ->
                NumeralSystem.EASTERN_ARABIC
            else -> NumeralSystem.LATIN
        }
    }

    // Uygulama diline göre sayı sistemi al
    fun getSystemForAppLanguage(application: AltinGunuApplication): NumeralSystem {
        return getSystemForLocale(Locale(application.currentLanguage))
    }

    private val EASTERN_ARABIC_NUMERALS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun convertToEasternArabic(input: String): String {
        return input.map { char ->
            if (char.isDigit()) EASTERN_ARABIC_NUMERALS[char.digitToInt()] else char
        }.joinToString("")
    }
}

// Extension function
fun String.convertNumerals(context: Context): String {
    val app = context.applicationContext as AltinGunuApplication
    return when (NumeralHelper.getSystemForAppLanguage(app)) {
        NumeralSystem.EASTERN_ARABIC -> NumeralHelper.convertToEasternArabic(this)
        NumeralSystem.LATIN -> this
    }
}