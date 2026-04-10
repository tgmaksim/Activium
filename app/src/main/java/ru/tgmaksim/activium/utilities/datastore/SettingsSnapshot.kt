package ru.tgmaksim.activium.utilities.datastore

data class SettingsSnapshot(
    val sessionId: String?,
    val activeChildId: Long,
    val darkTheme: Boolean,
    val beforeSchedule: Int,
    val afterSchedule: Int,
    val lastMarksPeriod: Int,
    val showNullSubjectMarks: Boolean
)