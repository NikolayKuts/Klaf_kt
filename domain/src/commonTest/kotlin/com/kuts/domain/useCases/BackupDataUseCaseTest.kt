package com.kuts.domain.useCases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackupDataUseCaseTest {

    @Test
    fun `backup uploads unique mnemonic images to provided path before cards and version`() =
        runTest {
            val backupPath = "backup@example.com"
            val sharedAssetBytes = byteArrayOf(8, 9, 10)
            val localCards = listOf(
                testCard(id = 1, deckId = 1, imageAssetId = "shared-asset"),
                testCard(id = 2, deckId = 1, imageAssetId = "shared-asset"),
                testCard(id = 3, deckId = 1, imageAssetId = "unique-asset"),
            )
            val localDeck = testDeck(id = 1, cardQuantity = localCards.size)
            val remoteDeckRepository = TestDeckRepository()
            val remoteCardRepository = TestCardRepository()
            val remoteVersionRepository = TestStorageSaveVersionRepository()
            val remoteImageRepository = TestMnemonicImageRemoteRepository()
            val useCase = BackupDataUseCase(
                localDeckRepository = TestDeckRepository(decks = listOf(localDeck)),
                localCardRepository = TestCardRepository(cards = localCards),
                localStorageSaveVersionRepository = TestStorageSaveVersionRepository(initialVersion = 3),
                localMnemonicImageAssetRepository = TestMnemonicImageAssetRepository(
                    initialSavedImages = mapOf(
                        "shared-asset" to sharedAssetBytes,
                        "unique-asset" to byteArrayOf(1, 2, 3),
                    ),
                ),
                remoteDeckRepository = remoteDeckRepository,
                remoteCardRepository = remoteCardRepository,
                remoteStorageSaveVersionRepository = remoteVersionRepository,
                remoteMnemonicImageRepository = remoteImageRepository,
                coroutineContextProvider = TestCoroutineContextProvider(
                    io = UnconfinedTestDispatcher(testScheduler),
                ),
            )

            useCase.invoke(backupPath = backupPath)

            assertEquals(
                expected = listOf("shared-asset", "unique-asset"),
                actual = requireNotNull(remoteImageRepository.scopedUploadCalls[backupPath]).sorted(),
            )
            assertContentEquals(
                expected = sharedAssetBytes,
                actual = requireNotNull(remoteImageRepository.remoteImages["$backupPath::shared-asset"]),
            )
            assertEquals(
                expected = localCards,
                actual = requireNotNull(remoteCardRepository.insertedCardsAtPath[backupPath]),
            )
            assertEquals(
                expected = listOf(localDeck),
                actual = requireNotNull(remoteDeckRepository.insertedDecksAtPath[backupPath]),
            )
            assertEquals(3L, remoteVersionRepository.insertedVersionsAtPath[backupPath]?.single()?.version)
        }

    @Test
    fun `backup failure on missing local image leaves remote backup data untouched`() = runTest {
        val backupPath = "backup@example.com"
        val localCard = testCard(id = 1, deckId = 1, imageAssetId = "missing-asset")
        val remoteDeckRepository = TestDeckRepository()
        val remoteCardRepository = TestCardRepository()
        val remoteVersionRepository = TestStorageSaveVersionRepository()
        val remoteImageRepository = TestMnemonicImageRemoteRepository()
        val useCase = BackupDataUseCase(
            localDeckRepository = TestDeckRepository(decks = listOf(testDeck(id = 1))),
            localCardRepository = TestCardRepository(cards = listOf(localCard)),
            localStorageSaveVersionRepository = TestStorageSaveVersionRepository(initialVersion = 2),
            localMnemonicImageAssetRepository = TestMnemonicImageAssetRepository(),
            remoteDeckRepository = remoteDeckRepository,
            remoteCardRepository = remoteCardRepository,
            remoteStorageSaveVersionRepository = remoteVersionRepository,
            remoteMnemonicImageRepository = remoteImageRepository,
            coroutineContextProvider = TestCoroutineContextProvider(
                io = UnconfinedTestDispatcher(testScheduler),
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            useCase.invoke(backupPath = backupPath)
        }

        assertTrue(remoteImageRepository.scopedUploadCalls.isEmpty())
        assertTrue(remoteCardRepository.insertedCardsAtPath.isEmpty())
        assertTrue(remoteDeckRepository.insertedDecksAtPath.isEmpty())
        assertTrue(remoteVersionRepository.insertedVersionsAtPath.isEmpty())
    }
}
