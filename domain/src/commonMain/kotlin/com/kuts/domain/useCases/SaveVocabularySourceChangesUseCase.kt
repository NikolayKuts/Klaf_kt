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
    ) {
        withContext(context = coroutineContextProvider.io) {
            localStorageTransactionRepository.performWithTransaction {
                vocabularySourceRepository.saveSource(source = source)

                if (shouldReplaceSourceItems) {
                    val ignoredItems = draftItems.filter { item ->
                        item.status == VocabularySourceItemStatus.IGNORED
                    }
                    ignoredVocabularyWordRepository.saveWords(
                        words = ignoredItems.map { item ->
                            item.toIgnoredVocabularyWord(createdAt = getCurrentDateAsLong())
                        },
                    )
                    vocabularySourceRepository.replaceNotAddedItemsBySourceId(
                        sourceId = source.id,
                        items = draftItems.filter { item ->
                            item.status == VocabularySourceItemStatus.PENDING
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
