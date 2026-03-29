package ru.tgmaksim.activium.pages.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import java.util.concurrent.CancellationException

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Request
import ru.tgmaksim.activium.api.Reviews
import ru.tgmaksim.activium.api.Settings
import ru.tgmaksim.activium.pages.UiText
import ru.tgmaksim.activium.api.MyReviewResult
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.api.StatusDnevnikNotificationsResult
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

class SettingsViewModel : ViewModel() {
    private val _childrenState = MutableStateFlow<ChildrenState>(ChildrenState.Null)
    val childrenState = _childrenState.asStateFlow()

    private val _statusDNState = MutableStateFlow<StatusDnevnikNotificationsState>(StatusDnevnikNotificationsState.Null)
    val statusDNState = _statusDNState.asStateFlow()

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Null)
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
            _childrenState.value = ChildrenState.Null
            _statusDNState.value = StatusDnevnikNotificationsState.Null
            _reviewState.value = ReviewState.Null

            MemoryDataManager.sessionId.value = null
            SettingsManager.setSessionId(null)
        }
    }

    fun loadChildren() {
        viewModelScope.launch {
            _childrenState.value = ChildrenState.Loading

            try {
                val response = Settings.getChildren()

                if (!response.status || response.answer == null) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at children: ${response.error.errorMessage}")

                    val unauthorized = response.error?.type == "Unauthorized"
                    if (response.error?.errorMessage != null)
                        _childrenState.value = ChildrenState.Error(UiText.DynamicString(response.error.errorMessage), unauthorized)
                    else
                        _childrenState.value = ChildrenState.Error(UiText.StringResource(R.string.error_api), unauthorized)
                } else {
                    _childrenState.value = ChildrenState.Success(response.answer)
                }
            } catch (_: CancellationException) {
                _childrenState.value = ChildrenState.Null
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet())
                    _childrenState.value = ChildrenState.Error(UiText.StringResource(R.string.error_internet))
                else
                    _childrenState.value = ChildrenState.Error(UiText.StringResource(R.string.error_children))
            }
        }
    }

    fun loadDnevnikNotifications() {
        viewModelScope.launch {
            fetchDnevnikNotifications()
        }
    }

    private suspend fun fetchDnevnikNotifications() {
        _statusDNState.value = StatusDnevnikNotificationsState.Loading

        try {
            val response = Settings.getStatusDnevnikNotifications()

            if (!response.status || response.answer == null) {
                if (response.error != null)
                    Utilities.log("API error(${response.error.type}) at dnevnikNotifications: ${response.error.errorMessage}")

                val unauthorized = response.error?.type == "UnauthorizedError"
                if (response.error?.errorMessage != null)
                    _statusDNState.value = StatusDnevnikNotificationsState.Error(UiText.DynamicString(response.error.errorMessage), unauthorized)
                else
                    _statusDNState.value = StatusDnevnikNotificationsState.Error(UiText.StringResource(R.string.error_api), unauthorized)
            } else {
                _statusDNState.value = StatusDnevnikNotificationsState.Success(response.answer)
            }
        } catch (_: CancellationException) {
            _statusDNState.value = StatusDnevnikNotificationsState.Null
        } catch (e: Exception) {
            Utilities.log(e)
            if (!Request.checkInternet())
                _statusDNState.value = StatusDnevnikNotificationsState.Error(UiText.StringResource(R.string.error_internet))
            else
                _statusDNState.value = StatusDnevnikNotificationsState.Error(UiText.StringResource(R.string.error_children))
        }
    }

    fun switchDnevnikNotifications(status: Boolean) {
        viewModelScope.launch {
            _statusDNState.value = StatusDnevnikNotificationsState.Loading

            try {
                val response = Settings.switchDnevnikNotifications(status)

                if (!response.status) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at switchDnevnikNotifications: ${response.error.errorMessage}")

                    val unauthorized = response.error?.type == "UnauthorizedError"
                    if (response.error?.errorMessage != null)
                        _statusDNState.value = StatusDnevnikNotificationsState.Error(UiText.DynamicString(response.error.errorMessage), unauthorized)
                    else
                        _statusDNState.value = StatusDnevnikNotificationsState.Error(UiText.StringResource(R.string.error_api), unauthorized)
                } else {
                    _statusDNState.value = StatusDnevnikNotificationsState.Success(StatusDnevnikNotificationsResult(status = status))
                }
            } catch (_: CancellationException) {
                _statusDNState.value = StatusDnevnikNotificationsState.Null
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet())
                    _statusDNState.value = StatusDnevnikNotificationsState.Error(UiText.StringResource(R.string.error_internet))
                else
                    _statusDNState.value = StatusDnevnikNotificationsState.Error(UiText.StringResource(R.string.error_children))
            }
        }
    }

    fun selectActiveChild(childId: Long) {
        viewModelScope.launch {
            _childrenState.value = ChildrenState.Loading
            _statusDNState.value = StatusDnevnikNotificationsState.Loading

            try {
                val response = Settings.setActiveChild(childId)

                if (!response.status || response.answer == null) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at selectChild: ${response.error.errorMessage}")

                    val unauthorized = response.error?.type == "UnauthorizedError"
                    if (response.error?.errorMessage != null) {
                        val uiText = UiText.DynamicString(response.error.errorMessage)
                        _childrenState.value = ChildrenState.Error(uiText, unauthorized)
                        _statusDNState.value = StatusDnevnikNotificationsState.Error(uiText, unauthorized)
                    } else {
                        val uiText = UiText.StringResource(R.string.error_api)
                        _childrenState.value = ChildrenState.Error(uiText, unauthorized)
                        _statusDNState.value = StatusDnevnikNotificationsState.Error(uiText, unauthorized)
                    }
                } else {
                    _childrenState.value = ChildrenState.Success(response.answer)
                    SettingsManager.setActiveChildId(childId)

                    fetchDnevnikNotifications()
                }
            } catch (_: CancellationException) {
                _childrenState.value = ChildrenState.Null
                _statusDNState.value = StatusDnevnikNotificationsState.Null
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet()) {
                    val uiText = UiText.StringResource(R.string.error_internet)
                    _childrenState.value = ChildrenState.Error(uiText)
                    _statusDNState.value = StatusDnevnikNotificationsState.Error(uiText)
                } else {
                    val uiText = UiText.StringResource(R.string.error_children)
                    _childrenState.value = ChildrenState.Error(uiText)
                    _statusDNState.value = StatusDnevnikNotificationsState.Error(uiText)
                }
            }
        }
    }

    fun loadReview() {
        viewModelScope.launch {
            _reviewState.value = ReviewState.Loading

            try {
                val response = Reviews.getMyReview()

                if (!response.status || response.answer == null) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at review: ${response.error.errorMessage}")

                    val unauthorized = response.error?.type == "UnauthorizedError"
                    if (response.error?.errorMessage != null)
                        _reviewState.value = ReviewState.Error(UiText.DynamicString(response.error.errorMessage), unauthorized)
                    else
                        _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_api), unauthorized)
                } else {
                    _reviewState.value = ReviewState.Success(response.answer)
                }
            } catch (_: CancellationException) {
                _reviewState.value = ReviewState.Null
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet())
                    _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_internet))
                else
                    _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_children))
            }
        }
    }

    fun deleteReview() {
        viewModelScope.launch {
            _reviewState.value = ReviewState.Loading

            try {
                val response = Reviews.deleteReview()

                if (!response.status || response.answer == null) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at review: ${response.error.errorMessage}")

                    val unauthorized = response.error?.type == "UnauthorizedError"
                    if (response.error?.errorMessage != null)
                        _reviewState.value = ReviewState.Error(UiText.DynamicString(response.error.errorMessage), unauthorized)
                    else
                        _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_api), unauthorized)
                } else {
                    _reviewState.value = ReviewState.Success(MyReviewResult(review = null, onModeration = false))
                }
            } catch (_: CancellationException) {
                _reviewState.value = ReviewState.Null
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet())
                    _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_internet))
                else
                    _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_children))
            }
        }
    }

    fun sendReview(stars: Int, text: String?) {
        viewModelScope.launch {
            _reviewState.value = ReviewState.Loading

            try {
                val response = Reviews.createReview(stars, text)

                if (!response.status || response.answer == null) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at sendReview: ${response.error.errorMessage}")

                    val unauthorized = response.error?.type == "UnauthorizedError"
                    if (response.error?.errorMessage != null)
                        _reviewState.value = ReviewState.Error(UiText.DynamicString(response.error.errorMessage), unauthorized)
                    else
                        _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_api), unauthorized)
                } else {
                    _reviewState.value = ReviewState.Success(response.answer)
                }
            } catch (_: CancellationException) {
                _reviewState.value = ReviewState.Null
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet())
                    _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_internet))
                else
                    _reviewState.value = ReviewState.Error(UiText.StringResource(R.string.error_children))
            }
        }
    }
}