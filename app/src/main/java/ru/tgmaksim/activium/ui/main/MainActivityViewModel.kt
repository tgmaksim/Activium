package ru.tgmaksim.activium.ui.main

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Status
import ru.tgmaksim.activium.api.School
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.api.VersionsResult
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.api.InformationResult
import ru.tgmaksim.activium.ui.core.setShownError
import ru.tgmaksim.activium.api.SchoolPostsWithoutVisionResult
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

class MainActivityViewModel : UiViewModel() {
    enum class StateType { Version, Info, SchoolPosts }

    private val _versionState = MutableStateFlow<LoadState<VersionsResult>>(LoadState.Empty)
    val versionState = _versionState.asStateFlow()

    private val _infoStatus = MutableStateFlow<LoadState<InformationResult>>(LoadState.Empty)
    val infoStatus = _infoStatus.asStateFlow()

    private val _schoolPostsState = MutableStateFlow<LoadState<SchoolPostsWithoutVisionResult>>(LoadState.Empty)
    val schoolPostsState = _schoolPostsState.asStateFlow()

    fun reset(stateType: StateType) {
        when (stateType) {
            StateType.Version -> _versionState.setShownError()
            StateType.Info -> _infoStatus.setShownError()
            StateType.SchoolPosts -> _schoolPostsState.setShownError()
        }
    }

    fun checkVersion(version: Int) {
        viewModelScope.launch {
            executeRequest(
                _versionState,
                "checkVersion",
                R.string.error_check_version,
                { Status.checkVersion(version) },
                { it.answer },
                { MemoryDataManager.versionStatus.value = it }
            )
        }
    }

    fun checkInfoNotifications() {
        viewModelScope.launch {
            executeRequest(
                _infoStatus,
                "infoNotifications",
                R.string.error_info_notifications,
                Status::checkInfoNotifications,
                { it.answer }
            )
        }
    }

    fun checkNewSchoolPosts() {
        viewModelScope.launch {
            executeRequest(
                _schoolPostsState,
                "checkNewSchoolPosts",
                R.string.error_new_school_posts,
                School::checkNewPosts,
                { it.answer }
            )
        }
    }
}