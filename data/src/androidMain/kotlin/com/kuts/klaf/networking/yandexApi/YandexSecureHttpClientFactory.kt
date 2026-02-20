package com.kuts.klaf.networking.yandexApi

import android.content.Context
import com.kuts.klaf.data.R
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class YandexSecureHttpClientFactory(
    private val context: Context,
) {

    companion object {

        private const val TIMEOUT = 10_000L
        private const val YANDEX_CERTIFICATE_ALIAS = "yandex_certificate_alias"
        private const val CERTIFICATE_FACTORY_TYPE = "X.509"
    }

    fun create(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            engine {
                requestTimeout = TIMEOUT
                https {
                    trustManager = createTrustManager(context = context)
                }
            }
        }
    }

    private fun createTrustManager(context: Context): X509TrustManager {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            val certificateFactory = context.resources
                .openRawResource(R.raw.yandex_dictionary_api_cert)
                .use { certInputStream ->
                    CertificateFactory.getInstance(CERTIFICATE_FACTORY_TYPE)
                        .generateCertificate(certInputStream)
                }

            load(null, null)
            setCertificateEntry(
                YANDEX_CERTIFICATE_ALIAS,
                certificateFactory,
            )
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm(),
        ).apply { init(keyStore) }

        val trustManagers = trustManagerFactory.trustManagers
        return trustManagers.first() as X509TrustManager
    }
}
