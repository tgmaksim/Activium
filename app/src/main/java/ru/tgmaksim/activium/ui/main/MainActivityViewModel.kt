package ru.tgmaksim.activium.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import java.util.concurrent.CancellationException

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Status
import ru.tgmaksim.activium.api.Request
import ru.tgmaksim.activium.pages.UiText
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

class MainActivityViewModel : ViewModel() {
    private val _versionState = MutableStateFlow<VersionState>(VersionState.Null)
    val versionState = _versionState.asStateFlow()

    fun checkVersion(version: Int) {
        viewModelScope.launch {
            _versionState.value = VersionState.Loading

            try {
                val response = Status.checkVersion(version)

                if (!response.status || response.answer == null) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at checkVersion: ${response.error.errorMessage}")

                    if (response.error?.errorMessage != null)
                        _versionState.value = VersionState.Error(UiText.DynamicString(response.error.errorMessage))
                    else
                        _versionState.value = VersionState.Error(UiText.StringResource(R.string.error_api))

                    return@launch
                }

                if (response.answer.latestVersionNumber <= version)
                    return@launch

                _versionState.value = VersionState.Success(response.answer)
                MemoryDataManager.versionStatus.value = response.answer
            } catch (_: CancellationException) {
                _versionState.value = VersionState.Null
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet())
                    _versionState.value = VersionState.Error(UiText.StringResource(R.string.error_internet))
                else
                    _versionState.value = VersionState.Error(UiText.StringResource(R.string.error_check_version))
            }
        }
    }
}