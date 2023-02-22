package com.example.klaf.data.room

import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.entities.RoomDeck
import com.example.klaf.data.room.entities.RoomStorageSaveVersion
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.entities.StorageSaveVersion

fun RoomDeck.mapToDomainEntity(): Deck = Deck(
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

fun StorageSaveVersion.mapToRoomEntity(): RoomStorageSaveVersion = RoomStorageSaveVersion(
    version = version
)

fun RoomStorageSaveVersion.mapToDomainEntity(): StorageSaveVersion = StorageSaveVersion(
    version = version
)