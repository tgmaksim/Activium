package ru.tgmaksim.activium.ui.pages.schedule

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.json
import ru.tgmaksim.activium.api.Dnevnik
import ru.tgmaksim.activium.ui.core.UiText
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.ui.core.setShownError
import ru.tgmaksim.activium.ui.core.setCacheError
import ru.tgmaksim.activium.ui.core.setCacheLoading
import ru.tgmaksim.activium.ui.core.setCacheSuccess
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.api.LessonRatingStatsResult
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class RatingViewModel : UiViewModel() {
    private val _lessonRatingState = MutableStateFlow<CacheDataLoadState>(CacheDataLoadState.Empty)
    val lessonRatingState = _lessonRatingState.asStateFlow()

    private val _lessonRatingData = MutableStateFlow<LessonRatingStatsResult?>(null)
    val lessonRatingData = _lessonRatingData.asStateFlow()

    private var loadCacheRatingJob: Job? = null
    private var loadCloudRatingJob: Job? = null

    companion object {
        private const val CACHE_LESSON_RATING_STATS_NAME = "lesson_rating_stats"
    }

    fun resetLessonRating() {
        _lessonRatingState.setShownError()
    }

    fun loadCacheLessonRatingStats(ratingKey: String) {
        val job = loadCacheRatingJob
        if (job?.isActive == true)
            return

        loadCacheRatingJob = viewModelScope.launch {
            _lessonRatingState.setCacheLoading()

            try {
                val childId = SettingsManager.getActiveChildId()

                try {
                    val entity = CacheManager.read(childId, CACHE_LESSON_RATING_STATS_NAME, ratingKey)
                        ?: throw CacheNullException()
                    val lessonRatingStats = json.decodeFromString<LessonRatingStatsResult>(entity.value)

                    _lessonRatingData.value = lessonRatingStats

                    _lessonRatingState.setCacheSuccess()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e !is CacheNullException) {
                        Utilities.log(e)
                        CacheManager.writeDnevnikCache(childId, CACHE_LESSON_RATING_STATS_NAME, param = ratingKey, value = "")
                    }

                    _lessonRatingState.setCacheSuccess()
                }
            } catch (_: CancellationException) {
                _lessonRatingState.setCacheError(UiText.StringResource(R.string.error_lesson_rating_stats))
            }
        }
    }

    fun loadCloudLessonRatingStats(ratingKey: String) {
        val job = loadCloudRatingJob
        if (job?.isActive == true)
            return

        loadCloudRatingJob = viewModelScope.launch {
            val childId = SettingsManager.getActiveChildId()

            executeRequest(
                _lessonRatingState,
                _lessonRatingData,
                "lessonRatingStats",
                R.string.error_lesson_rating_stats,
                { Dnevnik.getLessonRatingStats(ratingKey) },
                { it.answer }
            ) {
                it.answer ?: return@executeRequest
                CacheManager.writeDnevnikCache(
                    childId,
                    CACHE_LESSON_RATING_STATS_NAME,
                    param = ratingKey,
                    value = json.encodeToString(it.answer)
                )
            }
        }
    }
}