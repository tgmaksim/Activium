package ru.tgmaksim.activium.pages

sealed class UiText {
    data class StringResource(
        val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    data class DynamicString(
        val value: String
    ) : UiText()
}