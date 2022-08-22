package com.example.klaf.data.networking

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.entities.Card
import kotlinx.coroutines.*
import javax.inject.Inject

class CardAudioPlayer @Inject constructor() : DefaultLifecycleObserver {

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var cardForPreparing: Card? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var preparingJob: Job? = null

    companion object {

        private const val AUDIO_URI_TEMPLATE = "https://wooordhunt.ru/data/sound/sow/us/%s.mp3"
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            this.setOnPreparedListener { isPrepared = true }
        }

        cardForPreparing?.let { preparePronunciation(card = it) }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isPrepared = false
        preparingJob = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        coroutineScope.cancel()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun preparePronunciation(card: Card) {
        coroutineScope.launchWithExceptionHandler(
            onException = { _, _ -> }
        ) {
            mediaPlayer?.apply {
                preparingJob?.ensureActive()
                cardForPreparing = card
                isPrepared = false
                reset()
                setDataSource(card.buildAudioUri())
                prepare()
            }
        }
    }

    fun play() {
        if (isPrepared) {
            mediaPlayer?.start()
        }
    }

    private fun Card.buildAudioUri(): String {
        return AUDIO_URI_TEMPLATE.format(this.foreignWord)
    }
}