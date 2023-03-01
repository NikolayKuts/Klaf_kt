package com.example.klaf.presentation.deckRepetitionInfo

import androidx.lifecycle.ViewModel
import com.example.domain.entities.DeckRepetitionInfo
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckRepetitionInfoViewModel : ViewModel(), EventMessageSource {

    abstract val repetitionInfo: StateFlow<DeckRepetitionInfo?>
}