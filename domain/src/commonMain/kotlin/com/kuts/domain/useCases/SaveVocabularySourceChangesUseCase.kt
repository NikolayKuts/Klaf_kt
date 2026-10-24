package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.repositories.IVocabularySourceRepository
import com.kuts.domain.vocabularySource.VocabularySourceItemKey
import com.kuts.domain.vocabularySource.reviewKey
import kotlinx.coroutines.withContext

private const val LEGACY_VOCABULARY_SOURCE_LANGUAGE = "en"

class SaveVocabularySourceChangesUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
    private val ignoredVocabularyWordRepository: IIgnoredVocabularyWordRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(
        source: VocabularySource,
        draftItems: List<VocabularySourceItem>,
        shouldReplaceSourceItems: Boolean,
        ignoredDecisions: Map<VocabularySourceItemKey, Boolean> = emptyMap(),
    ) {
        withContext(context = coroutineContextProvider.io) {
            localStorageTransactionRepository.performWithTransaction {
                vocabularySourceRepository.saveSource(source = source)

                if (shouldReplaceSourceItems) {
                    ignoredVocabularyWordRepository.removeWords(
                        keys = ignoredDecisions.filterValues { !it }.keys,
                    )
                    ignoredVocabularyWordRepository.saveWords(
                        words = ignoredDecisions.filterValues { it }.keys.map { key ->
                            draftItems.firstOrNull { it.reviewKey() == key }
                                ?.toIgnoredVocabularyWord(createdAt = getCurrentDateAsLong())
                                ?: IgnoredVocabularyWord(key.language, key.foreignWord, key.nativeWord, getCurrentDateAsLong())
                        },
                    )
                    vocabularySourceRepository.replaceNotAddedItemsBySourceId(
                        sourceId = source.id,
                        items = draftItems.filter { item ->
                            item.status != VocabularySourceItemStatus.ADDED
                        },
                    )
                }

                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }

    private fun VocabularySourceItem.toIgnoredVocabularyWord(createdAt: Long): IgnoredVocabularyWord = IgnoredVocabularyWord(
        language = language.ifBlank { LEGACY_VOCABULARY_SOURCE_LANGUAGE },
        foreignWord = foreignWord.trim(),
        nativeWord = nativeWord.trim(),
        createdAt = createdAt,
    )
}
