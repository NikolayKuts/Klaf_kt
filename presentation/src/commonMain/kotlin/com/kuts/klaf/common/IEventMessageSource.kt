package com.kuts.klaf.common

import kotlinx.coroutines.flow.SharedFlow

interface IEventMessageSource {

    val eventMessage: SharedFlow<EventMessage>
}