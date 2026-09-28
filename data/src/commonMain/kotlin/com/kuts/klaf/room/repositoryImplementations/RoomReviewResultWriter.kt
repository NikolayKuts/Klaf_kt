package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.newSyncId
import com.kuts.klaf.room.toSyncReviewSummary
import com.kuts.klaf.server.contract.SyncOperation

/** Persists a completed review and its sync operation as one selected-database transaction. */
class RoomReviewResultWriter(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val outbox: RoomSyncOutbox,
    private val afterLocalWrite: suspend () -> Unit = {},
) {

    suspend fun save(updatedDeck: Deck, reviewInfo: DeckRepetitionInfo, baseRevision: Long) {
        require(updatedDeck.id > 0 && updatedDeck.id == reviewInfo.deckId) {
            "Review details must belong to the updated deck"
        }
        require(updatedDeck.reviewCount == reviewInfo.repetitionQuantity) {
            "Review counts must match"
        }
        require(baseRevision >= 0L) { "Base revision cannot be negative" }

        val selectedAccount = databaseSource.selection.value.accountEmail
        suspend fun writeLocal() {
            val deckDao = databaseSource.current().deckDao()
            val previous = requireNotNull(deckDao.getDeckById(updatedDeck.id)) { "Deck no longer exists" }
            require(previous.syncId == updatedDeck.syncId && previous.syncId.isNotBlank()) {
                "Review deck identity changed"
            }
            require(updatedDeck.reviewCount == previous.repetitionQuantity + 1) {
                "Review count is stale"
            }
            require(updatedDeck.lastChangedServerRevision == previous.lastChangedServerRevision) {
                "Review deck revision changed"
            }
            deckDao.insertDeck(previous.copy(
                repetitionIterationDates = updatedDeck.reviewPassDates,
                scheduledIterationDates = updatedDeck.scheduledReviewDates,
                scheduledDateInterval = updatedDeck.scheduledDateInterval,
                repetitionQuantity = updatedDeck.reviewCount,
                lastFirstRepetitionDuration = updatedDeck.lastFirstReviewDuration,
                lastSecondRepetitionDuration = updatedDeck.lastSecondReviewDuration,
                lastRepetitionIterationDuration = updatedDeck.lastReviewPassDuration,
                isLastIterationSucceeded = updatedDeck.isLastPassSucceeded,
                reviewCurrentDuration = reviewInfo.currentDuration,
                reviewPreviousDuration = reviewInfo.previousDuration,
                reviewScheduledDate = reviewInfo.scheduledDate,
                reviewPreviousScheduledDate = reviewInfo.previousScheduledDate,
                reviewLastIterationDate = reviewInfo.lastIterationDate,
                reviewCurrentSuccessMark = reviewInfo.currentIterationSuccessMark.name,
                reviewPreviousSuccessMark = reviewInfo.previousIterationSuccessMark.name,
            ))
            afterLocalWrite()
        }

        if (selectedAccount == null) {
            databaseSource.transaction {
                check(databaseSource.selection.value.accountEmail == null) {
                    "Selected account changed during guest review"
                }
                writeLocal()
            }
        } else {
            val operation = SyncOperation.FinishReview(
                operationId = newSyncId(),
                deckSyncId = updatedDeck.syncId,
                reviewCount = updatedDeck.reviewCount,
                lastReviewAt = updatedDeck.reviewPassDates.lastOrNull() ?: 0L,
                nextReviewAt = updatedDeck.scheduledReviewDates.lastOrNull() ?: 0L,
                scheduledDateInterval = updatedDeck.scheduledDateInterval,
                lastFirstReviewDuration = updatedDeck.lastFirstReviewDuration,
                lastSecondReviewDuration = updatedDeck.lastSecondReviewDuration,
                lastReviewPassDuration = updatedDeck.lastReviewPassDuration,
                isLastPassSucceeded = updatedDeck.isLastPassSucceeded,
                reviewSummary = reviewInfo.toSyncReviewSummary(),
                reviewPassDates = updatedDeck.reviewPassDates,
                scheduledReviewDates = updatedDeck.scheduledReviewDates,
            )
            outbox.recordChange(selectedAccount, baseRevision, operation, ::writeLocal)
        }
    }
}
