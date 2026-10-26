package com.kuts.klaf.vocabularySource

import androidx.lifecycle.ViewModelStore
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.*
import com.kuts.domain.managers.*
import com.kuts.domain.repositories.*
import com.kuts.domain.useCases.*
import com.kuts.domain.vocabularySource.VocabularySourceItemKey
import com.kuts.domain.vocabularySource.reviewKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

private class DetailTestSources : IVocabularySourceRepository {
    val source = MutableStateFlow<VocabularySource?>(VocabularySource("Initial", createdAt = 1, updatedAt = 1, id = 1))
    val items = MutableStateFlow(emptyList<VocabularySourceItem>())
    var saveGate: CompletableDeferred<Unit>? = null
    override fun observeSources() = flowOf(listOf(source.value!!))
    override fun observeSourceById(sourceId: Int) = source
    override fun observeItemsBySourceId(sourceId: Int) = items
    override fun observeAllItems() = items
    override suspend fun fetchSources() = listOf(source.value!!)
    override suspend fun fetchSourceById(sourceId: Int) = source.value
    override suspend fun fetchItemsBySourceId(sourceId: Int) = items.value
    override suspend fun fetchItemById(itemId: Int) = items.value.firstOrNull { it.id == itemId }
    override suspend fun saveSource(source: VocabularySource): Int {
        saveGate?.await()
        this.source.value = source
        return source.id
    }
    override suspend fun saveItems(items: List<VocabularySourceItem>) { this.items.value = items }
    override suspend fun replaceNotAddedItemsBySourceId(sourceId: Int, items: List<VocabularySourceItem>) { this.items.value = items }
    override suspend fun removeSource(sourceId: Int) { source.value = null }
    override suspend fun removeItemsBySourceId(sourceId: Int) { items.value = emptyList() }
    override suspend fun removeItem(itemId: Int) = Unit
}

private object DetailTestCards : ICardRepository {
    override fun observeAllCards() = kotlinx.coroutines.flow.flow { emit(fetchAllCards()) }
    override suspend fun fetchAllCards() = emptyList<Card>()
    override suspend fun fetchCardQuantityByDeckId(deckId: Int) = 0
    override suspend fun insertCard(card: Card) = error("Unused")
    override suspend fun insertCardAtPath(card: Card, rootEmailPath: String) = Unit
    override fun fetchObservableCardById(cardId: Int) = flowOf<Card?>(null)
    override fun fetchObservableCardsByDeckId(deckId: Int) = flowOf(emptyList<Card>())
    override suspend fun fetchCardsByDeckId(deckId: Int) = emptyList<Card>()
    override suspend fun deleteCard(cardId: Int) = Unit
    override suspend fun removeCardsOfDeck(deckId: Int) = Unit
    override suspend fun checkIfCardExists(foreignWord: String) = emptyList<Deck>()
}

private object DetailTestDecks : IDeckRepository {
    override fun fetchDeckSource() = flowOf(emptyList<Deck>())
    override suspend fun fetchAllDecks() = emptyList<Deck>()
    override fun fetchObservableDeckById(deckId: Int) = flowOf<Deck?>(null)
    override suspend fun insertDeck(deck: Deck) = error("Unused")
    override suspend fun insertDeckAtPath(deck: Deck, rootEmailPath: String) = Unit
    override suspend fun removeDeck(deckId: Int) = Unit
    override suspend fun getDeckById(deckId: Int): Deck? = null
    override suspend fun getCardQuantityInDeck(deckId: Int) = 0
}

private object DetailTestVersions : IStorageSaveVersionRepository {
    override suspend fun fetchVersion(): StorageSaveVersion? = null
    override suspend fun insertVersion(version: StorageSaveVersion) = Unit
    override suspend fun insertVersionAtPath(version: StorageSaveVersion, rootEmailPath: String) = Unit
    override suspend fun increaseVersion() = Unit
}

private object DetailTestTransaction : IStorageTransactionRepository {
    override suspend fun <R> performWithTransaction(block: suspend () -> R) = block()
}

