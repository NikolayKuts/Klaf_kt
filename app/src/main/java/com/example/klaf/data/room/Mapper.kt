package com.example.klaf.data.room

import androidx.room.PrimaryKey
import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.entities.RoomDeck
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck

fun RoomDeck.mapToDeck(): Deck = Deck(
    name = name,
    creationDate = creationDate,
    id = id,
    cardQuantity = cardQuantity,
    repeatDay = repeatDay,
    scheduledDate = scheduledDate,
    lastRepeatDate = lastRepeatDate,
    repeatQuantity = repeatQuantity,
    lastRepeatDuration = lastRepeatDuration,
    isLastRepetitionSucceeded = isLastRepetitionSucceeded
)

fun Deck.mapToRoomEntity(): RoomDeck = RoomDeck(
    name = name,
    creationDate = creationDate,
    id = id,
    cardQuantity = cardQuantity,
    repeatDay = repeatDay,
    scheduledDate = scheduledDate,
    lastRepeatDate = lastRepeatDate,
    repeatQuantity = repeatQuantity,
    lastRepeatDuration = lastRepeatDuration,
    isLastRepetitionSucceeded = isLastRepetitionSucceeded
)

fun RoomCard.mapToCard(): Card = Card(
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