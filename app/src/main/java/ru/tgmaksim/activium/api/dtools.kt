package ru.tgmaksim.activium.api

import kotlin.time.Instant
import kotlinx.serialization.Serializable

import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Заметка
 * @param classId Идентификатор класса
 * @param lessonKey Ключ к уроку, к которому создана заметка
 * @param text Текст заметки
 * @param public Заметка доступна родителю
 */
@Serializable
data class Note(
    override val classId: Int = CLASS_ID,
    val lessonKey: String,
    val text: String,
    val public: Boolean,
    val remindTime: Instant?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x4A
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Результат запроса создания или получения заметки к уроку
 * @param classId Идентификатор класса
 * @param note Созданная заметка к уроку
 */
@Serializable
data class NoteResult(
    override val classId: Int = CLASS_ID,
    val note: Note?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x4B
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос создания заметки к уроку
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Созданная заметка
 */
@Serializable
data class CreateNoteApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: NoteResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x4C
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения заметки к уроку
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Заметка к уроку
 */
@Serializable
data class NoteApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: NoteResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x4D
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос удаления заметки к уроку
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 */
@Serializable
data class DeleteNoteApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiBase?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x39
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
        if (answer != null) throw ClassCastException()
    }
}

/**
 * Ответ на запрос отправки похвалы
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 */
@Serializable
data class PraiseApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiBase?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x49
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
        if (answer != null) throw ClassCastException()
    }
}

/**
 * Ответ на запрос выделения одноклассника в рейтингах
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 */
@Serializable
data class HighlightPersonApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiBase?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x3E
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
        if (answer != null) throw ClassCastException()
    }
}

/**
 * Ответ на запрос отмены выделения одноклассника в рейтингах
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 */
@Serializable
data class UnhighlightPersonApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiBase?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x3F
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
        if (answer != null) throw ClassCastException()
    }
}

/**
 * API-singleton для запросов группы дневник_tools
 */
object DnevnikTools {
    private const val PATH_PREFIX = "dtools"

    private const val PATH_CREATE_NOTE = "createNote"
    private const val PATH_GET_NOTE = "getNote"
    private const val PATH_DELETE_NOTE = "deleteNote"
    private const val PATH_SEND_PRAISE = "sendPraise"
    private const val PATH_HIGHLIGHT_PERSON = "highlightPerson"
    private const val PATH_UNHIGHLIGHT_PERSON = "unhighlightPerson"

    private const val CREATE_NOTE_VERSION = 1
    private const val GET_NOTE_VERSION = 1
    private const val DELETE_NOTE_VERSION = 0
    private const val SEND_PRAISE_VERSION = 1
    private const val HIGHLIGHT_PERSON_VERSION = 0
    private const val UNHIGHLIGHT_PERSON_VERSION = 0

    /**
     * Создание или изменение текстовой заметки к уроку.
     * Синхронизируется с родителем.
     */
    suspend fun createNote(lessonKey: String, text: String, public: Boolean, remindTime: java.time.Instant?): CreateNoteApiResponse {
        val params = mutableMapOf<String, Any>("lessonKey" to lessonKey, "public" to public)
        if (remindTime != null)
            params["remindTime"] = remindTime

        return Request.post(
            listOf(PATH_PREFIX, PATH_CREATE_NOTE, CREATE_NOTE_VERSION).joinToString("/"),
            params = params,
            body = text,
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Получение текстовой заметки к уроку.
     */
    suspend fun getNote(lessonKey: String): NoteApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_GET_NOTE, GET_NOTE_VERSION).joinToString("/"),
            params = mapOf("lessonKey" to lessonKey),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Удаление текстовой заметки к уроку.
     */
    suspend fun deleteNote(lessonKey: String): DeleteNoteApiResponse {
        return Request.delete(
            listOf(PATH_PREFIX, PATH_DELETE_NOTE, DELETE_NOTE_VERSION).joinToString("/"),
            params = mapOf("lessonKey" to lessonKey),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Отправка похвалы активному ребенку от родителя на полученные оценки.
     */
    suspend fun sendPraise(lessonKey: String?, ratingKey: String?, text: String?): PraiseApiResponse {
        return Request.post(
            listOf(PATH_PREFIX, PATH_SEND_PRAISE, SEND_PRAISE_VERSION).joinToString("/"),
            params = if (lessonKey != null) mapOf("lessonKey" to lessonKey) else mapOf("ratingKey" to ratingKey!!),
            body = text,
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Выделение одноклассника во всех рейтингах и списках других оценок.
     */
    suspend fun highlightPerson(personKey: String): HighlightPersonApiResponse {
        return Request.put(
            listOf(PATH_PREFIX, PATH_HIGHLIGHT_PERSON, HIGHLIGHT_PERSON_VERSION).joinToString("/"),
            params = mapOf("personKey" to personKey),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Отмена ранее включенного выделения одноклассника в рейтингах.
     */
    suspend fun unhighlightPerson(personKey: String): UnhighlightPersonApiResponse {
        return Request.put(
            listOf(PATH_PREFIX, PATH_UNHIGHLIGHT_PERSON, UNHIGHLIGHT_PERSON_VERSION).joinToString("/"),
            params = mapOf("personKey" to personKey),
            sessionId = SettingsManager.getSessionId()
        )
    }
}