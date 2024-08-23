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

    init {
        mediaPlayer = getNewPlayerInstance()
    }

    companion object {

        private const val AUDIO_URI_TEMPLATE = "https://wooordhunt.ru/data/sound/sow/us/%s.mp3"
        private const val LOADING_TIMEOUT_INTERVAL = 6000L
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
        resetPreparingJob()
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
        resetPreparingJob()
        if (word.isNotEmpty()) {
            preparingJob = coroutineScope?.launchWithState {
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

    private fun getNewPlayerInstance(): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )

        setOnPreparedListener {
            isPrepared = true
            resetPreparingJob()
            _loadingState.value = LoadingState.Success(Unit)
        }

        setOnErrorListener { notNullableMediaPlayer, _, _ ->
            notNullableMediaPlayer.reset()
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