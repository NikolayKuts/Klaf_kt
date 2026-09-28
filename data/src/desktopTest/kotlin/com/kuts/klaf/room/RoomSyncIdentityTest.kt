package com.kuts.klaf.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.useCases.TransferCardsToDeckUseCase
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.dao.RoomCardLocalUpdate
import com.kuts.klaf.room.dao.RoomDeckLocalUpdate
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.runBlocking

class RoomSyncIdentityTest {

    @Test
    fun `new decks and cards receive distinct stable sync IDs on first save`() = withDatabase { database ->
        val decks = DeckRepositoryRoom(database)
        val cards = CardRepositoryRoom(database)
        val firstDeckId = decks.insertDeck(Deck(name = "first", creationDate = 1L))
        val secondDeckId = decks.insertDeck(Deck(name = "second", creationDate = 2L))
        val firstCardId = cards.insertCard(card(deckId = firstDeckId, foreignWord = "one"))
        val secondCardId = cards.insertCard(card(deckId = firstDeckId, foreignWord = "two"))

        val firstDeck = requireNotNull(decks.getDeckById(firstDeckId))
        val secondDeck = requireNotNull(decks.getDeckById(secondDeckId))
        val firstCard = cards.fetchAllCards().single { it.id == firstCardId }
        val secondCard = cards.fetchAllCards().single { it.id == secondCardId }

        assertTrue(listOf(firstDeck, secondDeck).all { it.syncId.isNotBlank() })
        assertTrue(listOf(firstCard, secondCard).all { it.syncId.isNotBlank() })
        assertEquals(4, setOf(firstDeck.syncId, secondDeck.syncId, firstCard.syncId, secondCard.syncId).size)
        assertEquals(0L, firstDeck.lastChangedServerRevision)
        assertEquals(0L, firstCard.lastChangedServerRevision)
    }

    @Test
    fun `indexed sync IDs locate one exact deck and card`() = withDatabase { database ->
        val decks = DeckRepositoryRoom(database)
        val cards = CardRepositoryRoom(database)
        val firstDeckId = decks.insertDeck(Deck(name = "first", creationDate = 1L))
        decks.insertDeck(Deck(name = "second", creationDate = 2L))
        val cardId = cards.insertCard(card(deckId = firstDeckId, foreignWord = "one"))
        cards.insertCard(card(deckId = firstDeckId, foreignWord = "two"))
        val deckRow = requireNotNull(database.deckDao().getDeckById(firstDeckId))
        val cardRow = requireNotNull(database.cardDao().getCardById(cardId))

        assertEquals(deckRow, database.deckDao().getDeckBySyncId(deckRow.syncId))
        assertEquals(cardRow, database.cardDao().getCardBySyncId(cardRow.syncId))
        assertEquals(null, database.deckDao().getDeckBySyncId("missing-deck"))
        assertEquals(null, database.cardDao().getCardBySyncId("missing-card"))
    }

    @Test
    fun `ordinary edits and card moves preserve IDs and last server revisions`() = withDatabase { database ->
        val decks = DeckRepositoryRoom(database)
        val cards = CardRepositoryRoom(database)
        val sourceId = decks.insertDeck(Deck(name = "source", creationDate = 1L))
        val destinationId = decks.insertDeck(Deck(name = "destination", creationDate = 2L))
        val cardId = cards.insertCard(card(deckId = sourceId, foreignWord = "one"))

        val sourceRow = requireNotNull(database.deckDao().getDeckById(sourceId))
        val cardRow = database.cardDao().getAllCards().single { it.id == cardId }
        assertEquals(1, database.deckDao().updateServerRevision(sourceId, sourceRow.syncId, 8L))
        assertEquals(1, database.cardDao().updateServerRevision(cardId, cardRow.syncId, 7L))

        decks.insertDeck(Deck(name = "renamed", creationDate = 1L, id = sourceId))
        cards.insertCard(card(deckId = destinationId, foreignWord = "changed").copy(id = cardId))

        val editedDeck = requireNotNull(decks.getDeckById(sourceId))
        val movedCard = cards.fetchAllCards().single { it.id == cardId }
        assertEquals(sourceRow.syncId, editedDeck.syncId)
        assertEquals(8L, editedDeck.lastChangedServerRevision)
        assertEquals("renamed", editedDeck.name)
        assertEquals(cardRow.syncId, movedCard.syncId)
        assertEquals(7L, movedCard.lastChangedServerRevision)
        assertEquals(destinationId, movedCard.deckId)
        assertEquals("changed", movedCard.foreignWord)
        assertNotEquals(sourceId, movedCard.deckId)
    }

