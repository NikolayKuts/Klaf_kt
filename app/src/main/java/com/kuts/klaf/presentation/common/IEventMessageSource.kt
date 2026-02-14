package com.kuts.klaf.presentation.common

import kotlinx.coroutines.flow.SharedFlow

interface IEventMessageSource {

    val eventMessage: SharedFlow<EventMessage>
}