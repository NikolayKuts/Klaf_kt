package com.kuts.klaf.data.networking.yandexApi

import android.content.Context
import com.kuts.domain.common.LoadingError
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.WordInfo
import com.kuts.domain.repositories.WordInfoRepository
import com.kuts.klaf.R
import com.kuts.klaf.common.SecretConstants
import com.kuts.klaf.data.networking.toDomainEntity
import com.kuts.klaf.data.networking.yandexApi.YandexWordInfoProvider.WordInfoLoadingError.Common
import com.kuts.klaf.data.networking.yandexApi.YandexWordInfoProvider.WordInfoLoadingError.JsonConvert
import com.kuts.klaf.data.networking.yandexApi.entities.YandexWordInfo
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logW
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.inject.Inject
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.cancellation.CancellationException

class YandexWordInfoProvider @Inject constructor(
    @ApplicationContext context: Context,
) : WordInfoRepository {

    companion object {

        private const val BASE_URL = "https://dictionary.yandex.net/"
        private const val PATH = "api/v1/dicservice.json/lookup"
        private const val TIMEOUT = 10000L
        private const val YANDEX_CERTIFICATE_ALIAS = "yandex_certificate_alias"
        private const val CERTIFICATE_FACTORY_TYPE = "X.509"
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
        engine {
            requestTimeout = TIMEOUT
            https { trustManager = createTrustManager(context = context) }
        }
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

    private fun createTrustManager(context: Context): X509TrustManager {
        val certInputStream = context.resources.openRawResource(R.raw.yandex_dictionary_api_cert)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            val certificateFactory = CertificateFactory.getInstance(CERTIFICATE_FACTORY_TYPE)
                .generateCertificate(certInputStream)

            load(null, null)
            setCertificateEntry(YANDEX_CERTIFICATE_ALIAS, certificateFactory)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        ).apply { init(keyStore) }

        val trustManagers = trustManagerFactory.trustManagers

        return trustManagers.first() as X509TrustManager
    }
}