package com.kuts.klaf.room

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.CardMnemonic
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomStorageSaveVersion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val cardJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

fun RoomDeck.toDomainEntity(): Deck = Deck(
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

fun Deck.toRoomEntity(): RoomDeck = RoomDeck(
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
    id = id,
)

fun RoomCard.toDomainEntity(): Card = Card(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = cardJson.decodeFromString(string = ipa),
    wordMeaningInsights = wordMeaningInsights,
    mnemonic = mnemonicJson.toCardMnemonic(),
    id = id
)

fun Card.toRoomEntity(): RoomCard = RoomCard(
    deckId = deckId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipa = cardJson.encodeToString(value = ipa),
    wordMeaningInsights = wordMeaningInsights,
    mnemonicJson = cardJson.encodeToString(value = mnemonic),
    id = id
)

fun StorageSaveVersion.toRoomEntity(): RoomStorageSaveVersion = RoomStorageSaveVersion(
    version = version
)

fun RoomStorageSaveVersion.toDomainEntity(): StorageSaveVersion = StorageSaveVersion(
    version = version
)

private fun String.toCardMnemonic(): CardMnemonic {
    if (isBlank()) return CardMnemonic.EMPTY

    return runCatching {
        cardJson.decodeFromString<CardMnemonic>(this)
    }.getOrDefault(defaultValue = CardMnemonic.EMPTY)
}