private object DetailTestIgnored : IIgnoredVocabularyWordRepository {
    override fun observeWords() = kotlinx.coroutines.flow.flow { emit(fetchWords()) }
    override suspend fun removeWords(keys: Set<com.kuts.domain.vocabularySource.VocabularySourceItemKey>) = Unit
    override suspend fun fetchWords() = emptyList<IgnoredVocabularyWord>()
    override suspend fun saveWords(words: List<IgnoredVocabularyWord>) = Unit
}

private object DetailTestAnalysis : IVocabularySourceAnalysisRepository {
    override suspend fun analyze(cleanText: String, sourceId: Int?, sourceTitle: String?) =
        VocabularySourceAnalysisResult(VocabularySourceAnalysis("en", emptyList()), false)
}

private class DetailReviewCards : ICardRepository by DetailTestCards {
    val records = MutableStateFlow(emptyList<Card>())
    override fun observeAllCards(): Flow<List<Card>> = records
    override suspend fun fetchAllCards(): List<Card> = records.value
}

private class DetailReviewIgnored : IIgnoredVocabularyWordRepository {
    val records = MutableStateFlow(emptyList<IgnoredVocabularyWord>())
    override fun observeWords(): Flow<List<IgnoredVocabularyWord>> = records
    override suspend fun fetchWords(): List<IgnoredVocabularyWord> = records.value
    override suspend fun saveWords(words: List<IgnoredVocabularyWord>) {
        records.value = (records.value + words).distinctBy { it.reviewKey() }
    }
    override suspend fun removeWords(keys: Set<VocabularySourceItemKey>) {
        records.value = records.value.filterNot { it.reviewKey() in keys }
    }
}

private object DetailTestTranscription : IVocabularySourceTranscriptionRepository {
    override fun transcribe(sourceId: Int?, sourceTitle: String?, fileName: String, audioFormat: String,
        byteSize: Long, audioSource: suspend (suspend (ByteArray) -> Unit) -> Unit): Flow<VocabularySourceTranscriptionUpdate> = error("Unused")
}

private class DetailGateTranscription : IVocabularySourceTranscriptionRepository {
    val result = CompletableDeferred<VocabularySourceTranscriptionUpdate.Success>()
    override fun transcribe(sourceId: Int?, sourceTitle: String?, fileName: String, audioFormat: String,
        byteSize: Long, audioSource: suspend (suspend (ByteArray) -> Unit) -> Unit): Flow<VocabularySourceTranscriptionUpdate> = flow {
        emit(result.await())
    }
}

private class DetailGateAnalysis : IVocabularySourceAnalysisRepository {
    val gate = CompletableDeferred<Unit>()
    override suspend fun analyze(cleanText: String, sourceId: Int?, sourceTitle: String?): VocabularySourceAnalysisResult {
        gate.await()
        return VocabularySourceAnalysisResult(VocabularySourceAnalysis("en", emptyList()), false)
    }
}

private object DetailTestAnalysisBackground : IVocabularySourceAnalysisBackgroundManager {
    override fun startAnalysis(sourceId: Int, sourceTitle: String) = VocabularySourceAnalysisHandle { _, _ -> }
}

private object DetailTestTranscriptionBackground : IVocabularySourceTranscriptionBackgroundManager {
    override fun startTranscription(sourceId: Int, sourceTitle: String) = VocabularySourceTranscriptionHandle { _, _ -> }
}

