package com.kuts.klaf.firestore

import com.kuts.klaf.firestore.entities.FirestoreAutocompleteWord
import com.kuts.klaf.firestore.entities.FirestoreCard
import com.kuts.klaf.firestore.entities.FirestoreDeck
import com.kuts.klaf.firestore.entities.FirestoreStorageSaveVersion
import com.kuts.klaf.firestore.entities.FirestoreWordMeaningInsights
import com.kuts.klaf.firestore.entities.FirestoreWordMeaningItem
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.entities.WordMeaningItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun FirestoreDeck.toDomainEntity(): Deck = Deck(
    name = name,
    creationDate = creationDate,
    reviewPassDates = repetitionIterationDates,
    scheduledReviewDates = scheduledIterationDates,
    scheduledDateInterval = scheduledDateInterval,
    reviewCount = repetitionQuantity,
    cardQuantity = cardQuantity,
    lastFirstReviewDuration = lastFirstRepetitionDuration,
    lastSecondReviewDuration = lastSecondRepetitionDuration,
    lastReviewPassDuration = lastRepetitionIterationDuration,
    isLastPassSucceeded = isLastIterationSucceeded,
    id = id
)

fun Deck.toFirestoreEntity(): FirestoreDeck = FirestoreDeck(
    name = name,
    creationDate = creationDate,
    repetitionIterationDates = reviewPassDates,
    scheduledIterationDates = scheduledReviewDates,
    scheduledDateInterval = scheduledDateInterval,
    repetitionQuantity = reviewCount,
    cardQuantity = cardQuantity,
    lastFirstRepetitionDuration = lastFirstReviewDuration,
    lastSecondRepetitionDuration = lastSecondReviewDuration,
    lastRepetitionIterationDuration = lastReviewPassDuration,
    isLastIterationSucceeded = isLastPassSucceeded,
    id = id
)

fun FirestoreCard.toDomainEntity(): Card = Card(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = Json.decodeFromString(string = ipa),
    wordMeaningInsights = wordMeaningInsights?.toDomainEntity() ?: WordMeaningInsights.EMPTY,
    id = id
)

fun Card.toFirestoreEntity(): FirestoreCard = FirestoreCard(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = Json.encodeToString(value = ipa),
    wordMeaningInsights = wordMeaningInsights.toFirestoreEntityOrNull(),
    id = id
)

fun FirestoreStorageSaveVersion.toDomainEntity(): StorageSaveVersion = StorageSaveVersion(
    version = version
)

fun StorageSaveVersion.toFirestoreEntity(): FirestoreStorageSaveVersion {
    return FirestoreStorageSaveVersion(
        version = version
    )
}

fun FirestoreAutocompleteWord.toDomainEntity(): AutocompleteWord = AutocompleteWord(
    value = word
)

private fun FirestoreWordMeaningInsights.toDomainEntity(): WordMeaningInsights {
    val mappedWord = word.trim()
    val mappedLanguage = language.trim()
    val mappedMeanings = meanings
        .map { meaning -> meaning.toDomainEntity() }
        .filter { meaning -> meaning.translation.isNotBlank() }

    return if (mappedWord.isBlank() || mappedMeanings.isEmpty()) {
        WordMeaningInsights.EMPTY
    } else {
        WordMeaningInsights(
            word = mappedWord,
            language = mappedLanguage,
            meanings = mappedMeanings,
        )
    }
}

private fun FirestoreWordMeaningItem.toDomainEntity(): WordMeaningItem {
    return WordMeaningItem(
        frequencyRank = frequencyRank,
        translation = translation.trim(),
        proficiencyLevel = proficiencyLevel.toCefrLevelOrDefault(),
        context = context.trim(),
        examples = examples.map { example -> example.trim() }.filter { it.isNotBlank() },
    )
}

private fun WordMeaningInsights.toFirestoreEntityOrNull(): FirestoreWordMeaningInsights? {
    if (!hasData()) return null

    return FirestoreWordMeaningInsights(
        word = word.trim(),
        language = language.trim(),
        meanings = meanings.map { meaning -> meaning.toFirestoreEntity() },
    )
}

private fun WordMeaningItem.toFirestoreEntity(): FirestoreWordMeaningItem {
    return FirestoreWordMeaningItem(
        frequencyRank = frequencyRank,
        translation = translation.trim(),
        proficiencyLevel = proficiencyLevel.name,
        context = context.trim(),
        examples = examples.map { example -> example.trim() }.filter { it.isNotBlank() },
    )
}

private fun String.toCefrLevelOrDefault(): CefrLevel {
    return runCatching {
        CefrLevel.valueOf(this.trim().uppercase())
    }.getOrDefault(defaultValue = CefrLevel.A1)
}
