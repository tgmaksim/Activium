package ru.tgmaksim.activium.api

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

/**
 * Отзыв
 * @param classId Идентификатор класса
 * @param reviewId Идентификатор отзыва
 * @param name Имя автора отзыва
 * @param stars Оценка отзыва
 * @param text Текст отзыва
 * @param likes Количество реакций на отзыв
 * @param createdAt Дата и время написания отзыва
 * @param isUpdated Отзыв был изменен после написания
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable
data class Review(
    override val classId: Int = CLASS_ID,
    val reviewId: Long,
    val name: String,
    val stars: Int,
    val text: String?,
    val likes: Int,
    val createdAt: Instant,
    val isUpdated: Boolean
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x29
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса получения своего отзыва
 * @param classId Идентификатор класса
 * @param review Отзыв написанный пользователем, если есть
 * @param onModeration Отзыв на модерации
 * @author Максим Дрючин (tgmaksim)
 * @see MyReviewApiResponse
 * @see CreateReviewApiResponse
 * */
@Serializable data class MyReviewResult(
    override val classId: Int = CLASS_ID,
    val review: Review?,
    val onModeration: Boolean
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x2A
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос создания отзыва
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Отзыв, который создан
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class CreateReviewApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MyReviewResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x2B
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения своего отзыва
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Отзыв, написанный пользователем, если есть
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class MyReviewApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MyReviewResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x2C
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос удаления отзыва
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class DeleteReviewApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiBase?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x2D
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
        if (answer != null)
            throw ClassCastException()
    }
}

/**
 * Результат запроса получения списка отзывов
 * @param classId Идентификатор класса
 * @param reviews Список отзывов по запросу
 * @param nextOffset Смещение для показа следующих отзывов
 * @author Максим Дрючин (tgmaksim)
 * @see ReviewsApiResponse
 * */
@Serializable data class ReviewsResult(
    override val classId: Int = CLASS_ID,
    val reviews: List<Review>,
    val nextOffset: Int?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x2E
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения списка отзывов
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Отзывы
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class ReviewsApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ReviewsResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x2F
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса постановки реакции на отзыв
 * @param classId Идентификатор класса
 * @param review Данные отзыва после постановки реакции
 * @author Максим Дрючин (tgmaksim)
 * @see LikeReviewApiResponse
 * */
@Serializable data class LikeReviewResult(
    override val classId: Int = CLASS_ID,
    val review: Review
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x30
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос постановки лайка
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Данные отзыва после постановки лайка
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class LikeReviewApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: LikeReviewResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x31
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса удаления реакции с отзыва
 * @param classId Идентификатор класса
 * @param review Данные отзыва после удаления реакции
 * @author Максим Дрючин (tgmaksim)
 * @see DeleteReviewLikeApiResponse
 * */
@Serializable data class DeleteReviewLikeResult(
    override val classId: Int = CLASS_ID,
    val review: Review
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x32
    }
    init {
        if (classId != CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос удаления реакции
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Данные отзыва после удаления реакции
 * @author Максим Дрючин (tgmaksim)
 * */
@Serializable data class DeleteReviewLikeApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: DeleteReviewLikeResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x33
    }
    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * API-singleton для запросов группы reviews
 * @property PATH_PREFIX Группа API-запросов
 * @author Максим Дрючин (tgmaksim)
 * */
object Reviews {
    private const val PATH_PREFIX = "reviews"
    private const val PATH_CREATE_REVIEW = "createReview"
    private const val PATH_GET_MY_REVIEW = "getMyReview"
    private const val PATH_DELETE_REVIEW = "deleteReview"
    private const val PATH_GET_REVIEWS = "getReviews"
    private const val PATH_LIKE_REVIEW = "likeReview"
    private const val PATH_DELETE_REVIEW_LIKE = "deleteReviewLike"

    private const val CREATE_REVIEW_VERSION = 0
    private const val GET_MY_REVIEW_VERSION = 0
    private const val DELETE_REVIEW_VERSION = 0
    private const val GET_REVIEWS_VERSION = 0
    private const val LIKE_REVIEW_VERSION = 0
    private const val DELETE_REVIEW_LIKE_VERSION = 0

    /**
     * Отправка или редактирование отзыва о приложение. Отзыв будет опубликован после модерации
     * @param stars Оценка от 1 до 5
     * @param text Текст отзыва
     * @return Ответ сервера в виде [CreateReviewApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun createReview(stars: Int, text: String?): CreateReviewApiResponse {
        val response = Request.post<CreateReviewApiResponse>(
            listOf(PATH_PREFIX, PATH_CREATE_REVIEW, CREATE_REVIEW_VERSION).joinToString("/"),
            params = mapOf("stars" to stars),
            body = text,
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }

    /**
     * Получение своего отзыва, если такой есть
     * @return Ответ сервера в виде [MyReviewApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun getMyReview(): MyReviewApiResponse {
        val response = Request.get<MyReviewApiResponse>(
            listOf(PATH_PREFIX, PATH_GET_MY_REVIEW, GET_MY_REVIEW_VERSION).joinToString("/"),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }

    /**
     * Удаление отзыва, если он был ранее опубликован пользователем
     * @return Ответ сервера в виде [DeleteReviewApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun deleteReview(): DeleteReviewApiResponse {
        val response = Request.delete<DeleteReviewApiResponse>(
            listOf(PATH_PREFIX, PATH_DELETE_REVIEW, DELETE_REVIEW_VERSION).joinToString("/"),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }

    /**
     * Получение отзывов, написанных другими пользователями, с нужной фильтрацией
     * @param mode Фильтрация отзывов (likes, max_stars, min_stars)
     * @param offset Смещение для получения следующих отзывов
     * @param limit Лимит отзывов
     * @return Ответ сервера в виде [ReviewsApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun getReviews(
        mode: String = "likes",
        offset: Int? = null,
        limit: Int = 16
    ): ReviewsApiResponse {
        val params = mutableMapOf<String, Any>("mode" to mode, "limit" to limit)
        offset?.let { params["offset"] = it }

        val response = Request.get<ReviewsApiResponse>(
            listOf(PATH_PREFIX, PATH_GET_REVIEWS, GET_REVIEWS_VERSION).joinToString("/"),
            params = params
        )

        return response
    }

    /**
     * Поставить реакцию на отзыв, чтобы поднять его в рейтинге
     * @param reviewId Идентификатор отзыва
     * @return Ответ сервера в виде [LikeReviewApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun likeReview(reviewId: Long): LikeReviewApiResponse {
        val response = Request.post<LikeReviewApiResponse>(
            listOf(PATH_PREFIX, PATH_LIKE_REVIEW, LIKE_REVIEW_VERSION).joinToString("/"),
            params = mapOf("reviewId" to reviewId),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }

    /**
     * Удалить ранее поставленную реакцию с отзыва
     * @param reviewId Идентификатор отзыва
     * @return Ответ сервера в виде [DeleteReviewLikeApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun deleteReviewLike(reviewId: Long): DeleteReviewLikeApiResponse {
        val response = Request.delete<DeleteReviewLikeApiResponse>(
            listOf(PATH_PREFIX, PATH_DELETE_REVIEW_LIKE, DELETE_REVIEW_LIKE_VERSION).joinToString("/"),
            params = mapOf("reviewId" to reviewId),
            sessionId = MemoryDataManager.sessionId.value
        )

        return response
    }
}