package com.kuts.klaf.common

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.getCurrentDateAsLong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainViewModel : BaseMainViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    private var lastEventMessage: EventMessage? = null
    private var lastEventMessageReceivingTime: Long? = null

    override fun notify(message: EventMessage) {
        viewModelScope.launch {
            val passedTime = lastEventMessageReceivingTime?.let { messageTime ->
                getCurrentDateAsLong() - messageTime
            } ?: 0L
            val lastEventMessageDuration = lastEventMessage?.duration?.value ?: 0L

            val isDifferent = message != lastEventMessage

            if (isDifferent || passedTime > lastEventMessageDuration) {
                eventMessage.emit(value = message)
                lastEventMessage = message
                lastEventMessageReceivingTime = getCurrentDateAsLong()
            }
        }
    }
}
