package com.kuts.klaf.navigation

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.entities.Deck
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.deckList.drawer.DrawerAction
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent.Non
import com.kuts.klaf.webContent.WebContentSource
import kotlinx.serialization.Serializable

internal sealed interface AppDestination {

    @Serializable
    data object DeckList : AppDestination

    @Serializable
    data object DeckCreationDialog : AppDestination

    @Serializable
    data class DeckNavigationDialog(
        val deckId: Int,
        val deckName: String,
    ) : AppDestination

    @Serializable
    data class DeckRenamingDialog(val deckId: Int) : AppDestination

    @Serializable
    data class DeckDeletingDialog(
        val deckId: Int,
        val deckName: String,
    ) : AppDestination

    @Serializable
    data class DeckRepetition(
        val deckId: Int,
        val deckName: String,
    ) : AppDestination

    @Serializable
    data class DeckRepetitionCardDeletingDialog(
        val deckId: Int,
        val cardId: Int,
    ) : AppDestination

    @Serializable
    data class DeckRepetitionInfoDialog(
        val deckId: Int,
        val deckName: String,
        val repetitionInfoEvent: RepetitionInfoEvent = Non,
    ) : AppDestination

    @Serializable
    data class CardAddition(
        val deckId: Int = Deck.INTERIM_DECK_ID,
    ) : AppDestination

    @Serializable
    data class CardEditing(
        val deckId: Int,
        val cardId: Int,
    ) : AppDestination

    @Serializable
    data class CardViewing(
        val deckId: Int,
        val deckName: String,
    ) : AppDestination

    @Serializable
    data class DataSynchronizationDialog(
        val authenticationAction: AuthenticationAction? = null,
        val isSuccessful: Boolean = false,
    ) : AppDestination

    @Serializable
    data class SigningTypeChoosingDialog(
        val fromSourceDestination: NavigationDestination,
    ) : AppDestination

    @Serializable
    data class Authentication(
        val authenticationAction: AuthenticationAction,
        val fromSourceDestination: NavigationDestination,
    ) : AppDestination

    @Serializable
    data class DrawerActionDialog(
        val drawerAction: DrawerAction,
    ) : AppDestination

    @Serializable
    data class CardTransferring(
        val sourceDeckId: Int,
    ) : AppDestination

    @Serializable
    data class CardTransferringDeletingDialog(
        val cardQuantity: Int,
    ) : AppDestination

    @Serializable
    data object CardMovingDialog : AppDestination

    @Serializable
    data class DeckManagement(
        val deckId: Int,
    ) : AppDestination

    @Serializable
    data class WebContent(
        val source: WebContentSource,
    ) : AppDestination
}
