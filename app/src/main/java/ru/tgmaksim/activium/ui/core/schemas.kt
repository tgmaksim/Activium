package ru.tgmaksim.activium.ui.core

import kotlin.time.Instant

import ru.tgmaksim.activium.api.MarkLog
import ru.tgmaksim.activium.api.WorkType

data class UiMarksOther(
    val number: Int?,
    val name: String,
    val personKey: String?,
    val isHighlighting: Boolean?,
    val marks: List<MarkLog>,
    val isOldMark: Boolean = false
)

data class UiMarkLog(
    val mood: String,
    val value: String,
    val work: WorkType?,
    val created: Instant?,
    val ratingKey: String?,
    val lessonKey: String?
)