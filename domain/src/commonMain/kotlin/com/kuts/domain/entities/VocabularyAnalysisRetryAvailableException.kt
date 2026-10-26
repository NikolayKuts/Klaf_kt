package com.kuts.domain.entities

class VocabularyAnalysisRetryAvailableException(
    val retryToken: String,
    serverNotificationSent: Boolean = false,
) : VocabularyAnalysisFailedException("Analysis failed. Try again.", serverNotificationSent)

class VocabularyAnalysisRetryExpiredException(serverNotificationSent: Boolean = false) :
    VocabularyAnalysisFailedException("Please start a new analysis.", serverNotificationSent)
