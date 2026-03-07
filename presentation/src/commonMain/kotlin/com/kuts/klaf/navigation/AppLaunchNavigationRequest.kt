package com.kuts.klaf.navigation

import com.kuts.domain.entities.Deck

sealed interface AppLaunchNavigationRequest {

    data object OpenDeckList : AppLaunchNavigationRequest

    data object OpenInterimCardAddition : AppLaunchNavigationRequest

    data class OpenDeckRepetition(
        val deckId: Int,
        val deckName: String,
    ) : AppLaunchNavigationRequest
}

object AppLaunchNavigationExtras {

    const val DESTINATION_KEY = "launch_destination"
    const val DECK_ID_KEY = "launch_deck_id"
    const val DECK_NAME_KEY = "launch_deck_name"

    const val DESTINATION_DECK_LIST = "deck_list"
    const val DESTINATION_INTERIM_CARD_ADDITION = "interim_card_addition"
    const val DESTINATION_DECK_REPETITION = "deck_repetition"

    const val DEFAULT_DECK_ID = Deck.INTERIM_DECK_ID
    const val DEFAULT_DECK_NAME = Deck.INTERIM_DECK_NAME
}
