package com.kuts.domain.managers

import com.kuts.domain.common.LoadingState
import kotlinx.coroutines.flow.StateFlow

interface IAudioPlayerManager {

    val loadingState: StateFlow<LoadingState<Unit, Unit>>

    fun onCreate()

    fun onResume()

    fun onStop()

    fun onDestroy()

    fun preparePronunciation(word: String)

    fun play()

    fun preparePronunciationAndPlay(word: String)
}
