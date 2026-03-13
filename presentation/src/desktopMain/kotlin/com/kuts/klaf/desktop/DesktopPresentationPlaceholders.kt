package com.kuts.klaf.desktop

import com.kuts.domain.common.CardRepetitionOrder
import com.kuts.domain.common.CardSide
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.UnitSurrogate
import com.kuts.domain.entities.Card
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.cardManagement.common.CambridgeWordData
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.common.ButtonState
import com.kuts.klaf.deckRepetition.DeckReviewState
import com.kuts.klaf.deckRepetition.IDeckReviewStateStore
import com.kuts.klaf.deckRepetition.RepetitionScreenState
import com.kuts.klaf.deckRepetition.RepetitionScreenState.StartState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopNoOpDeckReviewNotifier : IDeckReviewNotifierManager {

    override fun showNotification(deckName: String, deckId: Int) = Unit
    override fun showCommonNotification() = Unit
    override fun removeNotificationFromNotificationBar(deckId: Int) = Unit
}

class DesktopNoOpCambridgeWordDataProvider : ICambridgeWordDataProvider {

    override suspend fun fetchWordData(word: String): CambridgeWordData? = null
}

class DesktopInMemoryDeckReviewStateStore : IDeckReviewStateStore {

    override val mainButtonState = MutableStateFlow(ButtonState.UNPRESSED)
    override val screenState = MutableSharedFlow<RepetitionScreenState>(replay = 1).apply {
        tryEmit(StartState)
    }
    override val cardDeletingState = MutableStateFlow<LoadingState<UnitSurrogate, UnitSurrogate>>(
        value = LoadingState.Non
    )
    override val repetitionCards = MutableStateFlow<List<Card>>(emptyList())
    override val cardSide = MutableStateFlow(CardSide.FRONT)
    override val repetitionOrder = MutableStateFlow(CardRepetitionOrder.NATIVE_TO_FOREIGN)
    override val reviewedCardIds = MutableStateFlow<Set<Int>>(emptySet())
    override var savedTime: Long = 0L
    override val deckReviewState = MutableStateFlow(DeckReviewState())
    override var startRepetitionCard: Card? = null
    override var lastRepetitionCard: Card? = null
    override var isAllCardsRepeated: Boolean = false
    override var isWaitingForFinish: Boolean = false
    override val savedProgressCards = MutableStateFlow<List<Card>>(emptyList())
    override var timerTime: Long = 0L
}
