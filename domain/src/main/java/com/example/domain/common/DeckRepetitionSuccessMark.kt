package com.example.domain.common

import kotlinx.serialization.Serializable

@Serializable
enum class DeckRepetitionSuccessMark {

    UNASSIGNED,
    SUCCESS,
    FAILURE
}
