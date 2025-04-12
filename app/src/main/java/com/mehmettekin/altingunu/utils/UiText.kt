package com.mehmettekin.altingunu.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }.toString()
    }

    companion object {
        fun dynamicString(value: String): UiText = DynamicString(value)
        fun stringResource(@StringRes resId: Int, vararg args: Any): UiText = StringResource(resId, *args)
    }
}