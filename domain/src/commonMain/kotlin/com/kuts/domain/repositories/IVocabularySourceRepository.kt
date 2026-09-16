package com.kuts.domain.repositories

import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import kotlinx.coroutines.flow.Flow

interface IVocabularySourceRepository {

    fun observeSources(): Flow<List<VocabularySource>>

    fun observeSourceById(sourceId: Int): Flow<VocabularySource?>

    fun observeItemsBySourceId(sourceId: Int): Flow<List<VocabularySourceItem>>

    fun observeAllItems(): Flow<List<VocabularySourceItem>>

    suspend fun fetchSources(): List<VocabularySource>

    suspend fun fetchSourceById(sourceId: Int): VocabularySource?

    suspend fun fetchItemsBySourceId(sourceId: Int): List<VocabularySourceItem>

    suspend fun fetchItemById(itemId: Int): VocabularySourceItem?

    suspend fun saveSource(source: VocabularySource): Int

    suspend fun saveItems(items: List<VocabularySourceItem>)

    suspend fun replaceNotAddedItemsBySourceId(sourceId: Int, items: List<VocabularySourceItem>)

    suspend fun removeSource(sourceId: Int)

    suspend fun removeItemsBySourceId(sourceId: Int)

    suspend fun removeItem(itemId: Int)
}
