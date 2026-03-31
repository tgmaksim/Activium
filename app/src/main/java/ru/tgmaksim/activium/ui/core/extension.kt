package ru.tgmaksim.activium.ui.core

import kotlinx.coroutines.flow.MutableStateFlow

fun <T> MutableStateFlow<LoadState<T>>.setLoading(): LoadState.Loading {
    value = LoadState.Loading
    return LoadState.Loading
}

fun <T> MutableStateFlow<LoadState<T>>.setSuccess(data: T): LoadState.Success<T> {
    val success = LoadState.Success(data)
    value = success
    return success
}

fun <T> MutableStateFlow<LoadState<T>>.setError(
    message: UiText,
    unauthorized: Boolean = false
): LoadState.Error {
    val error = LoadState.Error(message, unauthorized)
    value = error
    return error
}

fun <T> MutableStateFlow<LoadState<T>>.setShownError(): LoadState.ShownError {
    value = LoadState.ShownError
    return LoadState.ShownError
}

fun MutableStateFlow<CacheDataLoadState>.setCacheLoading(): CacheDataLoadState.CacheLoading {
    value = CacheDataLoadState.CacheLoading
    return CacheDataLoadState.CacheLoading
}

fun MutableStateFlow<CacheDataLoadState>.setCacheSuccess(): CacheDataLoadState.CacheSuccess {
    value = CacheDataLoadState.CacheSuccess
    return CacheDataLoadState.CacheSuccess
}

fun MutableStateFlow<CacheDataLoadState>.setCacheError(message: UiText): CacheDataLoadState.CacheError {
    val error = CacheDataLoadState.CacheError(message)
    value = error
    return error
}

fun MutableStateFlow<CacheDataLoadState>.setCloudLoading(): CacheDataLoadState.CloudLoading {
    value = CacheDataLoadState.CloudLoading
    return CacheDataLoadState.CloudLoading
}

fun MutableStateFlow<CacheDataLoadState>.setCloudSuccess(): CacheDataLoadState.CloudSuccess {
    value = CacheDataLoadState.CloudSuccess
    return CacheDataLoadState.CloudSuccess
}

fun MutableStateFlow<CacheDataLoadState>.setCloudError(
    message: UiText,
    unauthorized: Boolean = false
): CacheDataLoadState.CloudError {
    val error = CacheDataLoadState.CloudError(message, unauthorized)
    value = error
    return error
}

fun MutableStateFlow<CacheDataLoadState>.setShownError(): CacheDataLoadState.ShownError {
    value = CacheDataLoadState.ShownError
    return CacheDataLoadState.ShownError
}