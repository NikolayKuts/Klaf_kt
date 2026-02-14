package com.kuts.klaf.deckList.deckNavigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.*
import com.kuts.klaf.theme.MainTheme

@Composable
fun DeckNavigationDialogView(
    deckName: String,
    eventMessage: EventMessage?,
    onDeleteDeckClick: () -> Unit,
    onRenameDeckClick: () -> Unit,
    onBrowseDeckClick: () -> Unit,
    onAddCardsClick: () -> Unit,
    onTransferCardsClick: () -> Unit,
    onRepetitionInfoClick: () -> Unit,
    onDeckManagementClick: () -> Unit,
    onCloseDialogClick: () -> Unit,
    onCraftStoryClick: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable { onCloseDialogClick() },
        dialogMode = true,
        eventContent = {
            eventMessage.ifNotNull { EventMessageView(message = it) }
        },
    ) {
        FullBackgroundDialog(
            onBackgroundClick = onCloseDialogClick,
            mainContent = {
                Column {
                    DialogTitle(deckName = deckName)
                    DialogItem(
                        textId = R.string.deck_navigation_dialog_item_delete_deck,
                        onClick = onDeleteDeckClick
                    )
                    SeparationLine()
                    DialogItem(
                        textId = R.string.deck_navigation_dialog_item_rename_deck,
                        onClick = onRenameDeckClick
                    )
                    SeparationLine()
                    DialogItem(
                        textId = R.string.deck_navigation_dialog_item_browse_cards,
                        onClick = onBrowseDeckClick
                    )
                    SeparationLine()
                    DialogItem(
                        textId = R.string.deck_navigation_dialog_item_add_cards,
                        onClick = onAddCardsClick
                    )
                    SeparationLine()
                    DialogItem(
                        textId = R.string.deck_navigation_dialog_item_transfer_cards,
                        onClick = onTransferCardsClick
                    )
                    SeparationLine()
                    DialogItem(
                        textId = R.string.deck_navigation_dialog_item_info,
                        onClick = onRepetitionInfoClick
                    )
                    SeparationLine()
                    DialogItem(
                        textId = R.string.deck_navigation_dialog_deck_management,
                        onClick = onDeckManagementClick
                    )
                    SeparationLine()
                    DialogItem(
                        textId = R.string.deck_navigation_dialog_item_copy_craft_story,
                        onClick = onCraftStoryClick
                    )
                }
            },
            bottomContent = {
                RoundButton(
                    background = MainTheme.colors.common.neutralDialogButton,
                    iconId = R.drawable.ic_close_24,
                    onClick = onCloseDialogClick
                )
            }
        )
    }
}

@Composable
private fun ColumnScope.DialogTitle(deckName: String) {
    Text(
        modifier = Modifier
            .align(alignment = Alignment.CenterHorizontally)
            .padding(bottom = 16.dp),
        text = deckName,
        style = MainTheme.typographies.deckNavigationDialogTitle,
    )
}

@Composable
private fun DialogItem(
    @StringRes textId: Int,
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(id = textId),
        modifier = Modifier
            .width(MinElementWidth)
            .clickable { onClick() }
            .padding(top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SeparationLine() {
    Spacer(
        modifier = Modifier
            .width(400.dp)
            .height(1.dp)
            .background(MainTheme.colors.common.separator)
    )
}