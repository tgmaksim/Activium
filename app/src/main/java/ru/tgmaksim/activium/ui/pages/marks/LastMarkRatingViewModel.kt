package ru.tgmaksim.activium.ui.pages.marks

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.json
import ru.tgmaksim.activium.api.Dnevnik
import ru.tgmaksim.activium.api.DnevnikTools
import ru.tgmaksim.activium.ui.core.UiText
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.ui.core.setShownError
import ru.tgmaksim.activium.ui.core.setCacheError
import ru.tgmaksim.activium.ui.core.setCacheLoading
import ru.tgmaksim.activium.ui.core.setCacheSuccess
import ru.tgmaksim.activium.api.MarksRatingStatsResult
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class LastMarkRatingViewModel : UiViewModel() {
    private val _marksState = MutableStateFlow<CacheDataLoadState>(CacheDataLoadState.Empty)
    val marksState = _marksState.asStateFlow()

    private val _marksData = MutableStateFlow<MarksRatingStatsResult?>(null)
    val marksData = _marksData.asStateFlow()

    private var loadCacheMarksJob: Job? = null
    private var loadCloudMarksJob: Job? = null

    companion object {
        private const val CACHE_MARKS_RATING_STATS_NAME = "marks_rating_stats"
    }

    fun resetError() {
        _marksState.setShownError()
    }

    fun loadCacheMarksRatingStats(ratingKey: String) {
        val job = loadCacheMarksJob
        if (job?.isActive == true)
            return

        loadCacheMarksJob = viewModelScope.launch {
            _marksState.setCacheLoading()

            try {
                val childId = SettingsManager.getActiveChildId()

                try {
                    val entity = CacheManager.read(childId, CACHE_MARKS_RATING_STATS_NAME, ratingKey)
                        ?: throw CacheNullException()
                    val marksRatingStats = json.decodeFromString<MarksRatingStatsResult>(entity.value)

                    _marksData.value = marksRatingStats

                    _marksState.setCacheSuccess()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e !is CacheNullException) {
                        Utilities.log(e)
                        CacheManager.writeDnevnikCache(childId, CACHE_MARKS_RATING_STATS_NAME, param = ratingKey, value = "")
                    }

                    _marksState.setCacheSuccess()
                }
            } catch (_: CancellationException) {
                _marksState.setCacheError(UiText.StringResource(R.string.error_lesson_rating_stats))
            }
        }
    }

    fun loadCloudMarksRatingStats(ratingKey: String) {
        val job = loadCloudMarksJob
        if (job?.isActive == true)
            return

        loadCloudMarksJob = viewModelScope.launch {
            val childId = SettingsManager.getActiveChildId()

            executeRequest(
                _marksState,
                _marksData,
                "marksRatingStats",
                R.string.error_marks,
                { Dnevnik.getMarksRatingStats(ratingKey) },
                { it.answer }
            ) {
                it.answer ?: return@executeRequest
                CacheManager.writeDnevnikCache(
                    childId,
                    CACHE_MARKS_RATING_STATS_NAME,
                    param = ratingKey,
                    value = json.encodeToString(it.answer)
                )
            }
        }
    }

    fun highlightPerson(personKey: String, ratingKey: String, highlight: Boolean) {
        viewModelScope.launch {
            executeRequest(
                _marksState,
                MutableStateFlow(Unit),
                "highlightPerson($highlight)",
                R.string.error_marks,
                {
                    if (highlight) DnevnikTools.highlightPerson(personKey)
                    else DnevnikTools.unhighlightPerson(personKey)
                },
                { }
            ) {
                loadCloudMarksRatingStats(ratingKey)
            }
        }
    }
}