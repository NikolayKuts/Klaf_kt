package com.kuts.klaf.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.performInTransaction
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class RoomCardDeckIntegrityContractTest {

    @Test
    fun `card cannot reference a missing deck`() = withDatabase { database ->
        val result = runCatching {
            database.cardDao().insetCard(card(deckId = 404))
        }

        assertTrue(result.isFailure)
        assertTrue(database.cardDao().getAllCards().isEmpty())
    }

    @Test
    fun `deleting a deck also deletes its cards`() = withDatabase { database ->
        database.deckDao().insertDeck(deck(id = 1))
        database.cardDao().insetCard(card(deckId = 1))

        database.deckDao().deleteDeck(deckId = 1)

        assertTrue(database.cardDao().getAllCards().isEmpty())
    }

    @Test
    fun `deleting one deck does not delete another deck's cards`() = withDatabase { database ->
        database.deckDao().insertDeck(deck(id = 1))
        database.deckDao().insertDeck(deck(id = 2))
        database.cardDao().insetCard(card(deckId = 1).copy(id = 11))
        val otherCard = card(deckId = 2).copy(id = 22)
        database.cardDao().insetCard(otherCard)

        database.deckDao().deleteDeck(deckId = 1)

        assertEquals(otherCard, database.cardDao().getAllCards().firstOrNull { it.id == 22 })
    }

    @Test
    fun `updating a deck does not cascade-delete its existing cards`() = withDatabase { database ->
        database.deckDao().insertDeck(deck(id = 1))
        val existingCard = card(deckId = 1)
        database.cardDao().insetCard(existingCard)

        database.deckDao().insertDeck(deck(id = 1).copy(name = "renamed"))

        assertEquals("renamed", database.deckDao().getDeckById(1)?.name)
        assertEquals(listOf(existingCard), database.cardDao().getAllCards())
    }

    @Test
    fun `moving a card updates one existing row without losing its data`() = withDatabase { database ->
        database.deckDao().insertDeck(deck(id = 1))
        database.deckDao().insertDeck(deck(id = 2))
        val original = card(deckId = 1).copy(mnemonicJson = "{\"hint\":\"remember me\"}")
        database.cardDao().insetCard(original)

        database.cardDao().insetCard(original.copy(deckId = 2))

        assertEquals(listOf(original.copy(deckId = 2)), database.cardDao().getAllCards())
        assertTrue(database.cardDao().getCardsByDeckId(1).isEmpty())
    }

    @Test
    fun `moving a card to a missing deck keeps the original row`() = withDatabase { database ->
        database.deckDao().insertDeck(deck(id = 1))
        val original = card(deckId = 1)
        database.cardDao().insetCard(original)

        val result = runCatching {
            database.cardDao().insetCard(original.copy(deckId = 404))
        }

        assertTrue(result.isFailure)
        assertEquals(listOf(original), database.cardDao().getAllCards())
    }

    @Test
    fun `card deck reference has a foreign key and a non unique lookup index`() =
        withDatabaseAndPath { database, path ->
            database.deckDao().insertDeck(deck(id = 1))
            BundledSQLiteDriver().open(path.toString()).use { connection ->
                var hasCascadingDeckForeignKey = false
                connection.prepare("PRAGMA foreign_key_list('cards')").use { statement ->
                    while (statement.step()) {
                        hasCascadingDeckForeignKey = hasCascadingDeckForeignKey ||
                            statement.getText(2) == "decks" &&
                            statement.getText(3) == "deckId" &&
                            statement.getText(4) == "id" &&
                            statement.getText(6).equals("CASCADE", ignoreCase = true)
                    }
                }

                val nonUniqueIndexes = mutableListOf<String>()
                connection.prepare("PRAGMA index_list('cards')").use { statement ->
                    while (statement.step()) {
                        if (statement.getLong(2) == 0L) nonUniqueIndexes += statement.getText(1)
                    }
                }

                val hasDeckLookupIndex = nonUniqueIndexes.any { name ->
                    val escapedName = name.replace("'", "''")
                    connection.prepare("PRAGMA index_info('$escapedName')").use { statement ->
                        statement.step() && statement.getLong(0) == 0L && statement.getText(2) == "deckId"
                    }
                }

                assertTrue(hasCascadingDeckForeignKey)
                assertTrue(hasDeckLookupIndex)
            }
        }

    @Test
    fun `a failed grouped write leaves neither deck nor card`() = withDatabase { database ->
        val result = runCatching {
            database.performInTransaction {
                database.deckDao().insertDeck(deck(id = 1))
                database.cardDao().insetCard(card(deckId = 1))
                error("simulated interrupted write")
            }
        }

        assertTrue(result.isFailure)
        assertTrue(database.deckDao().getAllDecks().isEmpty())
        assertTrue(database.cardDao().getAllCards().isEmpty())
    }

    @Test
    fun `a committed grouped write contains both parent and child`() = withDatabase { database ->
        database.performInTransaction {
            database.deckDao().insertDeck(deck(id = 1))
            database.cardDao().insetCard(card(deckId = 1))
        }

        assertEquals(1, database.deckDao().getAllDecks().size)
        assertEquals(1, database.cardDao().getAllCards().size)
    }

    private fun withDatabase(block: suspend (KlafRoomDatabase) -> Unit) =
        withDatabaseAndPath { database, _ -> block(database) }

    private fun withDatabaseAndPath(block: suspend (KlafRoomDatabase, Path) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-room-integrity-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-room-integrity-"))
        val path = directory.resolve("test.db")
        val database = Room.databaseBuilder<KlafRoomDatabase>(
            name = path.toString(),
        ).setDriver(BundledSQLiteDriver()).build()

        try {
            block(database, path)
        } finally {
            database.close()
            directory.toFile().deleteRecursively()
        }
    }

    private fun deck(id: Int): RoomDeck = RoomDeck(
        id = id,
        name = "deck-$id",
        creationDate = 1_000L,
        repetitionIterationDates = emptyList(),
        scheduledIterationDates = emptyList(),
        scheduledDateInterval = 0L,
        repetitionQuantity = 0,
        cardQuantity = 0,
        lastFirstRepetitionDuration = 0L,
        lastSecondRepetitionDuration = 0L,
        lastRepetitionIterationDuration = 0L,
        isLastIterationSucceeded = true,
    )

    private fun card(deckId: Int): RoomCard = RoomCard(
        id = 1,
        deckId = deckId,
        nativeWord = "native",
        foreignWord = "foreign",
        ipa = "[]",
        wordMeaningInsights = WordMeaningInsights.EMPTY,
        mnemonicJson = "{}",
    )
}
