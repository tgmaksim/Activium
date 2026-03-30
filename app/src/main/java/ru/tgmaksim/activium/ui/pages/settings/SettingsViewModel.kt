package ru.tgmaksim.activium.ui.pages.settings

import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Reviews
import ru.tgmaksim.activium.api.Settings
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.api.MyReviewResult
import ru.tgmaksim.activium.api.ChildrenResult
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.api.StatusDnevnikNotificationsResult

class SettingsViewModel : UiViewModel() {
    private val _childrenState = MutableStateFlow<LoadState<ChildrenResult>>(LoadState.Empty)
    val childrenState = _childrenState.asStateFlow()

    private val _statusDNState = MutableStateFlow<LoadState<StatusDnevnikNotificationsResult>>(LoadState.Empty)
    val statusDNState = _statusDNState.asStateFlow()

    private val _reviewState = MutableStateFlow<LoadState<MyReviewResult>>(LoadState.Empty)
    val reviewState = _reviewState.asStateFlow()

    fun updateTheme(darkTheme: Boolean) {
        viewModelScope.launch {
            SettingsManager.setDarkTheme(darkTheme)
        }
    }

    fun setOpenWebView(openWebView: Boolean) {
        viewModelScope.launch {
            SettingsManager.setOpenWebView(openWebView)
        }
    }

    fun setRangeSchedule(before: Int, after: Int) {
        viewModelScope.launch {
            SettingsManager.setBeforeSchedule(before)
            SettingsManager.setAfterSchedule(after)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _childrenState.value = LoadState.Empty
            _statusDNState.value = LoadState.Empty
            _reviewState.value = LoadState.Empty

            LoginActivity.logout()
        }
    }

    fun loadChildren() {
        viewModelScope.launch {
            executeRequest(
                _childrenState,
                "children",
                R.string.error_children,
                Settings::getChildren,
                { it.answer },
                { SettingsManager.setActiveChildId(it.activeChildId) }
            )
        }
    }

    fun loadDnevnikNotifications() {
        viewModelScope.launch {
            executeRequest(
                _statusDNState,
                "dnevnikNotifications",
                R.string.error_dnevnik_notifications,
                Settings::getStatusDnevnikNotifications,
                { it.answer }
            )
        }
    }

    fun switchDnevnikNotifications(status: Boolean) {
        viewModelScope.launch {
            executeRequest(
                _statusDNState,
                "switchDnevnikNotifications",
                R.string.error_dnevnik_notifications,
                { Settings.switchDnevnikNotifications(status) },
                { StatusDnevnikNotificationsResult(status = status) }
            )
        }
    }

    fun selectActiveChild(childId: Long) {
        viewModelScope.launch {
            executeRequest(
                _childrenState,
                { state -> _statusDNState.value = state },
                "selectChild",
                R.string.error_children,
                { Settings.setActiveChild(childId) },
                { it.answer }
            ) {
                SettingsManager.setActiveChildId(childId)
                executeRequest(
                    _statusDNState,
                    "dnevnikNotifications",
                    R.string.error_dnevnik_notifications,
                    Settings::getStatusDnevnikNotifications,
                    { it.answer }
                )
            }
        }
    }

    fun loadReview() {
        viewModelScope.launch {
            executeRequest(
                _reviewState,
                "review",
                R.string.error_review,
                Reviews::getMyReview,
                { it.answer }
            )
        }
    }

    fun deleteReview() {
        viewModelScope.launch {
            executeRequest(
                _reviewState,
                "deleteReview",
                R.string.error_delete_review,
                Reviews::deleteReview,
                { MyReviewResult(review = null, onModeration = false) }
            )
        }
    }

    fun sendReview(stars: Int, text: String?) {
        viewModelScope.launch {
            executeRequest(
                _reviewState,
                "sendReview",
                R.string.error_create_review,
                { Reviews.createReview(stars, text) },
                { it.answer }
            )
        }
    }
}