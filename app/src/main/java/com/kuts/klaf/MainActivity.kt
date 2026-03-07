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
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessageView
import com.kuts.klaf.common.MainViewModel
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

    private val sharedViewModel: BaseMainViewModel by viewModels<MainViewModel>()
    private val notificationPermissionBinder: INotificationPermissionBinder by inject()
    private val notificationPermissionManager: INotificationPermissionManager by inject()
    private var shouldCheckNotificationPermission by mutableStateOf(false)

    private val launchRequests = MutableSharedFlow<AppLaunchNavigationRequest>(
        extraBufferCapacity = 1,
    )

    companion object {
        private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        notificationPermissionBinder.bind(activity = this)

        setContent {
            MainTheme {
                setStatusBarColor()

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidKlafNavHost(
                        sharedViewModel = sharedViewModel,
                        initialLaunchRequest = intent.toLaunchNavigationRequest(),
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

        setIntent(intent)
        intent.toLaunchNavigationRequest()?.let { request -> launchRequests.tryEmit(request) }
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
                    val deckId = getIntExtra(
                        AppLaunchNavigationExtras.DECK_ID_KEY,
                        AppLaunchNavigationExtras.DEFAULT_DECK_ID,
                    )
                    val deckName = getStringExtra(AppLaunchNavigationExtras.DECK_NAME_KEY)
                        ?: AppLaunchNavigationExtras.DEFAULT_DECK_NAME

                    AppLaunchNavigationRequest.OpenDeckRepetition(
                        deckId = deckId,
                        deckName = deckName,
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
}
