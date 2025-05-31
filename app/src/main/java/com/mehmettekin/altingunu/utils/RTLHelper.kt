package com.mehmettekin.altingunu.utils

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection



object RTLHelper {

    // Mevcut dil için global değişken
    private var currentLanguage: String = Constraints.DefaultSettings.DEFAULT_LANGUAGE

    /**
     * Dil ayarını güncelle
     */
    fun setLanguage(languageCode: String) {
        currentLanguage = languageCode
    }

    /**
     * Mevcut dilin RTL olup olmadığını kontrol eder
     */
    fun isRTL(): Boolean {
        return currentLanguage == "ar"
    }

    /**
     * Mevcut dile göre text direction döndürür
     */
    fun getTextDirection(): TextDirection {
        return if (isRTL()) TextDirection.Rtl else TextDirection.Ltr
    }

    /**
     * Mevcut dile göre text align döndürür (başlangıç için)
     */
    fun getStartTextAlign(): TextAlign {
        return if (isRTL()) TextAlign.Right else TextAlign.Start
    }

    /**
     * Mevcut dile göre text align döndürür (bitiş için)
     */
    fun getEndTextAlign(): TextAlign {
        return if (isRTL()) TextAlign.Left else TextAlign.End
    }

    /**
     * Mevcut dile göre layout direction döndürür
     */
    fun getLayoutDirection(): LayoutDirection {
        return if (isRTL()) LayoutDirection.Rtl else LayoutDirection.Ltr
    }
}

// Extension functions for TextStyle
fun TextStyle.withRTL(): TextStyle = this.copy(
    textDirection = RTLHelper.getTextDirection(),
    textAlign = RTLHelper.getStartTextAlign()
)

fun TextStyle.withRTLAlign(align: TextAlign = RTLHelper.getStartTextAlign()): TextStyle = this.copy(
    textDirection = RTLHelper.getTextDirection(),
    textAlign = align
)