private object DetailTestSpeech : ITextToSpeechManager {
    override fun speak(text: String) = Unit
    override fun stop() = Unit
    override fun shutdown() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularySourceDetailViewModelTest {
    @Test
    fun successPushMustNotSuppressLaterLocalProcessingFailure() = runTest {
        var completion: Pair<VocabularySourceAnalysisOutcome, Boolean>? = null
        val background = object : IVocabularySourceAnalysisBackgroundManager {
            override fun startAnalysis(sourceId: Int, sourceTitle: String) = VocabularySourceAnalysisHandle { outcome, pushed ->
                completion = outcome to pushed
            }
        }
        val analysis = object : IVocabularySourceAnalysisRepository {
            override suspend fun analyze(cleanText: String, sourceId: Int?, sourceTitle: String?) =
                VocabularySourceAnalysisResult(VocabularySourceAnalysis("en", emptyList()), true)
        }
        val cards = object : ICardRepository by DetailTestCards {
            override suspend fun fetchAllCards(): List<Card> = error("Synthetic local processing failure")
        }
        withViewModel(analysis = analysis, cards = cards, analysisBackground = background) { viewModel, _ ->
            viewModel.onRawTextChanged("A science project.")
            viewModel.analyze()
            advanceUntilIdle()
            assertEquals(VocabularySourceAnalysisOutcome.Failed to false, completion)
        }
    }

    @Test
    fun failedAnalysisPassesPushDeliveryToBackgroundManagerIncludingRetryErrors() = runTest {
        for (sent in listOf(false, true)) {
            var completion: Pair<VocabularySourceAnalysisOutcome, Boolean>? = null
            val background = object : IVocabularySourceAnalysisBackgroundManager {
                override fun startAnalysis(sourceId: Int, sourceTitle: String) = VocabularySourceAnalysisHandle { outcome, pushed ->
                    completion = outcome to pushed
                }
            }
            val analysis = object : IVocabularySourceAnalysisRepository {
                override suspend fun analyze(cleanText: String, sourceId: Int?, sourceTitle: String?): VocabularySourceAnalysisResult {
                    throw VocabularyAnalysisRetryAvailableException("retry-token", sent)
                }
            }
            withViewModel(analysis = analysis, analysisBackground = background) { viewModel, _ ->
                viewModel.onRawTextChanged("A science project.")
                viewModel.analyze()
                advanceUntilIdle()
                assertEquals(VocabularySourceAnalysisOutcome.Failed to sent, completion)
                assertTrue(viewModel.state.value.canRetryAnalysis)
            }
        }
    }

    @Test
    fun clearAnalysisDropsRetryCheckpointAndNextAnalyzeStartsFresh() = runTest {
        var fullCalls = 0
        val analysis = object : IVocabularySourceAnalysisRepository {
            override suspend fun analyze(cleanText: String, sourceId: Int?, sourceTitle: String?): VocabularySourceAnalysisResult {
                fullCalls++
                throw VocabularyAnalysisRetryAvailableException("old-checkpoint")
            }
            override suspend fun retry(cleanText: String, analysisRetryToken: String, sourceId: Int?, sourceTitle: String?) =
                error("Clear analysis must discard the old retry checkpoint")
        }
        withViewModel(analysis = analysis) { viewModel, _ ->
            viewModel.onRawTextChanged("A science project.")
            viewModel.analyze()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.canRetryAnalysis)
            viewModel.resetAnalysis()
            advanceUntilIdle()
            assertEquals(false, viewModel.state.value.canRetryAnalysis)
            viewModel.analyze()
            advanceUntilIdle()
            assertEquals(2, fullCalls)
        }
    }
    @Test
    fun retryActionReusesServerAnalysisAndTextEditsStartFreshAnalysis() = runTest {
        var fullCalls = 0
        var retryCalls = 0
        val analysis = object : IVocabularySourceAnalysisRepository {
            override suspend fun analyze(cleanText: String, sourceId: Int?, sourceTitle: String?): VocabularySourceAnalysisResult {
                fullCalls++
                throw VocabularyAnalysisRetryAvailableException("retry-token")
            }
            override suspend fun retry(
                cleanText: String,
                analysisRetryToken: String,
                sourceId: Int?,
                sourceTitle: String?,
            ): VocabularySourceAnalysisResult {
                retryCalls++
                assertEquals("retry-token", analysisRetryToken)
                return VocabularySourceAnalysisResult(VocabularySourceAnalysis("en", emptyList()))
            }
        }
        withViewModel(analysis = analysis) { viewModel, sources ->
            viewModel.onRawTextChanged("A science project.")
            viewModel.analyze()
            advanceUntilIdle()
            viewModel.analyze()
            advanceUntilIdle()
            assertEquals(1, fullCalls)
            assertEquals(1, retryCalls)
            assertTrue(viewModel.state.value.hasNewAnalysisDraft)
            assertTrue(sources.items.value.isEmpty())
            viewModel.onRawTextChanged("A different project.")
            viewModel.analyze()
            advanceUntilIdle()
            assertEquals(2, fullCalls)
        }
    }

