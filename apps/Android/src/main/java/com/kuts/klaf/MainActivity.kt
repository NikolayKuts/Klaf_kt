package com.kuts.klaf

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kuts.domain.managers.MnemonicGenerationLaunchExtras
import com.kuts.domain.managers.IReviewReminderScopeProvider
import com.kuts.domain.managers.IClientSessionScope
import com.kuts.domain.managers.VocabularySourceAnalysisLaunchExtras
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessageView
import com.kuts.klaf.common.MainViewModel
import com.kuts.klaf.common.permissions.IMicrophonePermissionBinder
import com.kuts.klaf.common.permissions.INotificationPermissionBinder
import com.kuts.klaf.common.permissions.INotificationPermissionManager
import com.kuts.klaf.common.permissions.NotificationPermissionDialogs
import com.kuts.klaf.navigation.AndroidKlafNavHost
import com.kuts.klaf.navigation.AppLaunchNavigationExtras
import com.kuts.klaf.navigation.AppLaunchNavigationRequest
import com.kuts.klaf.theme.MainTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    companion object {

        private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
        private const val FIREBASE_SOURCE_ID_KEY = "sourceId"
    }

    private val sharedViewModel: BaseMainViewModel by viewModels<MainViewModel>()
    private val notificationPermissionBinder: INotificationPermissionBinder by inject()
    private val microphonePermissionBinder: IMicrophonePermissionBinder by inject()
    private val notificationPermissionManager: INotificationPermissionManager by inject()
    private val reminderScope: IReviewReminderScopeProvider by inject()
    private val klafServerSession: IClientSessionScope by inject()
    private var shouldCheckNotificationPermission by mutableStateOf(false)

    private val launchRequests = MutableSharedFlow<AppLaunchNavigationRequest>(
        extraBufferCapacity = 1,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        notificationPermissionBinder.bind(activity = this)
        microphonePermissionBinder.bind(activity = this)
        val initialLaunchRequest = intent.toLaunchNavigationRequest()
        intent.clearMnemonicGenerationLaunchExtras()
        intent.clearVocabularySourceAnalysisLaunchExtras()
        intent.clearAppLaunchNavigationExtras()

        setContent {
            MainTheme {
                setStatusBarColor()

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidKlafNavHost(
                        sharedViewModel = sharedViewModel,
                        initialLaunchRequest = initialLaunchRequest,
                        launchRequests = launchRequests,
                        onRestartApp = ::finish,
                    )

                    sharedViewModel.eventMessage.collectAsState(initial = null)
                        .value
                        ?.let { message ->
                            EventMessageView(
                                modifier = Modifier.align(Alignment.TopCenter),
                                message = message,
                            )
                        }
                }

                NotificationPermissionDialogs(
                    shouldCheckPermission = shouldCheckNotificationPermission,
                    onPermissionCheckConsumed = { shouldCheckNotificationPermission = false },
                    permissionManager = notificationPermissionManager,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val launchRequest = intent.toLaunchNavigationRequest()
        intent.clearMnemonicGenerationLaunchExtras()
        intent.clearVocabularySourceAnalysisLaunchExtras()
        intent.clearAppLaunchNavigationExtras()
        setIntent(intent)
        launchRequest?.let { request -> launchRequests.tryEmit(request) }
    }

    override fun onStart() {
        super.onStart()
        shouldCheckNotificationPermission = true
    }

    @Composable
    private fun setStatusBarColor() {
        window.statusBarColor = MainTheme.colors.common.statusBarBackground.toArgb()
    }

    private fun Intent.toLaunchNavigationRequest(): AppLaunchNavigationRequest? {
        val notificationSession = getStringExtra(com.kuts.domain.managers.ClientSessionLaunchExtras.SESSION_ID_KEY)
        if (notificationSession != null && notificationSession != klafServerSession.clientSessionId) return null
        toMnemonicGenerationLaunchNavigationRequest()?.let { request -> return request }
        toVocabularySourceAnalysisLaunchNavigationRequest()?.let { request -> return request }

        val destination = getStringExtra(AppLaunchNavigationExtras.DESTINATION_KEY)

        if (destination != null) {
            return when (destination) {
                AppLaunchNavigationExtras.DESTINATION_DECK_LIST -> {
                    AppLaunchNavigationRequest.OpenDeckList
                }

                AppLaunchNavigationExtras.DESTINATION_INTERIM_CARD_ADDITION -> {
                    AppLaunchNavigationRequest.OpenInterimCardAddition
                }

                AppLaunchNavigationExtras.DESTINATION_DECK_REPETITION -> {
                    val incomingScope = getStringExtra(AppLaunchNavigationExtras.REMINDER_ACCOUNT_SCOPE_KEY)
                    if (incomingScope != reminderScope.currentScope()) return null
                    val deckId = getLaunchIntExtra(AppLaunchNavigationExtras.DECK_ID_KEY)
                        ?: AppLaunchNavigationExtras.DEFAULT_DECK_ID
                    val deckName = getStringExtra(AppLaunchNavigationExtras.DECK_NAME_KEY)
                        ?: AppLaunchNavigationExtras.DEFAULT_DECK_NAME

                    AppLaunchNavigationRequest.OpenDeckRepetition(
                        deckId = deckId,
                        deckName = deckName,
                    )
                }

                AppLaunchNavigationExtras.DESTINATION_CARD_EDITING -> {
                    val cardId = getLaunchIntExtra(AppLaunchNavigationExtras.CARD_ID_KEY)
                        ?: return null
                    AppLaunchNavigationRequest.OpenCardEditing(
                        deckId = getLaunchIntExtra(AppLaunchNavigationExtras.DECK_ID_KEY)
                            ?: AppLaunchNavigationExtras.DEFAULT_DECK_ID,
                        cardId = cardId,
                    )
                }

                else -> null
            }
        }

        return if (
            action == Intent.ACTION_PROCESS_TEXT
            && type?.startsWith(MIME_TYPE_TEXT_PLAIN) == true
        ) {
            AppLaunchNavigationRequest.OpenInterimCardAddition
        } else {
            null
        }
    }

    private fun Intent.toMnemonicGenerationLaunchNavigationRequest(): AppLaunchNavigationRequest? {
        val destination = getStringExtra(MnemonicGenerationLaunchExtras.DESTINATION_KEY)
            ?: return null
        val deckId = getLaunchIntExtra(MnemonicGenerationLaunchExtras.DECK_ID_KEY)
            ?: return null

        return when (destination) {
            MnemonicGenerationLaunchExtras.DESTINATION_CARD_ADDITION -> {
                AppLaunchNavigationRequest.OpenCardAdditionMnemonicManagement(deckId = deckId)
            }

            MnemonicGenerationLaunchExtras.DESTINATION_CARD_EDITING -> {
                val cardId = getLaunchIntExtra(MnemonicGenerationLaunchExtras.CARD_ID_KEY)
                    ?: return null

                AppLaunchNavigationRequest.OpenCardEditingMnemonicManagement(
                    deckId = deckId,
                    cardId = cardId,
                )
            }

            else -> null
        }
    }

    private fun Intent.toVocabularySourceAnalysisLaunchNavigationRequest(): AppLaunchNavigationRequest? {
        val sourceId = getLaunchIntExtra(VocabularySourceAnalysisLaunchExtras.SOURCE_ID_KEY)
            ?: getLaunchIntExtra(FIREBASE_SOURCE_ID_KEY)
            ?: return null

        return AppLaunchNavigationRequest.OpenVocabularySourceDetail(
            sourceId = sourceId,
        )
    }

    private fun Intent.clearMnemonicGenerationLaunchExtras() {
        removeExtra(MnemonicGenerationLaunchExtras.DESTINATION_KEY)
        removeExtra(MnemonicGenerationLaunchExtras.DECK_ID_KEY)
        removeExtra(MnemonicGenerationLaunchExtras.CARD_ID_KEY)
    }

    private fun Intent.clearVocabularySourceAnalysisLaunchExtras() {
        removeExtra(VocabularySourceAnalysisLaunchExtras.SOURCE_ID_KEY)
        removeExtra(FIREBASE_SOURCE_ID_KEY)
    }

    private fun Intent.clearAppLaunchNavigationExtras() {
        removeExtra(AppLaunchNavigationExtras.DESTINATION_KEY)
        removeExtra(AppLaunchNavigationExtras.DECK_ID_KEY)
        removeExtra(AppLaunchNavigationExtras.DECK_NAME_KEY)
        removeExtra(AppLaunchNavigationExtras.CARD_ID_KEY)
    }

    @Suppress("DEPRECATION")
    private fun Intent.getLaunchIntExtra(key: String): Int? {
        return when (val value = extras?.get(key)) {
            is Int -> value
            is Long -> value
                .takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
                ?.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }
}
