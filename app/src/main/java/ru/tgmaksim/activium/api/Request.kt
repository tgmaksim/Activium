package ru.tgmaksim.activium.api

import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.util.reflect.typeInfo
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json
import java.util.concurrent.CancellationException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
//import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.put

import ru.tgmaksim.activium.BuildConfig

/** Общие настройки Json для кеша и API-запросов */
val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Http-клиент для осуществления API-запросов */
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(json)
    }
}

/**
 * Singleton для осуществления всех API-запросов
 * @author Максим Дрючин (tgmaksim)
 * */
object Request {
    const val API_PREFIX = "api/v2"

    /**
     * Обобщенная функция для осуществления API-запросов с помощью метода GET
     * @param path Путь к нужному запросу
     * @param sessionId Идентификатор сессии
     * @param params Дополнительный параметры запроса в ?query
     * @return Десериализованный результат запроса в виде [TRes]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend inline fun <reified TRes : ApiResponse> get(
        path: String,
        sessionId: String? = null,
        params: Map<String, Any> = mapOf()
    ): TRes {
        return httpClient.get(listOf(BuildConfig.DOMAIN, API_PREFIX, path).joinToString("/")) {
            header("apiKey", BuildConfig.API_KEY)
            header("sessionId", sessionId)
            for (param in params) parameter(param.key, param.value)
        }.body(typeInfo<TRes>())
    }

    /**
     * Обобщенная функция для осуществления API-запросов с помощью метода POST
     * @param path Путь к нужному запросу
     * @param sessionId Идентификатор сессии
     * @param params Дополнительный параметры запроса в ?query
     * @return Десериализованный результат запроса в виде [TRes]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend inline fun <reified TRes : ApiResponse> post(
        path: String,
        sessionId: String? = null,
        params: Map<String, Any> = mapOf(),
        body: Any? = null
    ): TRes {
        return httpClient.post(listOf(BuildConfig.DOMAIN, API_PREFIX, path).joinToString("/")) {
            header("apiKey", BuildConfig.API_KEY)
            header("sessionId", sessionId)
            for (param in params) parameter(param.key, param.value)
            body?.let { setBody(it) }
        }.body(typeInfo<TRes>())
    }

    /**
     * Обобщенная функция для осуществления API-запросов с помощью метода PUT
     * @param path Путь к нужному запросу
     * @param sessionId Идентификатор сессии
     * @param params Дополнительный параметры запроса в ?query
     * @return Десериализованный результат запроса в виде [TRes]
     * @exception Exception
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend inline fun <reified TRes : ApiResponse> put(
        path: String,
        sessionId: String? = null,
        params: Map<String, Any> = mapOf(),
        body: Any? = null
    ): TRes {
        return httpClient.put(listOf(BuildConfig.DOMAIN, API_PREFIX, path).joinToString("/")) {
            header("apiKey", BuildConfig.API_KEY)
            header("sessionId", sessionId)
            for (param in params) parameter(param.key, param.value)
            body?.let { setBody(it) }
        }.body(typeInfo<TRes>())
    }

//    /**
//     * Обобщенная функция для осуществления API-запросов с помощью метода DELETE
//     * @param path Путь к нужному запросу
//     * @param sessionId Идентификатор сессии
//     * @param params Дополнительный параметры запроса в ?query
//     * @return Десериализованный результат запроса в виде [TRes]
//     * @exception Exception
//     * @author Максим Дрючин (tgmaksim)
//     * */
//    suspend inline fun <reified TRes : ApiResponse> delete(
//        path: String,
//        sessionId: String? = null,
//        params: Map<String, Any> = mapOf()
//    ): TRes {
//        return httpClient.delete(listOf(BuildConfig.DOMAIN, API_PREFIX, path).joinToString("/")) {
//            header("apiKey", BuildConfig.API_KEY)
//            header("sessionId", sessionId)
//            for (param in params) parameter(param.key, param.value)
//        }.body(typeInfo<TRes>())
//    }

    /**
     * Проверка соединения с интернетом путем попытки подключения к серверу API
     * @return Статус проверки
     * @author Максим Дрючин (tgmaksim)
     * */
    suspend inline fun checkInternet(): Boolean =
        try {
            httpClient.get(BuildConfig.CHECK_INTERNET_DOMAIN)
            true
        } catch (e: Exception) {
            e is CancellationException
        }
}