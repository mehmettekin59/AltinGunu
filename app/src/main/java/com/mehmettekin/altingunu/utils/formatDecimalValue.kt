package com.mehmettekin.altingunu.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

fun formatDecimalValue(value: String?, formatter: DecimalFormat): String {
    // 1. Girdi null veya boş mu kontrol et
    if (value.isNullOrBlank()) {
        return "0,00" // Null/blank için sabit fallback
    }

    // 2. Gelen String'i Double'a çevirmeyi dene.
    val number = value.toDoubleOrNull()

    // 3. Çevirme başarılı oldu mu?
    return if (number != null) {
        // 4. Başarılıysa, Locale'e uygun formatter ile formatla
        try {
            formatter.format(number)
        } catch (e: Exception) {
            // 5. Formatlama sırasında hata olursa (örn. çok büyük/küçük sayı, NaN/Infinity),
            //    daha basit, bölgeden bağımsız bir format veya yuvarlama dene
            println("Formatter Error: ${e.message} for number $number. Attempting simpler format.")
            try {
                // Basit, bölgeden bağımsız formatlama (Nokta ayırıcı, en fazla 2 ondalık)
                // Locale.ROOT kullanarak tamamen bölgeden bağımsız olmasını sağla
                val simpleFormatter = NumberFormat.getNumberInstance(Locale.ROOT) as DecimalFormat
                simpleFormatter.apply {
                    maximumFractionDigits = 2 // En fazla 2 ondalık göster
                    minimumFractionDigits = 2 // Gereksiz sondaki sıfırları gösterme (0.50 -> 0.5)
                    isGroupingUsed = false    // Binlik ayırıcı kullanma
                }
                simpleFormatter.format(number)

                // Alternatif: Tam sayıya yuvarlama
                // kotlin.math.roundToLong(number).toString()

            } catch (fallbackException: Exception) {
                // Eğer basit formatlama/yuvarlama da hata verirse
                println("Fallback Formatting Error: ${fallbackException.message}")
                "-" // Son çare: Sabit fallback
            }
        }
    } else {

        println("Parsing Error: Could not parse '$value' to Double. Returning fixed fallback.")
        value
    }
}