package com.kuts.klaf.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kuts.klaf.presentation.common.EventMessage.Type.*
import com.kuts.klaf.presentation.theme.MainTheme
import kotlinx.coroutines.delay

@Composable
fun EventMessageView(
    message: EventMessage,
    modifier: Modifier = Modifier,
    animationDuration: Int = 500,
    transitionDuration: Int = 500,
    initialElevation: Int = 1,
    targetElevation: Int = 12,
) {
    val visibilityState = remember(key1 = message) { MutableTransitionState(initialState = false) }

    val colorHolder = when (message.type) {
        Negative -> MainTheme.colors.eventMessageColors.negative
        Waring -> MainTheme.colors.eventMessageColors.warning
        Neutral -> MainTheme.colors.eventMessageColors.neutral
        Positive -> MainTheme.colors.eventMessageColors.positive
    }

    val infiniteTransition = rememberInfiniteTransition()

    val elevation by infiniteTransition.animateValue(
        initialValue = initialElevation,
        targetValue = targetElevation,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    val color by infiniteTransition.animateColor(
        initialValue = colorHolder.initial,
        targetValue = colorHolder.target,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1F,
        targetValue = 1.01F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    val additionOffset = 100

    AnimatedVisibility(
        modifier = modifier.padding(32.dp),
        visibleState = visibilityState,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = transitionDuration),
            initialOffsetY = { fullWidth -> - (fullWidth + additionOffset) },
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = transitionDuration),
            targetOffsetY = { fullWidth -> -(fullWidth + additionOffset) },
        )
    ) {
        Card(
            modifier = modifier.scale(scale),
            shape = MainTheme.shapes.small,
            backgroundColor = color,
            contentColor = MainTheme.colors.material.onBackground,
            elevation = elevation.dp
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(id = message.resId),
                style = MainTheme.typographies.materialTypographies.body1
            )
        }
    }

    LaunchedEffect(key1 = message) {
        visibilityState.targetState = true
        delay(message.duration.value)
        visibilityState.targetState = false
    }
}