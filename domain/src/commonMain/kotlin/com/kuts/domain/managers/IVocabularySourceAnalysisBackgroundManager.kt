package com.kuts.domain.managers

enum class VocabularySourceAnalysisOutcome {
    Succeeded,
    Failed,
    Cancelled,
}

fun interface VocabularySourceAnalysisHandle {
    fun finish(outcome: VocabularySourceAnalysisOutcome)
}

interface IVocabularySourceAnalysisBackgroundManager {

    fun startAnalysis(
        sourceId: Int,
        sourceTitle: String,
    ): VocabularySourceAnalysisHandle
}
