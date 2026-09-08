package com.kuts.klaf.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.DebouncedMutableStateFlow
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.generateLetterInfos
import com.kuts.domain.common.ifTrue
import com.kuts.domain.common.updatedAt
import com.kuts.domain.entities.AgentDriverConnectionState
import com.kuts.domain.entities.CardMnemonic
import com.kuts.domain.entities.Deck
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.entities.MnemonicIllustration
import com.kuts.domain.entities.MnemonicImageAsset
import com.kuts.domain.entities.MnemonicImageAssetStorage
import com.kuts.domain.entities.canSendRequests
import com.kuts.domain.entities.toSelections
import com.kuts.domain.ipa.toRowIpaItemHolders
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IMnemonicGenerationBackgroundManager
import com.kuts.domain.managers.ISpeechRecognitionManager
import com.kuts.domain.managers.MnemonicGenerationHandle
import com.kuts.domain.managers.MnemonicGenerationOutcome
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.managers.MnemonicGenerationType
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordInfoRepository.IWordInfoLoadingError
import com.kuts.domain.useCases.CheckIfCardExistsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchMnemonicAssociationUseCase
import com.kuts.domain.useCases.FetchMnemonicImageUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.domain.useCases.ObserveAgentDriverConnectionStateUseCase
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.cardManagement.cardAddition.NativeWordSuggestionItem
import com.kuts.klaf.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.permissions.IMicrophonePermissionManager
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class CardManagementViewModel(
    deckId: Int,
    audioPlayer: IAudioPlayerManager,
    cambridgeWordDataProvider: ICambridgeWordDataProvider,
    private val fetchMnemonicAssociation: FetchMnemonicAssociationUseCase,
    private val fetchMnemonicImage: FetchMnemonicImageUseCase,
    protected val mnemonicImageAssetRepository: IMnemonicImageAssetRepository,
    private val mnemonicGenerationBackgroundManager: IMnemonicGenerationBackgroundManager,
    private val mnemonicGenerationSource: MnemonicGenerationSource,
    private val fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    private val fetchWordInfo: FetchWordInfoUseCase,
    speechRecognitionManager: ISpeechRecognitionManager,
    microphonePermissionManager: IMicrophonePermissionManager,
    observeAgentDriverConnectionState: ObserveAgentDriverConnectionStateUseCase,
    protected val crashlytics: ICrashlyticsRepository,
    protected val checkIfWordExists: CheckIfCardExistsUseCase,
    protected val coroutineContextProvider: ICoroutineContextProvider,
    fetchDeckById: FetchDeckByIdUseCase,
) : BaseCardManagementViewModel(
    audioPlayer = audioPlayer,
    cambridgeWordDataProvider = cambridgeWordDataProvider
) {

    companion object {

        private const val MNEMONIC_VARIANT_ID_PREFIX = "mnemonic_variant_"
        private const val MNEMONIC_IMAGE_ID_PREFIX = "mnemonic_image_"

        private val ipaKeys = listOf(
            "θ", "ð", "ʃ", "ʒ", "ŋ", "tʃ", "dʒ", "ʔ", "ɹ",
            "æ", "ʌ", "ɜː", "ə", "ɪ", "ʊ", "ɔː", "ɒ",
            "aɪ", "eɪ", "aʊ", "ɔɪ", "əʊ", "ɪə", "eə", "ʊə"
        )
    }

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
            // logE("Failed to fetch deck for card management\n${throwable.stackTraceToString()}")
            eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val autocompleteState = DebouncedMutableStateFlow(value = AutocompleteState())
    override val pronunciationLoadingState: StateFlow<LoadingState<Unit, Unit>> =
        audioPlayer.loadingState
    override val nativeWordSuggestionsState = MutableStateFlow(value = NativeWordSuggestionsState())
    override val transcriptionState = MutableStateFlow(value = "")

    override val cardManagementState =
        MutableStateFlow<CardManagementState>(value = CardManagementState.InProgress())
    protected val nativeWordFieldValueState = MutableStateFlow(value = TextFieldValue())
    protected val foreignWordFieldValueState = MutableStateFlow(value = TextFieldValue())
    protected val textFieldValueIpaHoldersState =
        MutableStateFlow<List<TextFieldValueIpaHolder>>(value = emptyList())
    protected val letterInfosState = MutableStateFlow<List<LetterInfo>>(value = emptyList())
    override val isConfirmationEnabled: StateFlow<Boolean> = combine(
        nativeWordFieldValueState,
        foreignWordFieldValueState,
    ) { nativeWordFieldValue, foreignWordFieldValue ->
        nativeWordFieldValue.text.isNotBlank() && foreignWordFieldValue.text.isNotBlank()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    override val cambridgeDataState = MutableStateFlow<ICambridgeDataState>(
        value = ICambridgeDataState.Empty
    )

    override val ipaKeyboardState = MutableStateFlow(value = IpaKeyboardState(keys = ipaKeys))
    override val mnemonicManagementState = MutableStateFlow(MnemonicManagementUiState())
    override val agentDriverConnectionState: StateFlow<AgentDriverConnectionState> =
        observeAgentDriverConnectionState()
    private var nextMnemonicVariantId = 0L
    private var nextMnemonicImageVariantId = 0L
    private var mnemonicAssociationRequestJob: Job? = null
    private var mnemonicImageRequestJob: Job? = null

    private val speechInputCoordinator = MnemonicSpeechInputCoordinator(
        speechRecognitionManager = speechRecognitionManager,
        microphonePermissionManager = microphonePermissionManager,
        scope = viewModelScope,
    )

    init {
        combineAndObserveCardManagementChanges()
        observeForeignWordChanges()
        observeTextFieldValueIpaHoldersState()
        observeMnemonicSpeechInput()
    }

    protected open suspend fun onForeignWordChanged(word: String) {
        if (word.isEmpty()) return
        viewModelScope.launch(coroutineContextProvider.io) {
            val wordData = cambridgeWordDataProvider.fetchWordData(word = word)

            if (wordData == null) {
                cambridgeDataState.value = ICambridgeDataState.Empty
            } else {
                cambridgeDataState.value = ICambridgeDataState.Fetched(word = wordData)
            }
        }
    }

    override fun sendAction(action: ICardManagementAction) {
        when (action) {
            is ICardManagementAction.ChangeLetterSelectionWithIpaTemplate -> {
                changeLetterSelectionWithIpaTemplate(
                    index = action.index,
                    letterInfo = action.letterInfo
                )
            }

            is ICardManagementAction.UpdateDataOnForeignWordChanged -> {
                updateDataOnForeignWordChanged(wordFieldValue = action.wordFieldValue)
            }

            is ICardManagementAction.UpdateDataOnAutocompleteSelected -> {
                updateDataOnAutocompleteSelected(word = action.word)
            }

            is ICardManagementAction.NativeWordSelected -> {
                updateDataOnNativeWordSelected(wordIndex = action.wordIndex)
            }

            ICardManagementAction.ConfirmSuggestionsSelection -> {
                handleNativeWordSuggestionsSelectionConfirmation()
            }

            ICardManagementAction.ClearNativeWordSuggestionsSelectionClicked -> {
                handleClearNativeWordSuggestionsSelectionClicked()
            }

            is ICardManagementAction.UpdateIpa -> {
                updateIpa(letterGroupIndex = action.letterGroupIndex, ipaTextFieldValue = action.ipa)
            }

            is ICardManagementAction.UpdateNativeWord -> {
                updateNativeWord(wordFieldValue = action.wordFieldValue)
            }

            is ICardManagementAction.CardManagementConfirmed -> {
                onCardManagementConfirmed()
            }

            is ICardManagementAction.IpaTextFieldFocusChanged -> {
                handleIpaTextFieldFocusChanged(action = action)
            }

            ICardManagementAction.PronounceForeignWordClicked -> {
                audioPlayer.play()
            }

            ICardManagementAction.NativeWordFieldIconClicked -> {
                autocompleteState.update { it.copy(isActive = false) }
                nativeWordSuggestionsState.update { it.copy(isActive = !it.isActive) }
            }

            ICardManagementAction.CloseAutocompleteMenu -> {
                autocompleteState.update { it.copy(isActive = false) }
            }

            ICardManagementAction.CloseNativeWordSuggestionsMenu -> {
                nativeWordSuggestionsState.update { it.copy(isActive = false) }
            }
        }
    }

    override fun updateMnemonicRequestComment(value: TextFieldValue) {
        mnemonicManagementState.update { state -> state.copy(requestComment = value) }
    }

    override fun updateMnemonicImageRequestComment(value: TextFieldValue) {
        mnemonicManagementState.update { state -> state.copy(imageRequestComment = value) }
    }

    override fun clearMnemonicComment(field: MnemonicCommentField) {
        mnemonicManagementState.update { state -> state.clearComment(field = field) }
    }

    override fun startMnemonicCommentDictation(field: MnemonicCommentField) {
        speechInputCoordinator.start(field = field)
    }

    override fun cancelMnemonicCommentDictation() {
        speechInputCoordinator.cancel()
    }

    override fun requestMnemonicAssociation() {
        if (mnemonicAssociationRequestJob?.isActive == true) return

        val requestedWord = foreignWordFieldValueState.value.text.trim()

        if (!requestedWord.isValidMnemonicWordFormat()) {
            eventMessage.tryEmitAsNegative(resId = Res.string.mnemonic_association_invalid_word_format)
            return
        }

        if (!canStartAgentDriverRequest()) return

        val generationHandle = try {
            mnemonicGenerationBackgroundManager.startGeneration(
                type = MnemonicGenerationType.Text,
                source = mnemonicGenerationSource,
            )
        } catch (error: Throwable) {
            crashlytics.report(exception = error)
            eventMessage.tryEmitAsNegative(
                resId = Res.string.mnemonic_association_request_failed,
                duration = EventMessage.Duration.Long,
            )
            return
        }

        mnemonicManagementState.update { state -> state.startAssociationLoading() }

        val requestJob = viewModelScope.launchWithState(coroutineContextProvider.io) {
            val requestComment = mnemonicManagementState.value.associationRequestCommentOrNull()
            val excludedSoundAnchors = mnemonicManagementState.value.excludedSoundAnchors()
            val association = fetchMnemonicAssociation(
                word = requestedWord,
                comment = requestComment,
                excludedSoundAnchors = excludedSoundAnchors,
            )
            val newVariants = association.toSelections().map { selection ->
                createMnemonicVariant(
                    selection = selection,
                    requestComment = requestComment.orEmpty(),
                )
            }

            mnemonicManagementState.update { state ->
                state.appendVariants(newVariants).stopAssociationLoading()
            }
            eventMessage.tryEmitAsPositive(resId = Res.string.mnemonic_association_received)
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            mnemonicManagementState.update { state -> state.stopAssociationLoading() }
            eventMessage.tryEmitAsNegative(
                resId = Res.string.mnemonic_association_request_failed,
                duration = EventMessage.Duration.Long,
            )
        }

        mnemonicAssociationRequestJob = requestJob
        requestJob.invokeOnCompletion { failure ->
            finishMnemonicGeneration(handle = generationHandle, failure = failure)

            if (mnemonicAssociationRequestJob == requestJob) {
                mnemonicAssociationRequestJob = null
            }
        }
    }

    override fun cancelMnemonicAssociationRequest() {
        mnemonicAssociationRequestJob?.cancel()
        mnemonicManagementState.update { state -> state.stopAssociationLoading() }
    }

    override fun requestMnemonicImage() {
        if (mnemonicImageRequestJob?.isActive == true) return

        val selectedVariant = mnemonicManagementState.value.selectedVariant

        if (selectedVariant == null) {
            eventMessage.tryEmitAsNegative(resId = Res.string.mnemonic_image_variant_not_selected)
            return
        }

        if (!canStartAgentDriverRequest()) return

        val generationHandle = try {
            mnemonicGenerationBackgroundManager.startGeneration(
                type = MnemonicGenerationType.Image,
                source = mnemonicGenerationSource,
            )
        } catch (error: Throwable) {
            crashlytics.report(exception = error)
            eventMessage.tryEmitAsNegative(
                resId = Res.string.mnemonic_image_request_failed,
                duration = EventMessage.Duration.Long,
            )
            return
        }

        mnemonicManagementState.update { state -> state.startImageLoading() }

        val requestJob = viewModelScope.launchWithState(coroutineContextProvider.io) {
            val requestComment = mnemonicManagementState.value.imageRequestCommentOrNull()
            val imageBytes = fetchMnemonicImage(
                selection = selectedVariant.selection,
                comment = requestComment,
            )
            val storedDraftImage = mnemonicImageAssetRepository.createDraftImage(imageBytes = imageBytes)

            // The bytes are on disk before the variant reaches the state, and a cancel landing in
            // that window would leave a file nothing references: the draft ids are derived from
            // the state, so neither clearing the mnemonic nor onCleared would ever find it again.
            if (!currentCoroutineContext().isActive) {
                deleteDraftImagesAsync(assetIds = setOf(storedDraftImage.assetId))
                return@launchWithState
            }

            val newImageVariant = createMnemonicImageVariant(storedDraftImage = storedDraftImage)

            mnemonicManagementState.update { state ->
                state.appendImageVariant(
                    variantId = selectedVariant.id,
                    imageVariant = newImageVariant,
                ).stopImageLoading()
            }
            eventMessage.tryEmitAsPositive(resId = Res.string.mnemonic_image_received)
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            mnemonicManagementState.update { state -> state.stopImageLoading() }
            eventMessage.tryEmitAsNegative(
                resId = Res.string.mnemonic_image_request_failed,
                duration = EventMessage.Duration.Long,
            )
        }

        mnemonicImageRequestJob = requestJob
        requestJob.invokeOnCompletion { failure ->
            finishMnemonicGeneration(handle = generationHandle, failure = failure)

            if (mnemonicImageRequestJob == requestJob) {
                mnemonicImageRequestJob = null
            }
        }
    }

    override fun cancelMnemonicImageRequest() {
        mnemonicImageRequestJob?.cancel()
        mnemonicManagementState.update { state -> state.stopImageLoading() }
    }

    private fun finishMnemonicGeneration(
        handle: MnemonicGenerationHandle,
        failure: Throwable?,
    ) {
        val outcome = when {
            failure == null -> MnemonicGenerationOutcome.Succeeded
            failure is CancellationException -> MnemonicGenerationOutcome.Cancelled
            else -> MnemonicGenerationOutcome.Failed
        }
        runCatching {
            handle.finish(outcome = outcome)
        }.onFailure { backgroundManagerFailure ->
            crashlytics.report(exception = backgroundManagerFailure)
        }
    }

    override fun selectMnemonicVariant(variantId: String) {
        mnemonicManagementState.update { state ->
            state.selectVariantIfPresent(variantId = variantId)
        }
    }

    override fun selectMnemonicImageVariant(variantId: String, imageId: String) {
        mnemonicManagementState.update { state ->
            state.selectImageIfPresent(
                variantId = variantId,
                imageId = imageId,
            )
        }
    }

    override fun clearMnemonicSelection() {
        val draftAssetIdsToDelete = currentDraftImageAssetIds()

        deleteDraftImagesAsync(assetIds = draftAssetIdsToDelete)
        mnemonicManagementState.update { state ->
            state.resetGeneratedContent(markExplicitlyCleared = true)
        }
    }

    abstract fun onCardManagementConfirmed()

    private fun handleIpaTextFieldFocusChanged(
        action: ICardManagementAction.IpaTextFieldFocusChanged
    ) {
        textFieldValueIpaHoldersState.update {
            it.toMutableList().apply {
                forEachIndexed { index, _ ->
                    if (action.focusList.size <= index) return
                    this[index] = this[index].copy(isFocused = action.focusList[index].isFocused)
                }
            }
        }
    }

    private fun observeMnemonicSpeechInput() {
        speechInputCoordinator.state
            .onEach { speechInputState ->
                mnemonicManagementState.update { state -> state.copy(speechInput = speechInputState) }
            }.launchIn(viewModelScope)

        speechInputCoordinator.recognizedText
            .onEach { recognized ->
                mnemonicManagementState.update { state ->
                    state.appendRecognizedText(
                        field = recognized.field,
                        recognizedText = recognized.text,
                    )
                }
            }.launchIn(viewModelScope)

        speechInputCoordinator.failures
            .onEach { failure -> eventMessage.tryEmitAsNegative(resId = failure.toMessageResource()) }
            .launchIn(viewModelScope)
    }

    private fun MnemonicSpeechInputFailure.toMessageResource() = when (this) {
        MnemonicSpeechInputFailure.UNSUPPORTED -> Res.string.speech_input_unsupported
        MnemonicSpeechInputFailure.PERMISSION_DENIED -> Res.string.speech_input_permission_denied
        MnemonicSpeechInputFailure.PERMISSION_DENIED_ALWAYS -> {
            Res.string.speech_input_permission_denied_always
        }

        MnemonicSpeechInputFailure.NO_SPEECH_DETECTED -> Res.string.speech_input_no_speech_detected
        MnemonicSpeechInputFailure.NETWORK -> Res.string.speech_input_network_error
        MnemonicSpeechInputFailure.BUSY -> Res.string.speech_input_busy
        MnemonicSpeechInputFailure.UNKNOWN -> Res.string.speech_input_failed
    }

    private fun combineAndObserveCardManagementChanges() {
        combine(
            letterInfosState,
            nativeWordFieldValueState,
            textFieldValueIpaHoldersState,
            foreignWordFieldValueState,
        ) { letterInfos, nativeWordFieldValue, textFieldValueIpaHolders, foreignWordFieldValue ->
            CardManagementState.InProgress(
                letterInfos = letterInfos,
                nativeWord = nativeWordFieldValue,
                foreignWord = foreignWordFieldValue,
                ipaHolders = textFieldValueIpaHolders
            )
        }.onEach { addingState -> cardManagementState.value = addingState }
            .flowOn(coroutineContextProvider.io)
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeForeignWordChanges() {
        foreignWordFieldValueState.map { fieldValue -> fieldValue.text.trim() }
            .distinctUntilChanged()
            .debounce(1500L)
            .onEach { onForeignWordChanged(word = it) }
            .flowOn(coroutineContextProvider.io)
            .launchIn(viewModelScope)

        foreignWordFieldValueState.map { fieldValue -> fieldValue.text.trim() }
            .distinctUntilChanged()
            .debounce(300)
            .flatMapLatest { foreignWord ->
                audioPlayer.preparePronunciation(word = foreignWord)

                if (foreignWord.isNotEmpty()) {
                    fetchWordInfo.invoke(word = foreignWord)
                } else {
                    flow { emit(LoadingState.Non) }
                }
            }.onEach { wordInfoState ->
                when (wordInfoState) {
                    LoadingState.Non -> {
                        transcriptionState.value = ""
                        nativeWordSuggestionsState.value = NativeWordSuggestionsState()
                    }

                    is LoadingState.Error -> {
                        handleWordInfoError(loadingState = wordInfoState)
                    }

                    LoadingState.Loading -> {
                        transcriptionState.value = ""
                        nativeWordSuggestionsState.update {
                            it.copy(loadingState = LoadingState.Loading)
                        }
                    }

                    is LoadingState.Success -> {
                        transcriptionState.value =
                            getWrappedTranscription(value = wordInfoState.data.transcription)

                        val suggestionItems =
                            wordInfoState.data.translations.map { suggestion ->
                                NativeWordSuggestionItem(word = suggestion, isSelected = false)
                            }

                        nativeWordSuggestionsState.value = NativeWordSuggestionsState(
                            suggestions = suggestionItems,
                            isActive = false,
                            loadingState = LoadingState.Success(data = Unit)
                        )
                    }
                }

            }.flowOn(coroutineContextProvider.io)
            .launchIn(viewModelScope)
    }

    private fun observeTextFieldValueIpaHoldersState() {
        textFieldValueIpaHoldersState.onEach { ipaValueHolders ->
            ipaKeyboardState.update { keyboardState ->
                val focusedHolder = ipaValueHolders.firstOrNull { holder -> holder.isFocused }

                keyboardState.copy(
                    enabled = ipaValueHolders.any { it.isFocused }
                ).let {
                    if (focusedHolder != null) {
                        it.copy(
                            holderIndex = ipaValueHolders.indexOf(focusedHolder),
                            ipaTextFieldValue = focusedHolder.ipaTextFieldValue
                        )
                    } else {
                        it
                    }
                }
            }
        }.flowOn(coroutineContextProvider.io)
            .launchIn(viewModelScope)
    }

    private fun changeLetterSelectionWithIpaTemplate(index: Int, letterInfo: LetterInfo) {
        val updatedCheckState = when (letterInfo.letter) {
            LetterInfo.EMPTY_LETTER -> false
            else -> letterInfo.isNotChecked
        }

        letterInfosState.update { infos ->
            infos.updatedAt(
                index = index,
                newValue = letterInfo.copy(isChecked = updatedCheckState)
            )
        }

        textFieldValueIpaHoldersState.value = letterInfosState.value.toRowIpaItemHolders()
            .map { ipaHolder -> ipaHolder.toTextFieldValueIpaHolder() }
    }

    private fun updateDataOnForeignWordChanged(wordFieldValue: TextFieldValue) {
        val word = wordFieldValue.text
        val isForeignWordChanged = word != foreignWordFieldValueState.value.text

        if (word.length < MAX_FOREIGN_WORD_LENGTH) {
            foreignWordFieldValueState.value = wordFieldValue

            isForeignWordChanged.ifTrue {
                letterInfosState.value = word.generateLetterInfos()
                nativeWordFieldValueState.value = TextFieldValue()
                textFieldValueIpaHoldersState.value = emptyList()
                clearMnemonicDraftForWordChange(newWord = word)

                autocompleteState.launchUpdateWithState(
                    scope = viewModelScope,
                    context = coroutineContextProvider.io
                ) {
                    AutocompleteState(
                        prefix = word,
                        autocomplete = fetchWordAutocomplete(prefix = word),
                        isActive = true,
                    )
                }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
                    // logE("fetchWordAutocomplete() caught ERROR: ${throwable.stackTraceToString()}")
                }
            }
        } else {
            eventMessage.tryEmitAsNegative(
                resId = Res.string.foreign_word_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    override fun restoreMnemonicManagementState(snapshot: MnemonicManagementUiState) {
        val draftAssetIdsToKeep = snapshot.draftAssetIds()
        val draftAssetIdsToDelete = currentDraftImageAssetIds()
            .filterNot { assetId -> assetId in draftAssetIdsToKeep }

        deleteDraftImagesAsync(assetIds = draftAssetIdsToDelete)

        // The snapshot restores generated content, not the microphone. Speech input describes what
        // the device can do and what a session is doing right now, and writing a stale copy of it
        // back can hide the dictation buttons for good: the coordinator only pushes its state when
        // that state changes, so it would never correct the overwrite.
        mnemonicManagementState.update { state ->
            snapshot.copy(speechInput = state.speechInput)
        }
    }

    protected fun initializeMnemonicManagementState(mnemonic: CardMnemonic) {
        if (mnemonicManagementState.value.variants.isNotEmpty()) return

        val selectedAssociation = mnemonic.selectedAssociation ?: return
        val variantId = nextMnemonicVariantId()

        viewModelScope.launch(coroutineContextProvider.io) {
            val selectedImage = mnemonic.selectedIllustration?.imageAssetId
                ?.takeIf(String::isNotBlank)
                ?.let { imageAssetId -> mnemonicImageAssetRepository.resolveSavedImage(assetId = imageAssetId) }
                ?.let { storedImage -> createMnemonicImageVariant(storedDraftImage = storedImage) }
            val selectedImageId = selectedImage?.id

            mnemonicManagementState.value = mnemonicManagementState.value.copy(
                variants = listOf(
                    MnemonicVariantUiState(
                        id = variantId,
                        selection = selectedAssociation,
                        imageVariants = selectedImage?.let(::listOf).orEmpty(),
                        selectedImageId = selectedImageId,
                    ),
                ),
                selectedVariantId = variantId,
                isExplicitlyCleared = false,
            )
        }
    }

    protected fun currentMnemonicPreviewForSaving(foreignWord: String): CardMnemonic {
        if (mnemonicManagementState.value.isExplicitlyCleared) return CardMnemonic.EMPTY

        val selectedVariant = mnemonicManagementState.value.selectedVariant ?: return CardMnemonic.EMPTY
        return if (selectedVariant.selection.word.toWordKey() == foreignWord.toWordKey()) {
            selectedVariant.toCardMnemonicPreview()
        } else {
            CardMnemonic.EMPTY
        }
    }

    protected suspend fun materializeCurrentMnemonicForSaving(
        foreignWord: String,
    ): PreparedMnemonicSave {
        if (mnemonicManagementState.value.isExplicitlyCleared) {
            return PreparedMnemonicSave(mnemonic = CardMnemonic.EMPTY)
        }

        val selectedVariant = mnemonicManagementState.value.selectedVariant
            ?: return PreparedMnemonicSave(mnemonic = CardMnemonic.EMPTY)

        if (selectedVariant.selection.word.toWordKey() != foreignWord.toWordKey()) {
            return PreparedMnemonicSave(mnemonic = CardMnemonic.EMPTY)
        }

        val selectedImage = selectedVariant.selectedImage
        val retainedSavedAssetId: String?
        val newlyCreatedSavedAssetId: String?

        when (selectedImage?.storage) {
            MnemonicImageAssetStorage.Draft -> {
                val savedCopy = mnemonicImageAssetRepository.createSavedCopyFromDraft(
                    draftAssetId = selectedImage.assetId,
                )
                retainedSavedAssetId = savedCopy.assetId
                newlyCreatedSavedAssetId = savedCopy.assetId
            }

            MnemonicImageAssetStorage.Saved -> {
                val savedAsset = mnemonicImageAssetRepository.resolveSavedImage(
                    assetId = selectedImage.assetId,
                ) ?: throw IllegalStateException("Selected mnemonic image file is missing.")
                retainedSavedAssetId = savedAsset.assetId
                newlyCreatedSavedAssetId = null
            }

            null -> {
                retainedSavedAssetId = null
                newlyCreatedSavedAssetId = null
            }
        }

        return PreparedMnemonicSave(
            mnemonic = CardMnemonic(
                selectedAssociation = selectedVariant.selection,
                selectedIllustration = retainedSavedAssetId?.let { imageAssetId ->
                    MnemonicIllustration(imageAssetId = imageAssetId)
                },
            ),
            retainedSavedAssetId = retainedSavedAssetId,
            newlyCreatedSavedAssetId = newlyCreatedSavedAssetId,
        )
    }

    protected fun hasCurrentMnemonicSelectionForWord(foreignWord: String): Boolean {
        return currentMnemonicPreviewForSaving(foreignWord = foreignWord).selectedAssociation != null
    }

    protected suspend fun finalizeMnemonicSaveSuccess(
        retainedSavedAssetId: String?,
        previousSavedAssetId: String?,
    ) {
        deleteDraftImages(assetIds = currentDraftImageAssetIds())

        if (!previousSavedAssetId.isNullOrBlank() && previousSavedAssetId != retainedSavedAssetId) {
            mnemonicImageAssetRepository.deleteSavedImage(assetId = previousSavedAssetId)
        }
    }

    protected suspend fun rollbackPreparedMnemonicSave(preparedSave: PreparedMnemonicSave) {
        preparedSave.newlyCreatedSavedAssetId?.let { assetId ->
            mnemonicImageAssetRepository.deleteSavedImage(assetId = assetId)
        }
    }

    private fun clearMnemonicDraftForWordChange(newWord: String) {
        val newWordKey = newWord.toWordKey()
        val hasVariantsForAnotherWord = mnemonicManagementState.value.variants.any { variant ->
            variant.selection.word.toWordKey() != newWordKey
        }
        if (!hasVariantsForAnotherWord) return

        val draftAssetIdsToDelete = currentDraftImageAssetIds()
        deleteDraftImagesAsync(assetIds = draftAssetIdsToDelete)
        mnemonicManagementState.update { state -> state.resetGeneratedContent(markExplicitlyCleared = false) }
    }

    private fun nextMnemonicVariantId(): String =
        "$MNEMONIC_VARIANT_ID_PREFIX${nextMnemonicVariantId++}"

    private fun nextMnemonicImageVariantId(): String =
        "$MNEMONIC_IMAGE_ID_PREFIX${nextMnemonicImageVariantId++}"

    private fun currentDraftImageAssetIds(): List<String> {
        return mnemonicManagementState.value.draftAssetIds().toList()
    }

    private fun createMnemonicVariant(
        selection: com.kuts.domain.entities.MnemonicSelection,
        requestComment: String,
    ): MnemonicVariantUiState {
        return MnemonicVariantUiState(
            id = nextMnemonicVariantId(),
            selection = selection,
            requestComment = requestComment,
        )
    }

    private fun createMnemonicImageVariant(
        storedDraftImage: MnemonicImageAsset,
    ): MnemonicImageVariantUiState {
        return MnemonicImageVariantUiState(
            id = nextMnemonicImageVariantId(),
            assetId = storedDraftImage.assetId,
            imagePath = storedDraftImage.filePath,
            storage = storedDraftImage.storage,
        )
    }

    private fun deleteDraftImagesAsync(assetIds: Collection<String>) {
        if (assetIds.isEmpty()) return

        viewModelScope.launch(coroutineContextProvider.io) {
            deleteDraftImages(assetIds = assetIds.toList())
        }
    }

    private suspend fun deleteDraftImages(assetIds: List<String>) {
        assetIds.forEach { assetId ->
            mnemonicImageAssetRepository.deleteDraftImage(assetId = assetId)
        }
    }

    private fun String.toWordKey(): String = trim().lowercase()

    private fun String.isValidMnemonicWordFormat(): Boolean {
        val trimmedWord = trim()
        if (trimmedWord.isEmpty()) return false

        val hasOnlyAllowedCharacters = trimmedWord.all { symbol ->
            symbol.isLetter() || symbol == '\'' || symbol == '-'
        }
        if (!hasOnlyAllowedCharacters) return false

        val lettersOnly = trimmedWord.filter { symbol -> symbol.isLetter() }.lowercase()
        if (lettersOnly.isEmpty()) return false
        if (lettersOnly.length >= 4 && lettersOnly.toSet().size == 1) return false

        return true
    }

    private fun canStartAgentDriverRequest(): Boolean {
        if (agentDriverConnectionState.value.canSendRequests) return true

        eventMessage.tryEmitAsNegative(resId = Res.string.data_synchronization_network_connection_warning)
        return false
    }

    override fun onCleared() {
        speechInputCoordinator.release()
        runCatching {
            runBlocking {
                deleteDraftImages(assetIds = currentDraftImageAssetIds())
            }
        }
        super.onCleared()
    }

    private fun updateDataOnAutocompleteSelected(word: String) {
        foreignWordFieldValueState.update {
            TextFieldValue(text = word, selection = TextRange(word.length))
        }
        nativeWordFieldValueState.update { it.copy(text = "") }
        letterInfosState.value = word.generateLetterInfos()
        textFieldValueIpaHoldersState.value = emptyList()
        autocompleteState.value = AutocompleteState()
    }

    private fun updateDataOnNativeWordSelected(wordIndex: Int) {
        nativeWordSuggestionsState.update { suggestionsState ->
            val updatedSuggestions = suggestionsState.suggestions
                .updatedAt(index = wordIndex) { oldValue ->
                    oldValue.copy(isSelected = !oldValue.isSelected)
                }

            suggestionsState.copy(suggestions = updatedSuggestions)
        }
    }

    private fun handleNativeWordSuggestionsSelectionConfirmation() {
        nativeWordSuggestionsState.update { it.copy(isActive = false) }

        val selectedNativeWordSuggestions = nativeWordSuggestionsState.value.suggestions
            .filter { suggestionItem -> suggestionItem.isSelected }
            .joinToString(separator = ", ") { suggestionItem -> suggestionItem.word }

        if (selectedNativeWordSuggestions.length <= MAX_NATIVE_WORD_LENGTH) {
            nativeWordFieldValueState.value = TextFieldValue(
                text = selectedNativeWordSuggestions,
                selection = TextRange(selectedNativeWordSuggestions.length)
            )
        } else {
            eventMessage.tryEmitAsNegative(
                resId = Res.string.native_word_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun handleClearNativeWordSuggestionsSelectionClicked() {
        nativeWordSuggestionsState.update {
            val updatedSuggestions = it.suggestions.map { suggestionItem ->
                suggestionItem.copy(isSelected = false)
            }

            it.copy(suggestions = updatedSuggestions, isActive = false)
        }
        nativeWordFieldValueState.value = TextFieldValue()
    }

    private fun updateIpa(letterGroupIndex: Int, ipaTextFieldValue: TextFieldValue) {
        if (ipaTextFieldValue.text.length < MAX_IPA_LENGTH) {
            textFieldValueIpaHoldersState.update { textFieldValueIpaHolders ->
                textFieldValueIpaHolders.updatedAt(index = letterGroupIndex) { oldValue ->
                    oldValue.copy(ipaTextFieldValue = ipaTextFieldValue)
                }
            }
        } else {
            eventMessage.tryEmitAsNegative(
                resId = Res.string.ipa_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun updateNativeWord(wordFieldValue: TextFieldValue) {
        val word = wordFieldValue.text

        if (word.length < MAX_NATIVE_WORD_LENGTH) {
            nativeWordFieldValueState.value = wordFieldValue
        } else {
            eventMessage.tryEmitAsNegative(
                resId = Res.string.native_word_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun handleWordInfoError(loadingState: LoadingState.Error<IWordInfoRepository.IWordInfoLoadingError>) {
        // logE("fetchWordInfo() returned error state: ${loadingState.value}")
        val errorMessageId = when (loadingState.value) {
            IWordInfoLoadingError.Common,
            IWordInfoLoadingError.JsonConvert -> {
                Res.string.word_info_retrieving_common_warning_message
            }
        }

        eventMessage.tryEmitAsNegative(resId = errorMessageId)
    }

    private fun getWrappedTranscription(value: String): String {
        return if (value.isEmpty()) "" else "[$value]"
    }

    protected suspend fun checkIfForeignWordExists(word: String) {
        val decksWithSameForeignWord = checkIfWordExists.invoke(foreignWord = word.lowercase())

        if (decksWithSameForeignWord.isNotEmpty()) {
            val deckNamesAsString = decksWithSameForeignWord.joinToString(", ") { it.name }

            eventMessage.tryEmitAsNegative(
                resId = Res.string.foreign_word_already_exists,
                args = arrayOf(word, deckNamesAsString),
            )
        }
    }
}

data class PreparedMnemonicSave(
    val mnemonic: CardMnemonic,
    val retainedSavedAssetId: String? = null,
    val newlyCreatedSavedAssetId: String? = null,
)
