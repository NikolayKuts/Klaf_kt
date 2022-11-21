package com.example.klaf.presentation.deckRepetition

import androidx.lifecycle.ViewModel
import com.example.klaf.domain.entities.DeckRepetitionInfo
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.domain.common.DeckRepetitionState
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DifficultyRecallingLevel
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
    abstract val deckRepetitionInfo: SharedFlow<DeckRepetitionInfo?>

    abstract fun pronounceWord()
    abstract fun startRepeating()
    abstract fun moveToStartScreenState()
    abstract fun turnCard()
    abstract fun changeRepetitionOrder()
    abstract fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel)
    abstract fun resumeTimerCounting()
    abstract fun pauseTimerCounting()
    abstract fun deleteCard(cardId: Int, deckId: Int)
}