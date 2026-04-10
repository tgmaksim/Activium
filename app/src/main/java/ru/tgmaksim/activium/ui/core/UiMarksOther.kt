package ru.tgmaksim.activium.ui.core

import ru.tgmaksim.activium.api.MarkLog

data class UiMarksOther(
    val number: Int?,
    val name: String,
    val personKey: String?,
    val isHighlighting: Boolean?,
    val marks: List<MarkLog>,
    val isOldMark: Boolean = false
)