package com.example.klaf.data.common

import android.content.Context
import android.net.Uri
import com.example.klaf.data.room.entities.CARD_TABLE_NAME
import com.example.klaf.data.room.entities.RoomDeck.Companion.DECK_TABLE_NAME
import com.example.klaf.domain.entities.Card
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

            TODO("refactor")
//            while (cursor.moveToNext()) {
//                val deck = Deck(
//                    name = cursor.getString(cursor.getColumnIndex(Deck::name.name)),
//                    creationDate = cursor.getLong(cursor.getColumnIndex(Deck::creationDate.name)),
//                    id = cursor.getInt(cursor.getColumnIndex(Deck::id.name)),
//                    cardQuantity = cursor.getInt(cursor.getColumnIndex(Deck::cardQuantity.name)),
//                    repeatDay = cursor.getInt(cursor.getColumnIndex(Deck::repeatDay.name)),
//                    scheduledDate = cursor.getLong(cursor.getColumnIndex(Deck::scheduledDate.name)),
//                    lastRepeatDate = cursor.getLong(cursor.getColumnIndex(Deck::lastRepeatDate.name)),
//                    repeatQuantity = cursor.getInt(cursor.getColumnIndex(Deck::repeatQuantity.name)),
//                    lastRepeatDuration =
//                    cursor.getInt(cursor.getColumnIndex(Deck::lastRepeatDuration.name)).toLong(),
//                    isLastRepetitionSucceeded =
//                    cursor.getInt(cursor.getColumnIndex(Deck::isLastRepetitionSucceeded.name)) > 0
//                )
//
//                deckRepository.insertDeck(deck = deck)
//            }
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
        }
    }
}