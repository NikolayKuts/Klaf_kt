package com.kuts.domain.entities

data class SpeechToTextSegment(
    val startMillis: Long,
    val endMillis: Long,
    val text: String,
)
