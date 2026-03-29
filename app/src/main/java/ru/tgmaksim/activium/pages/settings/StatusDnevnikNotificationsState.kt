package ru.tgmaksim.activium.pages.settings

import ru.tgmaksim.activium.pages.UiText
import ru.tgmaksim.activium.api.StatusDnevnikNotificationsResult

sealed class StatusDnevnikNotificationsState {
    data object Null : StatusDnevnikNotificationsState()
    data object Loading : StatusDnevnikNotificationsState()
    data class Error(val message: UiText, val unauthorized: Boolean = false) : StatusDnevnikNotificationsState()
    data class Success(val data: StatusDnevnikNotificationsResult) : StatusDnevnikNotificationsState()
}