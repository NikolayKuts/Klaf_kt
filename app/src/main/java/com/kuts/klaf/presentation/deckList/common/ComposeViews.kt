package com.kuts.klaf.presentation.deckList.common

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.entities.Deck
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.ClosingButton
import com.kuts.klaf.presentation.common.ConfirmationButton
import com.kuts.klaf.presentation.common.ContentHolder
import com.kuts.klaf.presentation.common.DIALOG_APP_LABEL_SIZE
import com.kuts.klaf.presentation.common.DialogAppLabel
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.EventMessageView
import com.kuts.klaf.presentation.common.FullBackgroundDialog
import com.kuts.klaf.presentation.common.MinElementWidth
import com.kuts.klaf.presentation.common.ROUNDED_ELEMENT_SIZE
import com.kuts.klaf.presentation.common.ScrollableBox
import com.kuts.klaf.presentation.common.noRippleClickable
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun DeckNamingView(
    title: @Composable () -> Unit,
    onConfirmCreationClick: (deckName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String? = null,
    eventMessage: EventMessage? = null,
) {
    val deckNameState = rememberSaveable { mutableStateOf(value = initialName ?: "") }

    ScrollableBox(
        modifier = modifier.noRippleClickable { onCloseDialogClick() },
        dialogMode = true,
        eventContent = {
            eventMessage.ifNotNull { EventMessageView(message = it) }
        },
    ) {
        FullBackgroundDialog(
            onBackgroundClick = onCloseDialogClick,
            topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
            mainContent = {
                Column {
                    title()
                    Spacer(modifier = Modifier.height(16.dp))
                    DeckNameTextField(
                        deckNameState = deckNameState,
                        placeholder = { Text(text = stringResource(id = R.string.enter_deck_name)) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
            bottomContent = {
                ConfirmationButton(onClick = { onConfirmCreationClick(deckNameState.value) })
                ClosingButton(onClick = onCloseDialogClick)
            }
        )
    }
}

@Composable
internal fun AnimatedSynchronizationLabel() {
    val animationDuration = 1000
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = -360F,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )
    val color by infiniteTransition.animateColor(
        initialValue = MainTheme.colors.common.dialogBackground,
        targetValue = MainTheme.colors.dataSynchronizationView.targetLabelBackground,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    SynchronizationLabel(
        modifier = Modifier.rotate(degrees = rotation),
        color = color
    )
}

@Composable
internal fun SynchronizationLabel(
    size: Dp = ROUNDED_ELEMENT_SIZE.dp,
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.common.dialogBackground,
) {
    Card(
        modifier = Modifier
            .size(size)
            .noRippleClickable { },
        shape = RoundedCornerShape(size),
    ) {
        Icon(
            modifier = modifier
                .size(size)
                .background(color)
                .padding(8.dp),
            painter = painterResource(id = R.drawable.ic_sync_24),
            contentDescription = null,
        )
    }
}

@Composable
private fun DeckNameTextField(
    deckNameState: MutableState<String>,
    placeholder: @Composable () -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.width(MinElementWidth),
        value = deckNameState.value,
        singleLine = true,
        maxLines = 1,
        onValueChange = { updatedName ->
            if (updatedName.length <= Deck.MAX_NAME_LENGTH) {
                deckNameState.value = updatedName
            }
        },
        label = { Text(text = stringResource(R.string.deck_name_label)) },
        placeholder = placeholder,
    )
}