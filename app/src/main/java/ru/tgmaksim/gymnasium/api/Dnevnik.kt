package ru.tgmaksim.gymnasium.api

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.OffsetTime
import java.time.LocalDateTime
import java.time.OffsetDateTime

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import ru.tgmaksim.gymnasium.utilities.Utilities
import ru.tgmaksim.gymnasium.utilities.CacheManager

/**
 * Входные данные для запроса расписания на несколько дней
 * @param classId Идентификатор класса
 * @param session Строковый идентификатор сессии
 * @param before Количество дней расписания до сегодня
 * @param after Количество дней расписания после сегодня
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleApiRequest
 * */
@Serializable data class ScheduleInputData(
    override val classId: Int = CLASS_ID,
    val session: String,
    val before: Int,
    val after: Int
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x00000024
    }
}

/**
 * Запрос расписания на несколько дней
 * @param classId Идентификатор класса
 * @param data Данные сессии
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class ScheduleApiRequest(
    override val classId: Int = CLASS_ID,
    override val data: ScheduleInputData
) : ApiRequest() {
    companion object {
        const val CLASS_ID = 0x00000028
    }
}

/**
 * Прикрепленный файл к домашнему заданию
 * @param fileName Название файла
 * @param downloadUrl Ссылка для загрузки файла
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleLesson
 * */
