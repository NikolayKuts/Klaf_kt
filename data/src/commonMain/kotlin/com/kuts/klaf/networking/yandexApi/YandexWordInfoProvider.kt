package com.kuts.klaf.networking.yandexApi

import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.WordInfo
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordInfoRepository.IWordInfoLoadingError
import com.kuts.klaf.SecretConstants
import com.kuts.klaf.networking.toDomainEntity
import com.kuts.klaf.networking.yandexApi.entities.YandexWordInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

class YandexWordInfoProvider(
    private val client: HttpClient,
) : IWordInfoRepository {

    companion object {

        private const val LOOKUP_URL = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup"
    }

    override suspend fun fetchWordInfo(
        word: String,
    ): Flow<LoadingState<WordInfo, IWordInfoLoadingError>> = flow<LoadingState<WordInfo, IWordInfoLoadingError>> {
        emit(value = LoadingState.Loading)

        val apiKey = SecretConstants.YandexApi.YANDEX_WORD_INFO_API_KEY
        val url = buildUrl(
            apiKey = apiKey,
            word = word,
        )
        val wordInfo = client.get(url).body<YandexWordInfo>().toDomainEntity()
        emit(value = LoadingState.Success(data = wordInfo))
    }.catch { throwable ->
        print("---> YandexWordInfoProvider ${throwable.stackTraceToString()}")
        when (throwable) {
            is SerializationException -> {
                emit(value = LoadingState.Error(value = IWordInfoLoadingError.JsonConvert))
            }
            is CancellationException -> emit(value = LoadingState.Non)
            is NoTransformationFoundException -> emit(value = LoadingState.Non)
            else -> emit(value = LoadingState.Error(value = IWordInfoLoadingError.Common))
        }
    }.flowOn(context = Dispatchers.Default)

    private fun buildUrl(
        apiKey: String,
        word: String,
    ): String {
        return "$LOOKUP_URL?key=$apiKey&lang=en-ru&text=$word"
    }
}
