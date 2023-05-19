package com.kuts.klaf.presentation.deckRepetitionInfo

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.Emptiable
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckRepetitionInfoViewModel : ViewModel(), EventMessageSource {

    abstract val repetitionInfo: StateFlow<Emptiable<DeckRepetitionInfo?>>
}