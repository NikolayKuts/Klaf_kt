package com.kuts.klaf.vocabularySource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.kuts.klaf.common.verticalScrollbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class VocabularySourceScrollbarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun scrollbarMovesPastMiddleWithPinnedHeader() {
        val state = LazyListState()
        lateinit var scope: CoroutineScope
        compose.setContent {
            scope = rememberCoroutineScope()
            LazyColumn(
                state = state,
                modifier = Modifier.size(300.dp).background(Color.White)
                    .verticalScrollbar(state, width = 8.dp, color = Color.Red,
                        maxAlpha = 1f, alwaysVisible = true)
                    .testTag("list"),
            ) {
                stickyHeader { Box(Modifier.fillMaxWidth().height(30.dp).background(Color.White)) }
                items(100) { Box(Modifier.fillMaxWidth().height(100.dp)) }
            }
        }
        compose.runOnIdle { scope.launch { state.scrollToItem(70) } }
        compose.waitForIdle()
        val pixels = compose.onNodeWithTag("list").captureToImage().toPixelMap()
        val redRows = (0 until pixels.height).filter { y ->
            val pixel = pixels[pixels.width - 2, y]
            pixel.red > 0.9f && pixel.green < 0.1f
        }
        assertTrue(redRows.isNotEmpty(), "Scrollbar must be visible")
        assertTrue(redRows.first() > pixels.height / 2, "Scrollbar must move past the midpoint: $redRows")
    }
}
