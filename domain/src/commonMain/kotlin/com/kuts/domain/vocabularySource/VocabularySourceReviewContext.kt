package com.kuts.domain.vocabularySource

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus

data class VocabularySourceItemKey(
    val language: String,
    val foreignWord: String,
    val nativeWord: String,
)

data class VocabularySourceReviewContext(
    val cards: List<Card> = emptyList(),
    val ignoredWords: List<IgnoredVocabularyWord> = emptyList(),
) {
    private val cardsById = cards.associateBy(Card::id)
    private val cardsByWord = cards.groupBy { it.foreignWord.toVocabularyKey() }
    private val ignoredKeys = ignoredWords.map { it.reviewKey() }.toSet()

    fun resolve(
        items: List<VocabularySourceItem>,
        ignoredDecisions: Map<VocabularySourceItemKey, Boolean> = emptyMap(),
    ): List<VocabularySourceItem> {
        val effectiveIgnoredKeys = (ignoredKeys + ignoredDecisions.filterValues { it }.keys) -
            ignoredDecisions.filterValues { !it }.keys
        val ignoredByWord = effectiveIgnoredKeys.groupBy { it.language to it.foreignWord }
        return items.map { item ->
            val key = item.reviewKey()
            val linkedCard = item.createdCardId?.let(cardsById::get)
            val knownCards = cardsByWord[key.foreignWord].orEmpty()
            val hasKnownMeaning = knownCards.any { it.nativeWord.toMeaningKey() == key.nativeWord }
            val isIgnored = key in effectiveIgnoredKeys
            val hasOtherIgnoredMeaning = ignoredByWord[key.language to key.foreignWord].orEmpty()
                .any { it.nativeWord != key.nativeWord }
            item.copy(
                status = when {
                    linkedCard != null -> VocabularySourceItemStatus.ADDED
                    isIgnored -> VocabularySourceItemStatus.IGNORED
                    else -> VocabularySourceItemStatus.PENDING
                },
                createdCardId = linkedCard?.id,
                targetDeckId = linkedCard?.deckId,
                alreadyExists = linkedCard != null || hasKnownMeaning,
                category = when {
                    knownCards.isNotEmpty() && !hasKnownMeaning -> VocabularySourceItemCategory.POSSIBLE_NEW_MEANING
                    knownCards.isEmpty() && !isIgnored && hasOtherIgnoredMeaning -> VocabularySourceItemCategory.IGNORED_WORD_NEW_MEANING
                    else -> VocabularySourceItemCategory.NEW
                },
                knownMeaningsSnapshot = knownCards.map { it.nativeWord.trim() }.distinct().joinToString("; "),
            )
        }
    }
}

fun VocabularySourceItem.reviewKey(): VocabularySourceItemKey = vocabularySourceItemKey(language, foreignWord, nativeWord)

fun IgnoredVocabularyWord.reviewKey(): VocabularySourceItemKey = vocabularySourceItemKey(language, foreignWord, nativeWord)

fun vocabularySourceItemKey(
    language: String,
    foreignWord: String,
    nativeWord: String,
): VocabularySourceItemKey = VocabularySourceItemKey(
    language = language.ifBlank { "en" }.toLanguageKey(),
    foreignWord = foreignWord.toVocabularyKey(),
    nativeWord = nativeWord.toMeaningKey(),
)
