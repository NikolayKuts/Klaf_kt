package com.kuts.klaf.room

import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemConfidence
import com.kuts.domain.entities.VocabularySourceItemPartOfSpeech
import com.kuts.domain.vocabularySource.toLanguageKey
import com.kuts.domain.vocabularySource.toMeaningKey
import com.kuts.domain.vocabularySource.toVocabularyKey
import com.kuts.klaf.room.dao.ICardDao
import com.kuts.klaf.room.dao.IDeckDao
import com.kuts.klaf.room.dao.IVocabularySourceDao
import com.kuts.klaf.room.dao.IVocabularySourceItemDao
import com.kuts.klaf.room.entities.RoomIgnoredVocabularyWord
import com.kuts.klaf.room.entities.RoomVocabularySource
import com.kuts.klaf.room.entities.RoomVocabularySourceItem
import com.kuts.klaf.server.contract.SyncVocabularySource
import com.kuts.klaf.server.contract.SyncVocabularySourceItem
import com.kuts.klaf.server.contract.SyncIgnoredVocabularyWord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

fun SyncIgnoredVocabularyWord.identity(): String = "ignored:" + Json.encodeToString(
    listOf(language.toLanguageKey(), foreignWord.toVocabularyKey(), nativeWord.toMeaningKey()),
)

fun RoomIgnoredVocabularyWord.toSyncWord() = SyncIgnoredVocabularyWord(language, foreignWord, nativeWord, createdAt)

fun SyncIgnoredVocabularyWord.toRoomWord(revision: Long) = IgnoredVocabularyWord(
    language, foreignWord, nativeWord, createdAt,
).toRoomEntity().copy(lastChangedServerRevision = revision)

/** Clear obsolete links without changing the word's identity or its ADDED status. Call in a transaction. */
suspend fun clearMissingVocabularyLinks(
    items: IVocabularySourceItemDao,
    decks: IDeckDao,
    cards: ICardDao,
): Set<Int> {
    val deckIds = decks.getAllDecks().map { it.id }.toSet()
    val cardIds = cards.getAllCards().map { it.id }.toSet()
    val changed = items.getItems().mapNotNull { item ->
        val normalized = item.copy(
            createdCardId = item.createdCardId?.takeIf(cardIds::contains),
            targetDeckId = item.targetDeckId?.takeIf(deckIds::contains),
        )
        normalized.takeIf { it != item }
    }
    if (changed.isNotEmpty()) items.insertItems(changed)
    return changed.map { it.sourceId }.toSet()
}

suspend fun readSyncVocabularySources(
    sources: IVocabularySourceDao,
    items: IVocabularySourceItemDao,
    decks: IDeckDao,
    cards: ICardDao,
): List<SyncVocabularySource> {
    val deckIds = decks.getAllDecks().associate { it.id to it.syncId }
    val cardIds = cards.getAllCards().associate { it.id to it.syncId }
    val words = items.getItems().groupBy { it.sourceId }
    return sources.getSources().map { source ->
        SyncVocabularySource(
            syncId = source.syncId,
            title = source.title,
            description = source.description,
            url = source.url,
            rawText = source.rawText,
            cleanText = source.cleanText,
            analysisVersion = source.analysisVersion,
            createdAt = source.createdAt,
            updatedAt = source.updatedAt,
            lastAnalyzedAt = source.lastAnalyzedAt,
            items = words[source.id].orEmpty().map { item ->
                SyncVocabularySourceItem(
                    syncId = item.syncId,
                    language = item.language,
                    foreignWord = item.foreignWord,
                    transcription = item.transcription,
                    nativeWord = item.nativeWord,
                    originalText = item.originalText,
                    partOfSpeech = item.partOfSpeech,
                    cefrLevel = item.cefrLevel,
                    confidence = item.confidence,
                    category = item.category,
                    status = item.status,
                    sourceExample = item.sourceExample,
                    explanation = item.explanation,
                    knownMeaningsSnapshot = item.knownMeaningsSnapshot,
                    alreadyExists = item.alreadyExists,
                    occurrencesJson = item.occurrencesJson,
                    createdCardSyncId = item.createdCardId?.let(cardIds::get),
                    targetDeckSyncId = item.targetDeckId?.let(deckIds::get),
                    firstOccurrenceOrder = item.firstOccurrenceOrder,
                    isEdited = item.isEdited,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                )
            },
            lastChangedServerRevision = source.lastChangedServerRevision,
        )
    }
}

