package com.kuts.klaf.webContent

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebContentViewModel(
    initialUrl: String,
) : ViewModel() {

    private val _url = MutableStateFlow(value = initialUrl)
    val url = _url.asStateFlow()

    fun updateUrl(url: String) {
        _url.value = url
    }
}