    @Test
    fun `transfer use case preserves a card row and its sync identity in Room`() = withDatabase { database ->
        val decks = DeckRepositoryRoom(database)
        val cards = CardRepositoryRoom(database)
        val sourceId = decks.insertDeck(Deck(name = "source", creationDate = 1L, cardQuantity = 1))
        val destinationId = decks.insertDeck(Deck(name = "destination", creationDate = 2L))
        val cardId = cards.insertCard(card(deckId = sourceId, foreignWord = "one"))
        val original = cards.fetchAllCards().single()
        val originalRow = requireNotNull(database.cardDao().getCardById(cardId))
        assertEquals(1, database.cardDao().updateServerRevision(cardId, originalRow.syncId, 7L))

        TransferCardsToDeckUseCase(
            cardRepository = cards,
            deckRepository = decks,
            localStorageSaveVersionRepository = StorageSaveVersionRepositoryRoom(database),
            localStorageTransactionRepository = StorageTransactionRepositoryRoom(database),
            coroutineContextProvider = object : ICoroutineContextProvider {
                override val io = EmptyCoroutineContext
            },
        ).invoke(
            requireNotNull(decks.getDeckById(sourceId)),
            requireNotNull(decks.getDeckById(destinationId)),
            original,
        )

        val moved = cards.fetchAllCards().single()
        assertEquals(cardId, moved.id)
        assertEquals(original.syncId, moved.syncId)
        assertEquals(7L, moved.lastChangedServerRevision)
        assertEquals(destinationId, moved.deckId)
        assertEquals(original.nativeWord, moved.nativeWord)
        assertEquals(original.mnemonic, moved.mnemonic)
        assertEquals(0, requireNotNull(decks.getDeckById(sourceId)).cardQuantity)
        assertEquals(1, requireNotNull(decks.getDeckById(destinationId)).cardQuantity)
    }

    @Test
    fun `local row update cannot overwrite a newer confirmed server revision`() = withDatabase { database ->
        val decks = DeckRepositoryRoom(database)
        val cards = CardRepositoryRoom(database)
        val deckId = decks.insertDeck(Deck(name = "source", creationDate = 1L))
        val cardId = cards.insertCard(card(deckId = deckId, foreignWord = "one"))
        val staleDeck = requireNotNull(database.deckDao().getDeckById(deckId))
        val staleCard = requireNotNull(database.cardDao().getCardById(cardId))

        assertEquals(1, database.deckDao().updateServerRevision(deckId, staleDeck.syncId, 8L))
        assertEquals(1, database.cardDao().updateServerRevision(cardId, staleCard.syncId, 7L))
        assertEquals(1, database.deckDao().updateDeckRow(RoomDeckLocalUpdate(staleDeck.copy(name = "edited"))))
        assertEquals(1, database.cardDao().updateCardRow(RoomCardLocalUpdate(staleCard.copy(foreignWord = "changed"))))

        val editedDeck = requireNotNull(database.deckDao().getDeckById(deckId))
        val editedCard = requireNotNull(database.cardDao().getCardById(cardId))
        assertEquals("edited", editedDeck.name)
        assertEquals(8L, editedDeck.lastChangedServerRevision)
        assertEquals(staleDeck.syncId, editedDeck.syncId)
        assertEquals("changed", editedCard.foreignWord)
        assertEquals(7L, editedCard.lastChangedServerRevision)
        assertEquals(staleCard.syncId, editedCard.syncId)
    }

    @Test
    fun `duplicate sync ID cannot replace another local deck or card`() = withDatabase { database ->
        val decks = DeckRepositoryRoom(database)
        val cards = CardRepositoryRoom(database)
        val deckId = decks.insertDeck(Deck(name = "first", creationDate = 1L))
        val otherDeckId = decks.insertDeck(Deck(name = "second", creationDate = 2L))
        val firstCardId = cards.insertCard(card(deckId = deckId, foreignWord = "one"))
        val originalDeck = requireNotNull(database.deckDao().getDeckById(deckId))
        val originalCard = database.cardDao().getAllCards().single()

        assertFails {
            database.deckDao().insertDeck(originalDeck.copy(id = 0, name = "duplicate"))
        }
        assertFails {
            database.cardDao().insetCard(originalCard.copy(id = 0, deckId = otherDeckId))
        }

        assertEquals(originalDeck, database.deckDao().getDeckById(deckId))
        assertEquals(originalCard, database.cardDao().getAllCards().single { it.id == firstCardId })
        assertEquals(2, database.deckDao().getAllDecks().size)
        assertEquals(1, database.cardDao().getAllCards().size)
    }

    private fun withDatabase(block: suspend (KlafRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-sync-identity-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-sync-identity-"))
        val database = Room.databaseBuilder<KlafRoomDatabase>(
            name = directory.resolve("test.db").toString(),
        ).addMigrations(*com.kuts.klaf.room.databases.BranchIntegrationMigrations.all).setDriver(BundledSQLiteDriver()).build()
        try {
            block(database)
        } finally {
            database.close()
            directory.toFile().deleteRecursively()
        }
    }

    private fun card(deckId: Int, foreignWord: String): Card = Card(
        deckId = deckId,
        nativeWord = "native",
        foreignWord = foreignWord,
        ipa = emptyList(),
    )
}
