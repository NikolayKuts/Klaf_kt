package com.kuts.klaf.vocabularySource

import com.kuts.domain.managers.IVocabularySourceTranscriptionBackgroundManager
import com.kuts.domain.managers.VocabularySourceTranscriptionHandle

class NoOpVocabularySourceTranscriptionBackgroundManager : IVocabularySourceTranscriptionBackgroundManager {

    override fun startTranscription(
        sourceId: Int,
        sourceTitle: String,
    ): VocabularySourceTranscriptionHandle = VocabularySourceTranscriptionHandle { _, _ -> }
}
