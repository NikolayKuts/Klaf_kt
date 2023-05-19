package com.kuts.klaf.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.common.ifNull

@Composable
fun AdaptiveScalableBox(
    context: @Composable (adaptiveModifier: Modifier) -> Unit,
) {
    val localDensity = LocalDensity.current
    var containerSize by rememberAsMutableStateOf(value = 0.dp)
    var contentSize: Dp? by rememberAsMutableStateOf(value = null)
    var minContentSize by rememberAsMutableStateOf(value = Dp.Unspecified)
    var isContentScrollable by rememberAsMutableStateOf(value = true)
    var isContentVisible by rememberAsMutableStateOf(value = false)
    val contentVisibilityAlpha: () -> Float = { if (isContentVisible) 1F else 0F }

    Box(modifier = Modifier.alpha(contentVisibilityAlpha())) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    containerSize = localDensity.run { coordinates.size.height.toDp() }
                },
            userScrollEnabled = isContentScrollable,
        ) {
            item {
                LaunchedEffect(key1 = containerSize, key2 = contentSize) {
                    contentSize.ifNotNull { notNullChildSize ->
                        isContentVisible = true

                        contentSize = if (containerSize > notNullChildSize) {
                            isContentScrollable = false
                            containerSize
                        } else {
                            if (containerSize < minContentSize) {
                                isContentScrollable = true
                                minContentSize
                            } else {
                                isContentScrollable = false
                                containerSize
                            }
                        }
                    }
                }

                val adaptiveContentModifier = Modifier
                    .fillMaxWidth()
                    .height(contentSize ?: minContentSize)
                    .onGloballyPositioned { coordinates ->
                        val currentSize = localDensity.run { coordinates.size.height.toDp() }

                        contentSize ifNull {
                            minContentSize = currentSize
                            contentSize = currentSize
                        }
                    }

                context(adaptiveContentModifier)
            }
        }
    }
}