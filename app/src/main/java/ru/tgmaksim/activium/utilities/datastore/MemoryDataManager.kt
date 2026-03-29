package ru.tgmaksim.activium.utilities.datastore

import ru.tgmaksim.activium.api.VersionsResult
import kotlinx.coroutines.flow.MutableStateFlow

object MemoryDataManager {
    val darkTheme = MutableStateFlow(false)
    val sessionId = MutableStateFlow<String?>(null)
    val versionStatus = MutableStateFlow<VersionsResult?>(null)
}