    @Test
    fun cardChangesRefreshCandidatesWithoutReanalysisOrLosingSourceEdits() = runTest {
        val cards = DetailReviewCards()
        withViewModel(cards = cards) { viewModel, sources ->
            sources.items.value = listOf(VocabularySourceItem(1, foreignWord = "cat", nativeWord = "кот",
                category = VocabularySourceItemCategory.NEW, createdAt = 1, updatedAt = 1))
            runCurrent()
            viewModel.onTitleChanged("Keep this edit")
            assertEquals(1, viewModel.state.value.displayedItems.size)
            cards.records.value = listOf(Card(1, "кот", "cat", emptyList(), id = 7))
            runCurrent()
            assertTrue(viewModel.state.value.displayedItems.isEmpty())
            cards.records.value = emptyList()
            runCurrent()
            assertEquals(VocabularySourceItemStatus.PENDING, viewModel.state.value.displayedItems.single().status)
            assertEquals("Keep this edit", viewModel.state.value.title)
            assertTrue(viewModel.state.value.hasUnsavedChanges)
        }
    }

    @Test
    fun restoringIgnoredItemIsLocalUntilSavedAndSurvivesContextRefresh() = runTest {
        val cards = DetailReviewCards()
        val ignored = DetailReviewIgnored()
        ignored.records.value = listOf(IgnoredVocabularyWord("en", "cat", "кот", 1))
        withViewModel(cards = cards, ignored = ignored) { viewModel, sources ->
            sources.items.value = listOf(VocabularySourceItem(1, language = "en", foreignWord = "cat", nativeWord = "кот",
                category = VocabularySourceItemCategory.NEW, status = VocabularySourceItemStatus.IGNORED,
                createdAt = 1, updatedAt = 1))
            runCurrent()
            viewModel.changeItemSelectionState(0)
            viewModel.restoreSelectedItems()
            assertEquals(1, ignored.records.value.size)
            assertEquals(VocabularySourceItemStatus.PENDING, viewModel.state.value.displayedItems.single().status)
            cards.records.value = listOf(Card(1, "собака", "dog", emptyList(), id = 9))
            runCurrent()
            assertEquals(VocabularySourceItemStatus.PENDING, viewModel.state.value.displayedItems.single().status)
            viewModel.save()
            runCurrent()
            assertTrue(ignored.records.value.isEmpty())
            assertTrue(!viewModel.state.value.hasUnsavedChanges)
        }
    }

    @Test
    fun leavingAndRecreatingViewModelDoesNotCancelTranscriptionOrLoseResult() = runTest {
        val repository = DetailGateTranscription()
        val coordinator = VocabularySourceTranscriptionCoordinator(TranscribeVocabularySourceAudioUseCase(repository),
            DetailTestTranscriptionBackground, backgroundScope)
        withViewModel(coordinator = coordinator) { original, _ ->
            original.onAudioFileSelected("test.wav", 1, "audio/wav") {}
            original.onTranscribeAudio()
            runCurrent()
            ViewModelStore().apply { put("original", original); clear() }
            repository.result.complete(VocabularySourceTranscriptionUpdate.Success(VocabularySourceTranscriptionResult("New transcript", emptyList())))
            runCurrent()
            withViewModel(coordinator = coordinator) { returned, _ ->
                assertEquals("New transcript", returned.state.value.rawText)
                assertTrue(returned.state.value.hasUnsavedChanges)
                returned.save()
                runCurrent()
                returned.onTitleChanged("Later title")
                runCurrent()
                assertEquals("New transcript", returned.state.value.rawText)
            }
        }
    }

