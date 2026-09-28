package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.Deck
import com.kuts.klaf.room.converters.RoomDateConverter
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.performInTransaction
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomPendingSyncOperation
import com.kuts.klaf.room.toSyncReviewSummary
import com.kuts.klaf.server.contract.accountInterimDeckSyncId
import com.kuts.klaf.server.contract.SyncCard
import com.kuts.klaf.server.contract.SyncDeck
import com.kuts.klaf.server.contract.SyncOperation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val roomConverter = RoomDateConverter()

internal fun RoomDeck.toInitialSyncDeck(): SyncDeck = SyncDeck(
    syncId = syncId,
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
    reviewSummary = toSyncReviewSummary(),
)

internal fun RoomCard.toInitialSyncCard(deckSyncId: String): SyncCard = SyncCard(
    syncId = syncId,
    deckSyncId = deckSyncId,
    nativeWord = nativeWord,
    foreignWord = foreignWord,
    ipaJson = ipa,
    wordMeaningInsightsJson = roomConverter.fromWordMeaningInsightsToString(wordMeaningInsights),
    mnemonicJson = mnemonicJson,
)

private fun initialUploadRows(
    email: String,
    decks: List<RoomDeck>,
    cards: List<RoomCard>,
): List<RoomPendingSyncOperation> {
    val deckSyncIds = decks.associate { it.id to it.syncId }
    check(decks.all { it.id != 0 && it.syncId.isNotBlank() && it.lastChangedServerRevision == 0L })
    check(cards.all { it.id != 0 && it.syncId.isNotBlank() && it.lastChangedServerRevision == 0L &&
        deckSyncIds.containsKey(it.deckId) })
    val operations = decks.sortedBy(RoomDeck::id).map { deck ->
        SyncOperation.AddDeck("guest-deck-${deck.syncId}", deck.toInitialSyncDeck())
    } + cards.sortedBy(RoomCard::id).map { card ->
        SyncOperation.AddCard(
            "guest-card-${card.syncId}",
            card.toInitialSyncCard(requireNotNull(deckSyncIds[card.deckId])),
        )
    }
    return operations.map { operation ->
        RoomPendingSyncOperation(
            accountId = email.trim().lowercase(),
            operationId = operation.operationId,
            baseRevision = 0L,
            operationJson = Json.encodeToString<SyncOperation>(operation),
        )
    }
}

/** Copies guest content to a newly created account before removing its guest copy. */
class GuestAccountDataTransfer(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val afterAccountCopy: suspend () -> Unit = {},
) {

    suspend fun transfer(email: String) {
        databaseSource.withGuestAndAccount(email) { guest, account ->
            val decks = guest.deckDao().getAllDecks()
            val cards = guest.cardDao().getAllCards()
            val sources = guest.vocabularySourceDao().getSources()
            val items = guest.vocabularySourceItemDao().getItems()
            val ignoredWords = guest.ignoredVocabularyWordDao().getWords()
            check(items.all { item -> sources.any { it.id == item.sourceId } }) {
                "Guest vocabulary item has no source"
            }
            val accountDecks = decks.map { deck ->
                if (deck.id == Deck.INTERIM_DECK_ID) {
                    deck.copy(syncId = accountInterimDeckSyncId(email))
                } else deck
            }
            if (decks.isEmpty() && cards.isEmpty() && sources.isEmpty() && items.isEmpty() && ignoredWords.isEmpty()) {
                account.performInTransaction {
                    val existingDecks = account.deckDao().getAllDecks()
                    val existingCards = account.cardDao().getAllCards()
                    val existingPending = account.pendingSyncOperationDao().allPending()
                    if (existingDecks.isNotEmpty() || existingCards.isNotEmpty() || existingPending.isNotEmpty()) {
                        val expected = initialUploadRows(email, existingDecks, existingCards)
                        check(expected.isNotEmpty() && existingPending.map { it.copy(id = 0L) } == expected) {
                            "Account database is not a verified completed guest transfer"
                        }
                    }
                }
                return@withGuestAndAccount
            }
            val pending = initialUploadRows(email, accountDecks, cards)

            account.performInTransaction {
                val existingDecks = account.deckDao().getAllDecks()
                val existingCards = account.cardDao().getAllCards()
                val existingPending = account.pendingSyncOperationDao().allPending()
                val existingSources = account.vocabularySourceDao().getSources()
                val existingItems = account.vocabularySourceItemDao().getItems()
                val existingIgnoredWords = account.ignoredVocabularyWordDao().getWords()
                if (existingDecks.isEmpty() && existingCards.isEmpty() && existingPending.isEmpty() &&
                    existingSources.isEmpty() && existingItems.isEmpty() && existingIgnoredWords.isEmpty()) {
                    accountDecks.forEach { account.deckDao().insertNewDeck(it) }
                    cards.forEach { account.cardDao().insertNewCard(it) }
                    pending.forEach { account.pendingSyncOperationDao().insert(it) }
                    sources.forEach { account.vocabularySourceDao().insertSource(it) }
                    account.vocabularySourceItemDao().insertItems(items)
                    account.ignoredVocabularyWordDao().insertWords(ignoredWords)
                } else {
                    check(existingDecks.toSet() == accountDecks.toSet() && existingCards.toSet() == cards.toSet()) {
                        "Account database is not an exact copy of the guest data"
                    }
                    check(existingPending.map { it.copy(id = 0L) } == pending) {
                        "Account initial operations do not match the guest data"
                    }
                }
                check(account.vocabularySourceDao().getSources().toSet() == sources.toSet() &&
                    account.vocabularySourceItemDao().getItems().toSet() == items.toSet() &&
                    account.ignoredVocabularyWordDao().getWords().toSet() == ignoredWords.toSet()) {
                    "Account vocabulary data is not an exact copy of the guest data"
                }
            }

            afterAccountCopy()

            guest.performInTransaction {
                check(guest.deckDao().getAllDecks().toSet() == decks.toSet())
                check(guest.cardDao().getAllCards().toSet() == cards.toSet())
                check(guest.vocabularySourceDao().getSources().toSet() == sources.toSet())
                check(guest.vocabularySourceItemDao().getItems().toSet() == items.toSet())
                check(guest.ignoredVocabularyWordDao().getWords().toSet() == ignoredWords.toSet())
                sources.forEach {
                    guest.vocabularySourceItemDao().deleteItemsBySourceId(it.id)
                    guest.vocabularySourceDao().deleteSource(it.id)
                }
                guest.ignoredVocabularyWordDao().deleteWords()
                decks.forEach { guest.deckDao().deleteDeck(it.id) }
            }
        }
    }
}
