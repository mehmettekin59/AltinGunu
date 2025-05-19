package com.mehmettekin.altingunu.utils

import java.util.Locale

enum class NumeralSystem {
    LATIN, EASTERN_ARABIC
}

fun numeralSystemFromLocale(locale: Locale): NumeralSystem {
    return when {
        locale.language == "ar" -> NumeralSystem.EASTERN_ARABIC
        locale.country in setOf("EG", "SA", "AE", "BH", "KW", "QA", "OM", "JO", "SY", "LB", "IQ", "YE") -> NumeralSystem.EASTERN_ARABIC
        else -> NumeralSystem.LATIN
    }
}

fun String.convertNumerals(locale: Locale = Locale.getDefault()): String {
    val numeralSystem = numeralSystemFromLocale(locale)
    return when (numeralSystem) {
        NumeralSystem.EASTERN_ARABIC -> {
            val easternArabicNumerals = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            this.map { char ->
                if (char.isDigit()) easternArabicNumerals[char.digitToInt()] else char
            }.joinToString("")
        }
        NumeralSystem.LATIN -> this
    }
}