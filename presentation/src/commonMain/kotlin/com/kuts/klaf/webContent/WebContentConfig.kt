package com.kuts.klaf.webContent

data class WebContentConfig(
    val cachePolicy: WebContentCachePolicy = WebContentCachePolicy.DEFAULT,
    val allowHttp: Boolean = false,
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
)
