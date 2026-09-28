package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.IDeckReviewResultRepository
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase

class RoomDeckReviewResultRepository(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val outbox: RoomSyncOutbox = RoomSyncOutbox(databaseSource),
    private val writer: RoomReviewResultWriter = RoomReviewResultWriter(databaseSource, outbox),
) : IDeckReviewResultRepository {

    override suspend fun save(updatedDeck: Deck, reviewInfo: DeckRepetitionInfo) {
        val account = databaseSource.selection.value.accountEmail
        val baseRevision = account?.let { outbox.confirmedRevision(it) } ?: 0L
        writer.save(updatedDeck, reviewInfo, baseRevision)
    }
}
