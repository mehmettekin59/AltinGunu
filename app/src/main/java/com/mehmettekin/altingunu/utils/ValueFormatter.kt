package com.mehmettekin.altingunu.utils

import com.mehmettekin.altingunu.domain.model.ItemType
import java.text.NumberFormat
import java.util.Locale



object ValueFormatter {

    fun format(value: String?, itemType: ItemType): String {
        // Handle null or empty values
        if (value.isNullOrBlank()) {
            return if (itemType == ItemType.GOLD) "0" else "0,00"
        }

        try {
            // Create locale-aware parsers and formatters
            val parser = NumberFormat.getNumberInstance(Locale.getDefault())
            val formatter = NumberFormat.getNumberInstance(Locale.getDefault())

            // Parse the value according to local conventions
            val parsedValue = try {
                parser.parse(value)?.toDouble() ?: 0.0
            } catch (e: Exception) {
                // Fallback - try direct conversion if parsing fails
                value.toDoubleOrNull() ?: 0.0
            }

            // Format according to item type
            if (itemType == ItemType.GOLD) {
                // For gold, use integer with thousand separators
                formatter.minimumFractionDigits = 0
                formatter.maximumFractionDigits = 0
                return formatter.format(parsedValue.toInt())
            } else {
                // For currencies, ensure we always show 2 decimal places
                formatter.minimumFractionDigits = 2
                formatter.maximumFractionDigits = 2
                return formatter.format(parsedValue)
            }
        } catch (e: Exception) {
            return if (itemType == ItemType.GOLD) "0" else "0,00"
        }
    }



    fun formatWithSymbol(value: String?, itemType: ItemType, specificItem: String = ""): String {
        val formattedValue = format(value, itemType)
        return formattedValue
    }
}