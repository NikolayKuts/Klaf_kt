package com.example.domain.common

import javax.inject.Qualifier

@Qualifier
annotation class LocalDeckRepositoryImp

@Qualifier
annotation class RemoteDeckRepositoryImp

@Qualifier
annotation class LocalCardRepositoryImp

@Qualifier
annotation class RemoteCardRepositoryImp

@Qualifier
annotation class LocalStorageSaveVersionRepositoryImp

@Qualifier
annotation class RemoteStorageSaveVersionRepositoryImp