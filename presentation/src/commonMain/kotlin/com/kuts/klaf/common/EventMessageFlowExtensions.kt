package com.kuts.klaf.common

import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.resources.StringResource

fun MutableSharedFlow<EventMessage>.tryEmit(messageId: StringResource) {
    tryEmit(value = EventMessage(resId = messageId))
}

suspend fun MutableSharedFlow<EventMessage>.emit(messageId: StringResource) {
    emit(value = EventMessage(resId = messageId))
}

fun MutableSharedFlow<EventMessage>.tryEmitAsNegative(
    resId: StringResource,
    vararg args: Any = emptyArray(),
    duration: EventMessage.Duration = EventMessage.Duration.Long,
) {
    this.tryEmit(
        value = EventMessage(
            resId = resId,
            args = args,
            type = EventMessage.Type.Negative,
            duration = duration
        )
    )
}

fun MutableSharedFlow<EventMessage>.tryEmitAsPositive(
    resId: StringResource,
    duration: EventMessage.Duration = EventMessage.Duration.Medium,
) {
    this.tryEmit(
        value = EventMessage(
            resId = resId,
            type = EventMessage.Type.Positive,
            duration = duration
        )
    )
}

fun MutableSharedFlow<EventMessage>.tryEmitAsNeutral(
    resId: StringResource,
    duration: EventMessage.Duration = EventMessage.Duration.Medium,
) {
    tryEmit(
        value = EventMessage(resId = resId, type = EventMessage.Type.Neutral, duration = duration)
    )
}