fun SyncVocabularySource.validate() {
    require(syncId.isNotBlank() && title.isNotBlank()) { "Source ID and title are required" }
    require(title.length <= 80 && rawText.length <= 40_000 && cleanText.length <= 40_000) { "Source text exceeds limits" }
    require(items.map { it.syncId }.distinct().size == items.size && items.all { it.syncId.isNotBlank() }) {
        "Duplicate or empty vocabulary item identity"
    }
    items.forEach { item ->
        VocabularySourceItemStatus.valueOf(item.status)
        VocabularySourceItemCategory.valueOf(item.category)
        VocabularySourceItemConfidence.valueOf(item.confidence)
        VocabularySourceItemPartOfSpeech.valueOf(item.partOfSpeech)
        item.cefrLevel?.let(CefrLevel::valueOf)
        Json.parseToJsonElement(item.occurrencesJson).jsonArray
    }
}

/** Call inside the owner's Room transaction, after applying deck/card rows. */
suspend fun applySyncVocabularySource(
    incoming: SyncVocabularySource,
    sources: IVocabularySourceDao,
    items: IVocabularySourceItemDao,
    decks: IDeckDao,
    cards: ICardDao,
) {
    incoming.validate()
    val existing = sources.getSourceBySyncId(incoming.syncId)
    val local = RoomVocabularySource(
        title = incoming.title,
        description = incoming.description,
        url = incoming.url,
        rawText = incoming.rawText,
        cleanText = incoming.cleanText,
        analysisVersion = incoming.analysisVersion,
        createdAt = incoming.createdAt,
        updatedAt = incoming.updatedAt,
        lastAnalyzedAt = incoming.lastAnalyzedAt,
        id = existing?.id ?: 0,
        syncId = incoming.syncId,
        lastChangedServerRevision = incoming.lastChangedServerRevision,
    )
    val id = sources.insertSource(local).toInt().let { existing?.id ?: it }
    val previousItems = items.getItemsBySourceId(id).associateBy { it.syncId }
    val otherItems = items.getItems().filter { it.sourceId != id }.map { it.syncId }.toSet()
    require(incoming.items.none { it.syncId in otherItems }) { "Vocabulary word belongs to another source" }
    val deckIds = decks.getAllDecks().associate { it.syncId to it.id }
    val cardIds = cards.getAllCards().associate { it.syncId to it.id }
    items.deleteItemsBySourceId(id)
    items.insertItems(incoming.items.map { item ->
        RoomVocabularySourceItem(
            sourceId = id,
            language = item.language,
            foreignWord = item.foreignWord,
            transcription = item.transcription,
            nativeWord = item.nativeWord,
            originalText = item.originalText,
            partOfSpeech = item.partOfSpeech,
            cefrLevel = item.cefrLevel,
            confidence = item.confidence,
            category = item.category,
            status = item.status,
            sourceExample = item.sourceExample,
            explanation = item.explanation,
            knownMeaningsSnapshot = item.knownMeaningsSnapshot,
            alreadyExists = item.alreadyExists,
            occurrencesJson = item.occurrencesJson,
            createdCardId = item.createdCardSyncId?.let(cardIds::get),
            targetDeckId = item.targetDeckSyncId?.let(deckIds::get),
            firstOccurrenceOrder = item.firstOccurrenceOrder,
            isEdited = item.isEdited,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
            id = previousItems[item.syncId]?.id ?: 0,
            syncId = item.syncId,
        )
    })
}
