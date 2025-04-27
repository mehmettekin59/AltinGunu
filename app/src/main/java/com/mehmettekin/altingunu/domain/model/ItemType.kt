package com.mehmettekin.altingunu.domain.model

import com.mehmettekin.altingunu.utils.UiText
import com.mehmettekin.altingunu.R

enum class ItemType {
    TL, CURRENCY, GOLD;

    val displayName: UiText
        get() = when (this) {
            TL -> UiText.dynamicString("TL")
            CURRENCY -> UiText.stringResource(R.string.item_type_currency)
            GOLD -> UiText.stringResource(R.string.item_type_gold)
        }
}
