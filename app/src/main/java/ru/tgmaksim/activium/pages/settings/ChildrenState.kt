package ru.tgmaksim.activium.pages.settings

import ru.tgmaksim.activium.pages.UiText
import ru.tgmaksim.activium.api.ChildrenResult

sealed class ChildrenState {
    data object Null : ChildrenState()
    data object Loading : ChildrenState()
    data class Error(val message: UiText, val unauthorized: Boolean = false) : ChildrenState()
    data class Success(val data: ChildrenResult) : ChildrenState()
}