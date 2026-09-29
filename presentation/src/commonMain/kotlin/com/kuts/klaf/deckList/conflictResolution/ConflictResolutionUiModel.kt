package com.kuts.klaf.deckList.conflictResolution

import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.klaf.server.contract.SyncConflict
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncResponse
import com.kuts.klaf.server.contract.SyncVocabularySource

data class ConflictUiEntry(
    val operationId: String,
    val localDescription: String,
    val serverDescription: String,
    val serverChanges: List<String>,
    val availableActions: Set<ConflictResolutionAction>,
)

data class ConflictResolutionUiModel(
    val revision: Long,
    val conflicts: List<ConflictUiEntry>,
    val availableActions: Set<ConflictResolutionAction>,
    val manualSelectionAvailable: Boolean,
)

private fun SyncVocabularySource.describeAnalysis(): String =
    "$title: ${items.size} words; text: ${cleanText.ifBlank { rawText }.take(200)}; " +
        "words: ${items.take(10).joinToString { it.foreignWord }}" + if (items.size > 10) " …" else ""

private fun SyncConflict.canKeepDeckEdit(response: SyncResponse): Boolean {
    val edit = localOperation as? SyncOperation.EditDeck ?: return false
    return serverDeck?.syncId == edit.deckSyncId &&
        edit.deckSyncId !in response.delta.deletedDeckSyncIds && edit.name.isNotBlank()
}

private fun SyncConflict.canKeepCardEdit(response: SyncResponse): Boolean {
    val edit = localOperation as? SyncOperation.EditCard ?: return false
    return serverCard?.syncId == edit.cardSyncId && serverCard?.deckSyncId == edit.card.deckSyncId &&
        edit.card.syncId == edit.cardSyncId && edit.cardSyncId !in response.delta.deletedCardSyncIds &&
        edit.card.deckSyncId !in response.delta.deletedDeckSyncIds
}

private fun SyncConflict.isReviewedCardRemoval(): Boolean {
    val removal = localOperation as? SyncOperation.DeleteCard ?: return false
    val card = serverCard ?: return false
    val deck = serverDeck ?: return false
    return reason == "REVIEW_MEMBERSHIP_CHANGE" && card.syncId == removal.cardSyncId &&
        deck.syncId == card.deckSyncId && deck.reviewCount > 0
}

private fun SyncConflict.isMoveIntoReviewedDeck(): Boolean {
    val move = localOperation as? SyncOperation.MoveCard ?: return false
    val card = serverCard ?: return false
    val deck = serverDeck ?: return false
    return reason == "REVIEWED_DECK" && card.syncId == move.cardSyncId &&
        card.deckSyncId == move.sourceDeckSyncId && deck.syncId == move.targetDeckSyncId &&
        deck.reviewCount > 0
}

private fun SyncOperation.describe(
    deckNames: Map<String, String>,
    cardNames: Map<String, String>,
): String = when (this) {
    is SyncOperation.AddDeck -> "Add deck ${deck.name}"
    is SyncOperation.EditDeck -> "Rename deck ${deckNames[deckSyncId] ?: deckSyncId} to $name"
    is SyncOperation.DeleteDeck -> "Delete deck ${deckNames[deckSyncId] ?: deckSyncId} and its cards"
    is SyncOperation.AddCard -> "Add card ${card.foreignWord} to ${deckNames[card.deckSyncId] ?: card.deckSyncId}"
    is SyncOperation.EditCard -> "Edit card: foreign ${card.foreignWord}, native ${card.nativeWord}"
    is SyncOperation.DeleteCard -> "Delete card ${cardNames[cardSyncId] ?: cardSyncId}"
    is SyncOperation.MoveCard -> "Move card ${cardNames[cardSyncId] ?: cardSyncId} from " +
        "${deckNames[sourceDeckSyncId] ?: sourceDeckSyncId} to ${deckNames[targetDeckSyncId] ?: targetDeckSyncId}"
    is SyncOperation.FinishReview -> "Complete review of deck ${deckNames[deckSyncId] ?: deckSyncId}"
    is SyncOperation.MakeDeckDueNow -> "Make deck ${deckNames[deckSyncId] ?: deckSyncId} due for review now"
    is SyncOperation.UpsertVocabularySource -> "Save source and analysis: ${source.describeAnalysis()}"
    is SyncOperation.DeleteVocabularySource -> "Delete vocabulary source $sourceSyncId and its word list"
    is SyncOperation.AddIgnoredVocabularyWord -> "Ignore word ${word.foreignWord}: ${word.nativeWord}"
}

