package com.example.klaf.data.networking

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.klaf.domain.common.ifNull
import com.example.klaf.domain.common.ifTrue
import com.example.klaf.domain.common.launchWithExceptionHandler
import kotlinx.coroutines.*
import javax.inject.Inject

class CardAudioPlayer @Inject constructor() : DefaultLifecycleObserver {

    private var mediaPlayer: MediaPlayer? = getNewPlayerInstance()
    private var isPrepared = false
    private var wordForPreparing: String? = null
    private var coroutineScope: CoroutineScope? =
        CoroutineScope(context = Dispatchers.IO + SupervisorJob())
    private var preparingJob: Job? = null

    companion object {

        private const val AUDIO_URI_TEMPLATE = "https://wooordhunt.ru/data/sound/sow/us/%s.mp3"
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        coroutineScope.ifNull {
            coroutineScope = CoroutineScope(context = Dispatchers.IO + SupervisorJob())
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        mediaPlayer.ifNull { mediaPlayer = getNewPlayerInstance() }
        wordForPreparing?.let { preparePronunciation(word = it) }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        preparingJob?.cancel()
        preparingJob = null
        isPrepared = false
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        coroutineScope?.cancel()
        coroutineScope = null
    }

    fun preparePronunciation(word: String) {
        preparingJob?.cancel()
        preparingJob = coroutineScope?.launchWithExceptionHandler(
            onException = { _, _ -> mediaPlayer?.reset() }
        ) {
            mediaPlayer?.apply {
                preparingJob?.ensureActive()
                wordForPreparing = word
                isPrepared = false

                reset()
                setDataSource(word.buildAudioUri())
                prepare()
            }
        }
    }

    fun play() {
        isPrepared.ifTrue { mediaPlayer?.start() }
    }

    private fun getNewPlayerInstance(): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        setOnPreparedListener { isPrepared = true }
    }

    private fun String.buildAudioUri(): String {
        return AUDIO_URI_TEMPLATE.format(this.trim().lowercase())
    }
}