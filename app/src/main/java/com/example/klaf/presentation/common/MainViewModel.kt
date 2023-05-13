package com.example.klaf.presentation.common

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainViewModel : BaseMainViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override fun notify(message: EventMessage) {
        viewModelScope.launch(Dispatchers.IO) { eventMessage.emit(value = message) }
    }
}