package ru.tgmaksim.activium.utilities.datastore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.tgmaksim.activium.api.VersionsResult

object MemoryDataManager {
    private val versionStatus = MutableStateFlow<VersionsResult?>(null)
    val sessionId: StateFlow<VersionsResult?> = versionStatus

    fun setVersionResult(versionStatus: VersionsResult?) {
        this.versionStatus.value = versionStatus
    }

    fun getVersionStatus(): VersionsResult? {
        return versionStatus.value
    }
}