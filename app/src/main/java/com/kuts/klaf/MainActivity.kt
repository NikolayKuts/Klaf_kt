package com.kuts.klaf

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessageView
import com.kuts.klaf.common.MainViewModel
import com.kuts.klaf.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val sharedViewModel: BaseMainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                setStatusBarColor()
                sharedViewModel.eventMessage.collectAsState(null)
                    .value?.let { message -> EventMessageView(message = message) }
            }
        }
    }

    @Composable
    private fun setStatusBarColor() {
        window.statusBarColor = MainTheme.colors.common.statusBarBackground.toArgb()
    }
}
