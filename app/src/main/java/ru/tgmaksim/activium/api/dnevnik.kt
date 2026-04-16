package ru.tgmaksim.activium.api

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Прикрепленный файл к домашнему заданию
 * @param classId Идентификатор класса
 * @param fileName Название файла
 * @param downloadUrl Ссылка для загрузки файла
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleLesson
 */
@Serializable
data class ScheduleHomeworkDocument(
    override val classId: Int = CLASS_ID,
    val fileName: String,
    val downloadUrl: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0xA
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Время проведения урока или внеурочного занятия
 * @param classId Идентификатор класса
 * @param start Начало урока или внеурочного занятия
 * @param end Окончание урока или внеурочного занятия
 * @param string Строковое представление времени проведения
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class ScheduleHours(
    override val classId: Int = CLASS_ID,
    val start: Instant,
    val end: Instant,
    val string: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0xB
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Внеурочное занятие
 * @param classId Идентификатор класса
 * @param subject Название предмета внеурочного занятия
 * @param place Кабинет или другое место проведения внеурочного занятия
 * @param hours Время проведения внеурочного занятия
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class ScheduleExtracurricularActivity(
    override val classId: Int = CLASS_ID,
    val subject: String,
    val place: String,
    val hours: ScheduleHours
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0xC
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Тип работы на уроке
 * @param classId Идентификатор класса
 * @param title Название типа работы
 * @param abbr Аббревиатура типа работы
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class WorkType(
    override val classId: Int = CLASS_ID,
    val title: String,
    val abbr: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0xD
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Оценка или отметка посещаемости урока
 * @param classId Идентификатор класса
 * @param mood Тип оценки: хороший, средний, плохой или другой
 * @param value Полученная оценка или отметка посещаемости
 * @param work Тип работы, за что получена оценка
 * @param created Дата и время выставления оценки
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarkLog(
    override val classId: Int = CLASS_ID,
    val mood: String,
    val value: String,
    val work: WorkType?,
    val created: Instant?,
    val ratingKey: String?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0xE
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Оценки другого ученика(цы) за тот же урок
 * @param classId Идентификатор класса
 * @param number Место в рейтинге
 * @param name Имя и первая буква фамилии ученика(цы)
 * @param marks Оценки ученика(цы)
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarksOther(
    override val classId: Int = CLASS_ID,
    val number: Int?,
    val name: String,
    val personKey: String?,
    val isHighlighting: Boolean?,
    val marks: List<MarkLog>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0xF
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Урок
 * @param classId Идентификатор класса
 * @param lessonKey Ключ для создания заметок к уроку и отправки похвалы
 * @param number Порядковый номер урока начиная с 0
 * @param subject Название предмета урока
 * @param place Кабинет или другое место проведения урока
 * @param hours Время проведения урока
 * @param works Типы работ на уроке
 * @param logs Оценки и отметки посещаемости за урок
 * @param othersMarks Оценки других учеников за урок
 * @param avgGroupLessonMark Средний балл оценок за урок в классе
 * @param homework Домашнее задание к уроку
 * @param note Заметка к уроку
 * @param files Дополнительные файлы к домашнему заданию
 * @param ratingKey Ключ для получения дополнительной информации по оценкам
 * @param dnevnikruUrl Ссылка для открытия урока в Дневнике.ру
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class ScheduleLesson(
    override val classId: Int = CLASS_ID,
    val lessonKey: String,
    val number: Int,
    val subject: String,
    val place: String,
    val hours: ScheduleHours,
    val works: List<WorkType>,
    val logs: List<MarkLog>,
    val othersMarks: List<MarksOther>,
    val avgGroupLessonMark: MarkLog?,
    val homework: String?,
    val note: String?,
    val files: List<ScheduleHomeworkDocument>,
    val ratingKey: String?,
    val dnevnikruUrl: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x10
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * День в расписании с уроками и внеурочными занятиями
 * @param classId Идентификатор класса
 * @param date Дата дня в формате ISO
 * @param lessons Уроки в данный день
 * @param ea Внеурочные занятия в данный день
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class ScheduleDay(
    override val classId: Int = CLASS_ID,
    val date: LocalDate,
    val lessons: List<ScheduleLesson>,
    val ea: List<ScheduleExtracurricularActivity>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x11
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Результат запроса расписания на несколько дней
 * @param classId Идентификатор класса
 * @param schedule Расписание на несколько дней
 * @param timezone Часовой пояс
 * @param hasAbilityPraise Можно ли отправить похвалу ребенку от родителя
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleApiResponse
 */
@Serializable
data class ScheduleResult(
    override val classId: Int = CLASS_ID,
    val schedule: List<ScheduleDay>,
    val timezone: Int,
    val hasAbilityPraise: Boolean
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x12
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос расписания на несколько дней
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Данные о расписании на несколько дней
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class ScheduleApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ScheduleResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x13
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
    }
}

/**
 * Результат запроса получения дополнительной статистики по оценкам на уроке
 * @param classId Идентификатор класса
 * @param oldAvgMark Средний балл до получения оценок в день урока
 * @param newAvgMark Средний балл после получения оценок в день урока
 * @author Максим Дрючин (tgmaksim)
 * @see LessonRatingStatsApiResponse
 */
@Serializable
data class LessonRatingStatsResult(
    override val classId: Int = CLASS_ID,
    val oldAvgMark: MarkLog?,
    val newAvgMark: MarkLog?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x14
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения дополнительной статистики по оценкам на уроке
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Дополнительная статистика по оценкам на уроке
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class LessonRatingStatsApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: LessonRatingStatsResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x15
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
    }
}

/**
 * Оценка с рейтингом
 * @param classId Идентификатор класса
 * @param mark Полученная оценка
 * @param subject Название предмета, по которому получена оценка
 * @param lessonDate Дата урока, на котором поставлена оценка
 * @param humanLessonDate Дата урока в формате '%e %b.' для показа пользователю
 * @param ratingKey Ключ для получения оценок в классе за тот же урок
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarkLast(
    override val classId: Int = CLASS_ID,
    val mark: MarkLog,
    val subject: String,
    val lessonDate: LocalDate?,
    val humanLessonDate: String?,
    val ratingKey: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x16
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Оценки по предмету в отчетном периоде
 * @param classId Идентификатор класса
 * @param subject Название предмета
 * @param marks Оценки по предмету в порядке даты выставления
 * @param averageMark Средний балл оценок по предмету с точностью до 2 знаков после запятой
 * @param periodMark Оценка за отчетный период по предмету
 * @param ratingKey Ключ для получения рейтинга в классе по предмету
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarksSubjectPeriod(
    override val classId: Int = CLASS_ID,
    val subject: String,
    val marks: List<MarkLog>,
    val averageMark: MarkLog?,
    val periodMark: MarkLog?,
    val ratingKey: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x17
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Результат запроса получения оценок последних и по предметам
 * @param classId Идентификатор класса
 * @param recentMarks Последние оценки по дате выставления за последнюю неделю
 * @param periodMarks Оценки по предметам за текущий отчетный период
 * @param ratingKey Ключ для получения общего рейтинга в классе
 * @author Максим Дрючин (tgmaksim)
 * @see MarksApiResponse
 */
@Serializable
data class MarksResult(
    override val classId: Int = CLASS_ID,
    val recentMarks: List<MarkLast>,
    val periodMarks: List<MarksSubjectPeriod>,
    val ratingKey: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x18
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения оценок последних и по предметам
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Оценки последние и по предметам
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarksApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarksResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x19
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
    }
}

/**
 * Результат запроса получения дополнительной статистики по последней оценке
 * @param classId Идентификатор класса
 * @param othersMarks Оценки класса за тот же урок
 * @param avgGroupMark Средний балл оценок за урок в классе
 * @param oldAvgMark Средний балл до получения оценок в день урока
 * @param newAvgMark Средний балл после получения оценок в день урока
 * @author Максим Дрючин (tgmaksim)
 * @see MarksRatingStatsApiResponse
 */
@Serializable
data class MarksRatingStatsResult(
    override val classId: Int = CLASS_ID,
    val othersMarks: List<MarksOther>,
    val avgGroupMark: MarkLog?,
    val oldAvgMark: MarkLog?,
    val newAvgMark: MarkLog?,
    val hasAbilityPraise: Boolean
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x47
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения дополнительной статистики по последней оценке
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Дополнительная статистика по последней оценке
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarksRatingStatsApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarksRatingStatsResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x48
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
    }
}

/**
 * Результат запроса получения общего или предметного рейтинга
 * @param classId Идентификатор класса
 * @param rating Средние баллы одноклассников
 * @param oldMark Прошлая оценка с момента прошлого запроса
 * @author Максим Дрючин (tgmaksim)
 * @see MarksSubjectRatingApiResponse
 */
@Serializable
data class MarksSubjectRatingResult(
    override val classId: Int = CLASS_ID,
    val rating: List<MarksOther>,
    val oldMark: MarksOther?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x1C
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения общего или предметного рейтинга
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Рейтинг в классе и прошлое место в рейтинге
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarksSubjectRatingApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarksSubjectRatingResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x1D
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
    }
}

/**
 * Оценки по предмету за отчетные периоды и за год
 * @param classId Идентификатор класса
 * @param subject Название предмета
 * @param marks Оценки по предмету в порядке отчетных периодов
 * @param finalMark Итоговая оценка за год по предмету
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarksSubjectFinal(
    override val classId: Int = CLASS_ID,
    val subject: String,
    val marks: List<MarkLog?>,
    val finalMark: MarkLog?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x1E
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Результат запроса получения оценок за период и за год
 * @param classId Идентификатор класса
 * @param countPeriods Количество отчетных периодов
 * @param finalMarks Оценки по предметам за отчетные периоды и за год
 * @author Максим Дрючин (tgmaksim)
 * @see MarksFinalApiResponse
 */
@Serializable
data class MarksFinalResult(
    override val classId: Int = CLASS_ID,
    val countPeriods: Int,
    val finalMarks: List<MarksSubjectFinal>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x1F
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения оценок за период и за год
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Оценки по предметам за отчетные периоды и за год
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class MarksFinalApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarksFinalResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x20
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
    }
}

/**
 * API-singleton для запросов группы dnevnik
 * @property PATH_PREFIX Группа API-запросов
 * @author Максим Дрючин (tgmaksim)
 */
object Dnevnik {
    private const val PATH_PREFIX = "dnevnik"

    private const val PATH_SCHEDULE = "getSchedule"
    private const val PATH_LESSON_RATING_STATS = "getLessonRatingStats"
    private const val PATH_MARKS = "getMarks"
    private const val PATH_MARK_RATING_STATS = "getMarkRatingStats"
    private const val PATH_MARKS_SUBJECT_RATING = "getMarksSubjectRating"
    private const val PATH_FINAL_MARKS = "getFinalMarks"

    private const val SCHEDULE_VERSION = 0
    private const val LESSON_RATING_STATS_VERSION = 0
    private const val MARKS_VERSION = 0
    private const val MARK_RATING_STATS_VERSION = 1
    private const val MARKS_SUBJECT_RATING_VERSION = 0
    private const val FINAL_MARKS_VERSION = 0

    /**
     * Получение расписания на несколько дней с домашними заданиями, внеурочными занятиями и оценками с отметками о посещаемости
     * @param before Количество дней расписания до сегодня
     * @param after Количество дней после сегодня
     * @return Ответ сервера в виде [ScheduleApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun getSchedule(before: Int, after: Int): ScheduleApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_SCHEDULE, SCHEDULE_VERSION).joinToString("/"),
            params = mapOf("before" to before, "after" to after),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Получение дополнительной статистики по полученным оценкам по предмету в нужный день
     * @param ratingKey Ключ от урока, по которому получить статистику
     * @return Ответ сервера в виде [LessonRatingStatsApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun getLessonRatingStats(ratingKey: String): LessonRatingStatsApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_LESSON_RATING_STATS, LESSON_RATING_STATS_VERSION).joinToString("/"),
            params = mapOf("ratingKey" to ratingKey),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Получение последних оценок по дате выставления и оценок за текущий отчетный период
     * @param last Число дней, за которое будут запрошены последние по дате выставления оценки
     * @return Ответ сервера в виде [MarksApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun getMarks(last: Int): MarksApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_MARKS, MARKS_VERSION).joinToString("/"),
            params = mapOf("last" to last),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Получение оценок в классе за урок и дополнительной статистики по полученной оценке
     * @param ratingKey Ключ от последней оценки
     * @return Ответ сервера в виде [MarksRatingStatsApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun getMarksRatingStats(ratingKey: String): MarksRatingStatsApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_MARK_RATING_STATS, MARK_RATING_STATS_VERSION).joinToString("/"),
            params = mapOf("ratingKey" to ratingKey),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Получение общего или предметного рейтинга в классе с изменением места пользователя
     * @param ratingKey Ключ от предмета или общий ключ
     * @return Ответ сервера в виде [MarksSubjectRatingApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun getMarksSubjectRating(ratingKey: String): MarksSubjectRatingApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_MARKS_SUBJECT_RATING, MARKS_SUBJECT_RATING_VERSION).joinToString("/"),
            params = mapOf("ratingKey" to ratingKey),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Получение оценок за отчетный период и итоговых за год
     * @return Ответ сервера в виде [MarksFinalApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun getFinalMarks(): MarksFinalApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_FINAL_MARKS, FINAL_MARKS_VERSION).joinToString("/"),
            sessionId = SettingsManager.getSessionId()
        )
    }
}