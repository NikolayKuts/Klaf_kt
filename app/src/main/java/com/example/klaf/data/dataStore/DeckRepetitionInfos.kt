package com.example.klaf.data.dataStore

import kotlinx.serialization.Serializable

@Serializable
data class DeckRepetitionInfos(
    val content: Set<DeckRepetitionInfo> = emptySet()
)