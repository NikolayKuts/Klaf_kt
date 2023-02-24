package com.example.domain.repositories

interface PronunciationPlayerRepository {

    fun preparePronunciation(word: String)

    fun playPronunciation()

}