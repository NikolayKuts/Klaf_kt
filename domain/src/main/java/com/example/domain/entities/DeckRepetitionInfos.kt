package com.example.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class DeckRepetitionInfos(
    val content: Set<DeckRepetitionInfo> = emptySet(),
)