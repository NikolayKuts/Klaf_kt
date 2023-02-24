package com.example.presentation.common

import kotlinx.coroutines.flow.SharedFlow

interface EventMessageSource {

    val eventMessage: SharedFlow<EventMessage>
}