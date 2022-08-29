package com.example.klaf.data.firestore

import com.example.klaf.data.firestore.entities.FirestoreCard
import com.example.klaf.data.firestore.entities.FirestoreDeck
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck

fun FirestoreDeck.mapToDomainEntity(): Deck = Deck(
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

fun Deck.mapToFirestoreEntity(): FirestoreDeck = FirestoreDeck(
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

fun FirestoreCard.mapToDomainEntity(): Card = Card(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = ipa,
    id = id
)

fun Card.mapToFirestoreEntity(): FirestoreCard = FirestoreCard(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = ipa,
    id = id
)