package com.example.klaf.presentation.common

import kotlinx.coroutines.flow.SharedFlow

interface EventMessageSource {

    val eventMessage: SharedFlow<EventMessage>
}