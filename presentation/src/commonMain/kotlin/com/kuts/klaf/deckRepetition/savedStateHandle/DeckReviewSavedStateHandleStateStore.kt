package com.kuts.klaf.deckRepetition.savedStateHandle

import androidx.lifecycle.SavedStateHandle
import com.kuts.domain.common.CardRepetitionOrder
import com.kuts.domain.common.CardSide
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.UnitSurrogate
import com.kuts.domain.entities.Card
import com.kuts.klaf.common.ButtonState
import com.kuts.klaf.common.create
import com.kuts.klaf.common.mutList
import com.kuts.klaf.common.mutSharedFlow
import com.kuts.klaf.common.mutStateFlow
import com.kuts.klaf.deckRepetition.DeckReviewState
import com.kuts.klaf.deckRepetition.DeckReviewStateStore
import com.kuts.klaf.deckRepetition.RepetitionScreenState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class DeckReviewSavedStateHandleStateStore(handle: SavedStateHandle) : DeckReviewStateStore {

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

    override val mainButtonState: MutableStateFlow<ButtonState> by handle.mutStateFlow(
        key = MAIN_BUTTON_STATE,
        default = ButtonState.UNPRESSED,
    )

    override val screenState: MutableSharedFlow<RepetitionScreenState> by handle.mutSharedFlow(
        key = SCREEN_STATE,
        replay = 1,
        extraBufferCapacity = 4,
        encode = { state -> state.serialized() },
        decode = { value -> value.deserialized<RepetitionScreenState>() }
    )

    override val cardDeletingState: MutableStateFlow<LoadingState<UnitSurrogate, UnitSurrogate>> by handle.mutStateFlow(
        key = CARD_DELETING_STATE,
        default = LoadingState.Non,
        encode = { loadingState -> loadingState.serialized() },
        decode = { value -> value.deserialized<LoadingState<UnitSurrogate, UnitSurrogate>>() }
    )

    override val repetitionCards: MutableStateFlow<List<Card>> by handle.mutStateFlow(
        key = REPETITION_CARDS,
        default = emptyList(),
        encode = { cards -> cards.serialized() },
        decode = { value -> value.deserialized<List<Card>>() }
    )

    override val cardSide: MutableStateFlow<CardSide> by handle.mutStateFlow(
        key = CARD_SIDE,
        default = CardSide.FRONT
    )

    override val repetitionOrder: MutableStateFlow<CardRepetitionOrder> by handle.mutStateFlow(
        key = REPETITION_ORDER,
        default = CardRepetitionOrder.NATIVE_TO_FOREIGN
    )

    override val reviewedCardIds: MutableStateFlow<Set<Int>> by handle.mutStateFlow(
        key = REVIEWED_CARD_IDS_LIST,
        default = emptySet(),
        encode = { ids -> ids.serialized() },
        decode = { value -> value.deserialized<Set<Int>>() }
    )

    override var savedTime: Long by handle.create(
        key = SAVED_TIME,
        default = 0L
    )

    override val deckReviewState: MutableStateFlow<DeckReviewState> by handle.mutStateFlow(
        key = DECK_REVIEW_STATE,
        default = DeckReviewState(),
        encode = { state -> state.serialized() },
        decode = { value -> value.deserialized<DeckReviewState>() }
    )

    override var startRepetitionCard: Card? by handle.create(
        key = START_REPETITION_CARD,
        default = null,
        encode = { card -> card.serialized() },
        decode = { value -> value.deserialized<Card?>() }
    )

    override var lastRepetitionCard: Card? by handle.create(
        key = LAST_REPETITION_CARD,
        default = null,
        encode = { card -> card.serialized() },
        decode = { value -> value.deserialized<Card?>() }
    )

    override var isAllCardsRepeated: Boolean by handle.create(
        key = IS_ALL_CARDS_REPEATED,
        default = false
    )

    override var isWaitingForFinish: Boolean by handle.create(
        key = IS_WAITING_FOR_FINISH,
        default = false
    )

    override val savedProgressCards: MutableList<Card> by handle.mutList(
        key = SAVED_PROGRESS_CARDS,
        default = mutableListOf(),
        encode = { cards -> cards.serialized() },
        decode = { value -> value.deserialized<List<Card>>() }
    )

    override var timerTime: Long by handle.create(
        key = TIMER_TIME,
        default = 0L
    )

    init {
        if (screenState.replayCache.isEmpty()) {
            screenState.tryEmit(RepetitionScreenState.StartState)
        }
    }
}