    @Test
    fun oversizedTranscriptionKeepsExistingTextAndShowsFailure() = runTest {
        val repository = DetailGateTranscription()
        val coordinator = VocabularySourceTranscriptionCoordinator(TranscribeVocabularySourceAudioUseCase(repository),
            DetailTestTranscriptionBackground, backgroundScope)
        withViewModel(coordinator = coordinator) { viewModel, _ ->
            viewModel.onRawTextChanged("Existing text")
            viewModel.onAudioFileSelected("test.wav", 1, "audio/wav") {}
            viewModel.onTranscribeAudio()
            repository.result.complete(VocabularySourceTranscriptionUpdate.Success(
                VocabularySourceTranscriptionResult("x".repeat(VocabularySource.MAX_TEXT_LENGTH + 1), emptyList())))
            runCurrent()
            assertEquals("Existing text", viewModel.state.value.rawText)
            assertIs<TranscriptionUiState.Failed>(viewModel.state.value.transcriptionState)
        }
    }

    @Test
    fun textEditedDuringAnalysisMakesTheReturnedItemsStale() = runTest {
        val analysis = DetailGateAnalysis()
        withViewModel(analysis = analysis) { viewModel, _ ->
            viewModel.onRawTextChanged("Original text")
            viewModel.analyze()
            runCurrent()
            viewModel.onRawTextChanged("New text")
            analysis.gate.complete(Unit)
            runCurrent()
            assertTrue(viewModel.state.value.hasStaleItems)
            viewModel.save()
            runCurrent()
            assertTrue(viewModel.state.value.hasStaleItems)
        }
    }

    @Test
    fun sequentialEditsWithoutACollectorTickAreNotLost() = runTest {
        withViewModel { viewModel, _ ->
            viewModel.onTitleChanged("New title")
            viewModel.onDescriptionChanged("New description")
            runCurrent()
            assertEquals("New title", viewModel.state.value.title)
            assertEquals("New description", viewModel.state.value.description)
        }
    }

    @Test
    fun editingWhileAnOlderSnapshotIsSavingRemainsUnsaved() = runTest {
        withViewModel { viewModel, sources ->
            sources.saveGate = CompletableDeferred()
            viewModel.onTitleChanged("Saved title")
            runCurrent()
            viewModel.save()
            runCurrent()
            viewModel.onTitleChanged("Later title")
            runCurrent()
            sources.saveGate!!.complete(Unit)
            runCurrent()
            assertEquals("Saved title", sources.source.value!!.title)
            assertEquals("Later title", viewModel.state.value.title)
            assertTrue(viewModel.state.value.hasUnsavedChanges)
        }
    }

    private suspend fun TestScope.withViewModel(
        coordinator: VocabularySourceTranscriptionCoordinator = VocabularySourceTranscriptionCoordinator(
            TranscribeVocabularySourceAudioUseCase(DetailTestTranscription), DetailTestTranscriptionBackground, backgroundScope),
        analysis: IVocabularySourceAnalysisRepository = DetailTestAnalysis,
        cards: ICardRepository = DetailTestCards,
        ignored: IIgnoredVocabularyWordRepository = DetailTestIgnored,
        analysisBackground: IVocabularySourceAnalysisBackgroundManager = DetailTestAnalysisBackground,
        block: suspend TestScope.(VocabularySourceDetailViewModel, DetailTestSources) -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val sources = DetailTestSources()
        val context = object : ICoroutineContextProvider { override val io = dispatcher }
        val viewModel = VocabularySourceDetailViewModel(1, ObserveVocabularySourceByIdUseCase(sources),
            ObserveVocabularySourceItemsUseCase(sources),
            ObserveVocabularySourceReviewContextUseCase(cards, ignored),
            SaveVocabularySourceChangesUseCase(sources, ignored, DetailTestVersions, DetailTestTransaction, context),
            ResetVocabularySourceAnalysisUseCase(sources, DetailTestVersions, DetailTestTransaction, context),
            AnalyzeVocabularySourceTextUseCase(analysis, context), coordinator,
            FetchAllCardsUseCase(cards, context), FetchIgnoredVocabularyWordsUseCase(ignored, context),
            FetchDeckSourceUseCase(DetailTestDecks),
            AddVocabularySourceItemsToDeckUseCase(cards, DetailTestDecks, sources, DetailTestVersions,
                DetailTestTransaction, context, ignored),
            analysisBackground, DetailTestSpeech)
        val store = ViewModelStore().apply { put("test", viewModel) }
        try {
            runCurrent()
            block(viewModel, sources)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
