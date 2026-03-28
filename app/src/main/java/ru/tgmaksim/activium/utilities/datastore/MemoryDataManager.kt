package ru.tgmaksim.activium.utilities.datastore

import ru.tgmaksim.activium.api.VersionsResult
import kotlinx.coroutines.flow.MutableStateFlow

object MemoryDataManager {
    private val versionStatus = MutableStateFlow<VersionsResult?>(null)
    val sessionId = MutableStateFlow<String?>(null)

    fun setVersionResult(versionStatus: VersionsResult?) {
        this.versionStatus.value = versionStatus
    }

    fun getVersionStatus(): VersionsResult? {
        return versionStatus.value
    }
}