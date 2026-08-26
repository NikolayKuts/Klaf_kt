package com.kuts.domain.useCases

import com.kuts.domain.common.DataSynchronizationValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SynchronizeLocalAndRemoteDataUseCaseTest {

    @Test
    fun `newer remote data downloads images deletes obsolete local images and aligns versions`() =
        runTest {
            val remoteDeck = testDeck(id = 1, cardQuantity = 1)
            val remoteCard = testCard(id = 1, deckId = 1, imageAssetId = "remote-asset")
            val localDeck = testDeck(id = 9, cardQuantity = 1)
            val localCard = testCard(id = 9, deckId = 9, imageAssetId = "obsolete-local-asset")
            val localAssetRepository = TestMnemonicImageAssetRepository(
                initialSavedImages = mapOf("obsolete-local-asset" to byteArrayOf(9, 9)),
            )
            val remoteImageRepository = TestMnemonicImageRemoteRepository(
                initialImages = mapOf("remote-asset" to byteArrayOf(1, 2, 3)),
            )
            val localCardRepository = TestCardRepository(cards = listOf(localCard))
            val useCase = createUseCase(
                localDeckRepository = TestDeckRepository(decks = listOf(localDeck)),
                localCardRepository = localCardRepository,
                localStorageSaveVersionRepository = TestStorageSaveVersionRepository(initialVersion = 1),
                localMnemonicImageAssetRepository = localAssetRepository,
                remoteDeckRepository = TestDeckRepository(decks = listOf(remoteDeck)),
                remoteCardRepository = TestCardRepository(cards = listOf(remoteCard)),
                remoteStorageSaveVersionRepository = TestStorageSaveVersionRepository(initialVersion = 5),
                remoteMnemonicImageRepository = remoteImageRepository,
                io = UnconfinedTestDispatcher(testScheduler),
            )

            useCase.invoke().toList()

            assertEquals(listOf(remoteCard), localCardRepository.fetchAllCards())
            assertEquals(listOf("remote-asset"), localAssetRepository.importedSavedAssetIds)
            assertEquals(listOf("obsolete-local-asset"), localAssetRepository.deletedSavedAssetIds)
            assertEquals(setOf("remote-asset"), localAssetRepository.savedImages.keys)
            assertContentEquals(
                expected = byteArrayOf(1, 2, 3),
                actual = requireNotNull(localAssetRepository.savedImages["remote-asset"]),
            )
        }

    @Test
    fun `newer remote data keeps syncing when remote image payload is absent`() = runTest {
        val remoteDeck = testDeck(id = 1, cardQuantity = 1)
        val remoteCard = testCard(id = 1, deckId = 1, imageAssetId = "missing-remote-asset")
        val localVersionRepository = TestStorageSaveVersionRepository(initialVersion = 1)
        val remoteVersionRepository = TestStorageSaveVersionRepository(initialVersion = 5)
        val localAssetRepository = TestMnemonicImageAssetRepository()
        val localCardRepository = TestCardRepository()
        val useCase = createUseCase(
            localDeckRepository = TestDeckRepository(),
            localCardRepository = localCardRepository,
            localStorageSaveVersionRepository = localVersionRepository,
            localMnemonicImageAssetRepository = localAssetRepository,
            remoteDeckRepository = TestDeckRepository(decks = listOf(remoteDeck)),
                remoteCardRepository = TestCardRepository(cards = listOf(remoteCard)),
                remoteStorageSaveVersionRepository = remoteVersionRepository,
                remoteMnemonicImageRepository = TestMnemonicImageRemoteRepository(),
                io = UnconfinedTestDispatcher(testScheduler),
            )

        useCase.invoke().toList()

        assertEquals(listOf(remoteCard), localCardRepository.fetchAllCards())
        assertTrue(localAssetRepository.importedSavedAssetIds.isEmpty())
        assertEquals(5L, localVersionRepository.currentVersion?.version)
        assertEquals(5L, remoteVersionRepository.currentVersion?.version)
    }

    @Test
    fun `newer local data uploads images deletes obsolete remote images and updates versions`() =
        runTest {
            val localDeck = testDeck(id = 1, cardQuantity = 1)
            val localCard = testCard(id = 1, deckId = 1, imageAssetId = "local-asset")
            val remoteDeck = testDeck(id = 3, cardQuantity = 1)
            val remoteCard = testCard(id = 3, deckId = 3, imageAssetId = "obsolete-remote-asset")
            val remoteCardRepository = TestCardRepository(cards = listOf(remoteCard))
            val localVersionRepository = TestStorageSaveVersionRepository(initialVersion = 1)
            val remoteVersionRepository = TestStorageSaveVersionRepository(initialVersion = 0)
            val remoteImageRepository = TestMnemonicImageRemoteRepository(
                initialImages = mapOf("obsolete-remote-asset" to byteArrayOf(7, 7)),
            )
            val useCase = createUseCase(
                localDeckRepository = TestDeckRepository(decks = listOf(localDeck)),
                localCardRepository = TestCardRepository(cards = listOf(localCard)),
                localStorageSaveVersionRepository = localVersionRepository,
                localMnemonicImageAssetRepository = TestMnemonicImageAssetRepository(
                    initialSavedImages = mapOf("local-asset" to byteArrayOf(4, 5, 6)),
                ),
                remoteDeckRepository = TestDeckRepository(decks = listOf(remoteDeck)),
                remoteCardRepository = remoteCardRepository,
                remoteStorageSaveVersionRepository = remoteVersionRepository,
                remoteMnemonicImageRepository = remoteImageRepository,
                io = UnconfinedTestDispatcher(testScheduler),
            )

            useCase.invoke().toList()

            assertEquals(listOf(localCard), remoteCardRepository.fetchAllCards())
            assertEquals(listOf("local-asset"), remoteImageRepository.defaultUploadCalls)
            assertEquals(listOf("obsolete-remote-asset"), remoteImageRepository.deletedAssetIds)
            assertContentEquals(
                expected = byteArrayOf(4, 5, 6),
                actual = requireNotNull(remoteImageRepository.remoteImages["local-asset"]),
            )
            assertEquals(1L, localVersionRepository.currentVersion?.version)
            assertEquals(1L, remoteVersionRepository.currentVersion?.version)
        }

    @Test
    fun `newer local data failure before upload keeps versions and remote cards unchanged`() =
        runTest {
            val localCard = testCard(id = 1, deckId = 1, imageAssetId = "missing-local-asset")
            val remoteCard = testCard(id = 2, deckId = 2, imageAssetId = "remote-asset")
            val remoteCardRepository = TestCardRepository(cards = listOf(remoteCard))
            val localVersionRepository = TestStorageSaveVersionRepository(initialVersion = 1)
            val remoteVersionRepository = TestStorageSaveVersionRepository(initialVersion = 0)
            val remoteImageRepository = TestMnemonicImageRemoteRepository()
            val useCase = createUseCase(
                localDeckRepository = TestDeckRepository(decks = listOf(testDeck(id = 1))),
                localCardRepository = TestCardRepository(cards = listOf(localCard)),
                localStorageSaveVersionRepository = localVersionRepository,
                localMnemonicImageAssetRepository = TestMnemonicImageAssetRepository(),
                remoteDeckRepository = TestDeckRepository(decks = listOf(testDeck(id = 2))),
                remoteCardRepository = remoteCardRepository,
                remoteStorageSaveVersionRepository = remoteVersionRepository,
                remoteMnemonicImageRepository = remoteImageRepository,
                io = UnconfinedTestDispatcher(testScheduler),
            )

            assertFailsWith<IllegalArgumentException> {
                useCase.invoke().toList()
            }

            assertEquals(listOf(remoteCard), remoteCardRepository.fetchAllCards())
            assertTrue(remoteImageRepository.defaultUploadCalls.isEmpty())
            assertTrue(localVersionRepository.insertedVersions.isEmpty())
            assertTrue(remoteVersionRepository.insertedVersions.isEmpty())
            assertEquals(1L, localVersionRepository.currentVersion?.version)
            assertEquals(0L, remoteVersionRepository.currentVersion?.version)
        }

    @Test
    fun `newer local data skips image upload and local image reads when remote images are disabled`() =
        runTest {
            val localDeck = testDeck(id = 1, cardQuantity = 1)
            val localCard = testCard(id = 1, deckId = 1, imageAssetId = "missing-local-asset")
            val remoteCard = testCard(id = 2, deckId = 2, imageAssetId = "obsolete-remote-asset")
            val remoteCardRepository = TestCardRepository(cards = listOf(remoteCard))
            val localVersionRepository = TestStorageSaveVersionRepository(initialVersion = 1)
            val remoteVersionRepository = TestStorageSaveVersionRepository(initialVersion = 0)
            val localAssetRepository = TestMnemonicImageAssetRepository()
            val remoteImageRepository = TestMnemonicImageRemoteRepository(isEnabled = false)
            val useCase = createUseCase(
                localDeckRepository = TestDeckRepository(decks = listOf(localDeck)),
                localCardRepository = TestCardRepository(cards = listOf(localCard)),
                localStorageSaveVersionRepository = localVersionRepository,
                localMnemonicImageAssetRepository = localAssetRepository,
                remoteDeckRepository = TestDeckRepository(decks = listOf(testDeck(id = 2))),
                remoteCardRepository = remoteCardRepository,
                remoteStorageSaveVersionRepository = remoteVersionRepository,
                remoteMnemonicImageRepository = remoteImageRepository,
                io = UnconfinedTestDispatcher(testScheduler),
            )

            useCase.invoke().toList()

            assertEquals(listOf(localCard), remoteCardRepository.fetchAllCards())
            assertTrue(localAssetRepository.readSavedImageAssetIds.isEmpty())
            assertTrue(remoteImageRepository.defaultUploadCalls.isEmpty())
            assertTrue(remoteImageRepository.deletedAssetIds.isEmpty())
            assertEquals(1L, localVersionRepository.currentVersion?.version)
            assertEquals(1L, remoteVersionRepository.currentVersion?.version)
        }

    @Test
    fun `newer remote data skips image download when remote images are disabled`() = runTest {
        val remoteDeck = testDeck(id = 1, cardQuantity = 1)
        val remoteCard = testCard(id = 1, deckId = 1, imageAssetId = "remote-asset")
        val localCard = testCard(id = 9, deckId = 9, imageAssetId = "obsolete-local-asset")
        val localAssetRepository = TestMnemonicImageAssetRepository(
            initialSavedImages = mapOf("obsolete-local-asset" to byteArrayOf(9, 9)),
        )
        val localCardRepository = TestCardRepository(cards = listOf(localCard))
        val remoteImageRepository = TestMnemonicImageRemoteRepository(
            initialImages = mapOf("remote-asset" to byteArrayOf(1, 2, 3)),
            isEnabled = false,
        )
        val localVersionRepository = TestStorageSaveVersionRepository(initialVersion = 1)
        val remoteVersionRepository = TestStorageSaveVersionRepository(initialVersion = 5)
        val useCase = createUseCase(
            localDeckRepository = TestDeckRepository(decks = listOf(testDeck(id = 9))),
            localCardRepository = localCardRepository,
            localStorageSaveVersionRepository = localVersionRepository,
            localMnemonicImageAssetRepository = localAssetRepository,
            remoteDeckRepository = TestDeckRepository(decks = listOf(remoteDeck)),
            remoteCardRepository = TestCardRepository(cards = listOf(remoteCard)),
            remoteStorageSaveVersionRepository = remoteVersionRepository,
            remoteMnemonicImageRepository = remoteImageRepository,
            io = UnconfinedTestDispatcher(testScheduler),
        )

        useCase.invoke().toList()

        assertEquals(listOf(remoteCard), localCardRepository.fetchAllCards())
        assertTrue(remoteImageRepository.downloadCalls.isEmpty())
        assertTrue(localAssetRepository.importedSavedAssetIds.isEmpty())
        assertEquals(listOf("obsolete-local-asset"), localAssetRepository.deletedSavedAssetIds)
        assertEquals(5L, localVersionRepository.currentVersion?.version)
        assertEquals(5L, remoteVersionRepository.currentVersion?.version)
    }

    private fun createUseCase(
        localDeckRepository: TestDeckRepository,
        localCardRepository: TestCardRepository,
        localStorageSaveVersionRepository: TestStorageSaveVersionRepository,
        localMnemonicImageAssetRepository: TestMnemonicImageAssetRepository,
        remoteDeckRepository: TestDeckRepository,
        remoteCardRepository: TestCardRepository,
        remoteStorageSaveVersionRepository: TestStorageSaveVersionRepository,
        remoteMnemonicImageRepository: TestMnemonicImageRemoteRepository,
        io: CoroutineContext,
    ): SynchronizeLocalAndRemoteDataUseCase {
        return SynchronizeLocalAndRemoteDataUseCase(
            localDeckRepository = localDeckRepository,
            localCardRepository = localCardRepository,
            localStorageSaveVersionRepository = localStorageSaveVersionRepository,
            localMnemonicImageAssetRepository = localMnemonicImageAssetRepository,
            remoteDeckRepository = remoteDeckRepository,
            remoteCardRepository = remoteCardRepository,
            remoteStorageSaveVersionRepository = remoteStorageSaveVersionRepository,
            remoteMnemonicImageRepository = remoteMnemonicImageRepository,
            dataSynchronizationValidator = DataSynchronizationValidator(),
            coroutineContextProvider = TestCoroutineContextProvider(io = io),
        )
    }
}
