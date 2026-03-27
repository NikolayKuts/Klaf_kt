package com.kuts.klaf.networking.codexApp

import io.ktor.client.HttpClient

interface ICodexAppHttpClientFactory {

    fun create(): HttpClient
}
