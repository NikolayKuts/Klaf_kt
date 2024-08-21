package com.kuts.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class WordInfo(
    val transcription: String = "",
    val translations: List<String> = emptyList(),
)