private fun SyncConflict.describeServer(response: SyncResponse, deckNames: Map<String, String>): String {
    if (reason == "SOURCE_LINK_MISSING") return "A linked card or deck is missing on the server"
    serverSource?.let { return "Server source and analysis: ${it.describeAnalysis()}" }
    if (localOperation is SyncOperation.UpsertVocabularySource || localOperation is SyncOperation.DeleteVocabularySource) {
        return if (reason == "SOURCE_LINK_MISSING") "A linked card or deck is missing on the server" else
            "The source is absent or was deleted on the server"
    }
    if (isMoveIntoReviewedDeck()) {
        return "Destination deck ${requireNotNull(serverDeck).name} was reviewed before this move; " +
            "choose another unreviewed deck to keep the card"
    }
    if (isReviewedCardRemoval()) {
        return "Server completed a review of deck ${requireNotNull(serverDeck).name} while it still contained this card; " +
            "the next review was scheduled using that earlier deck content"
    }
    val deletedDeck = when (val operation = localOperation) {
        is SyncOperation.EditCard -> operation.card.deckSyncId
        is SyncOperation.MoveCard -> operation.sourceDeckSyncId
        else -> null
    }
    if (deletedDeck != null && deletedDeck in response.delta.deletedDeckSyncIds) {
        return "Server deleted deck ${deckNames[deletedDeck] ?: deletedDeck} and its cards in this update"
    }
    serverCard?.let { return "Server card: foreign ${it.foreignWord}, native ${it.nativeWord}" }
    serverDeck?.let { return "Server deck: ${it.name}" }
    return if (serverChanges.isEmpty()) "Server has a conflicting change ($reason)" else
        "Server has ${serverChanges.size} conflicting changes"
}

