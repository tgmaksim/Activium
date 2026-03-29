package ru.tgmaksim.activium.api

import kotlinx.serialization.Serializable
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

/**
 * Ребенок
 * @param classId Идентификатор класса
 * @param childId Идентификатор ребенка, который необходим для выбора активного
 * @param name Имя ребенка для показа в клиенте
 * @author Максим Дрючин (tgmaksim)
 * @see ChildrenResult
 * */
@Serializable data class Child(
    override val classId: Int = CLASS_ID,
    val childId: Long,
    val name: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x21
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса получения своих детей
 * @param classId Идентификатор класса
 * @param children Список детей, привязанных к пользователю сессии
 * @param activeChildId Идентификатор активного ребенка
 * @author Максим Дрючин (tgmaksim)
 * @see ChildrenApiResponse
 * */
@Serializable data class ChildrenResult(
    override val classId: Int = CLASS_ID,
    val children: List<Child>,
    val activeChildId: Long
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x22
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения своих детей
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Данные о детях пользователя
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class ChildrenApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ChildrenResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x23
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос изменения активного ребенка родителя
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class SwitchActiveChildApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ChildrenResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x24
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса получения статуса настройки уведомлений
 * @param classId Идентификатор класса
 * @param status Статус функции уведомлений о новых оценках
 * @author Максим Дрючин (tgmaksim)
 * @see StatusDnevnikNotificationsApiResponse
 * */
@Serializable data class StatusDnevnikNotificationsResult(
    override val classId: Int = CLASS_ID,
    val status: Boolean
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x25
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения статуса настройки уведомлений
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Статус настройки уведомлений
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class StatusDnevnikNotificationsApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: StatusDnevnikNotificationsResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x26
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос изменения настройки уведомлений
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class SwitchDnevnikNotificationsApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiBase?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x27
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
        if (answer != null)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос обновления firebase-токена для работы уведомлений
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class UpdateFirebaseApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiBase?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x28
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
        if (answer != null)
            throw ClassCastException()
    }
}

/**
 * API-singleton для запросов группы settings
 * @property PATH_PREFIX Группа API-запросов
 * @author Максим Дрючин (tgmaksim)
 * */
object Settings {
    private const val PATH_PREFIX = "settings"
    private const val PATH_CHILDREN = "getChildren"
    private const val PATH_ACTIVE_CHILD = "setActiveChild"
    private const val PATH_DNEVNIK_NOTIFICATIONS = "getStatusDnevnikNotifications"
    private const val PATH_SWITCH_DNEVNIK_NOTIFICATIONS = "switchDnevnikNotifications"
    private const val PATH_UPDATE_FIREBASE = "updateFirebase"

    private const val CHILDREN_VERSION = 0
    private const val ACTIVE_CHILD_VERSION = 0
    private const val DNEVNIK_NOTIFICATIONS_VERSION = 0
    private const val SWITCH_DNEVNIK_NOTIFICATIONS_VERSION = 0
    private const val UPDATE_FIREBASE_VERSION = 0

    /**
     * Получение списка детей, привязанных к пользователю сессии, и активного ребенка.
     * Необходимо для последующего выбора активного ребенка, с которым ведется взаимодействие
     * @return Ответ сервера в виде [ChildrenApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun getChildren(): ChildrenApiResponse {
        val response = Request.get<ChildrenApiResponse>(
            listOf(PATH_PREFIX, PATH_CHILDREN, CHILDREN_VERSION).joinToString("/"),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }

    /**
     * Выбор активного ребенка родителя, с которым ведется взаимодействие
     * @param childId Идентификатор ребенка, полученный запросом
     * @return Ответ сервера в виде [SwitchActiveChildApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun setActiveChild(childId: Long): SwitchActiveChildApiResponse {
        val response = Request.put<SwitchActiveChildApiResponse>(
            listOf(PATH_PREFIX, PATH_ACTIVE_CHILD, ACTIVE_CHILD_VERSION).joinToString("/"),
            params = mapOf("childId" to childId),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }

    /**
     * Получение статуса (включена или выключена) настройки уведомлений о новых оценках для активного ребенка
     * @return Ответ сервера в виде [StatusDnevnikNotificationsApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun getStatusDnevnikNotifications(): StatusDnevnikNotificationsApiResponse {
        val response = Request.get<StatusDnevnikNotificationsApiResponse>(
            listOf(PATH_PREFIX, PATH_DNEVNIK_NOTIFICATIONS, DNEVNIK_NOTIFICATIONS_VERSION).joinToString("/"),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }

    /**
     * Включение или выключение уведомлений о новых оценках для активного ребенка
     * @return Ответ сервера в виде [SwitchDnevnikNotificationsApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun switchDnevnikNotifications(status: Boolean): SwitchDnevnikNotificationsApiResponse {
        val response = Request.put<SwitchDnevnikNotificationsApiResponse>(
            listOf(PATH_PREFIX, PATH_SWITCH_DNEVNIK_NOTIFICATIONS, SWITCH_DNEVNIK_NOTIFICATIONS_VERSION).joinToString("/"),
            params = mapOf("status" to status),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }

    /**
     * Установление или обновление сохраненного firebase-токена для уведомлений
     * @param firebaseToken Firebase-токен для отправки уведомлений клиенту
     * @return Ответ сервера в виде [UpdateFirebaseApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun updateFirebase(firebaseToken: String): UpdateFirebaseApiResponse {
        val response = Request.put<UpdateFirebaseApiResponse>(
            listOf(PATH_PREFIX, PATH_UPDATE_FIREBASE, UPDATE_FIREBASE_VERSION).joinToString("/"),
            params = mapOf("firebaseToken" to firebaseToken),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }
}