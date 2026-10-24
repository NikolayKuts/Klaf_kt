package com.kuts.klaf

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.IgnoredVocabularyWordRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.VocabularySourceRepositoryRoom
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class VocabularySourceStartupPersistenceTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val deckTitle = "Startup preservation ${UUID.randomUUID()}"
    private lateinit var sources: VocabularySourceRepositoryRoom
    private lateinit var words: IgnoredVocabularyWordRepositoryRoom
    private var sourceId = 0
    private val seed = object : ExternalResource() {
        override fun before() = runBlocking {
            val active = GlobalContext.get().get<RoomDatabaseSource>() as ActiveLocalRoomDatabase
            active.selectAccount("startup-${UUID.randomUUID()}@example.test")
            val database = active.current()
            sources = VocabularySourceRepositoryRoom(database)
            words = IgnoredVocabularyWordRepositoryRoom(database)
            sourceId = sources.saveSource(VocabularySource(
                "Keep source", url = "https://example.test/source", rawText = "Keep transcript",
                createdAt = 1, updatedAt = 2,
            ))
            sources.saveItems(listOf(VocabularySourceItem(
                sourceId, foreignWord = "cat", nativeWord = "кот",
                category = VocabularySourceItemCategory.NEW, createdAt = 1, updatedAt = 2,
            )))
            words.saveWords(listOf(IgnoredVocabularyWord("en", "skip", "meaning", 1)))
            DeckRepositoryRoom(database).insertDeck(Deck(deckTitle, creationDate = 1))
            Unit
        }
    }

    @get:Rule
    val isolatedActivity: RuleChain = RuleChain.outerRule(IsolatedStorageTestRule()).around(seed).around(compose)

    @Test
    fun launchingAndRecreatingActivityPreservesVocabularyReviewData() {
        assertStartupPreservesData()
        compose.activityRule.scenario.recreate()
        assertStartupPreservesData()
    }

    private fun assertStartupPreservesData() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(deckTitle).fetchSemanticsNodes().isNotEmpty() ||
                runBlocking { sources.fetchItemsBySourceId(sourceId).isEmpty() }
        }
        runBlocking {
            assertEquals("Launching the app must preserve analyzed items", 1, sources.fetchItemsBySourceId(sourceId).size)
            assertEquals("Launching the app must preserve ignored words", 1, words.fetchWords().size)
            assertEquals("Keep transcript", sources.fetchSourceById(sourceId)?.rawText)
        }
        assertEquals(1, compose.onAllNodesWithText(deckTitle).fetchSemanticsNodes().size)
    }
}