fun SyncResponse.toConflictResolutionUiModel(
    deckNames: Map<String, String> = emptyMap(),
    cardNames: Map<String, String> = emptyMap(),
): ConflictResolutionUiModel {
    val operations = conflicts.map(SyncConflict::localOperation)
    val availableActions = mutableSetOf(ConflictResolutionAction.ACCEPT_SERVER)
    if (conflicts.isNotEmpty() && conflicts.all {
        it.reason != "SOURCE_LINK_MISSING" && (it.localOperation is SyncOperation.UpsertVocabularySource ||
            it.localOperation is SyncOperation.DeleteVocabularySource)
    }) availableActions += ConflictResolutionAction.KEEP_LOCAL_SOURCE
    if (conflicts.isNotEmpty() && conflicts.all { it.canKeepDeckEdit(this) }) {
        availableActions += ConflictResolutionAction.KEEP_LOCAL_DECK
    }
    if (operations.isNotEmpty() && operations.all { it is SyncOperation.EditCard }) {
        val edits = operations.filterIsInstance<SyncOperation.EditCard>()
        val deckIds = edits.map { it.card.deckSyncId }.distinct()
        if (deckIds.size == 1 && deckIds.single() in delta.deletedDeckSyncIds) {
            availableActions += ConflictResolutionAction.RESTORE_DELETED_DECK
        } else if (conflicts.all { it.canKeepCardEdit(this) }) {
            availableActions += ConflictResolutionAction.KEEP_LOCAL_CARD
        }
    }
    if (operations.isNotEmpty() && operations.all { it is SyncOperation.MoveCard }) {
        val moves = operations.filterIsInstance<SyncOperation.MoveCard>()
        if (moves.all { move ->
                move.sourceDeckSyncId in delta.deletedDeckSyncIds &&
                    move.targetDeckSyncId !in delta.deletedDeckSyncIds &&
                    delta.decks.none { it.syncId == move.targetDeckSyncId && it.reviewCount > 0 }
            }
        ) {
            availableActions += ConflictResolutionAction.RESCUE_MOVED_CARD
        }
    }
    if (conflicts.isNotEmpty() && conflicts.all(SyncConflict::isReviewedCardRemoval)) {
        availableActions += ConflictResolutionAction.KEEP_REMOVAL_RETAIN_SCHEDULE
        availableActions += ConflictResolutionAction.KEEP_REMOVAL_DUE_NOW
    }
    if (conflicts.size == 1 && conflicts.single().isMoveIntoReviewedDeck() &&
        (conflicts.single().localOperation as SyncOperation.MoveCard).movedCardReviewDuration != null
    ) {
        availableActions += ConflictResolutionAction.RETARGET_MOVED_CARD
    }
    return ConflictResolutionUiModel(
        revision = revision,
        conflicts = conflicts.map { conflict ->
            val knownDeckNames = buildMap {
                conflict.serverDeck?.let { put(it.syncId, it.name) }
                delta.decks.forEach { put(it.syncId, it.name) }
                putAll(deckNames)
            }
            val knownCardNames = buildMap {
                conflict.serverCard?.let { put(it.syncId, it.foreignWord) }
                delta.cards.forEach { put(it.syncId, it.foreignWord) }
                putAll(cardNames)
            }
            ConflictUiEntry(
                operationId = conflict.operationId,
                localDescription = conflict.localOperation.describe(knownDeckNames, knownCardNames),
                serverDescription = conflict.describeServer(this, knownDeckNames),
                serverChanges = conflict.serverChanges.map { change ->
                    "${change.deviceId}: ${change.action.replace('_', ' ').lowercase()} " +
                        change.affectedSyncIds.joinToString { id ->
                            knownDeckNames[id] ?: knownCardNames[id] ?: id
                        }
                },
                availableActions = buildSet {
                    add(ConflictResolutionAction.ACCEPT_SERVER)
                    when (val operation = conflict.localOperation) {
                        is SyncOperation.EditDeck -> if (conflict.canKeepDeckEdit(this@toConflictResolutionUiModel)) {
                            add(ConflictResolutionAction.KEEP_LOCAL_DECK)
                        }
                        is SyncOperation.EditCard -> if (conflict.canKeepCardEdit(this@toConflictResolutionUiModel)) {
                            add(ConflictResolutionAction.KEEP_LOCAL_CARD)
                        }
                        is SyncOperation.UpsertVocabularySource, is SyncOperation.DeleteVocabularySource ->
                            if (conflict.reason != "SOURCE_LINK_MISSING") add(ConflictResolutionAction.KEEP_LOCAL_SOURCE)
                        is SyncOperation.DeleteCard -> if (conflict.isReviewedCardRemoval()) {
                            add(ConflictResolutionAction.KEEP_REMOVAL_RETAIN_SCHEDULE)
                            add(ConflictResolutionAction.KEEP_REMOVAL_DUE_NOW)
                        }
                        is SyncOperation.MoveCard -> if (conflict.isMoveIntoReviewedDeck() &&
                            operation.movedCardReviewDuration != null
                        ) {
                            add(ConflictResolutionAction.RETARGET_MOVED_CARD)
                        }
                        else -> Unit
                    }
                },
            )
        },
        availableActions = availableActions,
        manualSelectionAvailable = conflicts.size > 1 &&
            conflicts.all { it.localOperation is SyncOperation.EditDeck || it.localOperation is SyncOperation.EditCard ||
                it.localOperation is SyncOperation.UpsertVocabularySource || it.localOperation is SyncOperation.DeleteVocabularySource } &&
            conflicts.map { conflict ->
                when (val operation = conflict.localOperation) {
                    is SyncOperation.EditDeck -> "deck:${operation.deckSyncId}"
                    is SyncOperation.EditCard -> "card:${operation.cardSyncId}"
                    is SyncOperation.UpsertVocabularySource -> "source:${operation.source.syncId}"
                    is SyncOperation.DeleteVocabularySource -> "source:${operation.sourceSyncId}"
                    else -> error("Unexpected structural conflict")
                }
            }.distinct().size == conflicts.size,
    )
}
