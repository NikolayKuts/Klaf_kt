package com.kuts.domain.common

import javax.inject.Qualifier

@Qualifier
annotation class LocalDeckRepository

@Qualifier
annotation class RemoteDeckRepository

@Qualifier
annotation class LocalCardRepository

@Qualifier
annotation class RemoteCardRepository

@Qualifier
annotation class LocalStorageSaveVersionRepository

@Qualifier
annotation class RemoteStorageSaveVersionRepository