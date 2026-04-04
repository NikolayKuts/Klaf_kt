package com.kuts.klaf.di

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IAuthenticationSessionManager
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.managers.IWordInsightsProviderManager
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.common.IosAppMaintenanceManager
import com.kuts.klaf.common.IosCoroutineContextProvider
import com.kuts.klaf.dataStore.implementations.IosInMemoryDeckRepetitionInfoRepository
import com.kuts.klaf.ios.IosAuthenticationRepository
import com.kuts.klaf.ios.IosAuthenticationSessionManager
import com.kuts.klaf.ios.IosNoOpAudioPlayerManager
import com.kuts.klaf.ios.IosNoOpCrashlyticsRepository
import com.kuts.klaf.ios.IosNoOpDeckReviewScheduler
import com.kuts.klaf.ios.IosNoOpOldAppKlafDataTransferRepository
import com.kuts.klaf.ios.IosNoOpWordAutocompleteRepository
import com.kuts.klaf.ios.IosNoOpWordInsightsProviderManager
import com.kuts.klaf.networking.openai.OpenAiHttpClientFactory
import com.kuts.klaf.networking.openai.OpenAiWordMeaningInsightsRepository
import com.kuts.klaf.networking.yandexApi.YandexSecureHttpClientFactory
import com.kuts.klaf.networking.yandexApi.YandexWordInfoRepository
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabaseProvider
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
        OpenAiWordMeaningInsightsRepository(client = OpenAiHttpClientFactory().create())
    }
    single<IWordInsightsProviderManager> { IosNoOpWordInsightsProviderManager() }
    single<IDeckRepetitionInfoRepository> { IosInMemoryDeckRepetitionInfoRepository() }
    single<IOldAppKlafDataTransferRepository> { IosNoOpOldAppKlafDataTransferRepository() }
    single<ICrashlyticsRepository> { IosNoOpCrashlyticsRepository() }
}

private fun Module.iosInfrastructureModule() {
    single<KlafRoomDatabase> { KlafRoomDatabaseProvider.getInstance() }
    single<ICoroutineContextProvider> { IosCoroutineContextProvider() }
}

private fun Module.iosManagerBindings() {
    single<IAppMaintenanceManager> { IosAppMaintenanceManager() }
    factory<IAudioPlayerManager> { IosNoOpAudioPlayerManager() }
    single<IDeckReviewScheduler> { IosNoOpDeckReviewScheduler() }
}
