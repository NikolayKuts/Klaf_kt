package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.CardMnemonic
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.MnemonicIllustration
import com.kuts.domain.entities.MnemonicImageAsset
import com.kuts.domain.entities.MnemonicImageAssetStorage
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import com.kuts.domain.repositories.IMnemonicImageRemoteRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.coroutines.CoroutineContext

internal class TestCoroutineContextProvider(
    override val io: CoroutineContext,
) : ICoroutineContextProvider

internal class TestCardRepository(
    cards: List<Card> = emptyList(),
) : ICardRepository {

    private val cardsById = cards.associateBy { it.id }.toMutableMap()
    private var nextId = (cards.maxOfOrNull { it.id } ?: 0) + 1

    val insertedCardsAtPath = mutableMapOf<String, MutableList<Card>>()

    override suspend fun fetchCardQuantityByDeckId(deckId: Int): Int {
        return cardsById.values.count { card -> card.deckId == deckId }
    }

    override suspend fun fetchAllCards(): List<Card> {
        return cardsById.values.sortedBy { card -> card.id }
    }

    override suspend fun insertCard(card: Card): Int {
        val targetId = if (card.id == 0) nextId++ else card.id
        cardsById[targetId] = card.copy(id = targetId)
        return targetId
    }

    override suspend fun insertCardAtPath(card: Card, rootEmailPath: String) {
        insertedCardsAtPath.getOrPut(rootEmailPath) { mutableListOf() }.add(card)
    }

    override fun fetchObservableCardById(cardId: Int): Flow<Card?> = flowOf(cardsById[cardId])

    override fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>> {
        return flowOf(cardsById.values.filter { card -> card.deckId == deckId }.sortedBy(Card::id))
    }

    override suspend fun fetchCardsByDeckId(deckId: Int): List<Card> {
        return cardsById.values.filter { card -> card.deckId == deckId }.sortedBy(Card::id)
    }

    override suspend fun deleteCard(cardId: Int) {
        cardsById.remove(cardId)
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        cardsById.entries.removeAll { (_, card) -> card.deckId == deckId }
    }

    override suspend fun checkIfCardExists(foreignWord: String): List<Deck> {
        throw UnsupportedOperationException()
    }
}

internal class TestDeckRepository(
    decks: List<Deck> = emptyList(),
) : IDeckRepository {

    private val decksById = decks.associateBy { it.id }.toMutableMap()

    val insertedDecksAtPath = mutableMapOf<String, MutableList<Deck>>()

    override fun fetchDeckSource(): Flow<List<Deck>> {
        return flowOf(decksById.values.sortedBy { deck -> deck.id })
    }

    override suspend fun fetchAllDecks(): List<Deck> {
        return decksById.values.sortedBy { deck -> deck.id }
    }

    override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> = flowOf(decksById[deckId])

    override suspend fun insertDeck(deck: Deck): Int {
        decksById[deck.id] = deck
        return deck.id
    }

    override suspend fun insertDeckAtPath(deck: Deck, rootEmailPath: String) {
        insertedDecksAtPath.getOrPut(rootEmailPath) { mutableListOf() }.add(deck)
    }

    override suspend fun removeDeck(deckId: Int) {
        decksById.remove(deckId)
    }

    override suspend fun getDeckById(deckId: Int): Deck? = decksById[deckId]

    override suspend fun getCardQuantityInDeck(deckId: Int): Int {
        return decksById[deckId]?.cardQuantity ?: 0
    }
}

internal class TestStorageSaveVersionRepository(
    initialVersion: Long? = null,
) : IStorageSaveVersionRepository {

    var currentVersion: StorageSaveVersion? = initialVersion?.let(::StorageSaveVersion)
        private set

    val insertedVersions = mutableListOf<StorageSaveVersion>()
    val insertedVersionsAtPath = mutableMapOf<String, MutableList<StorageSaveVersion>>()

    override suspend fun fetchVersion(): StorageSaveVersion? = currentVersion

    override suspend fun insertVersion(version: StorageSaveVersion) {
        currentVersion = version
        insertedVersions += version
    }

    override suspend fun insertVersionAtPath(
        version: StorageSaveVersion,
        rootEmailPath: String,
    ) {
        insertedVersionsAtPath.getOrPut(rootEmailPath) { mutableListOf() }.add(version)
    }

    override suspend fun increaseVersion() {
        currentVersion = StorageSaveVersion(
            version = (currentVersion?.version ?: StorageSaveVersion.INITIAL_SAVE_VERSION) + 1,
        )
    }
}

