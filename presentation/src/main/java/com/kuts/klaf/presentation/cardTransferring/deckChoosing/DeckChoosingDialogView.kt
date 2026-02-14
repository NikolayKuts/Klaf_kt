package com.kuts.klaf.presentation.cardTransferring.deckChoosing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.kuts.klaf.presentation.R
import com.kuts.klaf.presentation.common.ClosingButton
import com.kuts.klaf.presentation.common.ConfirmationButton
import com.kuts.klaf.presentation.common.ContentHolder
import com.kuts.klaf.presentation.common.DIALOG_APP_LABEL_SIZE
import com.kuts.klaf.presentation.common.DialogAppLabel
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.EventMessageView
import com.kuts.klaf.presentation.common.FullBackgroundDialog
import com.kuts.klaf.presentation.common.ScrollableBox
import com.kuts.klaf.presentation.common.noRippleClickable
import com.kuts.klaf.presentation.common.rememberAsMutableStateOf
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
                onClick = { onItemClick(index) },
                text = {
                    Column {
                        Text(
                            text = deck.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MainTheme.typographies.cardTransferringScreenTextStyles.choosingContent
                        )

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                    }
                }
            )
        }
    }
}