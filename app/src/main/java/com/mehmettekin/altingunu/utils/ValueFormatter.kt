package com.mehmettekin.altingunu.utils

import com.mehmettekin.altingunu.domain.model.ItemType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale



object ValueFormatter {

    fun format(value: String?, itemType: ItemType, locale: Locale = Locale.getDefault(), specificItem: String = ""): String {
        // Handle null or empty values
        if (value.isNullOrBlank()) {
            return if (itemType == ItemType.GOLD) "0" else "0${decimalSeparator(locale)}00"
        }

        try {
            // Parse the value to double to clean up extra zeros
            val parsedValue = value.toDoubleOrNull() ?: 0.0

            // Use DecimalFormat for precise control
            val formatter = DecimalFormat().apply {
                // Use Western digits by forcing US locale for symbols, but keep decimal/grouping separators
                val symbols = DecimalFormatSymbols(Locale.US).apply {
                    decimalSeparator = DecimalFormatSymbols(locale).decimalSeparator
                    groupingSeparator = DecimalFormatSymbols(locale).groupingSeparator
                }
                decimalFormatSymbols = symbols
                if (itemType == ItemType.GOLD) {
                    minimumFractionDigits = 0
                    maximumFractionDigits = 2
                    isGroupingUsed = true
                } else {
                    // Special case for currencies like JPY (no decimals)
                    val minimumDecimals = if (specificItem == "JPYTRY") 0 else 2
                    minimumFractionDigits = minimumDecimals
                    maximumFractionDigits = minimumDecimals
                    isGroupingUsed = true
                }
            }

            return formatter.format(parsedValue)
        } catch (e: Exception) {
            return if (itemType == ItemType.GOLD) "0" else "0${decimalSeparator(locale)}00"
        }
    }

    fun formatWithSymbol(value: String?, itemType: ItemType, specificItem: String = ""): String {
        val formattedValue = format(value, itemType)
        return formattedValue
    }

    // Helper function to get locale-specific decimal separator
    private fun decimalSeparator(locale: Locale): String {
        return DecimalFormatSymbols(locale).decimalSeparator.toString()
    }
}

/*
object ValueFormatter {

    fun format(value: String?, itemType: ItemType): String {
        // Handle null or empty values
        if (value.isNullOrBlank()) {
            return if (itemType == ItemType.GOLD) "0" else "0,00"
        }

        try {
            // Create locale-aware parsers and formatters
            val parser = DecimalFormat.getNumberInstance(Locale.getDefault())
            val formatter = DecimalFormat.getNumberInstance(Locale.getDefault())

            // Parse the value according to local conventions
            val parsedValue = try {
                parser.parse(value)?.toDouble() ?: 0.0
            } catch (e: Exception) {
                e.message
                value.toDoubleOrNull() ?: 0.0
            }

            // Format according to item type
            if (itemType == ItemType.GOLD) {
                // For gold, use integer with thousand separators
                formatter.minimumFractionDigits = 0
                formatter.maximumFractionDigits = 2
                return formatter.format(parsedValue)
            } else {
                // For currencies, ensure we always show 2 decimal places
                formatter.minimumFractionDigits = 0
                formatter.maximumFractionDigits = 2
                return formatter.format(parsedValue)
            }
        } catch (e: Exception) {
            e.message
            return if (itemType == ItemType.GOLD) "0" else "0,00"
        }
    }



    fun formatWithSymbol(value: String?, itemType: ItemType, specificItem: String = ""): String {
        val formattedValue = format(value, itemType)
        return formattedValue
    }
}


 */
