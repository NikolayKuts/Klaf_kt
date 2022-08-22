package com.example.klaf.data.common

import android.content.Context
import android.net.Uri
import com.example.klaf.data.room.entities.CARD_TABLE_NAME
import com.example.klaf.data.room.entities.RoomDeck.Companion.DECK_TABLE_NAME
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.OldAppKlafDataTransferRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OldAppKlafDataTransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
) : OldAppKlafDataTransferRepository {

    companion object {

        private const val oldKlafAppAuthorities = "com.example.testpoject"
    }

    override suspend fun transferOldData() {
        transferDecks()
        transferCards()
    }

    private suspend fun transferDecks() {
        withContext(Dispatchers.IO) {
            val cursor = context.contentResolver.query(
                Uri.parse("content://$oldKlafAppAuthorities/$DECK_TABLE_NAME"),
                null,
                null,
                null,
                null,
                null,
            )

            cursor ?: throw Exception(
                "Transferring decks form old Klaf app is failed. Returned cursor is null"
            )

//            TODO("refactor")
            while (cursor.moveToNext()) {
                val scheduledDate = cursor.getLong(cursor.getColumnIndex(Deck::scheduledDate.name))
                val lastRepeatDate = cursor.getLong(cursor.getColumnIndex("lastRepeatDate"))
                val scheduledDateInterval = lastRepeatDate - scheduledDate

                val deck = Deck(
                    name = cursor.getString(cursor.getColumnIndex(Deck::name.name)),
                    creationDate = cursor.getLong(cursor.getColumnIndex(Deck::creationDate.name)),
                    repetitionIterationDates = listOf(),
                    scheduledIterationDates = listOf(scheduledDate),
                    scheduledDateInterval = scheduledDateInterval,
                    repetitionQuantity = cursor.getInt(cursor.getColumnIndex("repeatQuantity")),
                    cardQuantity = cursor.getInt(cursor.getColumnIndex(Deck::cardQuantity.name)),
                    lastFirstRepetitionDuration = 0,
                    lastSecondRepetitionDuration = 0,
                    lastRepetitionIterationDuration =
                    cursor.getInt(cursor.getColumnIndex("lastRepeatDuration")).toLong(),
                    isLastIterationSucceeded =
                    cursor.getInt(cursor.getColumnIndex("isLastRepetitionSucceeded")) > 0,
                    id = cursor.getInt(cursor.getColumnIndex(Deck::id.name)),
                )

                deckRepository.insertDeck(deck = deck)
            }

            cursor.close()
        }
    }

    private suspend fun transferCards() {
        withContext(Dispatchers.IO) {
            val cursor = context.contentResolver.query(
                Uri.parse("content://$oldKlafAppAuthorities/$CARD_TABLE_NAME"),
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
                    ipa = cursor.getString(cursor.getColumnIndex(Card::ipa.name)),
                    id = cursor.getInt(cursor.getColumnIndex(Card::id.name)),
                )

                cardRepository.insertCard(card = card)
            }

            cursor.close()
        }
    }
}