package com.kuts.klaf.room.maintenance

import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.room.readSyncVocabularySources
import com.kuts.klaf.room.toSyncWord
import com.kuts.klaf.server.contract.SyncVocabularySource
import com.kuts.klaf.server.contract.SyncIgnoredVocabularyWord
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface TemporaryCleanupReceiptStore {
    fun read(scope: String): String?
    fun write(scope: String, receipt: String)
}

@Serializable
data class TemporaryCleanupReceipt(
    val sourceTitle: String,
    val removedItems: Int,
    val removedIgnoredWords: Int,
    val syncRequired: Boolean,
)

sealed interface TemporaryCleanupResult {
    data class Completed(val receipt: TemporaryCleanupReceipt, val alreadyCompleted: Boolean) : TemporaryCleanupResult
    data class Skipped(val sourceCount: Int) : TemporaryCleanupResult
}

@Serializable
private data class TemporaryCleanupBackup(
    val source: SyncVocabularySource,
    val ignoredWords: List<SyncIgnoredVocabularyWord>,
)

private class TemporaryCleanupBypass(val result: TemporaryCleanupResult) : Exception()

// TEMPORARY: remove with the Android startup gate after user-confirmed cleanup.
class TemporaryVocabularyCleanup(
    private val databaseSource: RoomDatabaseSource,
    private val receiptStore: TemporaryCleanupReceiptStore,
    private val transactions: IStorageTransactionRepository = StorageTransactionRepositoryRoom(databaseSource),
) {
    private val mutex = Mutex()

    suspend fun run(backup: suspend (String) -> Unit): TemporaryCleanupResult = mutex.withLock {
        val selection = (databaseSource as? ActiveLocalRoomDatabase)?.selection?.value
        val database = selection?.database ?: databaseSource.current()
        val account = selection?.accountEmail
        val scope = if (databaseSource is ActiveLocalRoomDatabase) account?.let { "account:$it" } ?: "guest" else "legacy"
        receiptStore.read(scope)?.let { saved ->
            return@withLock TemporaryCleanupResult.Completed(Json.decodeFromString(saved), alreadyCompleted = true)
        }
        val result = try {
            transactions.performWithTransaction {
                check(databaseSource.current() === database) { "Selected database changed before cleanup" }
                receiptStore.read(scope)?.let { saved ->
                    throw TemporaryCleanupBypass(TemporaryCleanupResult.Completed(
                        Json.decodeFromString(saved), alreadyCompleted = true,
                    ))
                }
                val sources = database.vocabularySourceDao().getSources()
                if (sources.size != 1) throw TemporaryCleanupBypass(TemporaryCleanupResult.Skipped(sources.size))
                val source = sources.single()
                val items = database.vocabularySourceItemDao().getItemsBySourceId(source.id)
                val words = database.ignoredVocabularyWordDao().getWords()
                val portableSource = readSyncVocabularySources(
                    database.vocabularySourceDao(), database.vocabularySourceItemDao(), database.deckDao(), database.cardDao(),
                ).single()
                backup(Json.encodeToString(TemporaryCleanupBackup(portableSource, words.map { it.toSyncWord() })))
                database.vocabularySourceItemDao().deleteItemsBySourceId(source.id)
                database.ignoredVocabularyWordDao().deleteWords()
                StorageSaveVersionRepositoryRoom(databaseSource).increaseVersion()
                TemporaryCleanupResult.Completed(
                    receipt = TemporaryCleanupReceipt(source.title, items.size, words.size, syncRequired = account != null),
                    alreadyCompleted = false,
                )
            }
        } catch (bypass: TemporaryCleanupBypass) {
            bypass.result
        }
        if (result is TemporaryCleanupResult.Completed && !result.alreadyCompleted) {
            // Persist only after commit. Until this succeeds, the startup gate blocks editing.
            receiptStore.write(scope, Json.encodeToString(result.receipt))
        }
        result
    }
}
