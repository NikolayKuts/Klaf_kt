package com.example.klaf.presentation.common

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun EventMessageView(
    message: EventMessage,
    modifier: Modifier = Modifier,
) {
    val visibilityState = remember(key1 = message) { MutableTransitionState(initialState = false) }
    val maxBoxSize = LocalConfiguration.current.screenWidthDp.dp - 64.dp

    AnimatedVisibility(
        modifier = modifier.padding(16.dp),
        visibleState = visibilityState,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 500),
            initialOffsetY = { fullWidth -> -fullWidth },
        ) + fadeIn(),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 500),
            targetOffsetY = { fullWidth -> fullWidth },
        ) + fadeOut(),
    ) {
        Card(
            modifier = modifier.widthIn(max = maxBoxSize),
            shape = MainTheme.shapes.small,
            elevation = 4.dp
        ) {
            Text(
                modifier = Modifier
                .padding(16.dp),
                text = stringResource(id = message.resId),
                style = MainTheme.typographies.materialTypographies.body1
            )
        }
    }

    LaunchedEffect(key1 = message) {
        visibilityState.targetState = true
    }
}