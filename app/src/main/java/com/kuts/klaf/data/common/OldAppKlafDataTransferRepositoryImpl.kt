package com.kuts.klaf.data.common

import android.content.Context
import android.net.Uri
import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.CardRepository
import com.kuts.domain.repositories.DeckRepository
import com.kuts.domain.repositories.OldAppKlafDataTransferRepository
import com.kuts.klaf.data.room.entities.RoomCard.Companion.CARD_TABLE_NAME
import com.kuts.klaf.data.room.entities.RoomDeck.Companion.DECK_TABLE_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OldAppKlafDataTransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
    @LocalCardRepositoryImp
    private val cardRepository: CardRepository,
) : OldAppKlafDataTransferRepository {

    companion object {

        private const val oldKlafAppAuthorities = "com.kuts.testpoject"
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
                val scheduledDateInterval = scheduledDate - lastRepeatDate

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
                    ipa = emptyList(),
                    id = cursor.getInt(cursor.getColumnIndex(Card::id.name)),
                )

                cardRepository.insertCard(card = card)
            }

            cursor.close()
        }
    }
}