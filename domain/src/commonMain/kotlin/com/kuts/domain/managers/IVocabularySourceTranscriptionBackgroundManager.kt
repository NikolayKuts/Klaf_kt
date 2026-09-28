package com.kuts.domain.managers

enum class VocabularySourceTranscriptionOutcome {
    Succeeded,
    Failed,
    Cancelled,
}

fun interface VocabularySourceTranscriptionHandle {
    fun finish(
        outcome: VocabularySourceTranscriptionOutcome,
        serverNotificationSent: Boolean,
    )
}

interface IVocabularySourceTranscriptionBackgroundManager {

    fun startTranscription(
        sourceId: Int,
        sourceTitle: String,
    ): VocabularySourceTranscriptionHandle
}
