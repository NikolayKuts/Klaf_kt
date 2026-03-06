package com.kuts.klaf.deckList.deckNavigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.domain.common.ifNotNull
import com.kuts.klaf.common.*
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent.Non
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DeckNavigationDialog(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)

    DeckNavigationDialogContent(
        deckName = deckName,
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
        onDeleteDeckClick = {
            navController.navigateAndCloseCurrentDialog(
                backStackEntry = backStackEntry,
                destination = AppDestination.DeckDeletingDialog(
                    deckId = deckId,
                    deckName = deckName,
                )
            )
        },
        onRenameDeckClick = {
            navController.navigateAndCloseCurrentDialog(
                backStackEntry = backStackEntry,
                destination = AppDestination.DeckRenamingDialog(deckId = deckId)
            )
        },
        onBrowseDeckClick = {
            navController.navigateAndCloseCurrentDialog(
                backStackEntry = backStackEntry,
                destination = AppDestination.CardViewing(deckId = deckId, deckName = deckName)
            )
        },
        onAddCardsClick = {
            navController.navigateAndCloseCurrentDialog(
                backStackEntry = backStackEntry,
                destination = AppDestination.CardAddition(deckId = deckId)
            )
        },
        onTransferCardsClick = {
            navController.navigateAndCloseCurrentDialog(
                backStackEntry = backStackEntry,
                destination = AppDestination.CardTransferring(sourceDeckId = deckId)
            )
        },
        onRepetitionInfoClick = {
            navController.navigateAndCloseCurrentDialog(
                backStackEntry = backStackEntry,
                destination = AppDestination.DeckRepetitionInfoDialog(
                    deckId = deckId,
                    deckName = deckName,
                    repetitionInfoEvent = Non,
                )
            )
        },
        onDeckManagementClick = {
            navController.navigateAndCloseCurrentDialog(
                backStackEntry = backStackEntry,
                destination = AppDestination.DeckManagement(deckId = deckId)
            )
        },
        onCloseDialogClick = { navController.popBackStack() },
        onCraftStoryClick = { viewModel.generateGptPromptWithDeckContent(deckId = deckId) },
    )
}

private fun NavHostController.navigateAndCloseCurrentDialog(
    backStackEntry: NavBackStackEntry,
    destination: AppDestination
) {
    this.currentBackStackEntry
    navigate(route = destination) {
        popUpTo(id = backStackEntry.destination.id) {
            inclusive = true
        }
    }
}

@Composable
private fun DeckNavigationDialogContent(
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
                        textRes = Res.string.deck_navigation_dialog_item_delete_deck,
                        onClick = onDeleteDeckClick
                    )
                    SeparationLine()
                    DialogItem(
                        textRes = Res.string.deck_navigation_dialog_item_rename_deck,
                        onClick = onRenameDeckClick
                    )
                    SeparationLine()
                    DialogItem(
                        textRes = Res.string.deck_navigation_dialog_item_browse_cards,
                        onClick = onBrowseDeckClick
                    )
                    SeparationLine()
                    DialogItem(
                        textRes = Res.string.deck_navigation_dialog_item_add_cards,
                        onClick = onAddCardsClick
                    )
                    SeparationLine()
                    DialogItem(
                        textRes = Res.string.deck_navigation_dialog_item_transfer_cards,
                        onClick = onTransferCardsClick
                    )
                    SeparationLine()
                    DialogItem(
                        textRes = Res.string.deck_navigation_dialog_item_info,
                        onClick = onRepetitionInfoClick
                    )
                    SeparationLine()
                    DialogItem(
                        textRes = Res.string.deck_navigation_dialog_deck_management,
                        onClick = onDeckManagementClick
                    )
                    SeparationLine()
                    DialogItem(
                        textRes = Res.string.deck_navigation_dialog_item_copy_craft_story,
                        onClick = onCraftStoryClick
                    )
                }
            },
            bottomContent = {
                RoundButton(
                    background = MainTheme.colors.common.neutralDialogButton,
                    iconRes = Res.drawable.ic_close_24,
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
    textRes: StringResource,
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(resource = textRes),
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
