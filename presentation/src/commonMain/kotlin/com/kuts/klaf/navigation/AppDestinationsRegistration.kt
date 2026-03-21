package com.kuts.klaf.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.authentication.AuthenticationActionResult
import com.kuts.klaf.authentication.AuthenticationScreen
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionScreen
import com.kuts.klaf.cardManagement.cardEditing.CardEditingScreen
import com.kuts.klaf.cardTransferring.cardDeleting.CardTransferringDeletingDialog
import com.kuts.klaf.cardTransferring.common.CardTransferringScreen
import com.kuts.klaf.cardTransferring.deckChoosing.CardMovingDialog
import com.kuts.klaf.cardViewing.CardViewingScreen
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.common.externalActions.IExternalAppActions
import com.kuts.klaf.deckList.common.DeckListScreen
import com.kuts.klaf.deckList.dataSynchronization.DataSynchronizationDialog
import com.kuts.klaf.deckList.deckCreation.DeckCreationDialog
import com.kuts.klaf.deckList.deckDeleting.DeckDeletingDialog
import com.kuts.klaf.deckList.deckNavigation.DeckNavigationDialog
import com.kuts.klaf.deckList.deckRenaming.DeckRenamingDialog
import com.kuts.klaf.deckList.drawer.DrawerAction
import com.kuts.klaf.deckList.drawer.DrawerActionDialog
import com.kuts.klaf.deckList.sygningTypeChoosing.SigningTypeChoosingDialog
import com.kuts.klaf.deckManagment.DeckManagementScreen
import com.kuts.klaf.deckRepetition.DeckRepetitionScreen
import com.kuts.klaf.deckRepetition.cardDeleting.DeckRepetitionCardDeletingDialog
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoDialog
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent
import com.kuts.klaf.navigation.navType.enumNavTypeMap
import com.kuts.klaf.navigation.navType.serializableNavTypeMap
import com.kuts.klaf.webContent.WebContentScreen
import com.kuts.klaf.webContent.WebContentSource

internal fun NavGraphBuilder.registerAppDestinations(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    externalAppActions: IExternalAppActions,
    onRestartApp: () -> Unit,
) {
    buildComposableWithEntry<AppDestination.DeckList> { backStackEntry ->
        DeckListScreen(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            externalAppActions = externalAppActions,
            onRestartApp = onRestartApp,
        )
    }

    buildDialogWithEntry<AppDestination.DeckCreationDialog> { backStackEntry ->
        DeckCreationDialog(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
        )
    }

    buildDialog<AppDestination.DeckNavigationDialog> { backStackEntry, route ->
        DeckNavigationDialog(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
            deckName = route.deckName,
        )
    }

    buildDialog<AppDestination.DeckRenamingDialog> { backStackEntry, route ->
        DeckRenamingDialog(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
        )
    }

    buildDialog<AppDestination.DeckDeletingDialog> { backStackEntry, route ->
        DeckDeletingDialog(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
            deckName = route.deckName,
        )
    }

    buildComposable<AppDestination.DeckRepetition> { backStackEntry, route ->
        DeckRepetitionScreen(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
            deckName = route.deckName,
        )
    }

    buildDialog<AppDestination.DeckRepetitionCardDeletingDialog> { route ->
        DeckRepetitionCardDeletingDialog(
            navController = navController,
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
            cardId = route.cardId,
        )
    }

    buildDialog<AppDestination.DeckRepetitionInfoDialog>(
        typeMap = enumNavTypeMap<RepetitionInfoEvent>(),
    ) { route ->
        DeckRepetitionInfoDialog(
            navController = navController,
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
            deckName = route.deckName,
            repetitionInfoEvent = route.repetitionInfoEvent,
        )
    }

    buildComposable<AppDestination.CardAddition> { backStackEntry, route ->
        CardAdditionScreen(
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            externalAppActions = externalAppActions,
            deckId = route.deckId,
        )
    }

    buildComposable<AppDestination.CardEditing> { backStackEntry, route ->
        CardEditingScreen(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
            cardId = route.cardId,
        )
    }

    buildComposable<AppDestination.CardViewing> { route ->
        CardViewingScreen(
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
        )
    }

    buildDialog<AppDestination.DataSynchronizationDialog>(
        typeMap = enumNavTypeMap<AuthenticationAction>(includeNullable = true),
    ) { backStackEntry, route ->
        DataSynchronizationDialog(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            authenticationActionResult = route.authenticationAction?.let { action ->
                AuthenticationActionResult(
                    action = action,
                    isSuccessful = route.isSuccessful,
                )
            },
        )
    }

    buildDialog<AppDestination.SigningTypeChoosingDialog>(
        typeMap = enumNavTypeMap<NavigationDestination>(),
    ) { route ->
        SigningTypeChoosingDialog(
            navController = navController,
            fromSourceDestination = route.fromSourceDestination,
        )
    }

    buildComposable<AppDestination.Authentication>(
        typeMap = enumNavTypeMap<AuthenticationAction>() + enumNavTypeMap<NavigationDestination>(),
    ) { route ->
        AuthenticationScreen(
            navController = navController,
            sharedViewModel = sharedViewModel,
            authenticationAction = route.authenticationAction,
            fromSourceDestination = route.fromSourceDestination,
        )
    }

    buildDialog<AppDestination.DrawerActionDialog>(
        typeMap = enumNavTypeMap<DrawerAction>(),
    ) { backStackEntry, route ->
        DrawerActionDialog(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            drawerAction = route.drawerAction,
        )
    }

    buildComposable<AppDestination.CardTransferring> { backStackEntry, route ->
        CardTransferringScreen(
            navController = navController,
            backStackEntry = backStackEntry,
            sharedViewModel = sharedViewModel,
            sourceDeckId = route.sourceDeckId,
        )
    }

    buildDialog<AppDestination.CardTransferringDeletingDialog> { route ->
        CardTransferringDeletingDialog(
            navController = navController,
            sharedViewModel = sharedViewModel,
            cardQuantity = route.cardQuantity,
        )
    }

    buildDialogWithEntry<AppDestination.CardMovingDialog> {
        CardMovingDialog(
            navController = navController,
            sharedViewModel = sharedViewModel,
        )
    }

    buildComposable<AppDestination.DeckManagement> { route ->
        DeckManagementScreen(
            sharedViewModel = sharedViewModel,
            deckId = route.deckId,
        )
    }

    buildComposable<AppDestination.WebContent>(
        typeMap = serializableNavTypeMap<WebContentSource>(),
    ) { backStackEntry, route ->
        WebContentScreen(
            navController = navController,
            backStackEntry = backStackEntry,
            source = route.source,
        )
    }
}
