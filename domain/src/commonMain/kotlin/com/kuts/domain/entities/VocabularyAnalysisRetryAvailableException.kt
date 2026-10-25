package com.kuts.domain.entities

class VocabularyAnalysisRetryAvailableException(val retryToken: String) :
    RuntimeException("Analysis failed. Try again.")

class VocabularyAnalysisRetryExpiredException : RuntimeException("Please start a new analysis.")
