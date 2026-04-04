package ru.tgmaksim.activium.ui.pages.schedule

import kotlinx.datetime.LocalDate

import ru.tgmaksim.activium.ui.core.LoadState

import ru.tgmaksim.activium.api.MarkLog
import ru.tgmaksim.activium.api.WorkType
import ru.tgmaksim.activium.api.MarksOther
import ru.tgmaksim.activium.api.ScheduleHours
import ru.tgmaksim.activium.api.ScheduleHomeworkDocument
import ru.tgmaksim.activium.api.ScheduleExtracurricularActivity

data class UiScheduleResult(
    val schedule: List<UiScheduleDay?>,
    val timezone: Int,
    val hasAbilityPraise: Boolean
)

data class UiScheduleDay(
    val date: LocalDate,
    val lessons: List<UiScheduleLesson>,
    val ea: List<ScheduleExtracurricularActivity>
)

data class UiScheduleLesson(
    val lessonKey: String?,
    val number: Int,
    val subject: String,
    val place: String,
    val hours: ScheduleHours,
    val works: List<WorkType>,
    val logs: List<MarkLog>,
    val othersMarks: List<MarksOther>,
    val avgGroupLessonMark: MarkLog?,
    val homework: String?,
    val note: String?,
    val files: List<ScheduleHomeworkDocument>,
    val ratingKey: String?,
    val praiseState: LoadState<Unit>?,
    val isExtra: Boolean = false
) {
    init {
        if (!isExtra && lessonKey == null)
            throw ClassCastException()
    }
}