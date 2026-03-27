package com.kuts.klaf.firestore.repositoryImplementations

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.klaf.firestore.entities.FirestoreStorageSaveVersion
import com.kuts.klaf.firestore.rootCollection
import com.kuts.klaf.firestore.toDomainEntity
import com.kuts.klaf.firestore.toFirestoreEntity
import kotlinx.coroutines.tasks.await

class AndroidStorageSaveVersionRepositoryFirestore(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : IStorageSaveVersionRepository {

    companion object {

        private const val SAVE_VERSION_DOCUMENT_NAME = "storage_save_version"
    }

    override suspend fun fetchVersion(): StorageSaveVersion? {
        return getStorageSaveVersionDocument()
            .get()
            .await()
            .toObject<FirestoreStorageSaveVersion>()
            ?.toDomainEntity()
    }

    override suspend fun insertVersion(version: StorageSaveVersion) {
        getStorageSaveVersionDocument()
            .set(version.toFirestoreEntity())
            .await()
    }

    override suspend fun insertVersionAtPath(version: StorageSaveVersion, rootEmailPath: String) {
        firestore.rootCollection(email = rootEmailPath)
            .document(SAVE_VERSION_DOCUMENT_NAME)
            .set(version.toFirestoreEntity())
            .await()
    }

    override suspend fun increaseVersion(): Unit {
        val oldVersion = fetchVersion()?.version ?: StorageSaveVersion.INITIAL_SAVE_VERSION

        getStorageSaveVersionDocument()
            .set(FirestoreStorageSaveVersion(version = oldVersion + 1))
            .await()
    }

    private fun getStorageSaveVersionDocument(): DocumentReference {
        val userEmail = auth.currentUser?.email
            ?: throw RuntimeException("There is no authorized user")

        return firestore.rootCollection(email = userEmail)
            .document(SAVE_VERSION_DOCUMENT_NAME)
    }
}
