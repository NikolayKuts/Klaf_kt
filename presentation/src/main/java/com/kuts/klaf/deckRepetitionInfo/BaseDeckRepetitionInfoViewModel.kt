package com.kuts.klaf.deckRepetitionInfo

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.IEmptiable
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.common.IEventMessageSource
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckRepetitionInfoViewModel : ViewModel(), IEventMessageSource {

    abstract val repetitionInfo: StateFlow<IEmptiable<DeckRepetitionInfo?>>
}