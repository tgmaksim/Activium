package ru.tgmaksim.activium.ui.main

import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Status
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.api.VersionsResult
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

class MainActivityViewModel : UiViewModel() {
    private val _versionState = MutableStateFlow<LoadState<VersionsResult>>(LoadState.Empty)
    val versionState = _versionState.asStateFlow()

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
}