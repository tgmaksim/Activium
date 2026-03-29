package ru.tgmaksim.activium.ui.main

import ru.tgmaksim.activium.pages.UiText
import ru.tgmaksim.activium.api.VersionsResult

sealed class VersionState {
    data object Null : VersionState()
    data object Loading : VersionState()
    data class Error(val message: UiText) : VersionState()
    data class Success(val data: VersionsResult) : VersionState()
}