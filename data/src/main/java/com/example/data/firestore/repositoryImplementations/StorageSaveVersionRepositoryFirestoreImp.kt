package com.example.data.firestore.repositoryImplementations

import com.example.data.firestore.MAIN_COLLECTION_NAME
import com.example.data.firestore.entities.FirestoreStorageSaveVersion
import com.example.data.firestore.mapToDomainEntity
import com.example.data.firestore.mapToFirestoreEntity
import com.example.domain.entities.StorageSaveVersion
import com.example.domain.repositories.StorageSaveVersionRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StorageSaveVersionRepositoryFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StorageSaveVersionRepository {

    companion object {

        private const val SAVE_VERSION_DOCUMENT_NAME = "storage_save_version"
    }

    override suspend fun fetchVersion(): StorageSaveVersion? = withContext(Dispatchers.IO) {
        getStorageSaveVersionDocument()
            .get()
            .await()
            .toObject<FirestoreStorageSaveVersion>()
            ?.mapToDomainEntity()
    }

    override suspend fun insertVersion(version: StorageSaveVersion) {
        withContext(Dispatchers.IO) {
            getStorageSaveVersionDocument()
                .set(version.mapToFirestoreEntity())
                .await()
        }
    }

    override suspend fun increaseVersion() {
        val oldVersion = fetchVersion()?.version ?: StorageSaveVersion.INITIAL_SAVE_VERSION

        getStorageSaveVersionDocument()
            .set(FirestoreStorageSaveVersion(version = oldVersion + 1))
            .await()

    }

    private fun getStorageSaveVersionDocument(): DocumentReference {
        return firestore.collection(MAIN_COLLECTION_NAME)
            .document(SAVE_VERSION_DOCUMENT_NAME)
    }
}