@Serializable data class ScheduleHomeworkDocument(
    override val classId: Int = CLASS_ID,
    val fileName: String,
    val downloadUrl: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x0000000E
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Внеурочное занятие
 * @param subject Название предмета внеурочного занятия
 * @param place Кабинет или другое место проведения внеурочного занятия
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleDay
 * */
@Serializable data class ScheduleExtracurricularActivity(
    override val classId: Int = CLASS_ID,
    val subject: String,
    val place: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x0000000F
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Время проведения урока или внеурочного занятия
 * @param start Начало урока или внеурочного занятия
 * @param end Окончание урока или внеурочного занятия
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleLesson
 * @see ScheduleExtracurricularActivity
 * */
@Serializable data class ScheduleHours(
    override val classId: Int = CLASS_ID,
    val start: String,
    val end: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x00000010
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }

    @Contextual val stringFormat: String
        get() = "$start - $end"
    @Contextual val startTime: OffsetTime
        get() = LocalTime.parse(start).atOffset(ZoneOffset.ofHours(CacheManager.timezone))
}

@Serializable data class WorkType(
    override val classId: Int = CLASS_ID,
    val title: String,
    val abbr: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x00000029
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Оценка или отметка о посещаемости урока
 * @param classId Идентификатор класса
 * @param mood Тип оценки: хороший, средний, плохой или другой для отметки о посещаемости
 * @param value Полученная оценка или отметка о посещаемости
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleLesson
 * @see MarksOther
 * */
@Serializable data class MarkLog(
    override val classId: Int = CLASS_ID,
    val mood: String,
    val value: String,
    val work: WorkType?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x0000002A
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Оценки другого ученика(цы)
 * @param name Имя и первая буква фамилии ученика
 * @param marks Оценки
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleLesson
 * */
@Serializable data class MarksOther(
    override val classId: Int = CLASS_ID,
    val name: String,
    val marks: List<MarkLog>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x0000002B
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Урок
 * @param number Порядковый номер урока начиная с 0
 * @param subject Название предмета урока
 * @param place Кабинет или другое место проведения урока
 * @param hours Время проведения урока в виде [ScheduleHours]
 * @param logs Оценки и отметки о посещаемости урока
 * @param othersMarks Оценки других учеников за урок
 * @param homework Домашнее задание к уроку
 * @param files Дополнительные файлы к домашнему заданию в виде списка из файлов [ScheduleHomeworkDocument]
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleDay
 * */
@Serializable data class ScheduleLesson(
    override val classId: Int = CLASS_ID,
    val number: Int,
    val subject: String,
    val place: String,
    val works: List<WorkType>,
    val hours: ScheduleHours,
    val logs: List<MarkLog>,
    val othersMarks: List<MarksOther>,
    val homework: String?,
    val files: List<ScheduleHomeworkDocument>,
    @Contextual val isEA: Boolean = false  // Только для внутренних взаимодействий
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x0000002C
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * День в расписании с уроками и внеурочными занятиями
 * @param date Дата дня в формате ISO
 * @param lessons Уроки в данный день в виде списка из уроков [ScheduleLesson]
 * @param hoursExtracurricularActivities Часы проведения внеурочек (если есть в данный день) в виде [ScheduleHours]
 * @param extracurricularActivities Внеурочные занятия в данный день (если есть) в виде списка внеурочек [ScheduleExtracurricularActivity]
 * @property ea Краткое название для [extracurricularActivities]
 * @property hoursEA Краткое название для [hoursExtracurricularActivities]
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleResult
 * */
@Serializable data class ScheduleDay(
    override val classId: Int = CLASS_ID,
    @SerialName("date") val dateString: String,
    val lessons: List<ScheduleLesson>,
    val hoursExtracurricularActivities: ScheduleHours?,
    val extracurricularActivities: List<ScheduleExtracurricularActivity>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x0000002D
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }

    @Contextual val ea
        get() = extracurricularActivities
    @Contextual val hoursEA
        get() = hoursExtracurricularActivities
    @Contextual val date: OffsetDateTime
        get() = LocalDate.parse(dateString).atStartOfDay().atOffset(ZoneOffset.ofHours(CacheManager.timezone))
}

/**
 * Результат запроса расписания на несколько дней
 * @param classId Идентификатор класса
 * @param schedule Расписание на несколько дней
 * @param timezone Часовой пояс
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleApiResponse
 * */
@Serializable data class ScheduleResult(
    override val classId: Int = CLASS_ID,
    val schedule: List<ScheduleDay>,
    val timezone: Int
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x0000002E
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос расписания на несколько дней
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект API-ошибки
 * @param answer Ответ в случае успешной обработки
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class ScheduleApiResponse(
    override val classId: Int,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ScheduleResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x0000002F
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

@Serializable data class MarksApiRequest(
    override val classId: Int = CLASS_ID,
    override val data: ApiSession
) : ApiRequest() {
    companion object {
        const val CLASS_ID = 0x00000030
    }
}

@Serializable data class MarkLast(
    override val classId: Int = CLASS_ID,
    val mark: MarkLog,
    val work: WorkType?,
    val subject: String,
    @SerialName("sentDatetime") val sentDatetimeString: String,
    @SerialName("lessonDate") val lessonDateString: String?,
    val lessonDateFormat: String?,
    val othersMarks: List<MarksOther>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x00000031
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }

    @Contextual val sentDatetime: OffsetDateTime
        get() = LocalDateTime.parse(sentDatetimeString).atOffset(ZoneOffset.ofHours(CacheManager.timezone))
    /*
    @Contextual val lessonDate: OffsetDateTime?
        get() = lessonDateString?.let { LocalDate.parse(it).atStartOfDay().atOffset(ZoneOffset.ofHours(CacheManager.timezone)) }
    */
}

@Serializable data class MarksSubjectPeriod(
    override val classId: Int = CLASS_ID,
    val subject: String,
    val marks: List<MarkLog>,
    val averageMark: MarkLog?,
    val periodMark: MarkLog?,
    val othersAverageMark: List<MarksOther>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x00000035
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

@Serializable data class MarksResult(
    override val classId: Int = CLASS_ID,
    val lastMarks: List<MarkLast>,
    val periodMarks: List<MarksSubjectPeriod>,
    val classRating: List<MarksOther>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x00000032
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

@Serializable data class MarksApiResponse(
    override val classId: Int,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarksResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x00000033
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * API-singleton для взаимодействия с дневником пользователя
 * @property PATH_PREFIX Группа API-запросов
 * @property PATH_GET_SCHEDULE Название API-запроса для получения расписания
 * @author Максим Дрючин (tgmaksim)
 * */
object Dnevnik {
    private const val PATH_PREFIX = "dnevnik"
    private const val PATH_GET_SCHEDULE = "getSchedule"
    private const val PATH_GET_MARKS = "getMarks"

    /**
     * Запрос расписания на несколько дней
     * @return Ответ сервера в виде [ScheduleApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun getSchedule() : ScheduleApiResponse {
        val request = ScheduleApiRequest(data = ScheduleInputData(
            session = CacheManager.apiSession.toString(),
            before = CacheManager.scheduleBefore,
            after = CacheManager.scheduleAfter
        ))

        val response = Request.post<ScheduleApiRequest, ScheduleApiResponse>(
            listOf(PATH_PREFIX, PATH_GET_SCHEDULE, ScheduleApiRequest.CLASS_ID).joinToString("/"),
            request
        )

        // Сохранение расписания и часового пояса в кеш
        response.answer?.let {
            CacheManager.schedule = json.encodeToString(it.schedule)
            CacheManager.timezone = it.timezone
        }

        return response
    }

    /**
     * Получение сохраненного расписания из кеша
     * @return Расписание на несколько дней в виде списка из [ScheduleDay]
     * @author Максим Дрючин (tgmaksim)
     * */
    fun getCacheSchedule() : List<ScheduleDay> {
        return CacheManager.schedule?.let {
            try {
                json.decodeFromString<List<ScheduleDay>>(it)
            } catch (e: Exception) {
                // При возникновении ошибки десериализации расписание в кеше очищается
                Utilities.log(e)
                CacheManager.schedule = null
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun getMarks() : MarksApiResponse {
        val request = MarksApiRequest(data = ApiSession(session = CacheManager.apiSession.toString()))

        val response = Request.post<MarksApiRequest, MarksApiResponse>(
            listOf(PATH_PREFIX, PATH_GET_MARKS, MarksApiRequest.CLASS_ID).joinToString("/"),
            request
        )

        // Сохранение расписания в кеш
        response.answer?.let {
            CacheManager.lastMarks = json.encodeToString(it.lastMarks)
            CacheManager.periodMarks = json.encodeToString(it.periodMarks)
            CacheManager.classRating = json.encodeToString(it.classRating)
        }

        return response
    }

    fun getCacheLastMarks() : List<MarkLast> {
        return CacheManager.lastMarks?.let {
            try {
                json.decodeFromString<List<MarkLast>>(it)
            } catch (e: Exception) {
                // При возникновении ошибки десериализации расписание в кеше очищается
                Utilities.log(e)
                CacheManager.lastMarks = null
                emptyList()
            }
        } ?: emptyList()
    }

    fun getCachePeriodMarks() : List<MarksSubjectPeriod> {
        return CacheManager.periodMarks?.let {
            try {
                json.decodeFromString<List<MarksSubjectPeriod>>(it)
            } catch (e: Exception) {
                // При возникновении ошибки десериализации расписание в кеше очищается
                Utilities.log(e)
                CacheManager.periodMarks = null
                emptyList()
            }
        } ?: emptyList()
    }

    fun getCacheClassRating() : List<MarksOther> {
        return CacheManager.classRating?.let {
            try {
                json.decodeFromString<List<MarksOther>>(it)
            } catch (e: Exception) {
                // При возникновении ошибки десериализации расписание в кеше очищается
                Utilities.log(e)
                CacheManager.classRating = null
                emptyList()
            }
        } ?: emptyList()
    }
}