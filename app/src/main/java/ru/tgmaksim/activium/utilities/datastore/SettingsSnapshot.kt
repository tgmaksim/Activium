package ru.tgmaksim.activium.utilities.datastore

data class SettingsSnapshot(
    val sessionId: String?,
    val darkTheme: Boolean,
    val eaNotifications: Boolean,
    val beforeSchedule: Int,
    val afterSchedule: Int
)