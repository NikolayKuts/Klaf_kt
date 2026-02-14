package com.kuts.klaf.presentation.common

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainViewModel : BaseMainViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    private var lastEventMessage: EventMessage? = null
    private var lastEventMessageReceivingTime: Long? = null

    override fun notify(message: EventMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            val passedTime = lastEventMessageReceivingTime?.let { messageTime ->
                System.currentTimeMillis() - messageTime
            } ?: 0L
            val lastEventMessageDuration = lastEventMessage?.duration?.value ?: 0L

            val isDifferent = message.equalsTo(other = lastEventMessage).not()

            if (isDifferent || passedTime > lastEventMessageDuration) {
                eventMessage.emit(value = message)
                lastEventMessage = message
                lastEventMessageReceivingTime = System.currentTimeMillis()
            }
        }
    }

    private fun EventMessage.equalsTo(other: EventMessage?): Boolean {
        return other != null
                && resId == other.resId
                && type == other.type
                && duration == other.duration
    }
}