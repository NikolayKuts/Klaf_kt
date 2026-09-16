package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.common.simplifiedItemMap
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.repositories.IVocabularySourceRepository
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.entities.RoomVocabularySource
import com.kuts.klaf.room.toDomainEntity
import com.kuts.klaf.room.toRoomEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VocabularySourceRepositoryRoom(
    private val roomDatabase: KlafRoomDatabase,
) : IVocabularySourceRepository {

    override fun observeSources(): Flow<List<VocabularySource>> {
        return roomDatabase.vocabularySourceDao()
            .getObservableSources()
            .simplifiedItemMap { source -> source.toDomainEntity() }
    }

    override fun observeSourceById(sourceId: Int): Flow<VocabularySource?> {
        return roomDatabase.vocabularySourceDao()
            .getObservableSourceById(sourceId = sourceId)
            .map { source: RoomVocabularySource? -> source?.toDomainEntity() }
    }

    override fun observeItemsBySourceId(sourceId: Int): Flow<List<VocabularySourceItem>> {
        return roomDatabase.vocabularySourceItemDao()
            .getObservableItemsBySourceId(sourceId = sourceId)
            .simplifiedItemMap { item -> item.toDomainEntity() }
    }

    override fun observeAllItems(): Flow<List<VocabularySourceItem>> {
        return roomDatabase.vocabularySourceItemDao()
            .getObservableItems()
            .simplifiedItemMap { item -> item.toDomainEntity() }
    }

    override suspend fun fetchSources(): List<VocabularySource> {
        return roomDatabase.vocabularySourceDao()
            .getSources()
            .map { source -> source.toDomainEntity() }
    }

    override suspend fun fetchSourceById(sourceId: Int): VocabularySource? {
        return roomDatabase.vocabularySourceDao()
            .getSourceById(sourceId = sourceId)
            ?.toDomainEntity()
    }

    override suspend fun fetchItemsBySourceId(sourceId: Int): List<VocabularySourceItem> {
        return roomDatabase.vocabularySourceItemDao()
            .getItemsBySourceId(sourceId = sourceId)
            .map { item -> item.toDomainEntity() }
    }

    override suspend fun fetchItemById(itemId: Int): VocabularySourceItem? {
        return roomDatabase.vocabularySourceItemDao()
            .getItemById(itemId = itemId)
            ?.toDomainEntity()
    }

    override suspend fun saveSource(source: VocabularySource): Int {
        val sourceId = roomDatabase.vocabularySourceDao()
            .insertSource(source = source.toRoomEntity())
            .toInt()

        return source.id.takeIf { it > 0 } ?: sourceId
    }

    override suspend fun saveItems(items: List<VocabularySourceItem>) {
        roomDatabase.vocabularySourceItemDao()
            .insertItems(items = items.map { item -> item.toRoomEntity() })
    }

    override suspend fun replaceNotAddedItemsBySourceId(
        sourceId: Int,
        items: List<VocabularySourceItem>,
    ) {
        val itemDao = roomDatabase.vocabularySourceItemDao()

        itemDao.deleteItemsBySourceIdExceptStatus(
            sourceId = sourceId,
            status = VocabularySourceItemStatus.ADDED.name,
        )
        itemDao.insertItems(items = items.map { item -> item.toRoomEntity() })
    }

    override suspend fun removeSource(sourceId: Int) {
        roomDatabase.vocabularySourceItemDao().deleteItemsBySourceId(sourceId = sourceId)
        roomDatabase.vocabularySourceDao().deleteSource(sourceId = sourceId)
    }

    override suspend fun removeItemsBySourceId(sourceId: Int) {
        roomDatabase.vocabularySourceItemDao().deleteItemsBySourceId(sourceId = sourceId)
    }

    override suspend fun removeItem(itemId: Int) {
        roomDatabase.vocabularySourceItemDao().deleteItem(itemId = itemId)
    }
}
