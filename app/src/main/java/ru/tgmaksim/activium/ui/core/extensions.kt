package ru.tgmaksim.activium.ui.core

import ru.tgmaksim.activium.api.MarksOther
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.api.MarkLog
import ru.tgmaksim.activium.api.ScheduleDay
import ru.tgmaksim.activium.api.ScheduleLesson
import ru.tgmaksim.activium.ui.pages.schedule.UiScheduleDay
import ru.tgmaksim.activium.ui.pages.schedule.UiScheduleLesson

fun <T> MutableStateFlow<LoadState<T>>.setLoading(): LoadState.Loading {
    value = LoadState.Loading
    return LoadState.Loading
}

fun <T> MutableStateFlow<LoadState<T>>.setSuccess(data: T): LoadState.Success<T> {
    val success = LoadState.Success(data)
    value = success
    return success
}

fun <T> MutableStateFlow<LoadState<T>>.setError(
    message: UiText,
    unauthorized: Boolean = false
): LoadState.Error {
    val error = LoadState.Error(message, unauthorized)
    value = error
    return error
}

fun <T> MutableStateFlow<LoadState<T>>.setShownError(): LoadState.ShownError {
    value = LoadState.ShownError
    return LoadState.ShownError
}

fun MutableStateFlow<CacheDataLoadState>.setCacheLoading(): CacheDataLoadState.CacheLoading {
    value = CacheDataLoadState.CacheLoading
    return CacheDataLoadState.CacheLoading
}

fun MutableStateFlow<CacheDataLoadState>.setCacheSuccess(): CacheDataLoadState.CacheSuccess {
    value = CacheDataLoadState.CacheSuccess
    return CacheDataLoadState.CacheSuccess
}

fun MutableStateFlow<CacheDataLoadState>.setCacheError(message: UiText): CacheDataLoadState.CacheError {
    val error = CacheDataLoadState.CacheError(message)
    value = error
    return error
}

fun MutableStateFlow<CacheDataLoadState>.setCloudLoading(): CacheDataLoadState.CloudLoading {
    value = CacheDataLoadState.CloudLoading
    return CacheDataLoadState.CloudLoading
}

fun MutableStateFlow<CacheDataLoadState>.setCloudSuccess(): CacheDataLoadState.CloudSuccess {
    value = CacheDataLoadState.CloudSuccess
    return CacheDataLoadState.CloudSuccess
}

fun MutableStateFlow<CacheDataLoadState>.setCloudError(
    message: UiText,
    unauthorized: Boolean = false
): CacheDataLoadState.CloudError {
    val error = CacheDataLoadState.CloudError(message, unauthorized)
    value = error
    return error
}

fun MutableStateFlow<CacheDataLoadState>.setShownError(): CacheDataLoadState.ShownError {
    value = CacheDataLoadState.ShownError
    return CacheDataLoadState.ShownError
}

fun <K, V> MutableStateFlow<Map<K, LoadState<V>>>.setLoading(key: K): LoadState.Loading {
    value = value.toMutableMap().apply { put(key, LoadState.Loading) }
    return LoadState.Loading
}

fun <K, V> MutableStateFlow<Map<K, LoadState<V>>>.setSuccess(key: K, data: V): LoadState.Success<V> {
    val success = LoadState.Success(data)
    value = value.toMutableMap().apply { put(key, success) }
    return success
}

fun <K, V> MutableStateFlow<Map<K, LoadState<V>>>.resetSuccess(key: K) {
    value = value.toMutableMap().minus(key)
}

fun <K, V> MutableStateFlow<Map<K, LoadState<V>>>.setError(
    key: K,
    message: UiText,
    unauthorized: Boolean = false
): LoadState.Error {
    val error = LoadState.Error(message, unauthorized)
    value = value.toMutableMap().apply { put(key, error) }
    return error
}

fun <K, V> MutableStateFlow<Map<K, LoadState<V>>>.setShownError(key: K): LoadState.ShownError {
    value = value.toMutableMap().apply { put(key, LoadState.ShownError) }
    return LoadState.ShownError
}

fun ScheduleLesson.toUi(praiseState: LoadState.Empty? = null): UiScheduleLesson {
    return UiScheduleLesson(
        lessonKey = lessonKey,
        number = number,
        subject = subject,
        place = place,
        hours = hours,
        works = works,
        logs = logs,
        othersMarks = othersMarks,
        avgGroupLessonMark = avgGroupLessonMark,
        homework = homework,
        note = note,
        files = files,
        ratingKey = ratingKey,
        dnevnikruUrl = dnevnikruUrl,
        praiseState = praiseState,
        noteState = null
    )
}

fun List<ScheduleLesson>.toUi(praiseState: LoadState.Empty? = null): List<UiScheduleLesson> {
    return map { it.toUi(praiseState) }
}

fun ScheduleDay.toUi(praiseState: LoadState.Empty? = null): UiScheduleDay {
    return UiScheduleDay(
        date = date,
        lessons = lessons.toUi(praiseState),
        ea = ea,
        schoolPosts = schoolPosts
    )
}

fun List<MarksOther>.toUi(): List<UiMarksOther> {
    return this.map { it.toUi() }
}

fun MarksOther.toUi(isOldMark: Boolean = false): UiMarksOther {
    return UiMarksOther(
        number = number,
        name = name,
        personKey = personKey,
        isHighlighting = isHighlighting,
        marks = marks,
        isOldMark = isOldMark
    )
}


fun List<MarkLog>.toUi(lessonKey: String? = null): List<UiMarkLog> {
    return this.map { it.toUi(lessonKey) }
}

fun MarkLog.toUi(lessonKey: String? = null): UiMarkLog {
    return UiMarkLog(
        mood = mood,
        value = value,
        work = work,
        created = created,
        ratingKey = ratingKey,
        lessonKey = lessonKey
    )
}