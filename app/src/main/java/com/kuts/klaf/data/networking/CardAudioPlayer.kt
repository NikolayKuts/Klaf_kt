package com.kuts.klaf.data.networking

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.ifNull
import com.kuts.domain.common.ifTrue
import com.kuts.domain.repositories.CrashlyticsRepository
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class CardAudioPlayer @Inject constructor(
    private val crashlytics: CrashlyticsRepository,
    private var mediaPlayer: MediaPlayer? = null,
    private var isPrepared: Boolean = false,
    private var wordForPreparing: String? = null,
    private var coroutineScope: CoroutineScope? = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
    private var preparingJob: Job? = null,
) : DefaultLifecycleObserver {

    private val _loadingState = MutableStateFlow<LoadingState<Unit>>(value = LoadingState.Non)
    val loadingState = _loadingState.asStateFlow()

    private var onPronunciationPrepared: (() -> Unit)? = null

    init {
        mediaPlayer = getNewPlayerInstance()
    }

    companion object {

        private const val AUDIO_URI_TEMPLATE = "https://wooordhunt.ru/data/sound/sow/us/%s.mp3"
        private const val LOADING_TIMEOUT_INTERVAL = 6000L
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        logD {
            message("onCreate() called")
            message("owner: $owner")
        }
        coroutineScope.ifNull {
            coroutineScope = CoroutineScope(context = Dispatchers.IO + SupervisorJob())
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        logD {
            message("onResume() called")
            message("owner: $owner")
        }
        mediaPlayer.ifNull { mediaPlayer = getNewPlayerInstance() }

        val word = wordForPreparing

        if (preparingJob == null && word != null) {
            preparePronunciation(word = word)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        logD {
            message("onStop() called")
            message("owner: $owner")
        }
        resetPreparingJob()
        isPrepared = false
        mediaPlayer?.release()
        mediaPlayer = null
        onPronunciationPrepared = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        logD {
            message("onDestroy() called")
            message("owner: $owner")
        }
        coroutineScope?.cancel()
        coroutineScope = null
        onPronunciationPrepared = null
    }

    fun preparePronunciation(word: String) {
        logD("preparePronunciation() called")
        resetPreparingJob()
        if (word.isNotEmpty()) {
            preparingJob = coroutineScope?.launchWithState {
                startPronunciationPreparing(word = word)
            }?.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                mediaPlayer?.reset()
                _loadingState.value = LoadingState.Non
            }
        } else {
            _loadingState.value = LoadingState.Non
            wordForPreparing = null
        }
    }

    fun play() {
        isPrepared.ifTrue { mediaPlayer?.start() }
    }

    fun preparePronunciationAndPlay(word: String) {
        resetPreparingJob()

        if (word.isNotEmpty()) {
            onPronunciationPrepared = { play() }

            preparingJob = coroutineScope?.launchWithState {
                startPronunciationPreparing(word = word)
            }?.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                onPronunciationPrepared = null
                mediaPlayer?.reset()
                _loadingState.value = LoadingState.Non
            }
        } else {
            onPronunciationPrepared = null
            _loadingState.value = LoadingState.Non
            wordForPreparing = null
        }
    }

    private suspend fun startPronunciationPreparing(word: String) {
        mediaPlayer?.apply {
            _loadingState.value = LoadingState.Loading
            wordForPreparing = word
            isPrepared = false

            delay(500)
            reset()
            setDataSource(word.buildAudioUri())

            prepareAsync()
            delay(LOADING_TIMEOUT_INTERVAL)
            val shouldLoadingStateBeNon =
                !isPrepared && (_loadingState.value !is LoadingState.Success<Unit>)

            shouldLoadingStateBeNon.ifTrue { _loadingState.value = LoadingState.Non }
        }
    }

    private fun getNewPlayerInstance(): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )

        setOnPreparedListener {
            logD("Preparing finished")
            isPrepared = true
            resetPreparingJob()
            _loadingState.value = LoadingState.Success(Unit)
            onPronunciationPrepared?.invoke()
        }

        setOnErrorListener { notNullableMediaPlayer, what, extra ->
            logW("Error observed! what: $what, extra: $extra")
            notNullableMediaPlayer.reset()
            onPronunciationPrepared = null
            true
        }
    }

    private fun String.buildAudioUri(): String {
        return AUDIO_URI_TEMPLATE.format(this.trim().lowercase())
    }

    private fun resetPreparingJob() {
        preparingJob?.cancel()
        preparingJob = null
    }
}