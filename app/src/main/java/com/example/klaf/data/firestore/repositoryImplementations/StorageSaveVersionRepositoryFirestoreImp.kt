package com.example.klaf.data.firestore.repositoryImplementations

import com.example.domain.entities.StorageSaveVersion
import com.example.domain.repositories.StorageSaveVersionRepository
import com.example.klaf.data.firestore.entities.FirestoreStorageSaveVersion
import com.example.klaf.data.firestore.toDomainEntity
import com.example.klaf.data.firestore.toFirestoreEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StorageSaveVersionRepositoryFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : StorageSaveVersionRepository {

    companion object {

        private const val SAVE_VERSION_DOCUMENT_NAME = "storage_save_version"
    }

    override suspend fun fetchVersion(): StorageSaveVersion? = withContext(Dispatchers.IO) {
        getStorageSaveVersionDocument()
            .get()
            .await()
            .toObject<FirestoreStorageSaveVersion>()
            ?.toDomainEntity()
    }

    override suspend fun insertVersion(version: StorageSaveVersion) {
        withContext(Dispatchers.IO) {
            getStorageSaveVersionDocument()
                .set(version.toFirestoreEntity())
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
        val mainCollectionName = auth.currentUser?.email
            ?: throw RuntimeException("There is no authorized user")

        return firestore.collection(mainCollectionName)
            .document(SAVE_VERSION_DOCUMENT_NAME)
    }
}