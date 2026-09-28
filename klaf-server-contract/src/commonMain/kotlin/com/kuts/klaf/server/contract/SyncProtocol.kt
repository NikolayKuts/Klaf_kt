package com.kuts.klaf.server.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SyncReviewMark { UNASSIGNED, SUCCESS, FAILURE }

@Serializable
data class SyncReviewSummary(
    val currentDuration: Long,
    val previousDuration: Long,
    val scheduledDate: Long,
    val previousScheduledDate: Long,
    val lastIterationDate: Long? = null,
    val currentSuccessMark: SyncReviewMark,
    val previousSuccessMark: SyncReviewMark,
)

@Serializable
data class SyncDeck(
    val syncId: String,
    val name: String,
    val creationDate: Long,
    val reviewPassDates: List<Long> = emptyList(),
    val scheduledReviewDates: List<Long> = emptyList(),
    val scheduledDateInterval: Long = 0L,
    val reviewCount: Int = 0,
    val cardQuantity: Int = 0,
    val lastFirstReviewDuration: Long = 0L,
    val lastSecondReviewDuration: Long = 0L,
    val lastReviewPassDuration: Long = 0L,
    val isLastPassSucceeded: Boolean = true,
    val lastChangedServerRevision: Long = 0L,
    val reviewSummary: SyncReviewSummary? = null,
)

@Serializable
data class SyncCard(
    val syncId: String,
    val deckSyncId: String,
    val nativeWord: String,
    val foreignWord: String,
    val ipaJson: String = "[]",
    val wordMeaningInsightsJson: String = "{}",
    val mnemonicJson: String = "{}",
    val lastChangedServerRevision: Long = 0L,
)

@Serializable
sealed interface SyncOperation {
    val operationId: String

    @Serializable
    @SerialName("ADD_DECK")
    data class AddDeck(override val operationId: String, val deck: SyncDeck) : SyncOperation

    @Serializable
    @SerialName("EDIT_DECK")
    data class EditDeck(
        override val operationId: String,
        val deckSyncId: String,
        val name: String,
    ) : SyncOperation

    @Serializable
    @SerialName("DELETE_DECK")
    data class DeleteDeck(override val operationId: String, val deckSyncId: String) : SyncOperation

    @Serializable
    @SerialName("ADD_CARD")
    data class AddCard(override val operationId: String, val card: SyncCard) : SyncOperation

    @Serializable
    @SerialName("EDIT_CARD")
    data class EditCard(
        override val operationId: String,
        val cardSyncId: String,
        val card: SyncCard,
    ) : SyncOperation

    @Serializable
    @SerialName("DELETE_CARD")
    data class DeleteCard(override val operationId: String, val cardSyncId: String) : SyncOperation

    @Serializable
    @SerialName("MOVE_CARD")
    data class MoveCard(
        override val operationId: String,
        val cardSyncId: String,
        val sourceDeckSyncId: String,
        val targetDeckSyncId: String,
        val sourceLastReviewPassDuration: Long? = null,
        val targetLastReviewPassDuration: Long? = null,
        val movedCardReviewDuration: Long? = null,
    ) : SyncOperation

    @Serializable
    @SerialName("FINISH_REVIEW")
    data class FinishReview(
        override val operationId: String,
        val deckSyncId: String,
        val reviewCount: Int,
        val lastReviewAt: Long,
        val nextReviewAt: Long,
        val scheduledDateInterval: Long,
        val lastFirstReviewDuration: Long,
        val lastSecondReviewDuration: Long,
        val lastReviewPassDuration: Long,
        val isLastPassSucceeded: Boolean,
        val reviewSummary: SyncReviewSummary? = null,
        val reviewPassDates: List<Long>? = null,
        val scheduledReviewDates: List<Long>? = null,
    ) : SyncOperation

    @Serializable
    @SerialName("MAKE_DECK_DUE_NOW")
    data class MakeDeckDueNow(
        override val operationId: String,
        val deckSyncId: String,
    ) : SyncOperation
}

@Serializable
data class SyncRequest(
    val email: String,
    val deviceId: String,
    val protocolVersion: Int,
    val baseRevision: Long,
    val operations: List<SyncOperation>,
)

@Serializable
data class SyncPositionConfirmationRequest(
    val email: String,
    val deviceId: String,
    val protocolVersion: Int,
    val revision: Long,
)

@Serializable
data class SyncPositionConfirmationResponse(
    val revision: Long,
    val confirmedAtMillis: Long,
)

@Serializable
data class SyncHistoryItem(
    val operationId: String,
    val revision: Long,
    val deviceId: String,
    val action: String,
    val affectedSyncIds: List<String>,
    val occurredAtMillis: Long,
)

@Serializable
data class SyncHistoryResponse(
    val revision: Long,
    val entries: List<SyncHistoryItem>,
)

@Serializable
data class SyncConflict(
    val operationId: String,
    val reason: String,
    val localOperation: SyncOperation,
    val serverChanges: List<SyncHistoryItem>,
    val serverDeck: SyncDeck? = null,
    val serverCard: SyncCard? = null,
)

@Serializable
data class SyncResponse(
    val revision: Long,
    val acceptedOperationIds: Set<String> = emptySet(),
    val conflicts: List<SyncConflict> = emptyList(),
    val delta: SyncDelta,
)

@Serializable
data class SyncDelta(
    val fromRevision: Long,
    val toRevision: Long,
    val decks: List<SyncDeck>,
    val cards: List<SyncCard>,
    val deletedDeckSyncIds: List<String>,
    val deletedCardSyncIds: List<String>,
    val history: List<SyncHistoryItem>,
)

@Serializable
data class SyncBootstrapResponse(
    val revision: Long,
    val decks: List<SyncDeck>,
    val cards: List<SyncCard>,
)
