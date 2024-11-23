package com.kuts.klaf.data.networking.yandexApi

import com.kuts.domain.common.LoadingError
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.WordInfo
import com.kuts.domain.repositories.WordInfoRepository
import com.kuts.klaf.common.SecretConstants
import com.kuts.klaf.data.networking.toDomainEntity
import com.kuts.klaf.data.networking.yandexApi.YandexWordInfoProvider.WordInfoLoadingError.Common
import com.kuts.klaf.data.networking.yandexApi.YandexWordInfoProvider.WordInfoLoadingError.JsonConvert
import com.kuts.klaf.data.networking.yandexApi.entities.YandexWordInfo
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logW
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class YandexWordInfoProvider @Inject constructor() : WordInfoRepository {

    companion object {

        private const val BASE_URL = "https://dictionary.yandex.net/"
        private const val PATH = "api/v1/dicservice.json/lookup"
        private const val TIMEOUT = 10000L
    }

    sealed interface WordInfoLoadingError : LoadingError {

        data object Common : WordInfoLoadingError

        data object JsonConvert : WordInfoLoadingError
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
        defaultRequest { url(urlString = BASE_URL) }
        engine { requestTimeout = TIMEOUT }
    }

    override suspend fun fetchWordInfo(word: String): Flow<LoadingState<WordInfo>> = flow {
        emit(value = LoadingState.Loading)

        val apiKey = SecretConstants.YandexApi.YANDEX_WORD_INFO_API_KEY
        val url = buildUrl(apiKey = apiKey, word = word)
        val yandexWordInfoAsString = client.get(url).body<String>()
        val wordInfo = client.get(url).body<YandexWordInfo>().toDomainEntity()
        emit(value = LoadingState.Success(data = wordInfo))

        logD {
            message("fetchWordInfo() called")
            message("wordInfo = $yandexWordInfoAsString")
        }
    }.catch { throwable ->
        when (throwable) {
            is io.ktor.serialization.JsonConvertException -> {
                emit(value = LoadingState.Error(value = JsonConvert))
            }
            is CancellationException -> emit(value = LoadingState.Non)
            is NoTransformationFoundException -> emit(value = LoadingState.Non)
            else -> emit(value = LoadingState.Error(value = Common))
        }

        if (throwable !is CancellationException) {
            logW("fetchWordInfo caught ERROR: ${throwable.stackTraceToString()}")
        }
    }

    private fun buildUrl(apiKey: String, word: String): String {
        return "$PATH?key=$apiKey&lang=en-ru&text=$word"
    }
}