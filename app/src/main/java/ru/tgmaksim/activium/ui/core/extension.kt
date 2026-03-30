package ru.tgmaksim.activium.ui.core

import kotlinx.coroutines.flow.MutableStateFlow

fun <T> MutableStateFlow<LoadState<T>>.setLoading(): LoadState<Nothing> {
    value = LoadState.Loading
    return LoadState.Loading
}

fun <T> MutableStateFlow<LoadState<T>>.setEmpty(): LoadState<Nothing> {
    value = LoadState.Empty
    return LoadState.Empty
}

fun <T> MutableStateFlow<LoadState<T>>.setSuccess(data: T): LoadState<T> {
    val success = LoadState.Success(data)
    value = success
    return success
}

fun <T> MutableStateFlow<LoadState<T>>.setError(
    message: UiText,
    unauthorized: Boolean = false
): LoadState<Nothing> {
    val error = LoadState.Error(message, unauthorized)
    value = error
    return error
}