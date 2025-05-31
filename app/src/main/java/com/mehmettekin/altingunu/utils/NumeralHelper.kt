package com.mehmettekin.altingunu.utils

enum class NumeralSystem {
    LATIN, EASTERN_ARABIC
}

object NumeralHelper {

    // Latin rakamlar 0-9
    private val LATIN_NUMERALS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    // Doğu Arap rakamları ٠-٩
    private val EASTERN_ARABIC_NUMERALS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    // Mevcut dil için global değişken
    private var currentLanguage: String = Constraints.DefaultSettings.DEFAULT_LANGUAGE

    /**
     * Dil ayarını güncelle
     */
    fun setLanguage(languageCode: String) {
        currentLanguage = languageCode
    }

    /**
     * Mevcut dile göre hangi sayı sistemini kullanacağını belirler
     */
    fun getCurrentNumeralSystem(): NumeralSystem {
        return when (currentLanguage) {
            "ar" -> NumeralSystem.EASTERN_ARABIC
            else -> NumeralSystem.LATIN
        }
    }

    /**
     * Latin rakamları Doğu Arap rakamlarına çevirir
     */
    fun convertToEasternArabic(input: String): String {
        return input.map { char ->
            if (char.isDigit()) {
                EASTERN_ARABIC_NUMERALS[char.digitToInt()]
            } else {
                char
            }
        }.joinToString("")
    }

    /**
     * Doğu Arap rakamlarını Latin rakamlarına çevirir
     */
    fun convertToLatin(input: String): String {
        return input.map { char ->
            val index = EASTERN_ARABIC_NUMERALS.indexOf(char)
            if (index != -1) {
                LATIN_NUMERALS[index]
            } else {
                char
            }
        }.joinToString("")
    }

    /**
     * Kullanıcı girdisini normalize eder (her zaman Latin rakamlar döner)
     */
    fun normalizeInput(input: String): String {
        return convertToLatin(input)
    }

    /**
     * Görüntüleme için string'i uygun formata çevirir
     */
    fun formatForDisplay(input: String): String {
        return when (getCurrentNumeralSystem()) {
            NumeralSystem.EASTERN_ARABIC -> convertToEasternArabic(input)
            NumeralSystem.LATIN -> input
        }
    }
}

// Extension function - String için (artık context gerektirmiyor)
fun String.convertNumerals(): String {
    return NumeralHelper.formatForDisplay(this)
}

// Extension function - Number türleri için
fun Number.convertNumerals(): String {
    return NumeralHelper.formatForDisplay(this.toString())
}