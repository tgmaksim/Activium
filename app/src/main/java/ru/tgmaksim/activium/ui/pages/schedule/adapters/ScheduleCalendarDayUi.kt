package ru.tgmaksim.activium.ui.pages.schedule.adapters

import kotlinx.datetime.LocalDate

data class ScheduleCalendarDayUi(
    val date: LocalDate,
    val weekday: String,
    val dayNumber: String,
    val isSelected: Boolean
)