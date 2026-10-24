package com.kuts.domain.useCases

import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import com.kuts.domain.vocabularySource.VocabularySourceReviewContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveVocabularySourceReviewContextUseCase(
    private val cardRepository: ICardRepository,
    private val ignoredVocabularyWordRepository: IIgnoredVocabularyWordRepository,
) {
    operator fun invoke(): Flow<VocabularySourceReviewContext> = combine(
        cardRepository.observeAllCards(),
        ignoredVocabularyWordRepository.observeWords(),
    ) { cards, ignoredWords -> VocabularySourceReviewContext(cards, ignoredWords) }
}
