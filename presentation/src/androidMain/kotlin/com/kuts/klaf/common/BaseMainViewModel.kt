package com.kuts.klaf.common

import androidx.lifecycle.ViewModel

abstract class BaseMainViewModel : ViewModel(), IEventMessageSource {

    abstract fun notify(message: EventMessage)
}