package com.kuts.domain.entities

open class VocabularyAnalysisFailedException(
    message: String,
    val serverNotificationSent: Boolean = false,
) : RuntimeException(message)
