package com.mehmettekin.altingunu.utils


object Constraints {

    val goldCodeToName = mapOf(
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
        "ATA5_ESKI" to "5'li Ata (Eski)",
        "ATA5_YENI" to "5'li Ata (Yeni)",
        "GREMESE_ESKI" to "Gremse Altın(Eski)",
        "GREMESE_YENI" to "Gremse Altın(Yeni)"
    )

    val currencyCodeToName = mapOf(
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
    )

    // Listeleri de burada oluşturabiliriz (opsiyonel, UI'da da yapılabilir)
    val goldCodeList = goldCodeToName.keys.toList()
    val currencyCodeList = currencyCodeToName.keys.toList()


    // DataStore keys
    object DataStoreKeys {
        const val SETTINGS_DATASTORE = "settings_datastore"
        const val API_UPDATE_INTERVAL = "api_update_interval"
        const val LANGUAGE_CODE = "language_code"
    }

    // Default settings
    object DefaultSettings {
        const val DEFAULT_API_UPDATE_INTERVAL = 30 // seconds
        const val DEFAULT_LANGUAGE = "tr" // Turkish
    }
}