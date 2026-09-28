package com.kuts.klaf.room

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.server.contract.SyncOperation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

class ScopedKlafRoomDatabaseFactoryTest {

    @Test
    fun `outbox rejects an operation for a different account`() = withDirectory { directory ->
        val source = openSource(directory)
        try {
            val outbox = RoomSyncOutbox(source)
            assertFails {
                outbox.recordChange("alice@example.com", 0L, SyncOperation.DeleteDeck("guest", "deck")) {}
            }
            source.selectAccount("alice@example.com")
            val database = source.current()
            assertFails {
                outbox.recordChange("bob@example.com", 0L, SyncOperation.DeleteDeck("wrong", "deck")) {
                    database.deckDao().insertDeck(deck("wrong", "deck"))
                }
            }
            assertEquals(emptyList(), database.deckDao().getAllDecks())
        } finally {
            source.close()
        }
    }

    @Test
    fun `guest and two accounts keep decks and pending operations in separate files`() = withDirectory { directory ->
        val factory = ScopedKlafRoomDatabaseFactory(directory.toFile())
        val source = openSource(directory)
        val outbox = RoomSyncOutbox(source)
        try {
            source.current().deckDao().insertDeck(deck("guest", "guest-deck"))
            source.selectAccount(" Alice@Example.com ")
            outbox.applyAccepted("alice@example.com", emptyList(), newRevision = 2L) {}
            outbox.recordChange(
                "alice@example.com", 2L, SyncOperation.DeleteDeck("first-operation", "first-deck"),
            ) {
                source.current().deckDao().insertDeck(deck("first", "first-deck"))
            }
            source.selectAccount("bob@example.com")
            outbox.applyAccepted("bob@example.com", emptyList(), newRevision = 9L) {}
            outbox.recordChange(
                "bob@example.com", 9L, SyncOperation.DeleteDeck("second-operation", "second-deck"),
            ) {
                source.current().deckDao().insertDeck(deck("second", "second-deck"))
            }
        } finally {
            source.close()
        }

        val reopenedGuest = factory.openGuest()
        val reopenedFirst = factory.openAccount("alice@example.COM")
        val reopenedSecond = factory.openAccount("bob@example.com")
        try {
            assertEquals(listOf("guest"), reopenedGuest.deckDao().getAllDecks().map { it.name })
            assertEquals(listOf("first"), reopenedFirst.deckDao().getAllDecks().map { it.name })
            assertEquals(listOf("second"), reopenedSecond.deckDao().getAllDecks().map { it.name })
            assertEquals(2L, reopenedFirst.syncCheckpointDao().current()?.confirmedRevision)
            assertEquals(9L, reopenedSecond.syncCheckpointDao().current()?.confirmedRevision)
            assertEquals(emptyList(), reopenedGuest.pendingSyncOperationDao().pendingForAccount("alice@example.com"))
            assertEquals(
                listOf("first-operation"),
                reopenedFirst.pendingSyncOperationDao().pendingForAccount("alice@example.com").map { it.operationId },
            )
            assertEquals(
                listOf("second-operation"),
                reopenedSecond.pendingSyncOperationDao().pendingForAccount("bob@example.com").map { it.operationId },
            )
        } finally {
            reopenedGuest.close()
            reopenedFirst.close()
            reopenedSecond.close()
        }

        val names = Files.list(directory).use { stream -> stream.map { it.fileName.toString() }.toList() }
        assertEquals(3, names.count { it.endsWith(".db") })
        assertFalse(names.any { it.contains("alice", ignoreCase = true) || it.contains("bob", ignoreCase = true) })
    }

    @Test
    fun `opening scoped databases leaves the legacy database untouched`() = withDirectory { directory ->
        val legacy = directory.resolve("klaf_kt.db")
        Files.writeString(legacy, "legacy-data")
        val factory = ScopedKlafRoomDatabaseFactory(directory.toFile())

        factory.openGuest().close()
        factory.openAccount("alice@example.com").close()

        assertEquals("legacy-data", Files.readString(legacy))
    }

    private fun withDirectory(block: suspend (Path) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-scoped-db-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-scoped-db-"))
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun openSource(directory: Path): ActiveLocalRoomDatabase = ActiveLocalRoomDatabase(
        factory = ScopedKlafRoomDatabaseFactory(directory.toFile()),
        accountStore = DesktopSelectedAccountStore(directory.toFile()),
    )

    private fun deck(name: String, syncId: String) = RoomDeck(
        name = name,
        creationDate = 1L,
        repetitionIterationDates = emptyList(),
        scheduledIterationDates = emptyList(),
        scheduledDateInterval = 0L,
        repetitionQuantity = 0,
        cardQuantity = 0,
        lastFirstRepetitionDuration = 0L,
        lastSecondRepetitionDuration = 0L,
        lastRepetitionIterationDuration = 0L,
        isLastIterationSucceeded = true,
        syncId = syncId,
    )
}
