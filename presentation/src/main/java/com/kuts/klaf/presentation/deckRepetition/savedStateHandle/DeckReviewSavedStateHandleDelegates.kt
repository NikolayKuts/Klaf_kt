package com.kuts.klaf.presentation.deckRepetition.savedStateHandle

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.kuts.domain.common.CardRepetitionOrder
import com.kuts.domain.common.CardSide
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.UnitSurrogate
import com.kuts.domain.entities.Card
import com.kuts.klaf.common.create
import com.kuts.klaf.common.mutList
import com.kuts.klaf.common.mutSharedFlow
import com.kuts.klaf.common.mutStateFlow
import com.kuts.klaf.presentation.common.ButtonState
import com.kuts.klaf.presentation.deckRepetition.DeckReviewState
import com.kuts.klaf.presentation.deckRepetition.RepetitionScreenState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.LinkedList

val SavedStateHandle.deckReviewDelegates: DeckReviewSavedStateHandleDelegates
    get() = DeckReviewSavedStateHandleDelegates(this)

class DeckReviewSavedStateHandleDelegates(handle: SavedStateHandle) {

    companion object {

        private const val MAIN_BUTTON_STATE = "main_button_state"
        private const val SCREEN_STATE = "screen_state"
        private const val CARD_DELETING_STATE = "card_deleting_state"
        private const val REPETITION_CARDS = "repetition_cards"
        private const val CARD_SIDE = "card_side"
        private const val REPETITION_ORDER = "repetition_order"
        private const val REVIEWED_CARD_IDS_LIST = "reviewed_card_ids_list"
        private const val SAVED_TIME = "saved_time"
        private const val DECK_REVIEW_STATE = "deck_review_state"
        private const val START_REPETITION_CARD = "start_repetition_card"
        private const val LAST_REPETITION_CARD = "last_repetition_card"
        private const val IS_ALL_CARDS_REPEATED = "is_all_cards_repeated"
        private const val IS_WAITING_FOR_FINISH = "is_waiting_for_finish"
        private const val SAVED_PROGRESS_CARDS = "saved_progress_cards"
        private const val TIMER_TIME = "timer_time"
    }

    val mainButtonState: MutableStateFlow<ButtonState> by handle.mutStateFlow(
        key = MAIN_BUTTON_STATE,
        default = ButtonState.UNPRESSED,
    )

    val screenState: MutableSharedFlow<RepetitionScreenState> by handle.mutSharedFlow<RepetitionScreenState>(
        key = SCREEN_STATE,
        replay = 1,
        extraBufferCapacity = 4,
    )

    val cardDeletingState: MutableStateFlow<LoadingState<UnitSurrogate, UnitSurrogate>> by handle.mutStateFlow(
        key = CARD_DELETING_STATE,
        default = LoadingState.Non,
        encode = { loadingState -> loadingState.serialized(key = CARD_DELETING_STATE) },
        decode = { bundle ->
            bundle.deserialized<LoadingState<UnitSurrogate, UnitSurrogate>>(
                key = CARD_DELETING_STATE
            ) ?: LoadingState.Non
        }
    )

    val repetitionCards: MutableStateFlow<List<Card>> by handle.mutStateFlow(
        key = REPETITION_CARDS,
        default = LinkedList(),
        encode = { cards -> cards.serialized(key = REPETITION_CARDS) },
        decode = { bundle -> bundle.deserializedList(key = REPETITION_CARDS) }
    )

    val cardSide: MutableStateFlow<CardSide> by handle.mutStateFlow(
        key = CARD_SIDE,
        default = CardSide.FRONT
    )

    val repetitionOrder: MutableStateFlow<CardRepetitionOrder> by handle.mutStateFlow(
        key = REPETITION_ORDER,
        default = CardRepetitionOrder.NATIVE_TO_FOREIGN
    )

    val reviewedCardIds: MutableStateFlow<Set<Int>> by handle.mutStateFlow(
        key = REVIEWED_CARD_IDS_LIST,
        default = emptySet(),
        encode = { ids ->
            Bundle().apply { putIntArray(REVIEWED_CARD_IDS_LIST, ids.toIntArray()) }
        },
        decode = { bundle -> bundle.getIntArray(REVIEWED_CARD_IDS_LIST)?.toSet() ?: emptySet() }
    )

    var savedTime: Long by handle.create(
        key = SAVED_TIME,
        default = 0L
    )

    val deckReviewState: MutableStateFlow<DeckReviewState> by handle.mutStateFlow(
        key = DECK_REVIEW_STATE,
        default = DeckReviewState()
    )

    var startRepetitionCard: Card? by handle.create(
        key = START_REPETITION_CARD,
        default = null,
        encode = { card -> card.serialized(key = START_REPETITION_CARD) },
        decode = { bundle -> bundle.deserialized(key = START_REPETITION_CARD) }
    )

    var lastRepetitionCard: Card? by handle.create(
        key = LAST_REPETITION_CARD,
        default = null,
        encode = { card -> card.serialized(key = LAST_REPETITION_CARD) },
        decode = { bundle -> bundle.deserialized(key = LAST_REPETITION_CARD) }
    )

    var isAllCardsRepeated: Boolean by handle.create(
        key = IS_ALL_CARDS_REPEATED,
        default = false
    )

    var isWaitingForFinish: Boolean by handle.create(
        key = IS_WAITING_FOR_FINISH,
        default = false
    )

    val savedProgressCards: MutableList<Card> by handle.mutList(
        key = SAVED_PROGRESS_CARDS,
        default = LinkedList(),
        encode = { cards -> cards.serialized(key = SAVED_PROGRESS_CARDS) },
        decode = { bundle -> bundle.deserializedList(key = SAVED_PROGRESS_CARDS) }
    )

    var timerTime: Long by handle.create(
        key = TIMER_TIME,
        default = 0L
    )
}