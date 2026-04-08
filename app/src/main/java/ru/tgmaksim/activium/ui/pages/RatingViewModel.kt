package ru.tgmaksim.activium.ui.pages

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.json
import ru.tgmaksim.activium.api.Dnevnik
import ru.tgmaksim.activium.ui.core.UiText
import ru.tgmaksim.activium.ui.core.setError
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.ui.core.setLoading
import ru.tgmaksim.activium.ui.core.setSuccess
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.ui.core.setShownError
import ru.tgmaksim.activium.api.LessonRatingStatsResult
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class RatingViewModel : UiViewModel() {
    private val _lessonRatingState = MutableStateFlow<LoadState<LessonRatingStatsResult>>(LoadState.Empty)
    val lessonRatingState = _lessonRatingState.asStateFlow()

    companion object {
        private const val CACHE_LESSON_RATING_STATS_NAME = "lesson_rating_stats"
    }

    fun resetLessonRating() {
        _lessonRatingState.setShownError()
    }

    fun loadLessonRatingStats(ratingKey: String) {
        viewModelScope.launch {
            _lessonRatingState.setLoading()

            try {
                val childId = SettingsManager.getActiveChildId()

                try {
                    val entity = CacheManager.read(childId, CACHE_LESSON_RATING_STATS_NAME, ratingKey)
                        ?: throw CacheNullException()
                    val lessonRatingStats = json.decodeFromString<LessonRatingStatsResult>(entity.value)

                    _lessonRatingState.setSuccess(lessonRatingStats)
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (_: CacheNullException) {
                } catch (e: Exception) {
                    Utilities.log(e)
                    CacheManager.writeDnevnikCache(childId, CACHE_LESSON_RATING_STATS_NAME, ratingKey, value = "")
                }
            } catch (_: CancellationException) {
                _lessonRatingState.setError(UiText.StringResource(R.string.error_lesson_rating_stats))
                return@launch
            }

            // При отсутствии кэша
            executeRequest(
                _lessonRatingState,
                "lessonRatingStats",
                R.string.error_lesson_rating_stats,
                { Dnevnik.getLessonRatingStats(ratingKey) },
                { it.answer }
            ) {
                CacheManager.writeDnevnikCache(
                    SettingsManager.getActiveChildId(),
                    CACHE_LESSON_RATING_STATS_NAME,
                    ratingKey,
                    json.encodeToString(it)
                )
            }
        }
    }
}