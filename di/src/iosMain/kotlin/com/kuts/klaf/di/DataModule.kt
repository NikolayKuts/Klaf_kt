package com.kuts.klaf.di

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IKlafServerConnectionManager
import com.kuts.domain.managers.IAuthenticationSessionManager
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.managers.IMnemonicGenerationBackgroundManager
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.managers.ISpeechRecognitionManager
import com.kuts.domain.managers.ITextToSpeechManager
import com.kuts.domain.managers.IVocabularySourceAnalysisBackgroundManager
import com.kuts.domain.managers.SpeechRecognitionResult
import com.kuts.domain.managers.SpeechRecognitionState
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IMnemonicAssociationRepository
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import com.kuts.domain.repositories.IMnemonicImageRepository
import com.kuts.domain.repositories.IMnemonicImageRemoteRepository
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.domain.entities.MnemonicAssociationResult
import com.kuts.domain.entities.MnemonicImageResult
import com.kuts.klaf.common.IosAppMaintenanceManager
import com.kuts.klaf.common.IosCoroutineContextProvider
import com.kuts.klaf.dataStore.implementations.IosInMemoryDeckRepetitionInfoRepository
import com.kuts.klaf.ios.IosAuthenticationRepository
import com.kuts.klaf.ios.IosAuthenticationSessionManager
import com.kuts.klaf.ios.IosNoOpAudioPlayerManager
import com.kuts.klaf.ios.IosNoOpKlafServerConnectionManager
import com.kuts.klaf.ios.IosNoOpCrashlyticsRepository
import com.kuts.klaf.ios.IosNoOpDeckReviewScheduler
import com.kuts.klaf.ios.IosNoOpOldAppKlafDataTransferRepository
import com.kuts.klaf.ios.IosNoOpTextToSpeechManager
import com.kuts.klaf.ios.IosNoOpWordAutocompleteRepository
import com.kuts.klaf.ios.IosNoOpWordMeaningInsightsRepository
import com.kuts.klaf.mnemonic.IosNoOpMnemonicImageAssetRepository
import com.kuts.klaf.mnemonic.IosNoOpMnemonicImageRemoteRepository
import com.kuts.klaf.mnemonic.NoOpMnemonicGenerationBackgroundManager
import com.kuts.klaf.networking.yandexApi.YandexSecureHttpClientFactory
import com.kuts.klaf.networking.yandexApi.YandexWordInfoRepository
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabaseProvider
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.vocabularySource.NoOpVocabularySourceAnalysisBackgroundManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val iosDataModule = module {
    iosRepositoryModule()
    iosInfrastructureModule()
    iosManagerBindings()
}

private fun Module.iosRepositoryModule() {
    single<IDeckRepository>(
        qualifier = named(name = REMOTE_DECK_REPOSITORY),
    ) {
        get(qualifier = named(name = LOCAL_DECK_REPOSITORY))
    }
    single<ICardRepository>(
        qualifier = named(name = REMOTE_CARD_REPOSITORY),
    ) {
        get(qualifier = named(name = LOCAL_CARD_REPOSITORY))
    }
    single<IStorageSaveVersionRepository>(
        qualifier = named(name = REMOTE_STORAGE_SAVE_VERSION_REPOSITORY),
    ) {
        get(qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY))
    }

    single<IosAuthenticationRepository> { IosAuthenticationRepository() }
    single<IAuthenticationRepository> { get<IosAuthenticationRepository>() }
    single<IAuthenticationSessionManager> {
        IosAuthenticationSessionManager(authenticationRepository = get())
    }
    single<IWordInfoRepository> {
        YandexWordInfoRepository(client = YandexSecureHttpClientFactory().create())
    }
    single<IWordAutocompleteRepository> { IosNoOpWordAutocompleteRepository() }
    single<IWordMeaningInsightsRepository> {
        IosNoOpWordMeaningInsightsRepository()
    }
    single<IMnemonicAssociationRepository> { IosNoOpMnemonicAssociationRepository() }
    single<IMnemonicImageRepository> { IosNoOpMnemonicImageRepository() }
    single<IMnemonicImageAssetRepository> { IosNoOpMnemonicImageAssetRepository() }
    single<IMnemonicImageRemoteRepository> { IosNoOpMnemonicImageRemoteRepository() }
    single<IKlafServerConnectionManager> { IosNoOpKlafServerConnectionManager() }
    single<IDeckRepetitionInfoRepository> { IosInMemoryDeckRepetitionInfoRepository() }
    single<IOldAppKlafDataTransferRepository> { IosNoOpOldAppKlafDataTransferRepository() }
    single<ICrashlyticsRepository> { IosNoOpCrashlyticsRepository() }
}

private fun Module.iosInfrastructureModule() {
    single<KlafRoomDatabase> { KlafRoomDatabaseProvider.getInstance() }
    single<RoomDatabaseSource> { StaticRoomDatabaseSource(database = get()) }
    single<ICoroutineContextProvider> { IosCoroutineContextProvider() }
}

private fun Module.iosManagerBindings() {
    single<IAppMaintenanceManager> { IosAppMaintenanceManager() }
    single<IMnemonicGenerationBackgroundManager> {
        NoOpMnemonicGenerationBackgroundManager()
    }
    single<IVocabularySourceAnalysisBackgroundManager> {
        NoOpVocabularySourceAnalysisBackgroundManager()
    }
    factory<IAudioPlayerManager> { IosNoOpAudioPlayerManager() }
    factory<ITextToSpeechManager> { IosNoOpTextToSpeechManager() }
    factory<ISpeechRecognitionManager> { IosNoOpSpeechRecognitionManager() }
    single<IDeckReviewScheduler> { IosNoOpDeckReviewScheduler() }
}

private class IosNoOpMnemonicAssociationRepository : IMnemonicAssociationRepository {
    override suspend fun fetchMnemonicAssociation(
        word: String,
        comment: String?,
        excludedSoundAnchors: List<String>,
        launchSource: MnemonicGenerationSource?,
    ): MnemonicAssociationResult {
        throw UnsupportedOperationException("Mnemonic association requests are unavailable on iOS.")
    }
}

private class IosNoOpMnemonicImageRepository : IMnemonicImageRepository {
    override suspend fun fetchMnemonicImage(
        selection: com.kuts.domain.entities.MnemonicSelection,
        comment: String?,
        launchSource: MnemonicGenerationSource?,
    ): MnemonicImageResult {
        throw UnsupportedOperationException("Mnemonic image requests are unavailable on iOS.")
    }
}

private class IosNoOpSpeechRecognitionManager : ISpeechRecognitionManager {
    override val isSupported: Boolean = false
    override val state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Unsupported)
    override val recognizedText = MutableSharedFlow<SpeechRecognitionResult>()

    override fun startRecognition(targetId: String) = Unit
    override fun stopRecognition() = Unit
    override fun cancelRecognition() = Unit
    override fun release() = Unit
}
