package com.kuts.klaf.presentation.deckManagment

import androidx.lifecycle.ViewModel
import com.kuts.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckManagementViewModel : ViewModel(), EventMessageSource {

    abstract val deckState: StateFlow<DeckManagementState>

    abstract fun sendEvent(event: DeckManagementEvent)
}