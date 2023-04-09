package com.example.klaf.data.networking

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onException
import com.example.domain.common.LoadingState
import com.example.domain.common.ifNull
import com.example.domain.common.ifTrue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class CardAudioPlayer @Inject constructor() : DefaultLifecycleObserver {

    private var mediaPlayer: MediaPlayer? = getNewPlayerInstance()
    private var isPrepared = false
    private var wordForPreparing: String? = null
    private var coroutineScope: CoroutineScope? =
        CoroutineScope(context = Dispatchers.IO + SupervisorJob())
    private var preparingJob: Job? = null

    private val _loadingState = MutableStateFlow<LoadingState<Unit>>(value = LoadingState.Non)
    val loadingState = _loadingState.asStateFlow()

    companion object {

        private const val AUDIO_URI_TEMPLATE = "https://wooordhunt.ru/data/sound/sow/us/%s.mp3"
        private const val LOADING_TIME_INTERVAL = 6000L
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

        val word = wordForPreparing

        if (preparingJob == null && word != null) {
            preparePronunciation(word = word)
        }
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

        if (word.isNotEmpty()) {
            preparingJob = coroutineScope?.launchWithState {
                mediaPlayer?.apply {
                    launch {
                        delay(LOADING_TIME_INTERVAL)
                        _loadingState.value = LoadingState.Non
                    }

                    _loadingState.value = LoadingState.Loading
                    preparingJob?.ensureActive()
                    wordForPreparing = word
                    isPrepared = false

                    reset()
                    setDataSource(word.buildAudioUri())
                    prepare()
                }
            }?.onException { _, _ ->
                mediaPlayer?.reset()
            }
        } else {
            _loadingState.value = LoadingState.Non
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
        setOnPreparedListener {
            isPrepared = true
            preparingJob?.cancel()
            _loadingState.value = LoadingState.Success(Unit)
        }
    }

    private fun String.buildAudioUri(): String {
        return AUDIO_URI_TEMPLATE.format(this.trim().lowercase())
    }
}