internal class TestMnemonicImageAssetRepository(
    initialSavedImages: Map<String, ByteArray> = emptyMap(),
) : IMnemonicImageAssetRepository {

    private val draftImages = mutableMapOf<String, ByteArray>()
    val savedImages = initialSavedImages.mapValues { (_, bytes) -> bytes.copyOf() }.toMutableMap()
    val importedSavedAssetIds = mutableListOf<String>()
    val deletedSavedAssetIds = mutableListOf<String>()
    val readSavedImageAssetIds = mutableListOf<String>()

    override suspend fun createDraftImage(imageBytes: ByteArray): MnemonicImageAsset {
        val assetId = "draft-${draftImages.size + 1}"
        draftImages[assetId] = imageBytes.copyOf()
        return MnemonicImageAsset(
            assetId = assetId,
            filePath = "/draft/$assetId.png",
            storage = MnemonicImageAssetStorage.Draft,
        )
    }

    override suspend fun createSavedCopyFromDraft(draftAssetId: String): MnemonicImageAsset {
        val imageBytes = requireNotNull(draftImages[draftAssetId]) {
            "Draft mnemonic image is missing. assetId=$draftAssetId"
        }
        val assetId = "saved-${savedImages.size + 1}"
        savedImages[assetId] = imageBytes.copyOf()
        return MnemonicImageAsset(
            assetId = assetId,
            filePath = "/saved/$assetId.png",
            storage = MnemonicImageAssetStorage.Saved,
        )
    }

    override suspend fun importSavedImage(assetId: String, imageBytes: ByteArray): MnemonicImageAsset {
        savedImages[assetId] = imageBytes.copyOf()
        importedSavedAssetIds += assetId
        return MnemonicImageAsset(
            assetId = assetId,
            filePath = "/saved/$assetId.png",
            storage = MnemonicImageAssetStorage.Saved,
        )
    }

    override suspend fun resolveSavedImage(assetId: String): MnemonicImageAsset? {
        return savedImages[assetId]?.let {
            MnemonicImageAsset(
                assetId = assetId,
                filePath = "/saved/$assetId.png",
                storage = MnemonicImageAssetStorage.Saved,
            )
        }
    }

    override suspend fun readSavedImageBytes(assetId: String): ByteArray? {
        readSavedImageAssetIds += assetId
        return savedImages[assetId]?.copyOf()
    }

    override suspend fun deleteDraftImage(assetId: String) {
        draftImages.remove(assetId)
    }

    override suspend fun deleteSavedImage(assetId: String) {
        savedImages.remove(assetId)
        deletedSavedAssetIds += assetId
    }

    override suspend fun clearAllDraftImages() {
        draftImages.clear()
    }
}

internal class TestMnemonicImageRemoteRepository(
    initialImages: Map<String, ByteArray> = emptyMap(),
    private val failingUploadAssetIds: Set<String> = emptySet(),
    override val isEnabled: Boolean = true,
) : IMnemonicImageRemoteRepository {

    val remoteImages = initialImages.mapValues { (_, bytes) -> bytes.copyOf() }.toMutableMap()
    val defaultUploadCalls = mutableListOf<String>()
    val scopedUploadCalls = mutableMapOf<String, MutableList<String>>()
    val deletedAssetIds = mutableListOf<String>()
    val downloadCalls = mutableListOf<String>()

    override suspend fun uploadImage(assetId: String, imageBytes: ByteArray) {
        failIfRequired(assetId = assetId)
        defaultUploadCalls += assetId
        remoteImages[assetId] = imageBytes.copyOf()
    }

    override suspend fun uploadImageAtPath(
        assetId: String,
        imageBytes: ByteArray,
        rootEmailPath: String,
    ) {
        failIfRequired(assetId = assetId)
        scopedUploadCalls.getOrPut(rootEmailPath) { mutableListOf() }.add(assetId)
        remoteImages["$rootEmailPath::$assetId"] = imageBytes.copyOf()
    }

    override suspend fun downloadImage(assetId: String): ByteArray? {
        downloadCalls += assetId
        return remoteImages[assetId]?.copyOf()
    }

    override suspend fun deleteImage(assetId: String) {
        deletedAssetIds += assetId
        remoteImages.remove(assetId)
    }

    private fun failIfRequired(assetId: String) {
        if (assetId in failingUploadAssetIds) {
            throw IllegalStateException("Upload failed for assetId=$assetId")
        }
    }
}

internal fun testDeck(
    id: Int,
    name: String = "deck-$id",
    cardQuantity: Int = 1,
): Deck {
    return Deck(
        name = name,
        creationDate = id * 1_000L,
        reviewPassDates = listOf(id.toLong()),
        scheduledReviewDates = listOf(id.toLong() * 2),
        scheduledDateInterval = 100L + id,
        reviewCount = 1,
        cardQuantity = cardQuantity,
        lastFirstReviewDuration = 10L,
        lastSecondReviewDuration = 20L,
        lastReviewPassDuration = 30L,
        isLastPassSucceeded = true,
        id = id,
    )
}

internal fun testCard(
    id: Int,
    deckId: Int,
    imageAssetId: String? = null,
    nativeWord: String = "native-$id",
    foreignWord: String = "foreign-$id",
): Card {
    return Card(
        id = id,
        deckId = deckId,
        nativeWord = nativeWord,
        foreignWord = foreignWord,
        ipa = emptyList(),
        wordMeaningInsights = WordMeaningInsights.EMPTY,
        mnemonic = imageAssetId?.let { assetId ->
            CardMnemonic(
                selectedIllustration = MnemonicIllustration(imageAssetId = assetId),
            )
        } ?: CardMnemonic.EMPTY,
    )
}
