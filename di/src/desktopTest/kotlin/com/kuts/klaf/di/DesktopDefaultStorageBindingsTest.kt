package com.kuts.klaf.di

import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckReviewResultRepository
import com.kuts.domain.useCases.CreateDeckUseCase
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.repositoryImplementations.RoomDeckReviewResultRepository
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.server.contract.SyncOperation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class DesktopDefaultStorageBindingsTest {

    @Test
    fun `ordinary default binds scoped Room and records account edits but not guest edits`() = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-default-bindings-test-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-default-bindings-test-"))
        val configuration = DesktopStorageConfiguration.fromInputs(null, null, directory.toFile())
        val application = koinApplication {
            modules(appModules + module { single { configuration } })
        }
        var source: ActiveLocalRoomDatabase? = null
        try {
            val scoped = assertIs<ActiveLocalRoomDatabase>(application.koin.get<RoomDatabaseSource>())
            source = scoped
            assertSame(scoped, application.koin.get<ActiveLocalRoomDatabase>())
            assertSame(
                application.koin.get<com.kuts.klaf.networking.klafServer.IKlafServerSession>(),
                application.koin.get<com.kuts.domain.managers.IClientSessionScope>(),
            )
            assertIs<RoomDeckReviewResultRepository>(application.koin.get<IDeckReviewResultRepository>())
            val createDeck = application.koin.get<CreateDeckUseCase>()
            val outbox = application.koin.get<RoomSyncOutbox>()
            val account = "default-launch@example.test"

            assertNull(scoped.selection.value.accountEmail)
            createDeck(Deck("Guest offline", 1L))
            assertEquals(listOf("Guest offline"), scoped.current().deckDao().getAllDecks().map { it.name })
            assertTrue(scoped.current().pendingSyncOperationDao().pendingForAccount(account).isEmpty())

            scoped.selectAccount(account)
            assertTrue(scoped.current().deckDao().getAllDecks().isEmpty())
            createDeck(Deck("Account offline", 2L))
            assertEquals(listOf("Account offline"), scoped.current().deckDao().getAllDecks().map { it.name })
            val pending = outbox.pendingForAccount(account).single()
            assertEquals(0L, pending.baseRevision)
            assertEquals("Account offline", assertIs<SyncOperation.AddDeck>(pending.operation).deck.name)

            scoped.selectAccount(null)
            assertEquals(listOf("Guest offline"), scoped.current().deckDao().getAllDecks().map { it.name })
            assertTrue(Files.exists(directory.resolve("klaf_guest.db")))
            assertTrue(!Files.exists(directory.resolve("klaf_kt.db")))
        } finally {
            source?.close()
            application.close()
            directory.toFile().deleteRecursively()
        }
    }
}
