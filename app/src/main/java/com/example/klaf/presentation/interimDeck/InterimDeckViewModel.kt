package com.example.klaf.presentation.interimDeck

import com.example.klaf.presentation.common.EventMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class InterimDeckViewModel @Inject constructor() : BaseInterimDeckViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>()
}