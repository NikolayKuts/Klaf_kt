package com.example.klaf.data.networking

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.klaf.domain.entities.Card
import com.example.klaf.presentation.common.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CardAudioPlayer @Inject constructor(private val mediaPlayer: MediaPlayer) {

    companion object {

        private const val AUDIO_URI_TEMPLATE = "https://wooordhunt.ru/data/sound/sow/us/%s.mp3"
    }

    init {
        log(mediaPlayer.toString())

        mediaPlayer.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    suspend fun prepare(card: Card) {
        withContext(Dispatchers.IO) {
            mediaPlayer.apply {
                reset()
                setDataSource(card.buildAudioUri())
                prepare()
            }
        }
    }

    fun play() {
        mediaPlayer.start()
    }


    private fun Card.buildAudioUri(): String {
        return AUDIO_URI_TEMPLATE.format(this.foreignWord)
    }
}