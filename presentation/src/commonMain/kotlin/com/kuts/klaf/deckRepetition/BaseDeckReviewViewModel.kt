package com.kuts.klaf.deckRepetition

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.DeckRepetitionState
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.UnitSurrogate
import com.kuts.domain.entities.Deck
import com.kuts.domain.enums.DifficultyRecallingLevel
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.klaf.common.ButtonState
import com.kuts.klaf.common.IEventMessageSource
import com.kuts.klaf.common.RepetitionTimer
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckReviewViewModel : ViewModel(), IEventMessageSource {

    abstract val timer: RepetitionTimer
    abstract val audioPlayer: IAudioPlayerManager
    abstract val deck: SharedFlow<Deck?>
    abstract val screenState: SharedFlow<RepetitionScreenState>
    abstract val cardState: SharedFlow<DeckRepetitionState>
    abstract val isInsightsAvailable: StateFlow<Boolean>
    abstract val isInsightsSheetVisible: StateFlow<Boolean>
    abstract val mainButtonState: StateFlow<ButtonState>
    abstract val cardDeletingState: StateFlow<LoadingState<UnitSurrogate, UnitSurrogate>>
    abstract val deckReviewState: StateFlow<DeckReviewState>

    abstract fun pronounceWord()
    abstract fun showInsightsSheet()
    abstract fun hideInsightsSheet()
    abstract fun startRepeating()
    abstract fun turnCard()
    abstract fun changeRepetitionOrder()
    abstract fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel)
    abstract fun resumeTimerCounting()
    abstract fun pauseTimerCounting()
    abstract fun deleteCard(cardId: Int, deckId: Int)
    abstract fun changeButtonsStateOnCommonButtonClick()
    abstract fun resetScreenState()
}
