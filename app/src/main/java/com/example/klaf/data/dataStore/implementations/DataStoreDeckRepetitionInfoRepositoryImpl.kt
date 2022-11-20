package com.example.klaf.data.dataStore.implementations

import androidx.datastore.core.DataStore
import com.example.klaf.data.dataStore.DeckRepetitionInfo
import com.example.klaf.data.dataStore.DeckRepetitionInfos
import com.example.klaf.domain.repositories.DeckRepetitionInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreDeckRepetitionInfoRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<DeckRepetitionInfos>,
) : DeckRepetitionInfoRepository {

    override fun fetchDeckRepetitionInfo(deckId: Int): Flow<DeckRepetitionInfo?> {
        return dataStore.data.map { infos ->
            infos.content.firstOrNull { it.deckId == deckId }
        }
    }

    override suspend fun saveDeckRepetitionInfo(info: DeckRepetitionInfo) {
        dataStore.updateData { infos ->
            val updatedContent = infos.content.toMutableSet().apply {
                removeIf { filteredInfo -> filteredInfo.deckId == info.deckId }
                add(element = info)
            }

            infos.copy(content = updatedContent)
        }
    }
}