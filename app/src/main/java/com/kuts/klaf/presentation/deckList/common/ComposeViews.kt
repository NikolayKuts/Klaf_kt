package com.kuts.klaf.presentation.deckList.common

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.entities.Deck
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.*
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
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.common.dialogBackground,
) {
    Card(
        modifier = Modifier
            .size(ROUNDED_ELEMENT_SIZE.dp)
            .noRippleClickable { },
        shape = RoundedCornerShape(ROUNDED_ELEMENT_SIZE.dp),
        elevation = 0.dp,
    ) {
        Icon(
            modifier = modifier
                .size(20.dp)
                .background(color)
                .padding(8.dp)
                .then(modifier),
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