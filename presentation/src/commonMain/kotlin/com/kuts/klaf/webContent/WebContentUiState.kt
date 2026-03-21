package com.kuts.klaf.webContent

data class WebContentUiState(
    val source: WebContentSource? = null,
    val url: String? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val config: WebContentConfig = WebContentConfig(),
)
