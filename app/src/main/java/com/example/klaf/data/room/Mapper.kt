package com.example.klaf.data.room

import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.entities.RoomDeck
import com.example.klaf.data.room.entities.RoomStorageSaveVersion
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.entities.StorageSaveVersion
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun RoomDeck.toDomainEntity(): Deck = Deck(
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

fun Deck.toRoomEntity(): RoomDeck = RoomDeck(
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

fun RoomCard.toDomainEntity(): Card = Card(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = Json.decodeFromString(string = ipa),
    id = id
)

fun Card.toRoomEntity(): RoomCard = RoomCard(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = Json.encodeToString(value = ipa),
    id = id
)

fun StorageSaveVersion.toRoomEntity(): RoomStorageSaveVersion = RoomStorageSaveVersion(
    version = version
)

fun RoomStorageSaveVersion.toDomainEntity(): StorageSaveVersion = StorageSaveVersion(
    version = version
)