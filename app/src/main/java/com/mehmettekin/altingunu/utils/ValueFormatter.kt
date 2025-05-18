package com.mehmettekin.altingunu.utils

import com.mehmettekin.altingunu.domain.model.ItemType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale



object ValueFormatter {

    fun format(value: String?, itemType: ItemType, locale: Locale = Locale.getDefault(), specificItem: String = ""): String {
        // Handle null or empty values
        if (value.isNullOrBlank()) {
            return defaultValueFor(itemType, locale)
        }

        val result = tryFormatValue(value, itemType, locale)

        return when (result) {
            is ResultState.Success -> result.data
            is ResultState.Error -> {
                // İsteğe bağlı olarak hatayı loglamak için
                // Log.e("ValueFormatter", "Value formatting error: ${result.message.asString()}")
                defaultValueFor(itemType, locale)
            }
            else -> defaultValueFor(itemType, locale) // Loading ve Idle durumları için varsayılan değer
        }
    }

    private fun tryFormatValue(value: String?, itemType: ItemType, locale: Locale): ResultState<String> {
        return try {
            // Parse the value to double to clean up extra zeros
            val parsedValue = value?.toDoubleOrNull() ?: 0.0

            // Use DecimalFormat for precise control
            val formatter = DecimalFormat().apply {
                // Use Western digits by forcing US locale for symbols, but keep decimal/grouping separators
                val symbols = DecimalFormatSymbols(Locale.US).apply {
                    decimalSeparator = DecimalFormatSymbols(locale).decimalSeparator
                    groupingSeparator = DecimalFormatSymbols(locale).groupingSeparator
                }
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

            val formatted = formatter.format(parsedValue).convertNumerals(locale)
            ResultState.Success(formatted)

        } catch (e: Exception) {
            ResultState.Error(UiText.dynamicString(e.message ?: "Formatlama hatası"))
        }
    }

    fun formatWithSymbol(value: String?, itemType: ItemType, specificItem: String = ""): String {
        return format(value, itemType, Locale.getDefault(), specificItem)
    }

    // Varsayılan değerler için yardımcı fonksiyon
    private fun defaultValueFor(itemType: ItemType, locale: Locale): String {
        return if (itemType == ItemType.GOLD) {
            "0"
        } else {
            "0${decimalSeparator(locale)}00"
        }
    }

    // Ondalık ayırıcıyı alma yardımcı fonksiyonu
    private fun decimalSeparator(locale: Locale): String {
        return DecimalFormatSymbols(locale).decimalSeparator.toString()
    }
}



