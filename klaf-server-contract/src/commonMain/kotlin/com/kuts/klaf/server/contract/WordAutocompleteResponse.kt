package com.kuts.klaf.server.contract

import kotlinx.serialization.Serializable

@Serializable
data class WordAutocompleteResponse(val words: List<String>)
