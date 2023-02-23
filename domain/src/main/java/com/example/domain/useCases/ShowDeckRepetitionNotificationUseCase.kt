package com.example.domain.useCases

import com.example.domain.repositories.DeckRepetitionNotifierRepository
import javax.inject.Inject

class ShowDeckRepetitionNotificationUseCase @Inject constructor(
    private val notifier: DeckRepetitionNotifierRepository
) {

    operator fun invoke(deckName: String, deckId: Int) {
        notifier.showNotification(deckName = deckName, deckId = deckId)
    }
}