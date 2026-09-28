package com.kuts.klaf.room

import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.server.contract.SyncReviewMark
import com.kuts.klaf.server.contract.SyncReviewSummary

fun DeckRepetitionInfo.toSyncReviewSummary(): SyncReviewSummary = SyncReviewSummary(
    currentDuration = currentDuration,
    previousDuration = previousDuration,
    scheduledDate = scheduledDate,
    previousScheduledDate = previousScheduledDate,
    lastIterationDate = lastIterationDate,
    currentSuccessMark = SyncReviewMark.valueOf(currentIterationSuccessMark.name),
    previousSuccessMark = SyncReviewMark.valueOf(previousIterationSuccessMark.name),
)

fun RoomDeck.toSyncReviewSummary(): SyncReviewSummary? = toReviewInfo()?.toSyncReviewSummary()

fun RoomDeck.withSyncReviewSummary(summary: SyncReviewSummary?): RoomDeck = copy(
    reviewCurrentDuration = summary?.currentDuration,
    reviewPreviousDuration = summary?.previousDuration,
    reviewScheduledDate = summary?.scheduledDate,
    reviewPreviousScheduledDate = summary?.previousScheduledDate,
    reviewLastIterationDate = summary?.lastIterationDate,
    reviewCurrentSuccessMark = summary?.currentSuccessMark?.name,
    reviewPreviousSuccessMark = summary?.previousSuccessMark?.name,
)
