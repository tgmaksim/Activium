package ru.tgmaksim.activium.ui.core

sealed class UiText {
    data class StringResource(
        val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    data class DynamicString(
        val value: String
    ) : UiText()
}