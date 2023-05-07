package com.example.klaf.presentation.common

import androidx.lifecycle.ViewModel

abstract class BaseMainViewModel : ViewModel(), EventMessageSource {

    abstract fun notify(message: EventMessage)
}