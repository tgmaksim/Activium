package ru.tgmaksim.activium.api

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Школьный пост
 * @param classId Идентификатор класса
 * @param postId Идентификатор поста
 * @param title Заголовок поста
 * @param description Короткое описание поста, если есть
 * @param imageUrl Ссылка на главную картинку поста
 * @param author Имя автора поста
 * @param authorVerified Автор является сотрудником Активиум
 * @param scheduleDate Дата мероприятия в расписании
 * @param humanScheduleDate Дата мероприятия в расписании в нужном формате строки для показа пользователю
 * @param isUpdated Пост был отредактирован после написания
 * @param countViewings Количество полных просмотров поста
 * @param countLikes Количество реакций
 * @param hasMyLike Поставлена реакция на пост
 * @param createdAt Время написания поста
 * @param humanCreatedAt Время написания поста в нужном формате строки для показа пользователю
 * @param postUrl Ссылка на открытие поста
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class SchoolPost(
    override val classId: Int = CLASS_ID,
    val postId: Long,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val author: String,
    val authorVerified: Boolean,
    val scheduleDate: LocalDate?,
    val humanScheduleDate: String?,
    val isUpdated: Boolean,
    val countViewings: Int,
    val countLikes: Int,
    val hasMyLike: Boolean,
    val isSaw: Boolean,
    val postUrl: String,
    val createdAt: Instant,
    val humanCreatedAt: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x4E
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Результат запроса получения последних постов
 * @param classId Идентификатор класса
 * @param posts Список постов
 * @param nextOffset Смещение для получения следующих постов
 * @author Максим Дрючин (tgmaksim)
 * @see SchoolPostsApiResponse
 */
@Serializable
data class SchoolPostsResult(
    override val classId: Int = CLASS_ID,
    val posts: List<SchoolPost>,
    val nextOffset: Int?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x4F
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения последних постов
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Список последних постов
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class SchoolPostsApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: SchoolPostsResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x50
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса получения неувиденных постов
 * @param classId Идентификатор класса
 * @param countPosts Количество неувиденных постов
 * @author Максим Дрючин (tgmaksim)
 * @see SchoolPostsWithoutVisionApiResponse
 */
@Serializable
data class SchoolPostsWithoutVisionResult(
    override val classId: Int = CLASS_ID,
    val countPosts: Int
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x51
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения неувиденных постов
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Количество неувиденных постов
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class SchoolPostsWithoutVisionApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: SchoolPostsWithoutVisionResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x52
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса пометки поста
 * @param classId Идентификатор класса
 * @param post Обновленный пост
 * @param countPostsWithoutVision Количество неувиденных постов
 * @author Максим Дрючин (tgmaksim)
 * @see SeeSchoolPostApiResponse
 * @see ClickSchoolPostApiResponse
 * @see ViewSchoolPostApiResponse
 * @see LikeSchoolPostApiResponse
 * @see UnlikeSchoolPostApiResponse
 */
@Serializable
data class MarkSchoolPostResult(
    override val classId: Int = CLASS_ID,
    val post: SchoolPost,
    val countPostsWithoutVision: Int
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x53
    }

    init {
        if (classId != CLASS_ID) throw ClassCastException()
    }
}

/**
 * Ответ на запрос пометки поста как увиденного
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Обновленный пост и количество неувиденных постов
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class SeeSchoolPostApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarkSchoolPostResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x54
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос пометки поста как нажатого
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Обновленный пост и количество неувиденных постов
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class ClickSchoolPostApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarkSchoolPostResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x55
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос просмотра поста
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Обновленный пост и количество неувиденных постов
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class ViewSchoolPostApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarkSchoolPostResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x56
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос постановки реакции
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Обновленный пост и количество неувиденных постов
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class LikeSchoolPostApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarkSchoolPostResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x57
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос удаления реакции
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Обновленный пост и количество неувиденных постов
 * @author Максим Дрючин (tgmaksim)
 */
@Serializable
data class UnlikeSchoolPostApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: MarkSchoolPostResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x58
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * API-singleton для запросов группы school
 * @property PATH_PREFIX Группа API-запросов
 * @author Максим Дрючин (tgmaksim)
 */
object School {
    private const val PATH_PREFIX = "school"

    private const val PATH_GET_POSTS = "getPosts"
    private const val PATH_CHECK_NEW_POSTS = "checkNewPosts"
    private const val PATH_SEE_POST = "seePost"
    private const val PATH_CLICK_POST = "clickPost"
    private const val PATH_VIEW_POST = "viewPost"
    private const val PATH_LIKE_POST = "likePost"
    private const val PATH_UNLIKE_POST = "unlikePost"

    private const val GET_POSTS_VERSION = 0
    private const val CHECK_NEW_POSTS_VERSION = 0
    private const val SEE_POST_VERSION = 0
    private const val CLICK_POST_VERSION = 0
    private const val VIEW_POST_VERSION = 0
    private const val LIKE_POST_VERSION = 0
    private const val UNLIKE_POST_VERSION = 0

    /**
     * Получение последних постов, отсортированных по дате публикации
     * @param offset Смещение постов
     * @return Ответ сервера в виде [SchoolPostsApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun getPosts(offset: Int): SchoolPostsApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_GET_POSTS, GET_POSTS_VERSION).joinToString("/"),
            params = mapOf("offset" to offset),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Получение количества неувиденных постов, но только тех,
     * которые были опубликованы не ранее, чем 14 дней назад
     * @return Ответ сервера в виде [SchoolPostsWithoutVisionApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun checkNewPosts(): SchoolPostsWithoutVisionApiResponse {
        return Request.get(
            listOf(PATH_PREFIX, PATH_CHECK_NEW_POSTS, CHECK_NEW_POSTS_VERSION).joinToString("/"),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Пометить пост увиденным. После этого метод /checkNewPosts не будет считать его
     * @param postId Идентификатор поста
     * @return Ответ сервера в виде [SeeSchoolPostApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun seePost(postId: Long): SeeSchoolPostApiResponse {
        return Request.put(
            listOf(PATH_PREFIX, PATH_SEE_POST, SEE_POST_VERSION).joinToString("/"),
            params = mapOf("postId" to postId),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Пометить пост нажатым
     * @param postId Идентификатор поста
     * @return Ответ сервера в виде [ClickSchoolPostApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun clickPost(postId: Long): ClickSchoolPostApiResponse {
        return Request.put(
            listOf(PATH_PREFIX, PATH_CLICK_POST, CLICK_POST_VERSION).joinToString("/"),
            params = mapOf("postId" to postId),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Пометить пост просмотренным
     * @param postId Идентификатор поста
     * @return Ответ сервера в виде [ViewSchoolPostApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun viewPost(postId: Long): ViewSchoolPostApiResponse {
        return Request.put(
            listOf(PATH_PREFIX, PATH_VIEW_POST, VIEW_POST_VERSION).joinToString("/"),
            params = mapOf("postId" to postId),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Поставить реакцию на пост
     * @param postId Идентификатор поста
     * @return Ответ сервера в виде [LikeSchoolPostApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun likePost(postId: Long): LikeSchoolPostApiResponse {
        return Request.put(
            listOf(PATH_PREFIX, PATH_LIKE_POST, LIKE_POST_VERSION).joinToString("/"),
            params = mapOf("postId" to postId),
            sessionId = SettingsManager.getSessionId()
        )
    }

    /**
     * Убрать реакцию с поста
     * @param postId Идентификатор поста
     * @return Ответ сервера в виде [UnlikeSchoolPostApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     */
    suspend fun unlikePost(postId: Long): UnlikeSchoolPostApiResponse {
        return Request.put(
            listOf(PATH_PREFIX, PATH_UNLIKE_POST, UNLIKE_POST_VERSION).joinToString("/"),
            params = mapOf("postId" to postId),
            sessionId = SettingsManager.getSessionId()
        )
    }
}