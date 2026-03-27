package com.kuts.klaf.common

import android.content.Context
import android.net.Uri
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME
import com.kuts.klaf.room.entities.RoomDeck.Companion.DECK_TABLE_NAME

class AndroidOldAppKlafDataTransferRepository(
    private val context: Context,
    private val deckRepository: IDeckRepository,
    private val cardRepository: ICardRepository,
) : IOldAppKlafDataTransferRepository {

    companion object {
        private const val OLD_KLAF_APP_AUTHORITIES = "com.kuts.testpoject"
    }

    override suspend fun transferOldData() {
        transferDecks()
        transferCards()
    }

    private suspend fun transferDecks() {
        val cursor = context.contentResolver.query(
            Uri.parse("content://$OLD_KLAF_APP_AUTHORITIES/$DECK_TABLE_NAME"),
            null,
            null,
            null,
            null,
            null,
        )

        cursor ?: throw Exception(
            "Transferring decks form old Klaf app is failed. Returned cursor is null"
        )

        while (cursor.moveToNext()) {
            val scheduledDate = cursor.getLong(cursor.getColumnIndex(Deck::scheduledDate.name))
            val lastRepeatDate = cursor.getLong(cursor.getColumnIndex("lastRepeatDate"))
            val scheduledDateInterval = scheduledDate - lastRepeatDate

            val deck = Deck(
                name = cursor.getString(cursor.getColumnIndex(Deck::name.name)),
                creationDate = cursor.getLong(cursor.getColumnIndex(Deck::creationDate.name)),
                reviewPassDates = listOf(),
                scheduledReviewDates = listOf(scheduledDate),
                scheduledDateInterval = scheduledDateInterval,
                reviewCount = cursor.getInt(cursor.getColumnIndex("repeatQuantity")),
                cardQuantity = cursor.getInt(cursor.getColumnIndex(Deck::cardQuantity.name)),
                lastFirstReviewDuration = 0,
                lastSecondReviewDuration = 0,
                lastReviewPassDuration =
                cursor.getInt(cursor.getColumnIndex("lastRepeatDuration")).toLong(),
                isLastPassSucceeded =
                cursor.getInt(cursor.getColumnIndex("isLastRepetitionSucceeded")) > 0,
                id = cursor.getInt(cursor.getColumnIndex(Deck::id.name)),
            )

            deckRepository.insertDeck(deck = deck)
        }

        cursor.close()
    }

    private suspend fun transferCards() {
        val cursor = context.contentResolver.query(
            Uri.parse("content://$OLD_KLAF_APP_AUTHORITIES/$CARD_TABLE_NAME"),
            null,
            null,
            null,
            null,
            null,
        )

        cursor ?: throw Exception(
            "Transferring decks form old Klaf app is failed. Returned cursor is null"
        )

        while (cursor.moveToNext()) {
            val card = Card(
                deckId = cursor.getInt(cursor.getColumnIndex(Card::deckId.name)),
                nativeWord = cursor.getString(cursor.getColumnIndex(Card::nativeWord.name)),
                foreignWord = cursor.getString(cursor.getColumnIndex(Card::foreignWord.name)),
                ipa = emptyList(),
                id = cursor.getInt(cursor.getColumnIndex(Card::id.name)),
            )

            cardRepository.insertCard(card = card)
        }

        cursor.close()
    }
}
