package com.example.klaf.presentation.deckRepetition

import androidx.lifecycle.ViewModel
import com.example.domain.common.DeckRepetitionState
import com.example.domain.common.LoadingState
import com.example.domain.entities.Deck
import com.example.domain.enums.DifficultyRecallingLevel
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.presentation.common.ButtonState
import com.example.klaf.presentation.common.EventMessageSource
import com.example.klaf.presentation.common.RepetitionTimer
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckRepetitionViewModel : ViewModel(), EventMessageSource {

    abstract val timer: RepetitionTimer
    abstract val audioPlayer: CardAudioPlayer
    abstract val deck: SharedFlow<Deck?>
    abstract val screenState: StateFlow<RepetitionScreenState>
    abstract val cardState: SharedFlow<DeckRepetitionState>
    abstract val mainButtonState: StateFlow<ButtonState>
    abstract val cardDeletingState: StateFlow<LoadingState<Unit>>

    abstract fun pronounceWord()
    abstract fun startRepeating()
    abstract fun turnCard()
    abstract fun changeRepetitionOrder()
    abstract fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel)
    abstract fun resumeTimerCounting()
    abstract fun pauseTimerCounting()
    abstract fun deleteCard(cardId: Int, deckId: Int)
    abstract fun changeStateOnMainButtonClick()
}