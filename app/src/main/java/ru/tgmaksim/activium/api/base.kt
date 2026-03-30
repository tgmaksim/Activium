package ru.tgmaksim.activium.api

import kotlinx.serialization.Serializable

/**
 * Базовый класс для любой API-сущности
 * @property classId Идентификатор класса
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable abstract class ApiBase {
    abstract val classId: Int
}

/**
 * Класс для всех ошибок в API-ответах
 * @param classId Идентификатор класса
 * @param type Определенный тип ошибки из возможных
 * @param errorMessage Сообщение об ошибке для показа пользователю коротким оповещением
 * @author Максим Дрючин (tgmaksim)
 * @see ApiResponse
 * */
@Serializable data class ApiError(
    override val classId: Int = CLASS_ID,
    val type: String,
    val errorMessage: String?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x1
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Базовый класс для всех API-ответов
 * @property classId Идентификатор класса
 * @property status Статус выполненного запроса
 * @property error Объект API-ошибки
 * @property answer Ответ в случае успешной обработки
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable abstract class ApiResponse : ApiBase() {
    abstract override val classId: Int
    abstract val status: Boolean
    abstract val error: ApiError?
    abstract val answer: ApiBase?

    companion object {
        const val CLASS_ID = 0x2
    }
}
