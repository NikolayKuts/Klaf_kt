package com.example.klaf.presentation.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class LifecycleObservingLogger(private val ownerName: String) : LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        val eventMessage = when (event) {
            Lifecycle.Event.ON_CREATE -> "on create"
            Lifecycle.Event.ON_START -> "on start"
            Lifecycle.Event.ON_RESUME -> "on resume"
            Lifecycle.Event.ON_PAUSE -> "on pause"
            Lifecycle.Event.ON_STOP -> "on stop"
            Lifecycle.Event.ON_DESTROY -> "on destroy"
            Lifecycle.Event.ON_ANY -> ""
        }

        log(message = eventMessage, pointerMessage = ownerName)
    }
}