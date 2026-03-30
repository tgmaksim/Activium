package ru.tgmaksim.activium.ui.core

sealed interface LoadState<out T> {
    data object Empty : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Error(val message: UiText, val unauthorized: Boolean = false) : LoadState<Nothing>
    data class Success<out T>(val data: T) : LoadState<T>
}