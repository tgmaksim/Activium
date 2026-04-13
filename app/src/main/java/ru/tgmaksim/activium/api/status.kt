package ru.tgmaksim.activium.api

import kotlinx.serialization.Serializable
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Результат запроса данных о последней версии приложения
 * @param classId Идентификатор класса
 * @param latestVersionNumber Последняя доступная версия (номер сборки) приложения
 * @param latestVersionString Последняя доступная версия приложения
 * @param date Дата выпуска последней доступной версии приложения
 * @param versionStatusId Числовой статус новой версии, означающий важность обновления
 * @param versionStatus Статус новой версии, означающий важность обновления
 * @param updateLogs Изменения в последней версии приложения (latestVersion), которые можно показать пользователю
 * @author Максим Дрючин (tgmaksim)
 * @see VersionsApiResponse
 * */
@Serializable data class VersionsResult(
    override val classId: Int = CLASS_ID,
    val latestVersionNumber: Int,
    val latestVersionString: String,
    val date: String,
    val versionStatusId: Float,
    val versionStatus: String,
    val info: String?,
    val updateLogs: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x43
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос данных о последней версии приложения
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Ответ в случае успешной обработки
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class VersionsApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: VersionsResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x44
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

@Serializable data class HealthApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiResponse?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x5
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Информационное сообщение
 * @param classId Идентификатор класса
 * @param title Заголовок сообщения
 * @param text Текст сообщения
 */
@Serializable
data class Message(
    override val classId: Int = CLASS_ID,
    val title: String,
    val text: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x40
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса получения информационных сообщений
 * @param classId Идентификатор класса
 * @param messages Информационные сообщения для пользователя, если есть
 */
@Serializable
data class InformationResult(
    override val classId: Int = CLASS_ID,
    val messages: List<Message>
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x41
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения информационных сообщений
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Различная информация для показа пользователю
 */
@Serializable
data class InformationApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: InformationResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x42
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * API-singleton для запросов группы status
 * @property PATH_STATUS Название группы API-запросов
 * @property PATH_CHECK_VERSION Название API-запроса для проверки версии
 * @author Максим Дрючин (tgmaksim)
 * */
object Status {
    private const val PATH_STATUS = "status"
    private const val PATH_CHECK_VERSION = "checkVersion"
    private const val PATH_HEALTH = "health"
    private const val PATH_CHECK_INFO_NOTIFICATIONS = "checkInfoNotifications"

    private const val CHECK_VERSION_VERSION = 1
    private const val HEALTH_VERSION = 0
    private const val CHECK_INFO_NOTIFICATIONS_VERSION = 0

    /**
     * Получение данных о последней доступной версии приложения
     * @return Ответ сервера в виде [VersionsApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun checkVersion(versionCode: Int): VersionsApiResponse {
        val response = Request.get<VersionsApiResponse>(
            listOf(PATH_STATUS, PATH_CHECK_VERSION, CHECK_VERSION_VERSION).joinToString("/"),
            params = mapOf("versionNumber" to versionCode)
        )

        return response
    }

    /**
     * Проверка работоспособности сервера
     * @return Ответ сервера в виде [HealthApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun health(): HealthApiResponse {
        val response = Request.get<HealthApiResponse>(
            listOf(PATH_STATUS, PATH_HEALTH, HEALTH_VERSION).joinToString("/")
        )

        return response
    }

    /**
     * Проверка наличия и получение коротких оповещений для пользователя
     * @return Ответ сервера в виде [InformationApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun checkInfoNotifications(): InformationApiResponse {
        val response = Request.get<InformationApiResponse>(
            listOf(PATH_STATUS, PATH_CHECK_INFO_NOTIFICATIONS, CHECK_INFO_NOTIFICATIONS_VERSION).joinToString("/"),
            sessionId = SettingsManager.getSessionId()
        )

        return response
    }

    suspend fun checkHealth(): Boolean =
        try {
            health().status
        } catch (_: Exception) {
            false
        }
}