package com.kuts.klaf.presentation.cardTransferring.deckChoosing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.entities.Deck
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.*
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun DeckChoosingDialogView(
    decks: List<Deck>,
    eventMessage: EventMessage?,
    onConfirmClick: (Deck) -> Unit,
    onCloseClick: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable { onCloseClick() },
        dialogMode = true,
        eventContent = {
            eventMessage.ifNotNull { EventMessageView(message = it) }
        }
    ) {
        var selectedIndex by rememberAsMutableStateOf(value = 0)

        FullBackgroundDialog(
            onBackgroundClick = onCloseClick,
            topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
            mainContent = {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    var expandedState by rememberAsMutableStateOf(value = false)

                    Text(
                        text = stringResource(R.string.title_card_moving_dialog),
                        modifier = Modifier
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DeckChoosingDropdownMenu(
                        expandedState = expandedState,
                        decks = decks,
                        onDismissRequest = { expandedState = false },
                        onItemClick = { index ->
                            selectedIndex = index
                            expandedState = false
                        },
                    )

                    ChosenDeck(
                        deckName = decks[selectedIndex].name,
                        expandedState = expandedState,
                        onClick = { expandedState = !expandedState }
                    )
                }
            },

            bottomContent = {
                ConfirmationButton(onClick = { onConfirmClick(decks[selectedIndex]) })
                ClosingButton(onClick = onCloseClick)
            },
        )
    }
}

@Composable
private fun ChosenDeck(
    deckName: String,
    expandedState: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = MainTheme.colors.cardTransferringScreen.chosenDeckBoxBorder,
                shape = RoundedCornerShape(size = 4.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = deckName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MainTheme.typographies.cardTransferringScreenTextStyles.choosingContent
        )

        val rotation by animateFloatAsState(
            targetValue = if (expandedState) 180f else 0F
        )

        Icon(
            modifier = Modifier.rotate(degrees = rotation),
            painter = painterResource(id = R.drawable.ic_arrow_drop_down_24),
            contentDescription = null,
        )
    }
}

@Composable
private fun DeckChoosingDropdownMenu(
    expandedState: Boolean,
    decks: List<Deck>,
    onItemClick: (index: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = expandedState,
        onDismissRequest = onDismissRequest
    ) {
        decks.onEachIndexed { index, deck ->
            DropdownMenuItem(
                onClick = { onItemClick(index) }
            ) {
                Column {
                    Text(
                        text = deck.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MainTheme.typographies.cardTransferringScreenTextStyles.choosingContent
                    )

                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}