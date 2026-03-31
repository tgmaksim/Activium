package ru.tgmaksim.activium.ui.pages.schedule

import ru.tgmaksim.activium.api.ScheduleDay

data class UiScheduleResult(
    val schedule: List<ScheduleDay?>,
    val timezone: Int,
    val hasAbilityPraise: Boolean
)