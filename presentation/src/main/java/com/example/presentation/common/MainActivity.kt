package com.example.presentation.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import com.example.klaf.R
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface { setStatusBarColor() }
            }
        }
    }

    @Composable
    private fun setStatusBarColor() {
        window.statusBarColor = MainTheme.colors.statusBarBackground.toArgb()
    }
}