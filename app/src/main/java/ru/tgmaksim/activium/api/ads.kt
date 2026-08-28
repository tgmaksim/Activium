package ru.tgmaksim.activium.api

import kotlinx.serialization.Serializable
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Рекламное объявление
 * @param classId Идентификатор класса
 * @param adId Идентификатор рекламного объявления
 * @param title Заголовок рекламного объявления
 * @param text Текст рекламного объявления
 * @param imageUrl Ссылка для скачивания рекламной картинки
 * @param url URL для открытия страницы при нажатии на рекламу
 */
@Serializable
data class Ad(
    override val classId: Int = CLASS_ID,
    val adId: Int,
    val title: String,
    val text: String,
    val imageUrl: String,
    val url: String
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x61
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Результат запроса получения рекламного объявления
 * @param classId Идентификатор класса
 * @param ad Рекламное объявление для показа, если доступно
 */
@Serializable
data class AdResult(
    override val classId: Int = CLASS_ID,
    val ad: Ad?
) : ApiBase() {
    companion object {
        const val CLASS_ID = 0x62
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос получения рекламного объявления
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Данные для показа рекламы
 */
@Serializable
data class AdApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: AdResult?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x63
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID)
            throw ClassCastException()
    }
}

/**
 * Ответ на запрос клика на рекламу
 * @param classId Идентификатор класса
 * @param status Статус выполненного запроса
 * @param error Объект ошибки
 * @param answer Всегда null
 */
@Serializable
data class ClickAdApiResponse(
    override val classId: Int = CLASS_ID,
    override val status: Boolean,
    override val error: ApiError?,
    override val answer: ApiBase?
) : ApiResponse() {
    companion object {
        const val CLASS_ID = 0x64
    }

    init {
        if (classId != CLASS_ID && classId != ApiResponse.CLASS_ID) throw ClassCastException()
        if (answer != null) throw ClassCastException()
    }
}

/**
 * API-singleton для запросов группы ads
 * @property PATH_ADS Название группы API-запросов
 * @author Максим Дрючин (tgmaksim)
 * */
object Ads {
    private const val PATH_ADS = "ads"
    private const val PATH_CHECK_ACCESSIBLE_AD = "checkAccessibleAd"
    private const val PATH_CLICK_AD = "clickAd"

    private const val CHECK_ACCESSIBLE_AD = 0
    private const val CLICK_AD = 0

    /**
     * Проверка наличия и получение рекламного объявления
     * @return Ответ сервера в виде [AdApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun checkAccessibleAd(): AdApiResponse {
        val response = Request.post<AdApiResponse>(
            listOf(PATH_ADS, PATH_CHECK_ACCESSIBLE_AD, CHECK_ACCESSIBLE_AD).joinToString("/"),
            sessionId = SettingsManager.getSessionId()
        )

        return response
    }

    /**
     * Записать в статистику клик на рекламу и открытие связанного url
     * @return Ответ сервера в виде [ClickAdApiResponse]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend fun clickAd(adId: Int): ClickAdApiResponse {
        val response = Request.put<ClickAdApiResponse>(
            listOf(PATH_ADS, PATH_CLICK_AD, CLICK_AD).joinToString("/"),
            sessionId = SettingsManager.getSessionId(),
            params = mapOf("adId" to adId)
        )

        return response
    }
}