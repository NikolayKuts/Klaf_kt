package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.domain.common.ConflictResolutionDecision
import com.kuts.domain.entities.Deck
import com.kuts.klaf.room.converters.RoomDateConverter
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomPendingSyncOperation
import com.kuts.klaf.room.newSyncId
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Applies explicit choices to a saved conflict in one Room transaction. */
class RoomSyncConflictResolver(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val outbox: RoomSyncOutbox,
    private val applier: RoomSyncDeltaApplier,
) {

    private val json = Json { classDiscriminator = "type" }
    private val converter = RoomDateConverter()

    suspend fun acceptServerForAll(accountId: String): Long = resolve(accountId) { _, _ -> }

    suspend fun resolveSelected(accountId: String, decisions: List<ConflictResolutionDecision>): Long = resolve(
        accountId = accountId,
        beforeServerDelta = { _, response -> validateSelectedDecisions(response, decisions) },
    ) { database, response ->
        val decisionsById = decisions.associateBy(ConflictResolutionDecision::operationId)
        response.conflicts.forEach { conflict ->
            when (decisionsById.getValue(conflict.operationId).action) {
                ConflictResolutionAction.ACCEPT_SERVER -> Unit
                ConflictResolutionAction.KEEP_LOCAL_DECK -> rebaseDeckEdit(
                    database, accountId, response.revision,
                    conflict.localOperation as SyncOperation.EditDeck,
                )
                ConflictResolutionAction.KEEP_LOCAL_CARD -> rebaseCardEdit(
                    database, accountId, response.revision,
                    conflict.localOperation as SyncOperation.EditCard,
                )
                else -> error("Structural manual choice is not supported yet")
            }
        }
    }

    suspend fun keepLocalDeckEdits(accountId: String): Long = resolve(accountId) { database, response ->
        val edits = response.conflicts.map { conflict ->
            requireNotNull(conflict.localOperation as? SyncOperation.EditDeck) {
                "Keep-local rebase is not available for this conflict type"
            }
        }
        edits.forEach { edit -> rebaseDeckEdit(database, accountId, response.revision, edit) }
    }

    suspend fun keepLocalCardEdits(accountId: String): Long = resolve(accountId) { database, response ->
        val edits = response.conflicts.map { conflict ->
            requireNotNull(conflict.localOperation as? SyncOperation.EditCard) {
                "Keep-local card rebase is not available for this conflict type"
            }
        }
        edits.forEach { edit -> rebaseCardEdit(database, accountId, response.revision, edit) }
    }

    suspend fun rescueMovedCardsAfterSourceDeletion(accountId: String): Long {
        var savedMoves = emptyList<Pair<SyncOperation.MoveCard, RoomCard>>()
        return resolve(
            accountId = accountId,
            beforeServerDelta = { database, response ->
                savedMoves = response.conflicts.map { conflict ->
                    val move = requireNotNull(conflict.localOperation as? SyncOperation.MoveCard) {
                        "Moved-card rescue is not available for this conflict type"
                    }
                    require(move.sourceDeckSyncId in response.delta.deletedDeckSyncIds) {
                        "Source deck was not deleted"
                    }
                    val card = requireNotNull(database.cardDao().getCardBySyncId(move.cardSyncId)) {
                        "The moved card is missing locally"
                    }
                    val destination = requireNotNull(database.deckDao().getDeckBySyncId(move.targetDeckSyncId)) {
                        "The destination deck is missing locally"
                    }
                    require(card.deckId == destination.id) { "The card is no longer in the selected destination" }
                    move to card
                }
            },
        ) { database, response ->
            savedMoves.forEach { (move, card) ->
                val destination = requireNotNull(database.deckDao().getDeckBySyncId(move.targetDeckSyncId)) {
                    "The destination deck was deleted on the server"
                }
                require(destination.repetitionQuantity == 0) { "The destination deck has already been reviewed" }
                require(database.cardDao().getCardBySyncId(card.syncId) == null) {
                    "The card still exists on the server"
                }
                database.cardDao().insertNewCard(card.copy(deckId = destination.id))
                database.deckDao().insertDeck(destination.copy(
                    cardQuantity = database.cardDao().getCardQuantityInDeck(destination.id),
                ))
                val rescue = SyncOperation.AddCard(newSyncId(), card.toInitialSyncCard(destination.syncId))
                database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                    accountId = accountId,
                    operationId = rescue.operationId,
                    baseRevision = response.revision,
                    operationJson = json.encodeToString<SyncOperation>(rescue),
                ))
            }
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun keepCardRemovalAfterReview(accountId: String, makeDueNow: Boolean): Long {
        var removals = emptyList<Pair<SyncOperation.DeleteCard, String>>()
        return resolve(
            accountId = accountId,
            beforeServerDelta = { _, response ->
                removals = response.conflicts.map { conflict ->
                    val removal = requireNotNull(conflict.localOperation as? SyncOperation.DeleteCard) {
                        "Card-removal choice is not available for this conflict type"
                    }
                    val serverCard = requireNotNull(conflict.serverCard) { "Server card is missing" }
                    val serverDeck = requireNotNull(conflict.serverDeck) { "Reviewed deck is missing" }
                    require(conflict.reason == "REVIEW_MEMBERSHIP_CHANGE" &&
                        serverCard.syncId == removal.cardSyncId &&
                        serverDeck.syncId == serverCard.deckSyncId &&
                        serverDeck.reviewCount > 0
                    ) { "The server did not complete a review of the card's deck" }
                    removal to serverDeck.syncId
                }
                require(removals.map { it.first.cardSyncId }.distinct().size == removals.size) {
                    "One card has multiple pending removals"
                }
            },
        ) { database, response ->
            removals.forEach { (removal, deckSyncId) ->
                val card = requireNotNull(database.cardDao().getCardBySyncId(removal.cardSyncId)) {
                    "Server card disappeared before removal was rebased"
                }
                val deck = requireNotNull(database.deckDao().getDeckBySyncId(deckSyncId)) {
                    "Reviewed deck disappeared before removal was rebased"
                }
                require(card.deckId == deck.id) { "Card moved out of the reviewed deck" }
                database.cardDao().deleteCard(card.id)
                database.deckDao().insertDeck(deck.copy(
                    cardQuantity = database.cardDao().getCardQuantityInDeck(deck.id),
                ))
                val rebased = removal.copy(operationId = newSyncId())
                database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                    accountId = accountId,
                    operationId = rebased.operationId,
                    baseRevision = response.revision,
                    operationJson = json.encodeToString<SyncOperation>(rebased),
                ))
            }
            if (makeDueNow) {
                removals.map { it.second }.distinct().forEach { deckSyncId ->
                    val deck = requireNotNull(database.deckDao().getDeckBySyncId(deckSyncId))
                    require(deck.repetitionQuantity > 0 && deck.scheduledIterationDates.isNotEmpty()) {
                        "Reviewed deck has no current schedule"
                    }
                    database.deckDao().insertDeck(deck.copy(
                        scheduledIterationDates = deck.scheduledIterationDates.dropLast(1) +
                            kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    ))
                    val dueNow = SyncOperation.MakeDeckDueNow(newSyncId(), deckSyncId)
                    database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                        accountId = accountId,
                        operationId = dueNow.operationId,
                        baseRevision = response.revision,
                        operationJson = json.encodeToString<SyncOperation>(dueNow),
                    ))
                }
            }
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun retargetMoveIntoUnreviewedDeck(
        accountId: String,
        targetDeckSyncId: String?,
        newDeckName: String?,
    ): Long {
        require((targetDeckSyncId != null) != (newDeckName != null)) {
            "Choose one existing destination or a new deck name"
        }
        val normalizedName = newDeckName?.trim()
        require(targetDeckSyncId == null || targetDeckSyncId.isNotBlank()) { "Destination ID is blank" }
        require(normalizedName == null || normalizedName.length in 1..Deck.MAX_NAME_LENGTH) {
            "New deck name is invalid"
        }
        var savedMove: SyncOperation.MoveCard? = null
        return resolve(
            accountId = accountId,
            beforeServerDelta = { database, response ->
                val conflict = response.conflicts.single()
                val move = requireNotNull(conflict.localOperation as? SyncOperation.MoveCard) {
                    "Retargeting is available only for a moved card"
                }
                val serverCard = requireNotNull(conflict.serverCard) { "Server card is missing" }
                val reviewed = requireNotNull(conflict.serverDeck) { "Reviewed destination is missing" }
                require(conflict.reason == "REVIEWED_DECK" &&
                    reviewed.syncId == move.targetDeckSyncId && reviewed.reviewCount > 0 &&
                    serverCard.syncId == move.cardSyncId && serverCard.deckSyncId == move.sourceDeckSyncId
                ) { "The move did not conflict with a newly reviewed destination" }
                requireNotNull(move.movedCardReviewDuration) {
                    "Original card duration is unavailable for a lossless retarget"
                }
                val source = requireNotNull(database.deckDao().getDeckBySyncId(move.sourceDeckSyncId)) {
                    "Source deck is missing locally"
                }
                val oldTarget = requireNotNull(database.deckDao().getDeckBySyncId(move.targetDeckSyncId)) {
                    "Original destination is missing locally"
                }
                val card = requireNotNull(database.cardDao().getCardBySyncId(move.cardSyncId)) {
                    "Moved card is missing locally"
                }
                require(card.deckId == oldTarget.id) { "Card no longer belongs to the conflicted destination" }
                if (targetDeckSyncId != null) {
                    require(targetDeckSyncId != source.syncId && targetDeckSyncId != oldTarget.syncId) {
                        "Choose a different destination"
                    }
                    val chosen = requireNotNull(database.deckDao().getDeckBySyncId(targetDeckSyncId)) {
                        "Chosen destination is missing locally"
                    }
                    require(chosen.repetitionQuantity == 0) { "Chosen destination has already been reviewed" }
                }
                database.cardDao().insetCard(card.copy(deckId = source.id))
                database.deckDao().insertDeck(source.copy(
                    cardQuantity = database.cardDao().getCardQuantityInDeck(source.id),
                ))
                database.deckDao().insertDeck(oldTarget.copy(
                    cardQuantity = database.cardDao().getCardQuantityInDeck(oldTarget.id),
                ))
                savedMove = move
            },
        ) { database, response ->
            val move = requireNotNull(savedMove)
            val source = requireNotNull(database.deckDao().getDeckBySyncId(move.sourceDeckSyncId)) {
                "Source deck was removed by the server"
            }
            val card = requireNotNull(database.cardDao().getCardBySyncId(move.cardSyncId)) {
                "Card was removed by the server"
            }
            require(card.deckId == source.id) { "Server moved the card to another source" }
            val target = if (targetDeckSyncId != null) {
                requireNotNull(database.deckDao().getDeckBySyncId(targetDeckSyncId)) {
                    "Chosen destination was removed by the server"
                }
            } else {
                val created = RoomDeck(
                    name = requireNotNull(normalizedName),
                    creationDate = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    repetitionIterationDates = emptyList(),
                    scheduledIterationDates = emptyList(),
                    scheduledDateInterval = 0L,
                    repetitionQuantity = 0,
                    cardQuantity = 0,
                    lastFirstRepetitionDuration = 0L,
                    lastSecondRepetitionDuration = 0L,
                    lastRepetitionIterationDuration = 0L,
                    isLastIterationSucceeded = true,
                )
                val id = database.deckDao().insertNewDeck(created).toInt()
                val newDeck = created.copy(id = id)
                val addDeck = SyncOperation.AddDeck(newSyncId(), newDeck.toInitialSyncDeck())
                database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                    accountId = accountId,
                    operationId = addDeck.operationId,
                    baseRevision = response.revision,
                    operationJson = json.encodeToString<SyncOperation>(addDeck),
                ))
                newDeck
            }
            require(target.syncId != source.syncId && target.syncId != move.targetDeckSyncId &&
                target.repetitionQuantity == 0
            ) { "Chosen destination is not available for a card move" }
            val movedDuration = requireNotNull(move.movedCardReviewDuration)
            val targetDuration = if (target.cardQuantity > 0) {
                target.lastRepetitionIterationDuration + target.lastRepetitionIterationDuration / target.cardQuantity
            } else movedDuration
            database.cardDao().insetCard(card.copy(deckId = target.id))
            database.deckDao().insertDeck(source.copy(
                cardQuantity = database.cardDao().getCardQuantityInDeck(source.id),
                lastRepetitionIterationDuration = move.sourceLastReviewPassDuration
                    ?: source.lastRepetitionIterationDuration,
            ))
            database.deckDao().insertDeck(target.copy(
                cardQuantity = database.cardDao().getCardQuantityInDeck(target.id),
                lastRepetitionIterationDuration = targetDuration,
            ))
            val rebased = move.copy(
                operationId = newSyncId(),
                targetDeckSyncId = target.syncId,
                targetLastReviewPassDuration = targetDuration,
            )
            database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                accountId = accountId,
                operationId = rebased.operationId,
                baseRevision = response.revision,
                operationJson = json.encodeToString<SyncOperation>(rebased),
            ))
        }
    }

    suspend fun restoreDeletedDeckWithLocalCardEdits(accountId: String): Long {
        var savedDeck: RoomDeck? = null
        var savedCards = emptyList<RoomCard>()
        return resolve(
            accountId = accountId,
            beforeServerDelta = { database, response ->
                val edits = response.conflicts.map { conflict ->
                    requireNotNull(conflict.localOperation as? SyncOperation.EditCard) {
                        "Deck restoration is not available for this conflict type"
                    }
                }
                val deckSyncId = edits.map { it.card.deckSyncId }.distinct().single()
                require(deckSyncId in response.delta.deletedDeckSyncIds) { "The deck was not deleted" }
                val deck = requireNotNull(database.deckDao().getDeckBySyncId(deckSyncId)) {
                    "The local deck is missing"
                }
                val cards = database.cardDao().getAllCards().filter { it.deckId == deck.id }
                require(cards.size == deck.cardQuantity && edits.all { edit ->
                    edit.card.syncId == edit.cardSyncId && cards.any { it.syncId == edit.cardSyncId }
                }) { "The local deck is incomplete" }
                savedDeck = deck
                savedCards = cards
            },
        ) { database, response ->
            val deck = requireNotNull(savedDeck)
            require(database.deckDao().getDeckBySyncId(deck.syncId) == null) {
                "The deck still exists on the server"
            }
            database.deckDao().insertNewDeck(deck.copy(cardQuantity = 0, lastChangedServerRevision = 0L))
            savedCards.forEach { card ->
                require(database.cardDao().getCardBySyncId(card.syncId) == null) {
                    "A child card still exists on the server"
                }
                database.cardDao().insertNewCard(card.copy(lastChangedServerRevision = 0L))
            }
            database.deckDao().insertDeck(deck.copy(lastChangedServerRevision = 0L))
            val addDeck = SyncOperation.AddDeck(newSyncId(), deck.toInitialSyncDeck())
            database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                accountId = accountId,
                operationId = addDeck.operationId,
                baseRevision = response.revision,
                operationJson = json.encodeToString<SyncOperation>(addDeck),
            ))
            savedCards.forEach { card ->
                val addCard = SyncOperation.AddCard(newSyncId(), card.toInitialSyncCard(deck.syncId))
                database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                    accountId = accountId,
                    operationId = addCard.operationId,
                    baseRevision = response.revision,
                    operationJson = json.encodeToString<SyncOperation>(addCard),
                ))
            }
        }
    }

    private fun validateSelectedDecisions(response: SyncResponse, decisions: List<ConflictResolutionDecision>) {
        val conflictIds = response.conflicts.map { it.operationId }
        val decisionIds = decisions.map { it.operationId }
        require(decisionIds.size == decisionIds.distinct().size && decisionIds.toSet() == conflictIds.toSet()) {
            "Exactly one decision is required for every saved conflict"
        }
        val targets = response.conflicts.map { conflict ->
            when (val operation = conflict.localOperation) {
                is SyncOperation.EditDeck -> "deck:${operation.deckSyncId}"
                is SyncOperation.EditCard -> "card:${operation.cardSyncId}"
                else -> error("Structural per-conflict resolution is not supported")
            }
        }
        require(targets.size == targets.distinct().size) { "Multiple edits to one item need a bulk resolution" }
        val decisionsById = decisions.associateBy { it.operationId }
        response.conflicts.forEach { conflict ->
            when (decisionsById.getValue(conflict.operationId).action) {
                ConflictResolutionAction.ACCEPT_SERVER -> Unit
                ConflictResolutionAction.KEEP_LOCAL_DECK -> {
                    val edit = requireNotNull(conflict.localOperation as? SyncOperation.EditDeck)
                    require(conflict.serverDeck?.syncId == edit.deckSyncId &&
                        edit.deckSyncId !in response.delta.deletedDeckSyncIds && edit.name.isNotBlank()
                    ) { "Deck cannot be rebased onto this server result" }
                }
                ConflictResolutionAction.KEEP_LOCAL_CARD -> {
                    val edit = requireNotNull(conflict.localOperation as? SyncOperation.EditCard)
                    require(conflict.serverCard?.syncId == edit.cardSyncId &&
                        conflict.serverCard?.deckSyncId == edit.card.deckSyncId &&
                        edit.card.syncId == edit.cardSyncId &&
                        edit.cardSyncId !in response.delta.deletedCardSyncIds &&
                        edit.card.deckSyncId !in response.delta.deletedDeckSyncIds
                    ) { "Card cannot be rebased onto this server result" }
                }
                else -> error("Structural manual choice is not supported yet")
            }
        }
    }

    private suspend fun rebaseDeckEdit(
        database: KlafRoomDatabase,
        accountId: String,
        revision: Long,
        edit: SyncOperation.EditDeck,
    ) {
        val deck = requireNotNull(database.deckDao().getDeckBySyncId(edit.deckSyncId)) {
            "Deck to keep is missing"
        }
        require(edit.name.isNotBlank()) { "Deck name is blank" }
        database.deckDao().insertDeck(deck.copy(name = edit.name))
        val rebased = edit.copy(operationId = newSyncId())
        database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
            accountId = accountId,
            operationId = rebased.operationId,
            baseRevision = revision,
            operationJson = json.encodeToString<SyncOperation>(rebased),
        ))
    }

    private suspend fun rebaseCardEdit(
        database: KlafRoomDatabase,
        accountId: String,
        revision: Long,
        edit: SyncOperation.EditCard,
    ) {
        require(edit.card.syncId == edit.cardSyncId) { "Edited card ID differs from its payload" }
        val card = requireNotNull(database.cardDao().getCardBySyncId(edit.cardSyncId)) {
            "Card to keep is missing"
        }
        val deck = requireNotNull(database.deckDao().getDeckBySyncId(edit.card.deckSyncId)) {
            "Edited card deck is missing"
        }
        require(card.deckId == deck.id) { "Card moved to another deck" }
        database.cardDao().insetCard(card.copy(
            nativeWord = edit.card.nativeWord,
            foreignWord = edit.card.foreignWord,
            ipa = edit.card.ipaJson,
            wordMeaningInsights = converter.fromStringToWordMeaningInsightsStrict(edit.card.wordMeaningInsightsJson),
            mnemonicJson = edit.card.mnemonicJson,
        ))
        val rebased = edit.copy(operationId = newSyncId(), card = edit.card.copy(lastChangedServerRevision = revision))
        database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
            accountId = accountId,
            operationId = rebased.operationId,
            baseRevision = revision,
            operationJson = json.encodeToString<SyncOperation>(rebased),
        ))
    }

    private suspend fun resolve(
        accountId: String,
        beforeServerDelta: suspend (KlafRoomDatabase, SyncResponse) -> Unit = { _, _ -> },
        afterServerDelta: suspend (KlafRoomDatabase, SyncResponse) -> Unit,
    ): Long {
        val selected = databaseSource.selection.value
        require(selected.accountEmail != null && selected.accountEmail == accountId) {
            "Conflict account does not match the selected database"
        }
        val snapshot = requireNotNull(selected.database.syncConflictSnapshotDao().current()) {
            "No saved conflict exists for this account"
        }
        require(snapshot.accountId == accountId) { "Conflict belongs to another account" }
        val response = json.decodeFromString<SyncResponse>(snapshot.responseJson)
        require(response.conflicts.isNotEmpty()) { "Saved response has no conflicts" }
        val operationIds = response.acceptedOperationIds + response.conflicts.map { it.operationId }

        outbox.applyAccepted(accountId, operationIds.toList(), response.revision) {
            val database = databaseSource.current()
            val stored = requireNotNull(database.syncConflictSnapshotDao().current()) {
                "Saved conflict disappeared before resolution"
            }
            require(stored == snapshot) { "Saved conflict changed before resolution" }
            val checkpoint = database.syncCheckpointDao().current()?.confirmedRevision ?: 0L
            require(response.delta.fromRevision == snapshot.baseRevision &&
                (checkpoint == snapshot.baseRevision || checkpoint == response.revision)) {
                "Saved conflict is based on another checkpoint"
            }
            require(response.delta.toRevision == response.revision) { "Response and delta revisions differ" }
            val pending = database.pendingSyncOperationDao().pendingForAccount(accountId)
            val expectedPendingIds = if (checkpoint == snapshot.baseRevision) {
                operationIds
            } else {
                response.conflicts.map { it.operationId }.toSet()
            }
            require(pending.map { it.operationId }.toSet() == expectedPendingIds) {
                "Pending operations changed after the conflict response"
            }
            require(pending.all { it.baseRevision == snapshot.baseRevision }) {
                "Pending operation has a different base revision"
            }
            val pendingOperations = pending.associate { row ->
                row.operationId to json.decodeFromString<SyncOperation>(row.operationJson)
            }
            require(response.conflicts.all { conflict ->
                pendingOperations[conflict.operationId] == conflict.localOperation
            }) { "Saved conflict no longer matches the local operation" }
            beforeServerDelta(database, response)
            applier.applyRows(database, response.delta)
            afterServerDelta(database, response)
            database.syncConflictSnapshotDao().clear()
        }
        return response.revision
    }
}
