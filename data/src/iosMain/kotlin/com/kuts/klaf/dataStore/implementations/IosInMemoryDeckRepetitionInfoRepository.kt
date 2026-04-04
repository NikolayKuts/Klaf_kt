package com.kuts.klaf.dataStore.implementations

import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class IosInMemoryDeckRepetitionInfoRepository : IDeckRepetitionInfoRepository {
    private val source = MutableStateFlow<Map<Int, DeckRepetitionInfo>>(emptyMap())

    override fun fetchDeckRepetitionInfo(deckId: Int): Flow<DeckRepetitionInfo?> {
        return source.map { infos -> infos[deckId] }
    }

    override suspend fun saveDeckRepetitionInfo(info: DeckRepetitionInfo) {
        source.update { infos -> infos + (info.deckId to info) }
    }

    override suspend fun removeDeckRepetitionInfo(deckId: Int) {
        source.update { infos -> infos - deckId }
    }
}
