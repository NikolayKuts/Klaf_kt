package com.kuts.klaf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuts.klaf.room.maintenance.TemporaryCleanupReceipt
import com.kuts.klaf.room.maintenance.TemporaryCleanupResult
import com.kuts.klaf.room.maintenance.TemporaryCleanupStartupState
import com.kuts.klaf.theme.MainTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val screenCleanupResult = TemporaryCleanupStartupState.Report(
    TemporaryCleanupResult.Completed(TemporaryCleanupReceipt("Source", 379, 100, true), false),
)

@RunWith(AndroidJUnit4::class)
class TemporaryCleanupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun maintenanceTextIsReadableInDarkTheme() {
        compose.setContent {
            MainTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MainTheme.colors.material.surface)) {
                    TemporaryCleanupReport(screenCleanupResult, {}, {})
                }
            }
        }
        val pixels = compose.onNodeWithText("Временная сборка: разовая очистка Vocabulary Source")
            .captureToImage().toPixelMap()
        var lightTextPixels = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val pixel = pixels[x, y]
                if (pixel.red > 0.65f && pixel.green > 0.65f && pixel.blue > 0.65f) lightTextPixels++
            }
        }
        assertTrue("Dark maintenance screen must render readable light text", lightTextPixels > 10)
    }

    @Test
    fun continuationCanBeReachedInSmallViewport() {
        var opened = false
        compose.setContent {
            MainTheme {
                Box(Modifier.requiredSize(320.dp, 150.dp)) {
                    TemporaryCleanupReport(screenCleanupResult, { opened = true }, {})
                }
            }
        }
        compose.onNodeWithText("Открыть приложение").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue("Continuation action must remain reachable", opened) }
    }
}
