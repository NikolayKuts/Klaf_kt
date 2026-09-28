package com.kuts.klaf.room

import com.kuts.domain.common.DeckReviewPassSuccessMark
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckReviewInfoRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import com.kuts.klaf.room.repositoryImplementations.RoomReviewResultWriter
import com.kuts.klaf.room.repositoryImplementations.RoomDeckReviewResultRepository
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.server.contract.SyncOperation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class RoomDeckReviewInfoTest {

    @Test
    fun `review details remain separate for guest and account with identical deck IDs`() = withSource { source ->
        val decks = DeckRepositoryRoom(source)
        val reviews = DeckReviewInfoRepositoryRoom(source)
        val guestId = decks.insertDeck(Deck(
            name = "guest",
            creationDate = 1L,
            reviewCount = 2,
            scheduledReviewDates = listOf(13L),
        ))
        val guestReview = sampleReview(guestId, 10L)
        reviews.saveDeckRepetitionInfo(guestReview)

        source.selectAccount("alice@example.test")
        val accountId = decks.insertDeck(Deck(
            name = "account",
            creationDate = 2L,
            reviewCount = 2,
            scheduledReviewDates = listOf(23L),
        ))
        assertEquals(guestId, accountId)
        assertEquals(null, reviews.fetchDeckRepetitionInfo(accountId).first())
        val accountReview = sampleReview(accountId, 20L)
        reviews.saveDeckRepetitionInfo(accountReview)
        assertEquals(accountReview, reviews.fetchDeckRepetitionInfo(accountId).first())

        source.selectAccount(null)
        assertEquals(guestReview, reviews.fetchDeckRepetitionInfo(guestId).first())
    }

    @Test
    fun `guest transfer includes the detailed review summary`() = withSource { source ->
        val decks = DeckRepositoryRoom(source)
        val reviews = DeckReviewInfoRepositoryRoom(source)
        val id = decks.insertDeck(Deck(
            name = "reviewed",
            creationDate = 1L,
            reviewCount = 2,
            scheduledReviewDates = listOf(33L),
        ))
        val review = sampleReview(id, 30L)
        reviews.saveDeckRepetitionInfo(review)

        GuestAccountDataTransfer(source).transfer("alice@example.test")
        assertEquals(null, reviews.fetchDeckRepetitionInfo(id).first())
        source.selectAccount("alice@example.test")
        assertEquals(review, reviews.fetchDeckRepetitionInfo(id).first())
        val pending = source.current().pendingSyncOperationDao().allPending().single()
        val upload = Json.decodeFromString<SyncOperation>(pending.operationJson) as SyncOperation.AddDeck
        assertEquals(review.scheduledDate, upload.deck.reviewSummary?.scheduledDate)
    }

    @Test
    fun `review details roll back with a failed deck update and disappear with deck deletion`() = withSource { source ->
        val decks = DeckRepositoryRoom(source)
        val reviews = DeckReviewInfoRepositoryRoom(source)
        val id = decks.insertDeck(Deck(
            name = "reviewed",
            creationDate = 1L,
            reviewCount = 2,
            scheduledReviewDates = listOf(43L),
        ))
        val review = sampleReview(id, 40L)
        runCatching {
            source.transaction {
                reviews.saveDeckRepetitionInfo(review)
                decks.insertDeck(requireNotNull(decks.getDeckById(id)).copy(name = "changed"))
                error("rollback")
            }
        }
        assertEquals("reviewed", decks.getDeckById(id)?.name)
        assertEquals(null, reviews.fetchDeckRepetitionInfo(id).first())

        reviews.saveDeckRepetitionInfo(review)
        decks.removeDeck(id)
        assertEquals(null, reviews.fetchDeckRepetitionInfo(id).first())
    }

    @Test
    fun `account review and its outbox operation commit or roll back together`() = withSource { source ->
        source.selectAccount("alice@example.test")
        val decks = DeckRepositoryRoom(source)
        val id = decks.insertDeck(Deck(name = "words", creationDate = 1L))
        val original = requireNotNull(decks.getDeckById(id))
        val updated = original.copy(
            reviewCount = 1,
            lastFirstReviewDuration = 11L,
            scheduledReviewDates = listOf(200L),
        )
        val review = sampleReview(id, 50L).copy(repetitionQuantity = 1)

        assertFails {
            RoomReviewResultWriter(source, RoomSyncOutbox(source), afterLocalWrite = { error("interrupted") })
                .save(updated, review, baseRevision = 0L)
        }
        assertEquals(0, decks.getDeckById(id)?.reviewCount)
        assertEquals(null, decks.getDeckById(id)?.reviewInfo)
        assertEquals(emptyList(), source.current().pendingSyncOperationDao().allPending())

        RoomReviewResultWriter(source, RoomSyncOutbox(source)).save(updated, review, baseRevision = 0L)
        assertEquals(review, decks.getDeckById(id)?.reviewInfo)
        val pending = source.current().pendingSyncOperationDao().allPending().single()
        val operation = Json.decodeFromString<SyncOperation>(pending.operationJson) as SyncOperation.FinishReview
        assertEquals(emptyList(), operation.reviewPassDates)
        assertEquals(listOf(200L), operation.scheduledReviewDates)
        assertEquals(review.scheduledDate, operation.reviewSummary?.scheduledDate)
    }

    @Test
    fun `review result repository reads selected checkpoint and saves one operation`() = withSource { source ->
        source.selectAccount("alice@example.test")
        val decks = DeckRepositoryRoom(source)
        val id = decks.insertDeck(Deck(name = "words", creationDate = 1L))
        val updated = requireNotNull(decks.getDeckById(id)).copy(
            reviewCount = 1,
            scheduledReviewDates = listOf(200L),
        )
        val review = sampleReview(id, 80L).copy(repetitionQuantity = 1)

        RoomSyncOutbox(source).applyAccepted("alice@example.test", emptyList(), 3L) { }
        RoomDeckReviewResultRepository(source).save(updated, review)

        assertEquals(review, decks.getDeckById(id)?.reviewInfo)
        val queued = RoomSyncOutbox(source).pendingForAccount("alice@example.test")
        assertEquals(1, queued.size)
        assertEquals(3L, queued.single().baseRevision)
        assertEquals(1, (queued.single().operation as SyncOperation.FinishReview).reviewCount)
    }

    @Test
    fun `guest review persists locally without creating an account outbox operation`() = withSource { source ->
        val decks = DeckRepositoryRoom(source)
        val id = decks.insertDeck(Deck(name = "guest", creationDate = 1L))
        val updated = requireNotNull(decks.getDeckById(id)).copy(reviewCount = 1)
        val review = sampleReview(id, 60L).copy(repetitionQuantity = 1)

        RoomReviewResultWriter(source, RoomSyncOutbox(source)).save(updated, review, baseRevision = 0L)

        assertEquals(review, decks.getDeckById(id)?.reviewInfo)
        assertEquals(emptyList(), source.current().pendingSyncOperationDao().allPending())
    }

    @Test
    fun `review result preserves a concurrent local deck rename`() = withSource { source ->
        source.selectAccount("alice@example.test")
        val decks = DeckRepositoryRoom(source)
        val id = decks.insertDeck(Deck(name = "words", creationDate = 1L))
        val snapshot = requireNotNull(decks.getDeckById(id))
        decks.insertDeck(snapshot.copy(name = "renamed"))

        RoomReviewResultWriter(source, RoomSyncOutbox(source)).save(
            updatedDeck = snapshot.copy(reviewCount = 1),
            reviewInfo = sampleReview(id, 70L).copy(repetitionQuantity = 1),
            baseRevision = 0L,
        )

        assertEquals("renamed", decks.getDeckById(id)?.name)
        assertEquals(1, decks.getDeckById(id)?.reviewCount)
    }

    private fun sampleReview(deckId: Int, offset: Long) = DeckRepetitionInfo(
        deckId = deckId,
        currentDuration = offset + 1,
        previousDuration = offset + 2,
        scheduledDate = offset + 3,
        previousScheduledDate = offset + 4,
        lastIterationDate = offset + 5,
        repetitionQuantity = 2,
        currentIterationSuccessMark = DeckReviewPassSuccessMark.SUCCESS,
        previousIterationSuccessMark = DeckReviewPassSuccessMark.FAILURE,
    )

    private fun withSource(block: suspend (ActiveLocalRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-review-room-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-review-room-"))
        val source = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(directory.toFile()),
            DesktopSelectedAccountStore(directory.toFile()),
        )
        try {
            block(source)
        } finally {
            source.close()
            directory.toFile().deleteRecursively()
        }
    }
}
