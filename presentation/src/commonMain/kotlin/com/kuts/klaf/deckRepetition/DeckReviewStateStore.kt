package com.kuts.klaf.deckRepetition

import com.kuts.domain.common.CardRepetitionOrder
import com.kuts.domain.common.CardSide
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.UnitSurrogate
import com.kuts.domain.entities.Card
import com.kuts.klaf.common.ButtonState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

interface DeckReviewStateStore {
    val mainButtonState: MutableStateFlow<ButtonState>
    val screenState: MutableSharedFlow<RepetitionScreenState>
    val cardDeletingState: MutableStateFlow<LoadingState<UnitSurrogate, UnitSurrogate>>
    val repetitionCards: MutableStateFlow<List<Card>>
    val cardSide: MutableStateFlow<CardSide>
    val repetitionOrder: MutableStateFlow<CardRepetitionOrder>
    val reviewedCardIds: MutableStateFlow<Set<Int>>
    var savedTime: Long
    val deckReviewState: MutableStateFlow<DeckReviewState>
    var startRepetitionCard: Card?
    var lastRepetitionCard: Card?
    var isAllCardsRepeated: Boolean
    var isWaitingForFinish: Boolean
    val savedProgressCards: MutableList<Card>
    var timerTime: Long
}
