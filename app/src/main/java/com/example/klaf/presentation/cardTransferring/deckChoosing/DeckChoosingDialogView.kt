package com.example.klaf.presentation.cardTransferring.deckChoosing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.domain.entities.Deck
import com.example.klaf.presentation.cardTransferring.common.BaseCardTransferringViewModel
import com.example.klaf.presentation.common.ClosingButton
import com.example.klaf.presentation.common.ConfirmationButton
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DeckChoosingDialogView(
    viewModel: BaseCardTransferringViewModel,
    onCloseClick: () -> Unit,
) {
    val decks by viewModel.decks.collectAsState()
    var selectedIndex by rememberAsMutableStateOf(value = 0)

    FullBackgroundDialog(
        onBackgroundClick = onCloseClick,
        topContent = {},
        mainContent = {
            Column {
                var expandedState by rememberAsMutableStateOf(value = false)

                Text(
                    text = stringResource(R.string.title_card_moving_dialog),
                    modifier = Modifier
                        .fillMaxWidth()
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
            ConfirmationButton(
                onClick = { viewModel.moveCards(targetDeck = decks[selectedIndex]) }
            )
            ClosingButton(onClick = onCloseClick)
        },
    )
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