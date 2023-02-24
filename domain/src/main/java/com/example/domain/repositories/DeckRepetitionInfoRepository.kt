package com.example.domain.repositories

import com.example.domain.entities.DeckRepetitionInfo
import kotlinx.coroutines.flow.Flow

interface DeckRepetitionInfoRepository {

    fun fetchDeckRepetitionInfo(deckId: Int): Flow<DeckRepetitionInfo?>

    suspend fun saveDeckRepetitionInfo(info: DeckRepetitionInfo)

    suspend fun removeDeckRepetitionInfo(deckId: Int)
}