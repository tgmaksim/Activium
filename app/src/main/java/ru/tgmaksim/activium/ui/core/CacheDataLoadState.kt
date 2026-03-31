package ru.tgmaksim.activium.ui.core

sealed interface CacheDataLoadState {
    data object Empty : CacheDataLoadState
    data object CacheLoading : CacheDataLoadState
    data object CacheSuccess : CacheDataLoadState
    data class CacheError(val message: UiText) : CacheDataLoadState
    data object CloudLoading : CacheDataLoadState
    data object CloudSuccess : CacheDataLoadState
    data class CloudError(val message: UiText, val unauthorized: Boolean = false) : CacheDataLoadState
    data object ShownError : CacheDataLoadState
}