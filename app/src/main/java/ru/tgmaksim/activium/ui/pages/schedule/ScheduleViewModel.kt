package ru.tgmaksim.activium.ui.pages.schedule

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinMonth

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.json
import ru.tgmaksim.activium.api.Dnevnik
import ru.tgmaksim.activium.ui.core.UiText
import ru.tgmaksim.activium.api.ScheduleDay
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.core.setCacheError
import ru.tgmaksim.activium.ui.core.setShownError
import ru.tgmaksim.activium.ui.core.setCacheLoading
import ru.tgmaksim.activium.ui.core.setCacheSuccess
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class ScheduleViewModel : UiViewModel() {
    enum class StateType { Schedule }

    private val _scheduleData = MutableStateFlow<UiScheduleResult?>(null)
    val scheduleData = _scheduleData.asStateFlow()

    private val _scheduleState = MutableStateFlow<CacheDataLoadState>(CacheDataLoadState.Empty)
    val scheduleState = _scheduleState.asStateFlow()

    private var loadCacheScheduleJob: Job? = null
    private var loadCloudCacheScheduleJob: Job? = null

    companion object {
        private const val CACHE_SCHEDULE_NAME = "schedule"
        private const val CACHE_TIMEZONE_NAME = "timezone"
    }

    fun resetSchedule() {
        _scheduleState.value = CacheDataLoadState.Empty
        _scheduleData.value = null
    }

    fun resetError(stateType: StateType) {
        when (stateType) {
            StateType.Schedule -> _scheduleState.setShownError()
        }
    }

    fun logout() {
        viewModelScope.launch {
            LoginActivity.logout()
        }
    }

    fun loadCacheSchedule() {
        val job = loadCacheScheduleJob
        if (job?.isActive == true)
            return

        loadCacheScheduleJob = viewModelScope.launch {
            _scheduleState.setCacheLoading()

            try {
                val before = SettingsManager.getBeforeSchedule()
                val after = SettingsManager.getAfterSchedule()
                val totalDays = before + 1 + after

                val childId = SettingsManager.getActiveChildId()

                try {
                    val timezone = CacheManager.read(childId, CACHE_TIMEZONE_NAME)?.value?.toInt()
                        ?: throw CacheNullException()

                    val entity = CacheManager.read(childId, CACHE_SCHEDULE_NAME)
                        ?: throw CacheNullException()
                    val schedule = json.decodeFromString<List<ScheduleDay>>(entity.value)

                    _scheduleData.value = UiScheduleResult(
                        schedule = normalizeSchedule(schedule, before, after, timezone),
                        timezone = timezone,
                        hasAbilityPraise = false
                    )
                    _scheduleState.setCacheSuccess()
                } catch (e: Exception) {
                    if (e !is CacheNullException) {
                        Utilities.log(e)
                        CacheManager.writeDnevnikCache(childId, CACHE_SCHEDULE_NAME, value = "")
                    }
                    _scheduleData.value = UiScheduleResult(
                        schedule = List(totalDays) { null },
                        timezone = 0,
                        hasAbilityPraise = false
                    )
                    _scheduleState.setCacheSuccess()
                }
            } catch (_: CancellationException) {
                _scheduleState.setCacheError(UiText.StringResource(R.string.error_schedule))
            }
        }
    }

    private fun normalizeSchedule(
        schedule: List<ScheduleDay>,
        before: Int,
        after: Int,
        timezone: Int
    ): List<ScheduleDay?> {
        val currentDate = todayInTimezone(timezone)
        val firstDate = currentDate.minusDays(before.toLong())
        val byDate = schedule.associateBy { it.date }

        val days = ArrayList<ScheduleDay?>(before + 1 + after)
        for (i in 0..<before + 1 + after) {
            val date = firstDate.plusDays(i.toLong())
            days += byDate[LocalDate(date.year, date.month.toKotlinMonth(), date.dayOfMonth)]
        }

        return days
    }

    private fun todayInTimezone(offsetSeconds: Int): ZonedDateTime {
        val utcNow = Instant.now()
        val zone = ZoneOffset.ofTotalSeconds(offsetSeconds)
        return utcNow.atZone(zone)
    }

    fun loadCloudSchedule() {
        val job = loadCloudCacheScheduleJob
        if (job?.isActive == true)
            return

        loadCloudCacheScheduleJob = viewModelScope.launch {
            val childId = SettingsManager.getActiveChildId()
            val before = SettingsManager.getBeforeSchedule()
            val after = SettingsManager.getAfterSchedule()

            executeRequest(
                _scheduleState,
                _scheduleData,
                "schedule",
                R.string.error_schedule,
                { Dnevnik.getSchedule(before, after) },
                { it.answer?.let { answer ->
                    UiScheduleResult(
                        schedule = normalizeSchedule(answer.schedule, before, after, answer.timezone),
                        timezone = answer.timezone,
                        hasAbilityPraise = answer.hasAbilityPraise
                    )
                } }
            ) {
                it.answer ?: return@executeRequest
                CacheManager.writeDnevnikCache(
                    childId,
                    CACHE_TIMEZONE_NAME,
                    value = it.answer.timezone.toString()
                )
                CacheManager.writeDnevnikCache(
                    childId,
                    CACHE_SCHEDULE_NAME,
                    value = json.encodeToString(it.answer.schedule)
                )
            }
        }
    }
}

private class CacheNullException : Exception()