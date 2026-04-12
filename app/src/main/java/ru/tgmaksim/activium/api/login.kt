package ru.tgmaksim.activium.api

import kotlinx.serialization.Serializable

/**
 * Результат запроса на генерацию сессии и получение ссылки для ее авторизации
 * @param classId Идентификатор класса
 * @param loginUrl Ссылка для авторизации сессии (нужно открыть в браузере пользователя)
 * @param sessionId Строковый идентификатор сессии для персонализированных запросов
 * @author Максим Дрючин (tgmaksim)
 * @see LoginApiResponse
 * */
@Serializable data class LoginResult(
    override val classId: Int = CLASS_ID,
    val loginUrl: String,
    val sessionId: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x8
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос на генерацию сессии и получение ссылки для ее авторизации
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект API-ошибки
 * @param answer Ответ в случае успешной обработки
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class LoginApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: LoginResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x9
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * API-singleton для запросов группы login
 * @property PATH_PREFIX Группа API-запросов
 * @property PATH_LOGIN Название API-запроса для авторизации
 * @author Максим Дрючин (tgmaksim)
 * */
object Login {
    private const val PATH_PREFIX = "login"
    private const val PATH_LOGIN = "login"
    private const val LOGIN_VERSION = 0

    /**
     * Создание сессии или повторная авторизация. Полученная сессия сохраняется в кеш
     * @return Ответ сервера в виде [LoginApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun login(sessionId: String?, firebaseToken: String): LoginApiResponse {
        val parameters = mutableMapOf("firebaseToken" to firebaseToken)
        if (sessionId != null)
            parameters["sessionId"] = sessionId

        val response = Request.post<LoginApiResponse>(
            listOf(PATH_PREFIX, PATH_LOGIN, LOGIN_VERSION).joinToString("/"),
            params = parameters
        )

        return response
    }
}