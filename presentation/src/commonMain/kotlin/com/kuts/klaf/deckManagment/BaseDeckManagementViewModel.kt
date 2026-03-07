package com.kuts.klaf.deckManagment

import androidx.lifecycle.ViewModel
import com.kuts.klaf.common.IEventMessageSource
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckManagementViewModel : ViewModel(), IEventMessageSource {

    abstract val deckManagementState: StateFlow<DeckManagementState>

    abstract val event: StateFlow<IDeckManagementEvent>

    abstract fun sendAction(action: IDeckManagementAction)
}