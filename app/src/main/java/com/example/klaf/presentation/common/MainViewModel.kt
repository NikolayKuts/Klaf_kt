package com.example.klaf.presentation.common

import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MainViewModel : BaseMainViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>()

    override fun notify(message: EventMessage) {
        viewModelScope.launch(Dispatchers.IO) { eventMessage.emit(value = message) }
    }

    init {
        log("Init")
        viewModelScope.launch(Dispatchers.IO) {
            repeat(4) {
                eventMessage.emit(messageId = R.string.problem_fetching_decks)
                log("notify")
                delay(3000)
            }
        }
    }
}