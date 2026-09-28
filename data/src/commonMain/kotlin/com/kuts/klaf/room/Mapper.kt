package com.kuts.klaf.room

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.CardMnemonic
import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemConfidence
import com.kuts.domain.entities.VocabularySourceItemOccurrence
import com.kuts.domain.entities.VocabularySourceItemPartOfSpeech
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.vocabularySource.toLanguageKey
import com.kuts.domain.vocabularySource.toMeaningKey
import com.kuts.domain.vocabularySource.toVocabularyKey
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomIgnoredVocabularyWord
import com.kuts.klaf.room.entities.RoomStorageSaveVersion
import com.kuts.klaf.room.entities.RoomVocabularySource
import com.kuts.klaf.room.entities.RoomVocabularySourceItem
import com.lib.lokdroid.core.logE
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ROOM_JSON_LOG_PREVIEW_LENGTH = 300

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

fun RoomVocabularySource.toDomainEntity(): VocabularySource = VocabularySource(
    title = title,
    description = description,
    url = url,
    rawText = rawText,
    cleanText = cleanText,
    analysisVersion = analysisVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAnalyzedAt = lastAnalyzedAt,
    id = id,
)

fun VocabularySource.toRoomEntity(): RoomVocabularySource = RoomVocabularySource(
    title = title,
    description = description,
    url = url,
    rawText = rawText,
    cleanText = cleanText,
    analysisVersion = analysisVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAnalyzedAt = lastAnalyzedAt,
    id = id,
)

fun RoomVocabularySourceItem.toDomainEntity(): VocabularySourceItem = VocabularySourceItem(
    sourceId = sourceId,
    language = language,
    foreignWord = foreignWord,
    transcription = transcription,
    nativeWord = nativeWord,
    originalText = originalText,
    partOfSpeech = partOfSpeech.toEnumOrDefault(default = VocabularySourceItemPartOfSpeech.UNKNOWN),
    cefrLevel = cefrLevel?.toEnumOrDefault<CefrLevel>(),
    confidence = confidence.toEnumOrDefault(default = VocabularySourceItemConfidence.MEDIUM),
    category = category.toEnumOrDefault(default = VocabularySourceItemCategory.NEW),
    status = status.toEnumOrDefault(default = VocabularySourceItemStatus.PENDING),
    sourceExample = sourceExample,
    explanation = explanation,
    knownMeaningsSnapshot = knownMeaningsSnapshot,
    alreadyExists = alreadyExists,
    occurrences = occurrencesJson.toVocabularySourceItemOccurrences(),
    createdCardId = createdCardId,
    targetDeckId = targetDeckId,
    firstOccurrenceOrder = firstOccurrenceOrder,
    isEdited = isEdited,
    createdAt = createdAt,
    updatedAt = updatedAt,
    id = id,
)

fun VocabularySourceItem.toRoomEntity(): RoomVocabularySourceItem = RoomVocabularySourceItem(
    sourceId = sourceId,
    language = language,
    foreignWord = foreignWord,
    transcription = transcription,
    nativeWord = nativeWord,
    originalText = originalText,
    partOfSpeech = partOfSpeech.name,
    cefrLevel = cefrLevel?.name,
    confidence = confidence.name,
    category = category.name,
    status = status.name,
    sourceExample = sourceExample,
    explanation = explanation,
    knownMeaningsSnapshot = knownMeaningsSnapshot,
    alreadyExists = alreadyExists,
    occurrencesJson = cardJson.encodeToString(value = occurrences),
    createdCardId = createdCardId,
    targetDeckId = targetDeckId,
    firstOccurrenceOrder = firstOccurrenceOrder,
    isEdited = isEdited,
    createdAt = createdAt,
    updatedAt = updatedAt,
    id = id,
)

fun RoomIgnoredVocabularyWord.toDomainEntity(): IgnoredVocabularyWord = IgnoredVocabularyWord(
    language = language,
    foreignWord = foreignWord,
    nativeWord = nativeWord,
    createdAt = createdAt,
    id = id,
)

fun IgnoredVocabularyWord.toRoomEntity(): RoomIgnoredVocabularyWord = RoomIgnoredVocabularyWord(
    language = language.trim(),
    foreignWord = foreignWord.trim(),
    nativeWord = nativeWord.trim(),
    languageKey = language.toLanguageKey(),
    foreignWordKey = foreignWord.toVocabularyKey(),
    nativeWordKey = nativeWord.toMeaningKey(),
    createdAt = createdAt,
    id = id,
)

private fun String.toCardMnemonic(): CardMnemonic {
    if (isBlank()) return CardMnemonic.EMPTY

    return runCatching {
        cardJson.decodeFromString<CardMnemonic>(this)
    }.getOrElse { error ->
        logE(
            "Failed to parse card mnemonic from Room JSON: " +
                "length=$length, preview=${toRoomJsonLogPreview()}\n" +
                error.stackTraceToString()
        )
        CardMnemonic.EMPTY
    }
}

private fun String.toVocabularySourceItemOccurrences(): List<VocabularySourceItemOccurrence> {
    if (isBlank()) return emptyList()

    return runCatching {
        cardJson.decodeFromString<List<VocabularySourceItemOccurrence>>(this)
    }.getOrElse { error ->
        logE(
            "Failed to parse vocabulary source item occurrences from Room JSON: " +
                "length=$length, preview=${toRoomJsonLogPreview()}\n" +
                error.stackTraceToString()
        )
        emptyList()
    }
}

private fun String.toRoomJsonLogPreview(): String {
    return take(n = ROOM_JSON_LOG_PREVIEW_LENGTH)
        .replace(oldChar = '\n', newChar = ' ')
        .replace(oldChar = '\r', newChar = ' ')
}

private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
    return enumValues<T>().firstOrNull { value -> value.name == this } ?: default
}

private inline fun <reified T : Enum<T>> String.toEnumOrDefault(): T? {
    return enumValues<T>().firstOrNull { value -> value.name == this }
}
