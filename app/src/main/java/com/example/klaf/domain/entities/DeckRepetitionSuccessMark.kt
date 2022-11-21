package com.example.klaf.domain.entities

import androidx.annotation.StringRes
import com.example.klaf.R
import kotlinx.serialization.Serializable

@Serializable
enum class DeckRepetitionSuccessMark(@StringRes val markResId: Int) {

    UNASSIGNED(markResId = R.string.unassigned_string_value),

    SUCCESS(markResId = R.string.deck_repetition_mark_successful),

    FAILURE(markResId = R.string.deck_repetition_mark_failed)
}
