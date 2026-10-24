package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.common.simplifiedItemMap
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.repositories.IVocabularySourceRepository
import com.kuts.domain.vocabularySource.VocabularySourceReviewContext
import com.kuts.domain.vocabularySource.reviewKey
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.room.entities.RoomVocabularySource
import com.kuts.klaf.room.toDomainEntity
import com.kuts.klaf.room.toRoomEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularySourceRepositoryRoom(
    private val databaseSource: RoomDatabaseSource,
) : IVocabularySourceRepository {

    constructor(roomDatabase: KlafRoomDatabase) : this(StaticRoomDatabaseSource(roomDatabase))

    private val roomDatabase: KlafRoomDatabase
        get() = databaseSource.current()

    override fun observeSources(): Flow<List<VocabularySource>> {
        return databaseSource.databases.flatMapLatest { database ->
            database.vocabularySourceDao().getObservableSources()
                .simplifiedItemMap { source -> source.toDomainEntity() }
        }
    }

    override fun observeSourceById(sourceId: Int): Flow<VocabularySource?> {
        return databaseSource.databases.flatMapLatest { database ->
            database.vocabularySourceDao().getObservableSourceById(sourceId = sourceId)
                .map { source: RoomVocabularySource? -> source?.toDomainEntity() }
        }
    }

    override fun observeItemsBySourceId(sourceId: Int): Flow<List<VocabularySourceItem>> {
        return databaseSource.databases.flatMapLatest { database ->
            combine(
                database.vocabularySourceItemDao().getObservableItemsBySourceId(sourceId),
                database.cardDao().observeAllCards(),
                database.ignoredVocabularyWordDao().observeWords(),
            ) { items, cards, ignored ->
                VocabularySourceReviewContext(cards.map { it.toDomainEntity() }, ignored.map { it.toDomainEntity() })
                    .resolve(items.map { it.toDomainEntity() })
            }
        }
    }

    override fun observeAllItems(): Flow<List<VocabularySourceItem>> {
        return databaseSource.databases.flatMapLatest { database ->
            combine(
                database.vocabularySourceItemDao().getObservableItems(),
                database.cardDao().observeAllCards(),
                database.ignoredVocabularyWordDao().observeWords(),
            ) { items, cards, ignored ->
                VocabularySourceReviewContext(cards.map { it.toDomainEntity() }, ignored.map { it.toDomainEntity() })
                    .resolve(items.map { it.toDomainEntity() })
            }
        }
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
        val existing = roomDatabase.vocabularySourceDao().getSourceById(source.id)
        val sourceId = roomDatabase.vocabularySourceDao()
            .insertSource(source = source.toRoomEntity().let { row ->
                if (existing == null) row else row.copy(syncId = existing.syncId,
                    lastChangedServerRevision = existing.lastChangedServerRevision)
            })
            .toInt()

        return source.id.takeIf { it > 0 } ?: sourceId
    }

    override suspend fun saveItems(items: List<VocabularySourceItem>) {
        roomDatabase.vocabularySourceItemDao()
            .insertItems(items = items.map { item ->
                val existing = roomDatabase.vocabularySourceItemDao().getItemById(item.id)
                item.toRoomEntity().let { row -> if (existing == null) row else row.copy(syncId = existing.syncId) }
            })
    }

    override suspend fun replaceNotAddedItemsBySourceId(
        sourceId: Int,
        items: List<VocabularySourceItem>,
    ) {
        val itemDao = roomDatabase.vocabularySourceItemDao()
        val oldItems = itemDao.getItemsBySourceId(sourceId).associateBy { it.id }
        val cardIds = roomDatabase.cardDao().getAllCards().map { it.id }.toSet()
        val protectedAddedIds = oldItems.values.filter {
            it.status == VocabularySourceItemStatus.ADDED.name && it.createdCardId in cardIds
        }.map { it.id }.toSet()
        val protectedAddedKeys = oldItems.values.filter { it.id in protectedAddedIds }
            .map { it.toDomainEntity().reviewKey() }.toSet()

        if (protectedAddedIds.isEmpty()) {
            itemDao.deleteItemsBySourceId(sourceId)
        } else {
            itemDao.deleteItemsBySourceIdExceptIds(sourceId, protectedAddedIds.toList())
        }
        itemDao.insertItems(items = items.filterNot {
            it.id in protectedAddedIds || it.reviewKey() in protectedAddedKeys
        }.map { item ->
            item.toRoomEntity().let { row -> oldItems[item.id]?.let { row.copy(syncId = it.syncId) } ?: row }
        })
    }

    override suspend fun removeSource(sourceId: Int) {
        val database = roomDatabase
        database.vocabularySourceItemDao().deleteItemsBySourceId(sourceId = sourceId)
        database.vocabularySourceDao().deleteSource(sourceId = sourceId)
    }

    override suspend fun removeItemsBySourceId(sourceId: Int) {
        roomDatabase.vocabularySourceItemDao().deleteItemsBySourceId(sourceId = sourceId)
    }

    override suspend fun removeItem(itemId: Int) {
        roomDatabase.vocabularySourceItemDao().deleteItem(itemId = itemId)
    }
}
