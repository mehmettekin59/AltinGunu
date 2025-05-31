package com.mehmettekin.altingunu.utils

import com.mehmettekin.altingunu.domain.model.ItemType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale



object ValueFormatter {

    fun format(value: String?, itemType: ItemType): String {
        // Null veya boş değerler için varsayılan döndür
        if (value.isNullOrBlank()) {
            return defaultValueFor(itemType)
        }

        return try {
            // Değeri önce normalize et (Latin rakamlar)
            val normalizedValue = NumeralHelper.normalizeInput(value)

            // Double'a çevir ve formatla
            val parsedValue = normalizedValue.toDoubleOrNull() ?: 0.0

            // DecimalFormat ile formatla
            val formatter = DecimalFormat().apply {
                val symbols = DecimalFormatSymbols(Locale.getDefault())
                decimalFormatSymbols = symbols

                when (itemType) {
                    ItemType.GOLD -> {
                        minimumFractionDigits = 0
                        maximumFractionDigits = 2
                        isGroupingUsed = true
                    }
                    else -> {
                        minimumFractionDigits = 0
                        maximumFractionDigits = 2
                        isGroupingUsed = true
                    }
                }
            }

            val formatted = formatter.format(parsedValue)

            // Uygun sayı sistemine çevir
            formatted.convertNumerals()

        } catch (e: Exception) {
            defaultValueFor(itemType)
        }
    }

    fun formatWithSymbol(value: String?, itemType: ItemType, specificItem: String = ""): String {
        val formattedValue = format(value, itemType)
        return formattedValue // TL sembolü zaten currency_value string'inde var
    }

    private fun defaultValueFor(itemType: ItemType): String {
        return when (itemType) {
            ItemType.GOLD -> "0"
            else -> "0,00"
        }
    }
}



