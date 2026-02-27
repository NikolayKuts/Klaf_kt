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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessageView
import com.kuts.klaf.common.MainViewModel
import com.kuts.klaf.navigation.AppLaunchNavigationRequest
import com.kuts.klaf.navigation.KlafNavHost
import com.kuts.klaf.navigation.toAppLaunchNavigationRequest
import com.kuts.klaf.theme.MainTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : AppCompatActivity() {

    private val sharedViewModel: BaseMainViewModel by viewModels<MainViewModel>()

    private val launchRequests = MutableSharedFlow<AppLaunchNavigationRequest>(
        extraBufferCapacity = 1,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            MainTheme {
                setStatusBarColor()

                Box(modifier = Modifier.fillMaxSize()) {
                    KlafNavHost(
                        sharedViewModel = sharedViewModel,
                        initialLaunchRequest = intent.toAppLaunchNavigationRequest(),
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
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        intent.toAppLaunchNavigationRequest()?.let { request -> launchRequests.tryEmit(request) }
    }

    @Composable
    private fun setStatusBarColor() {
        window.statusBarColor = MainTheme.colors.common.statusBarBackground.toArgb()
    }
}
