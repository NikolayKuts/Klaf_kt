package com.example.klaf.data.room

import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.entities.RoomDeck
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck

fun RoomDeck.mapToDeck(): Deck = Deck(
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

fun Deck.mapToRoomEntity(): RoomDeck = RoomDeck(
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
    id = id,
)

fun RoomCard.mapToDomainEntity(): Card = Card(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = ipa,
    id = id
)

fun Card.mapToRoomEntity(): RoomCard = RoomCard(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = ipa,
    id = id
)