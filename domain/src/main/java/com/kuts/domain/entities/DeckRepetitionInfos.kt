package com.kuts.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class DeckRepetitionInfos(
    val content: Set<DeckRepetitionInfo> = emptySet(),
)