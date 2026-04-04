package com.kuts.klaf.webContent

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WebContentViewModel(
    source: WebContentSource,
) : ViewModel() {

    private val initialConfig = WebContentConfig()
    private val initialUrl = source.url?.let { rawUrl ->
        normalizeUrl(rawUrl = rawUrl, config = initialConfig)
    }

    private val _uiState = MutableStateFlow(
        value = WebContentUiState(
            source = source,
            url = initialUrl,
            config = initialConfig,
        )
    )
    val uiState = _uiState.asStateFlow()

    fun resolveNavigationRequest(requestedUrl: String?): String? {
        val normalizedUrl = requestedUrl?.let { rawUrl ->
            normalizeUrl(rawUrl = rawUrl, config = _uiState.value.config)
        }

        return if (normalizedUrl != null) {
            _uiState.update { currentState ->
                currentState.copy(
                    url = normalizedUrl,
                    progress = 0,
                    errorMessage = null,
                )
            }
            normalizedUrl
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = requestedUrl
                        ?.takeIf(String::isNotBlank)
                        ?.let { "Blocked unsupported URL scheme" }
                        ?: currentState.errorMessage,
                )
            }
            null
        }
    }

    fun onPageLoadStarted(url: String?) {
        _uiState.update { currentState ->
            currentState.copy(
                url = url?.let { normalizeUrl(rawUrl = it, config = currentState.config) } ?: currentState.url,
                isLoading = true,
                errorMessage = null,
            )
        }
    }

    fun onPageLoadFinished(url: String?) {
        _uiState.update { currentState ->
            currentState.copy(
                url = url?.let { normalizeUrl(rawUrl = it, config = currentState.config) } ?: currentState.url,
                isLoading = false,
                progress = 100,
            )
        }
    }

    fun onPageLoadProgressChanged(progress: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                progress = progress.coerceIn(minimumValue = 0, maximumValue = 100),
                isLoading = progress in 0..99,
            )
        }
    }

    fun onPageLoadError(errorMessage: String?) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                errorMessage = errorMessage?.takeIf { it.isNotBlank() },
            )
        }
    }

    private fun normalizeUrl(
        rawUrl: String,
        config: WebContentConfig,
    ): String? {
        val trimmedUrl = rawUrl.trim()
        if (trimmedUrl.isEmpty()) {
            return null
        }

        return when {
            trimmedUrl.startsWith(prefix = "https://", ignoreCase = true) -> trimmedUrl
            trimmedUrl.startsWith(prefix = "http://", ignoreCase = true) && config.allowHttp -> trimmedUrl
            trimmedUrl.startsWith(prefix = "http://", ignoreCase = true) ->
                "https://${trimmedUrl.substring(startIndex = "http://".length)}"
            trimmedUrl.hasUnsupportedSchemePrefix() -> null
            "://" in trimmedUrl -> null
            else -> "https://$trimmedUrl"
        }
    }
}

private fun String.hasUnsupportedSchemePrefix(): Boolean {
    return listOf(
        "javascript:",
        "about:",
        "blob:",
        "data:",
        "file:",
        "mailto:",
        "tel:",
    ).any { prefix ->
        startsWith(prefix = prefix, ignoreCase = true)
    }
}
