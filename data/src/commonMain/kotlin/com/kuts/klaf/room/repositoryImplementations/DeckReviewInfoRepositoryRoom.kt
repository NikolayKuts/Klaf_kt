package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.toReviewInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class DeckReviewInfoRepositoryRoom(
    private val databaseSource: RoomDatabaseSource,
) : IDeckRepetitionInfoRepository {

    override fun fetchDeckRepetitionInfo(deckId: Int): Flow<DeckRepetitionInfo?> =
        databaseSource.databases.flatMapLatest { database ->
            database.deckDao().getObservableDeckById(deckId).map { it?.toReviewInfo() }
        }

    override suspend fun saveDeckRepetitionInfo(info: DeckRepetitionInfo) {
        val dao = databaseSource.current().deckDao()
        val deck = requireNotNull(dao.getDeckById(info.deckId)) { "Review deck does not exist" }
        check(deck.repetitionQuantity == info.repetitionQuantity) { "Review count differs from the deck" }
        check(dao.updateReviewInfo(
            deckId = info.deckId,
            currentDuration = info.currentDuration,
            previousDuration = info.previousDuration,
            scheduledDate = info.scheduledDate,
            previousScheduledDate = info.previousScheduledDate,
            lastIterationDate = info.lastIterationDate,
            currentMark = info.currentIterationSuccessMark.name,
            previousMark = info.previousIterationSuccessMark.name,
        ) == 1)
    }

    override suspend fun removeDeckRepetitionInfo(deckId: Int) {
        databaseSource.current().deckDao().updateReviewInfo(
            deckId = deckId,
            currentDuration = null,
            previousDuration = null,
            scheduledDate = null,
            previousScheduledDate = null,
            lastIterationDate = null,
            currentMark = null,
            previousMark = null,
        )
    }
}
