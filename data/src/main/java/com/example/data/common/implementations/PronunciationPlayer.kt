package com.example.data.common.implementations

import com.example.data.networking.CardAudioPlayer
import com.example.domain.repositories.PronunciationPlayerRepository
import javax.inject.Inject

class PronunciationPlayer @Inject constructor(
    private val audioPlayer: CardAudioPlayer
) : PronunciationPlayerRepository {

    override fun preparePronunciation(word: String) {
        audioPlayer.preparePronunciation(word = word)
    }

    override fun playPronunciation() {
        audioPlayer.play()
    }
}