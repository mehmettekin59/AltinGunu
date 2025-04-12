package com.mehmettekin.altingunu.utils

sealed class ResultState<out T> {
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val message: UiText) : ResultState<Nothing>()
    data object Loading : ResultState<Nothing>()
    data object Idle : ResultState<Nothing>()

    fun isLoading(): Boolean = this is Loading
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    inline fun onSuccess(action: (T) -> Unit): ResultState<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (UiText) -> Unit): ResultState<T> {
        if (this is Error) action(message)
        return this
    }

    inline fun onLoading(action: () -> Unit): ResultState<T> {
        if (this is Loading) action()
        return this
    }
}