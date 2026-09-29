package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.entities.RoomIgnoredVocabularyWord
import com.kuts.klaf.room.identity
import com.kuts.klaf.room.newSyncId
import com.kuts.klaf.room.readSyncVocabularySources
import com.kuts.klaf.room.toSyncWord
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncVocabularySource

internal suspend fun KlafRoomDatabase.syncSources(): List<SyncVocabularySource> = readSyncVocabularySources(
    vocabularySourceDao(), vocabularySourceItemDao(), deckDao(), cardDao(),
)

internal fun planVocabularyChanges(
    before: List<SyncVocabularySource>,
    after: List<SyncVocabularySource>,
    beforeIgnored: List<RoomIgnoredVocabularyWord>,
    afterIgnored: List<RoomIgnoredVocabularyWord>,
): List<SyncOperation> {
    val previous = before.associateBy { it.syncId }
    val currentIds = after.map { it.syncId }.toSet()
    val oldWordIds = beforeIgnored.map { it.toSyncWord().identity() }.toSet()
    return buildList {
        after.filter { previous[it.syncId] != it }.forEach {
            add(SyncOperation.UpsertVocabularySource(newSyncId(), it))
        }
        before.filter { it.syncId !in currentIds }.forEach {
            add(SyncOperation.DeleteVocabularySource(newSyncId(), it.syncId))
        }
        afterIgnored.filter { it.toSyncWord().identity() !in oldWordIds }.forEach {
            add(SyncOperation.AddIgnoredVocabularyWord(newSyncId(), it.toSyncWord()))
        }
    }
}
