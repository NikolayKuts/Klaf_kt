package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.newSyncId
import com.kuts.klaf.room.toSyncReviewSummary
import com.kuts.klaf.server.contract.SyncOperation

/** Derives account operations from one committed user transaction, not individual DAO writes. */
internal fun planSyncOperations(
    beforeDecks: List<RoomDeck>,
    beforeCards: List<RoomCard>,
    afterDecks: List<RoomDeck>,
    afterCards: List<RoomCard>,
): List<SyncOperation> {
    val oldDecks = beforeDecks.associateBy(RoomDeck::id)
    val newDecks = afterDecks.associateBy(RoomDeck::id)
    val oldCards = beforeCards.associateBy(RoomCard::id)
    val newCards = afterCards.associateBy(RoomCard::id)
    val deletedDeckIds = oldDecks.keys - newDecks.keys
    val movedDeckIds = afterCards.mapNotNull { card ->
        val old = oldCards[card.id] ?: return@mapNotNull null
        if (old.deckId == card.deckId) null else listOf(old.deckId, card.deckId)
    }.flatten().toSet()
    val operations = mutableListOf<SyncOperation>()

    afterDecks.filter { it.id !in oldDecks }.forEach { deck ->
        operations += SyncOperation.AddDeck(newSyncId(), deck.toInitialSyncDeck())
    }
    afterDecks.forEach { deck ->
        val old = oldDecks[deck.id] ?: return@forEach
        require(old.syncId == deck.syncId && old.creationDate == deck.creationDate) {
            "Deck identity and creation date cannot change"
        }
        if (old.name != deck.name) {
            operations += SyncOperation.EditDeck(newSyncId(), deck.syncId, deck.name)
        }
        val reviewChanged = old.copy(
            name = deck.name,
            cardQuantity = deck.cardQuantity,
            lastRepetitionIterationDuration = if (deck.id in movedDeckIds) {
                deck.lastRepetitionIterationDuration
            } else old.lastRepetitionIterationDuration,
        ) !=
            deck.copy(lastChangedServerRevision = old.lastChangedServerRevision)
        if (reviewChanged) {
            require(deck.repetitionQuantity > old.repetitionQuantity) {
                "A deck review-field change requires a review operation"
            }
            operations += SyncOperation.FinishReview(
                operationId = newSyncId(),
                deckSyncId = deck.syncId,
                reviewCount = deck.repetitionQuantity,
                lastReviewAt = deck.repetitionIterationDates.lastOrNull() ?: 0L,
                nextReviewAt = deck.scheduledIterationDates.lastOrNull() ?: 0L,
                scheduledDateInterval = deck.scheduledDateInterval,
                lastFirstReviewDuration = deck.lastFirstRepetitionDuration,
                lastSecondReviewDuration = deck.lastSecondRepetitionDuration,
                lastReviewPassDuration = deck.lastRepetitionIterationDuration,
                isLastPassSucceeded = deck.isLastIterationSucceeded,
                reviewSummary = deck.toSyncReviewSummary(),
                reviewPassDates = deck.repetitionIterationDates,
                scheduledReviewDates = deck.scheduledIterationDates,
            )
        }
    }
    beforeCards.filter { it.id !in newCards && it.deckId !in deletedDeckIds }.forEach { card ->
        operations += SyncOperation.DeleteCard(newSyncId(), card.syncId)
    }
    afterCards.forEach { card ->
        val old = oldCards[card.id] ?: return@forEach
        require(old.syncId == card.syncId) { "Card sync ID cannot change" }
        if (old.deckId != card.deckId) {
            val source = requireNotNull(oldDecks[old.deckId])
            val target = requireNotNull(newDecks[card.deckId])
            operations += SyncOperation.MoveCard(
                operationId = newSyncId(),
                cardSyncId = card.syncId,
                sourceDeckSyncId = source.syncId,
                targetDeckSyncId = target.syncId,
                sourceLastReviewPassDuration = newDecks[old.deckId]?.lastRepetitionIterationDuration,
                targetLastReviewPassDuration = target.lastRepetitionIterationDuration,
                movedCardReviewDuration = if (source.cardQuantity > 0) {
                    source.lastRepetitionIterationDuration / source.cardQuantity
                } else 0L,
            )
        }
        if (old.copy(deckId = card.deckId) != card.copy(lastChangedServerRevision = old.lastChangedServerRevision)) {
            val deck = requireNotNull(newDecks[card.deckId])
            operations += SyncOperation.EditCard(newSyncId(), card.syncId, card.toInitialSyncCard(deck.syncId))
        }
    }
    afterCards.filter { it.id !in oldCards }.forEach { card ->
        val deck = requireNotNull(newDecks[card.deckId])
        operations += SyncOperation.AddCard(newSyncId(), card.toInitialSyncCard(deck.syncId))
    }
    beforeDecks.filter { it.id in deletedDeckIds }.forEach { deck ->
        operations += SyncOperation.DeleteDeck(newSyncId(), deck.syncId)
    }
    return operations
}
