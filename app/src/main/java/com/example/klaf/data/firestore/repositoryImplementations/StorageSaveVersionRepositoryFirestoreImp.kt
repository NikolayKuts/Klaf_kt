package com.example.klaf.data.firestore.repositoryImplementations

import com.example.klaf.data.firestore.MAIN_COLLECTION_NAME
import com.example.klaf.data.firestore.entities.StorageSaveVersion
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
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

    override suspend fun fetchVersion(): Long? = withContext(Dispatchers.IO) {
        firestore.collection(MAIN_COLLECTION_NAME)
            .document(SAVE_VERSION_DOCUMENT_NAME)
            .get()
            .await()
            .toObject<StorageSaveVersion>()
            ?.version
    }

    override suspend fun insertVersion(version: Long) {
        withContext(Dispatchers.IO) {
            firestore.collection(MAIN_COLLECTION_NAME)
                .document(SAVE_VERSION_DOCUMENT_NAME)
                .set(StorageSaveVersion(version = version))
                .await()
        }
    }
}