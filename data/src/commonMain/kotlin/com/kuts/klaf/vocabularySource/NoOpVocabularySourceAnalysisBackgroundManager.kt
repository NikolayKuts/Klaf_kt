package com.kuts.klaf.vocabularySource

import com.kuts.domain.managers.IVocabularySourceAnalysisBackgroundManager
import com.kuts.domain.managers.VocabularySourceAnalysisHandle
import com.kuts.domain.managers.VocabularySourceAnalysisOutcome

class NoOpVocabularySourceAnalysisBackgroundManager : IVocabularySourceAnalysisBackgroundManager {

    override fun startAnalysis(
        sourceId: Int,
        sourceTitle: String,
    ): VocabularySourceAnalysisHandle =
        VocabularySourceAnalysisHandle { _: VocabularySourceAnalysisOutcome, _: Boolean -> Unit }
}
