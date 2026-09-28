package com.kuts.klaf.room

import com.kuts.domain.entities.Deck
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.RoomReminderSelectionObserver
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.databases.ScopedDeckReminderActions
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class RoomReminderSelectionTest {

    @Test
    fun `switch cancels old reminders before scheduling colliding deck ids`() = withSource { source, events ->
        val decks = DeckRepositoryRoom(source)
        val guestId = decks.insertDeck(Deck(name = "guest", creationDate = 1L, scheduledReviewDates = listOf(100L)))

        source.selectAccount("alice@example.com")
        val aliceId = decks.insertDeck(Deck(name = "alice", creationDate = 2L, scheduledReviewDates = listOf(200L)))
        source.selectAccount("bob@example.com")
        val bobId = decks.insertDeck(Deck(name = "bob", creationDate = 3L, scheduledReviewDates = listOf(300L)))
        assertEquals(guestId, aliceId)
        assertEquals(aliceId, bobId)

        events.clear()
        source.selectAccount("alice@example.com")
        assertEquals(listOf("cancel:bob@example.com:$bobId", "dismiss:bob@example.com:$bobId", "schedule:alice@example.com:$aliceId"), events)

        events.clear()
        source.selectAccount(null)
        assertEquals(listOf("cancel:alice@example.com:$aliceId", "dismiss:alice@example.com:$aliceId", "schedule:guest:$guestId"), events)
    }

    @Test
    fun `unchanged selection does not replace reminders`() = withSource { source, events ->
        source.selectAccount("alice@example.com")
        events.clear()

        source.selectAccount(" ALICE@example.com ")

        assertEquals(emptyList(), events)
    }

    @Test
    fun `switch cancels stale alarms even if a deck no longer has a scheduled date`() = withSource { source, events ->
        val decks = DeckRepositoryRoom(source)
        val deckId = decks.insertDeck(Deck(name = "unscheduled guest", creationDate = 1L))

        source.selectAccount("alice@example.com")

        assertEquals(listOf("cancel:guest:$deckId", "dismiss:guest:$deckId"), events)
    }

    private fun withSource(block: suspend (ActiveLocalRoomDatabase, MutableList<String>) -> Unit) = runBlocking {
        val root = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(root, "klaf-reminder-switch-").toRealPath()
        require(directory.parent == root && directory.fileName.toString().startsWith("klaf-reminder-switch-"))
        val events = mutableListOf<String>()
        val actions = object : ScopedDeckReminderActions {
            override fun cancel(accountEmail: String?, deckId: Int) {
                events += "cancel:${accountEmail ?: "guest"}:$deckId"
            }

            override fun dismissNotification(accountEmail: String?, deckId: Int) {
                events += "dismiss:${accountEmail ?: "guest"}:$deckId"
            }

            override fun schedule(accountEmail: String?, deckName: String, deckId: Int, atTime: Long) {
                events += "schedule:${accountEmail ?: "guest"}:$deckId"
            }
        }
        val source = ActiveLocalRoomDatabase(
            factory = ScopedKlafRoomDatabaseFactory(directory.toFile()),
            accountStore = DesktopSelectedAccountStore(directory.toFile()),
            selectionObserver = RoomReminderSelectionObserver(actions),
        )
        try {
            block(source, events)
        } finally {
            source.close()
            directory.toFile().deleteRecursively()
        }
    }
}
