package com.mehmettekin.altingunu.utils

import java.util.Locale

object Constraints {

    // Çok dilli currency isimleri
    private val currencyCodeToNameMultilingual = mapOf(
        "tr" to mapOf(
            "JPYTRY" to "Japon Yeni",
            "CADTRY" to "Kanada Doları",
            "SARTRY" to "Arabistan Riyali",
            "EURTRY" to "Euro",
            "USDTRY" to "Dolar",
            "GBPTRY" to "Sterlin",
            "CHFTRY" to "İsviçre Frangı",
            "NOKTRY" to "Norveç Kronu",
            "DKKTRY" to "Danimarka Kronu",
            "SEKTRY" to "İsveç Kronu"
        ),
        "ar" to mapOf(
            "JPYTRY" to "ين ياباني",
            "CADTRY" to "دولار كندي",
            "SARTRY" to "ريال سعودي",
            "EURTRY" to "يورو",
            "USDTRY" to "دولار",
            "GBPTRY" to "جنيه إسترليني",
            "CHFTRY" to "فرنك سويسري",
            "NOKTRY" to "كرونة نرويجية",
            "DKKTRY" to "كرونة دنماركية",
            "SEKTRY" to "كرونة سويدية"
        ),
        "en" to mapOf(
            "JPYTRY" to "Japanese Yen",
            "CADTRY" to "Canadian Dollar",
            "SARTRY" to "Saudi Riyal",
            "EURTRY" to "Euro",
            "USDTRY" to "US Dollar",
            "GBPTRY" to "British Pound Sterling",
            "CHFTRY" to "Swiss Franc",
            "NOKTRY" to "Norwegian Krone",
            "DKKTRY" to "Danish Krone",
            "SEKTRY" to "Swedish Krona"
        )
    )

    // Çok dilli gold isimleri
    private val goldCodeToNameMultilingual = mapOf(
        "tr" to mapOf(
            "TEK_ESKI" to "Tam Cumhuriyet (Eski)",
            "TEK_YENI" to "Tam Cumhuriyet (Yeni)",
            "ATA_ESKI" to "Ata (Eski)",
            "ATA_YENI" to "Ata (Yeni)",
            "ALTIN" to "Gram Altın",
            "CEYREK_ESKI" to "Çeyrek (Eski)",
            "CEYREK_YENI" to "Çeyrek (Yeni)",
            "YARIM_ESKI" to "Yarım (Eski)",
            "YARIM_YENI" to "Yarım (Yeni)",
            "AYAR22" to "22 Ayar Altın",
            "AYAR14" to "14 Ayar Altın",
            "ATA5_ESKI" to "5li Ata (Eski)",
            "ATA5_YENI" to "5li Ata (Yeni)",
            "GREMESE_ESKI" to "Gremse (Eski)",
            "GREMESE_YENI" to "Gremse (Yeni)"
        ),
        "ar" to mapOf(
            "TEK_ESKI" to "جمهورية كاملة (قديمة)",
            "TEK_YENI" to "جمهورية كاملة (جديدة)",
            "ATA_ESKI" to "عطا (قديم)",
            "ATA_YENI" to "عطا (جديد)",
            "ALTIN" to "ذهب غرام",
            "CEYREK_ESKI" to "ربع (قديم)",
            "CEYREK_YENI" to "ربع (جديد)",
            "YARIM_ESKI" to "نصف (قديم)",
            "YARIM_YENI" to "نصف (جديد)",
            "AYAR22" to "ذهب عيار 22",
            "AYAR14" to "ذهب عيار 14",
            "ATA5_ESKI" to "عطا خماسي (قديم)",
            "ATA5_YENI" to "عطا خماسي (جديد)",
            "GREMESE_ESKI" to "غرامس (قديم)",
            "GREMESE_YENI" to "غرامس (جديد)",
        ),
        "en" to mapOf(
            "TEK_ESKI" to "Full Republic (Old)",
            "TEK_YENI" to "Full Republic (New)",
            "ATA_ESKI" to "Ata (Old)",
            "ATA_YENI" to "Ata (New)",
            "ALTIN" to "Gram Gold",
            "CEYREK_ESKI" to "Quarter (Old)",
            "CEYREK_YENI" to "Quarter (New)",
            "YARIM_ESKI" to "Half (Old)",
            "YARIM_YENI" to "Half (New)",
            "AYAR22" to "22 Carat Gold",
            "AYAR14" to "14 Carat Gold",
            "ATA5_ESKI" to "5 Ata (Old)",
            "ATA5_YENI" to "5 Ata (New)",
            "GREMESE_ESKI" to "Gremse (Old)",
            "GREMESE_YENI" to "Gremse (New)"
        )
    )

    // Mevcut dil için global değişken
    private var currentLanguage: String = DEFAULT_LANGUAGE


    fun setLanguage(languageCode: String) {
        currentLanguage = languageCode
    }


    //Mevcut dile göre currency isimleri - İsim aynı kalıyor!
    val currencyCodeToName: Map<String, String>
        get() = currencyCodeToNameMultilingual[currentLanguage]
            ?: currencyCodeToNameMultilingual[DEFAULT_LANGUAGE]
            ?: emptyMap()


    //Mevcut dile göre gold isimleri - İsim aynı kalıyor!

    val goldCodeToName: Map<String, String>
        get() = goldCodeToNameMultilingual[currentLanguage]
            ?: goldCodeToNameMultilingual[DEFAULT_LANGUAGE]
            ?: emptyMap()

    // Listeleri (bu değişmez, sadece key'ler)
    val goldCodeList = goldCodeToNameMultilingual["tr"]?.keys?.toList() ?: emptyList()
    val currencyCodeList = currencyCodeToNameMultilingual["tr"]?.keys?.toList() ?: emptyList()

    // DataStore keys
    object DataStoreNames {
        const val SETTINGS_PREFERENCES = "settings_preferences"
    }

    object DataStoreKeys {
        const val API_UPDATE_INTERVAL = "api_update_interval"
        const val LANGUAGE_CODE = "language_code"
        const val IS_FIRST_LAUNCH = "is_first_launch"
    }

    private const val DEFAULT_LANGUAGE = "tr"
    val SUPPORTED_LANGUAGES = setOf("tr", "en", "ar")

    object DefaultSettings {
        const val DEFAULT_API_UPDATE_INTERVAL = 60 // saniye
        const val DEFAULT_LANGUAGE = "tr"
    }

}
