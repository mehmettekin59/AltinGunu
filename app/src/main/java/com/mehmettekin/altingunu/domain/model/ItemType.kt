package com.mehmettekin.altingunu.domain.model

enum class ItemType {
    TL, CURRENCY, GOLD;

    val displayName: String
        get() = when (this) {
            TL -> "TL"
            CURRENCY -> "Döviz ($, €, £)"
            GOLD -> "Altın (🥇, 🪙, 💰)"
        }
}
