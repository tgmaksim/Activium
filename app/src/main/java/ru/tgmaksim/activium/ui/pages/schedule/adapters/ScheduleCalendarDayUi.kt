package ru.tgmaksim.activium.ui.pages.schedule.adapters

import kotlinx.datetime.LocalDate

data class ScheduleCalendarDayUi(
    val date: LocalDate,
    val weekday: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isWeekend: Boolean
)