package com.kuts.domain.managers

interface ITextToSpeechManager {

    fun speak(text: String)

    fun stop()

    fun shutdown()
}
