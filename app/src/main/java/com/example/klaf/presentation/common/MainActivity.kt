package com.example.klaf.presentation.common

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import com.example.klaf.R
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val sharedViewModel: BaseMainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
                    setStatusBarColor()
                    sharedViewModel.eventMessage.collectAsState(null)
                        .value?.let { message -> EventMessageView(message = message) }
                }
            }
        }
    }

    @Composable
    private fun setStatusBarColor() {
        window.statusBarColor = MainTheme.colors.common.statusBarBackground.toArgb()
    }
}

