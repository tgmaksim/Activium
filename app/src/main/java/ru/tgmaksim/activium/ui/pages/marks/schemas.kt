package ru.tgmaksim.activium.ui.pages.marks

import ru.tgmaksim.activium.api.MarkLast
import ru.tgmaksim.activium.api.MarksSubjectFinal
import ru.tgmaksim.activium.api.MarksSubjectPeriod

data class UiMarksResult(
    val recentMarks: List<MarkLast>,
    val periodMarks: List<MarksSubjectPeriod>,
    val ratingKey: String?,
    val finalMarks: List<MarksSubjectFinal>
)