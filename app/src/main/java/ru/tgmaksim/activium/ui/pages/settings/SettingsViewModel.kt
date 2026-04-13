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
import ru.tgmaksim.activium.ui.core.setShownError
import ru.tgmaksim.activium.api.ReferralParamsResult
import ru.tgmaksim.activium.api.StatusEANotificationsResult
import ru.tgmaksim.activium.api.StatusMarksNotificationsResult
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class SettingsViewModel : UiViewModel() {
    enum class StateType { Children, StatusMarksNotifications, StatusEANotifications, Review, ReferralParams }

    private val _childrenState = MutableStateFlow<LoadState<ChildrenResult>>(LoadState.Empty)
    val childrenState = _childrenState.asStateFlow()

    private val _statusMarksNotificationsState = MutableStateFlow<LoadState<StatusMarksNotificationsResult>>(LoadState.Empty)
    val statusMarksNotificationsState = _statusMarksNotificationsState.asStateFlow()

    private val _statusEANotificationsState = MutableStateFlow<LoadState<StatusEANotificationsResult>>(LoadState.Empty)
    val statusEANotificationsState = _statusEANotificationsState.asStateFlow()

    private val _reviewState = MutableStateFlow<LoadState<MyReviewResult>>(LoadState.Empty)
    val reviewState = _reviewState.asStateFlow()

    private val _referralState = MutableStateFlow<LoadState<ReferralParamsResult>>(LoadState.Empty)
    val referralState = _referralState.asStateFlow()

    fun resetError(stateType: StateType) {
        when (stateType) {
            StateType.Children -> _childrenState.setShownError()
            StateType.StatusMarksNotifications -> _statusMarksNotificationsState.setShownError()
            StateType.StatusEANotifications -> _statusEANotificationsState.setShownError()
            StateType.Review -> _reviewState.setShownError()
            StateType.ReferralParams -> _referralState.setShownError()
        }
    }

    fun updateTheme(darkTheme: Boolean) {
        viewModelScope.launch {
            SettingsManager.setDarkTheme(darkTheme)
        }
    }

    fun setRangeSchedule(before: Int, after: Int) {
        viewModelScope.launch {
            SettingsManager.setBeforeSchedule(before)
            SettingsManager.setAfterSchedule(after)
        }
    }

    fun setLastMarksPeriod(period: Int) {
        viewModelScope.launch {
            SettingsManager.setLastMarksPeriod(period)
        }
    }

    fun logout() {
        viewModelScope.launch {
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

    fun loadMarksNotifications() {
        viewModelScope.launch {
            executeRequest(
                _statusMarksNotificationsState,
                "marksNotifications",
                R.string.error_marks_notifications,
                Settings::getStatusMarksNotifications,
                { it.answer }
            )
        }
    }

    fun loadEANotifications() {
        viewModelScope.launch {
            executeRequest(
                _statusEANotificationsState,
                "eaNotifications",
                R.string.error_ea_notifications,
                Settings::getStatusEANotifications,
                { it.answer }
            )
        }
    }

    fun switchMarksNotifications(status: Boolean) {
        viewModelScope.launch {
            executeRequest(
                _statusMarksNotificationsState,
                "switchMarksNotifications",
                R.string.error_marks_notifications,
                { Settings.switchMarksNotifications(status) },
                { StatusMarksNotificationsResult(status = status) }
            )
        }
    }

    fun switchEANotifications(status: Boolean) {
        viewModelScope.launch {
            executeRequest(
                _statusEANotificationsState,
                "switchEANotifications",
                R.string.error_ea_notifications,
                { Settings.switchEANotifications(status) },
                { StatusEANotificationsResult(status = status) }
            )
        }
    }

    fun selectActiveChild(childId: Long) {
        viewModelScope.launch {
            executeRequest(
                _childrenState,
                { state ->
                    _statusMarksNotificationsState.value = state
                    _statusEANotificationsState.value = state
                },
                "selectChild",
                R.string.error_children,
                { Settings.setActiveChild(childId) },
                { it.answer }
            ) {
                SettingsManager.setActiveChildId(childId)
                executeRequest(
                    _statusMarksNotificationsState,
                    "marksNotifications",
                    R.string.error_marks_notifications,
                    Settings::getStatusMarksNotifications,
                    { it.answer }
                )
                executeRequest(
                    _statusEANotificationsState,
                    "eaNotifications",
                    R.string.error_ea_notifications,
                    Settings::getStatusEANotifications,
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

    fun updateShowNullSubjectMarks(show: Boolean) {
        viewModelScope.launch {
            SettingsManager.setShowNullSubjectMarks(show)
        }
    }

    fun loadReferralParams() {
        viewModelScope.launch {
            executeRequest(
                _referralState,
                "referralParams",
                R.string.error_referral,
                Settings::getReferralParams,
                { it.answer }
            )
        }
    }
}