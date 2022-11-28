package com.example.klaf.presentation.interimDeck.cardMoving

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.interimDeck.common.BaseInterimDeckViewModel
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardMovingDialogView(
    viewModel: BaseInterimDeckViewModel,
    onCloseClick: () -> Unit,
) {
    val decks by viewModel.decks.collectAsState()
    var selectedIndex by rememberAsMutableStateOf(value = 0)

    FullBackgroundDialog(
        onBackgroundClick = onCloseClick,
        mainContent = {
            Column {
                var expendedState by rememberAsMutableStateOf(value = false)

                Text(
                    text = stringResource(R.string.title_card_moving_dialog),
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = decks[selectedIndex].name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { expendedState = true })
                        .border(
                            width = 2.dp,
                            color = Color(0xFF64A4AC),
                            shape = RoundedCornerShape(size = 4.dp)
                        )
                        .padding(8.dp),
                )

                DropdownMenu(
                    expanded = expendedState,
                    onDismissRequest = { expendedState = false }
                ) {
                    decks.onEachIndexed { index, deck ->
                        DropdownMenuItem(
                            onClick = {
                                selectedIndex = index
                                expendedState = false
                            }
                        ) {
                            Text(text = deck.name)
                        }
                    }
                }
            }
        },
        buttonContent = {
            ConfirmationButton(
                onClick = { viewModel.moveCards(targetDeck = decks[selectedIndex]) }
            )
            ClosingButton(onClick = onCloseClick)
        },
    )
}