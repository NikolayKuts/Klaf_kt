package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.Deck
import com.kuts.klaf.room.converters.RoomDateConverter
import com.kuts.klaf.room.dao.RoomCardLocalUpdate
import com.kuts.klaf.room.dao.RoomDeckLocalUpdate
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.withSyncReviewSummary
import com.kuts.klaf.server.contract.SyncCard
import com.kuts.klaf.server.contract.SyncBootstrapResponse
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncDeck
import com.kuts.klaf.server.contract.SyncDelta
import com.kuts.klaf.server.contract.SyncResponse
import com.kuts.klaf.server.contract.accountInterimDeckSyncId
import kotlinx.serialization.json.Json

/** Applies a response only when every submitted operation was resolved without conflicts. */
class RoomSyncDeltaApplier(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val outbox: RoomSyncOutbox,
) {

    private val converter = RoomDateConverter()

    suspend fun applyInitialSnapshot(
        accountId: String,
        snapshot: SyncBootstrapResponse,
        acceptedInterimOperationIds: List<String> = emptyList(),
    ) {
        require(snapshot.revision >= 0L) { "Bootstrap revision cannot be negative" }
        outbox.applyAccepted(accountId, acceptedInterimOperationIds, snapshot.revision) {
            val database = databaseSource.current()
            val localDecks = database.deckDao().getAllDecks()
            val interimSyncId = accountInterimDeckSyncId(accountId)
            val onlyEmptyInterim = localDecks.size == 1 &&
                localDecks.single().id == Deck.INTERIM_DECK_ID &&
                localDecks.single().syncId == interimSyncId &&
                localDecks.single().cardQuantity == 0
            check(localDecks.isEmpty() || onlyEmptyInterim) {
                "Initial snapshot requires an empty account or only its empty interim deck"
            }
            check(database.cardDao().getAllCards().isEmpty()) {
                "Initial snapshot requires an empty account database"
            }
            val pending = database.pendingSyncOperationDao().pendingForAccount(accountId)
            check(pending.map { it.operationId }.toSet() == acceptedInterimOperationIds.toSet()) {
                "Initial snapshot cannot overwrite other pending local changes"
            }
            check(pending.isEmpty() || onlyEmptyInterim && pending.size == 1 &&
                (Json.decodeFromString<SyncOperation>(pending.single().operationJson) as? SyncOperation.AddDeck)
                    ?.deck?.syncId == interimSyncId
            ) { "Initial snapshot cannot overwrite a local edit" }
            if (onlyEmptyInterim && snapshot.decks.none { it.syncId == interimSyncId }) {
                database.deckDao().deleteDeck(Deck.INTERIM_DECK_ID)
            }
            applyRows(database, SyncDelta(
                fromRevision = 0L,
                toRevision = snapshot.revision,
                decks = snapshot.decks,
                cards = snapshot.cards,
                deletedDeckSyncIds = emptyList(),
                deletedCardSyncIds = emptyList(),
                history = emptyList(),
            ))
        }
    }

    suspend fun applyConflictFree(accountId: String, response: SyncResponse) {
        require(response.conflicts.isEmpty()) { "Conflict resolution is required before applying the delta" }
        require(response.delta.toRevision == response.revision) { "Response and delta revisions differ" }
        require(response.delta.fromRevision <= response.revision) { "Delta revision moves backwards" }

        outbox.applyAccepted(accountId, response.acceptedOperationIds.toList(), response.revision) {
            val database = databaseSource.current()
            val checkpoint = database.syncCheckpointDao().current()?.confirmedRevision ?: 0L
            require(response.delta.fromRevision == checkpoint) { "Delta is based on a stale checkpoint" }
            val pendingIds = database.pendingSyncOperationDao().pendingForAccount(accountId)
                .map { it.operationId }.toSet()
            require(pendingIds == response.acceptedOperationIds) {
                "Pending operations changed while the server request was in flight"
            }
            applyRows(database, response.delta)
            database.syncConflictSnapshotDao().clear()
        }
    }

    internal suspend fun applyRows(database: KlafRoomDatabase, delta: SyncDelta) {
        val deckIds = delta.decks.map(SyncDeck::syncId)
        val cardIds = delta.cards.map(SyncCard::syncId)
        require(deckIds.all(String::isNotBlank) && deckIds.distinct().size == deckIds.size) {
            "Delta contains duplicate or blank deck sync IDs"
        }
        require(cardIds.all(String::isNotBlank) && cardIds.distinct().size == cardIds.size) {
            "Delta contains duplicate or blank card sync IDs"
        }
        require(deckIds.none(delta.deletedDeckSyncIds::contains)) { "Delta both updates and deletes a deck" }
        require(cardIds.none(delta.deletedCardSyncIds::contains)) { "Delta both updates and deletes a card" }
        require(delta.cards.none { it.deckSyncId in delta.deletedDeckSyncIds }) {
            "Delta retains a card in a deleted deck"
        }

        val decks = database.deckDao()
        val cards = database.cardDao()
        delta.deletedCardSyncIds.forEach { syncId ->
            cards.getCardBySyncId(syncId)?.let { cards.deleteCard(it.id) }
        }
        delta.decks.forEach { incoming ->
            val existing = decks.getDeckBySyncId(incoming.syncId)
            val accountEmail = databaseSource.selection.value.accountEmail
            val localId = existing?.id ?: if (
                accountEmail != null && incoming.syncId == accountInterimDeckSyncId(accountEmail)
            ) Deck.INTERIM_DECK_ID else 0
            val local = incoming.toRoomDeck(localId)
            if (existing == null) {
                decks.insertNewDeck(local)
            } else {
                check(decks.updateDeckRow(RoomDeckLocalUpdate(local)) == 1)
                check(decks.updateServerRevision(existing.id, existing.syncId, incoming.lastChangedServerRevision) == 1)
            }
        }
        delta.cards.forEach { incoming ->
            val deck = requireNotNull(decks.getDeckBySyncId(incoming.deckSyncId)) {
                "Server card ${incoming.syncId} refers to a missing deck ${incoming.deckSyncId}"
            }
            val existing = cards.getCardBySyncId(incoming.syncId)
            val local = incoming.toRoomCard(deck.id, existing?.id ?: 0)
            if (existing == null) {
                cards.insertNewCard(local)
            } else {
                check(cards.updateCardRow(RoomCardLocalUpdate(local)) == 1)
                check(cards.updateServerRevision(existing.id, existing.syncId, incoming.lastChangedServerRevision) == 1)
            }
        }
        // Move surviving cards before cascading deletion of their previous parent.
        delta.deletedDeckSyncIds.forEach { syncId ->
            decks.getDeckBySyncId(syncId)?.let { decks.deleteDeck(it.id) }
        }
        delta.decks.forEach { incoming ->
            val deck = requireNotNull(decks.getDeckBySyncId(incoming.syncId))
            check(cards.getCardQuantityInDeck(deck.id) == incoming.cardQuantity) {
                "Server card count does not match deck ${incoming.syncId}"
            }
        }
    }

    private fun SyncDeck.toRoomDeck(localId: Int): RoomDeck = RoomDeck(
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
        id = localId,
        syncId = syncId,
        lastChangedServerRevision = lastChangedServerRevision,
    ).withSyncReviewSummary(reviewSummary)

    private fun SyncCard.toRoomCard(deckId: Int, localId: Int): RoomCard = RoomCard(
        deckId = deckId,
        nativeWord = nativeWord,
        foreignWord = foreignWord,
        ipa = ipaJson,
        wordMeaningInsights = converter.fromStringToWordMeaningInsightsStrict(wordMeaningInsightsJson),
        mnemonicJson = mnemonicJson,
        id = localId,
        syncId = syncId,
        lastChangedServerRevision = lastChangedServerRevision,
    )
}
