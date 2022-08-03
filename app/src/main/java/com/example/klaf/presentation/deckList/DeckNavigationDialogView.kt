package com.example.klaf.presentation.deckList

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.klaf.R
import com.example.klaf.presentation.common.DIALOG_BUTTON_SIZE
import com.example.klaf.presentation.common.DialogButton
import com.example.klaf.presentation.theme.MainTheme


@Composable
fun DeckNavigationDialogView(
    deckName: String,
    onDeleteDeckClick: () -> Unit,
    onRenameDeckClick: () -> Unit,
    onBrowseDeckClick: () -> Unit,
    onAddCardsClick: () -> Unit,
    onCloseDialogClick: () -> Unit,
) {
    Box {
        Card(
            modifier = Modifier
                .defaultMinSize(minHeight = 300.dp, minWidth = 300.dp)
                .padding(bottom = (DIALOG_BUTTON_SIZE / 2).dp)
        ) {
            Column(
                Modifier.padding(MainTheme.dimensions.dialogContentPadding)
            ) {
                Text(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp),
                    text = deckName,
                    style = MainTheme.typographies.materialTypographies.body1.copy(
                        fontSize = 30.sp,
                        fontStyle = FontStyle.Italic
                    ),
                )

                DialogItem(textId = R.string.deck_delete, onClick = onDeleteDeckClick)
                SeparationLine()
                DialogItem(textId = R.string.rename_deck, onClick = onRenameDeckClick)
                SeparationLine()
                DialogItem(textId = R.string.browse_cards, onClick = onBrowseDeckClick)
                SeparationLine()
                DialogItem(textId = R.string.add_cards, onClick = onAddCardsClick)
            }
        }
        Box(modifier = Modifier.align(alignment = Alignment.BottomCenter)) {
            DialogButton(
                background = MainTheme.colors.neutralDialogButton,
                iconId = R.drawable.ic_close_24,
                onClick = onCloseDialogClick
            )
        }
    }
}

@Composable
private fun DialogItem(
    @StringRes textId: Int,
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(id = textId),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SeparationLine() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x2DFFFFFF))
    )
}