package com.kuts.klaf.data.firestore

import com.kuts.klaf.data.firestore.entities.FirestoreAutocompleteWord
import com.kuts.klaf.data.firestore.entities.FirestoreCard
import com.kuts.klaf.data.firestore.entities.FirestoreDeck
import com.kuts.klaf.data.firestore.entities.FirestoreStorageSaveVersion
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun FirestoreDeck.toDomainEntity(): Deck = Deck(
    name = name,
    creationDate = creationDate,
    repetitionIterationDates = repetitionIterationDates,
    scheduledIterationDates = scheduledIterationDates,
    scheduledDateInterval = scheduledDateInterval,
    repetitionQuantity = repetitionQuantity,
    cardQuantity = cardQuantity,
    lastFirstRepetitionDuration = lastFirstRepetitionDuration,
    lastSecondRepetitionDuration = lastSecondRepetitionDuration,
    lastRepetitionIterationDuration = lastRepetitionIterationDuration,
    isLastIterationSucceeded = isLastIterationSucceeded,
    id = id
)

fun Deck.toFirestoreEntity(): FirestoreDeck = FirestoreDeck(
    name = name,
    creationDate = creationDate,
    repetitionIterationDates = repetitionIterationDates,
    scheduledIterationDates = scheduledIterationDates,
    scheduledDateInterval = scheduledDateInterval,
    repetitionQuantity = repetitionQuantity,
    cardQuantity = cardQuantity,
    lastFirstRepetitionDuration = lastFirstRepetitionDuration,
    lastSecondRepetitionDuration = lastSecondRepetitionDuration,
    lastRepetitionIterationDuration = lastRepetitionIterationDuration,
    isLastIterationSucceeded = isLastIterationSucceeded,
    id = id
)

fun FirestoreCard.toDomainEntity(): Card = Card(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = Json.decodeFromString(string = ipa),
    id = id
)

fun Card.toFirestoreEntity(): FirestoreCard = FirestoreCard(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = Json.encodeToString(value = ipa),
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