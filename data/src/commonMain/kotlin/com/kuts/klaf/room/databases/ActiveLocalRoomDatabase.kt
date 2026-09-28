package com.kuts.klaf.room.databases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LocalRoomSelection(
    val accountEmail: String?,
    val database: KlafRoomDatabase,
)

fun interface LocalRoomSelectionObserver {

    suspend fun onSelectionChanged(previous: LocalRoomSelection, current: LocalRoomSelection)
}

interface LocalRoomDatabaseFactory {

    fun openGuest(): KlafRoomDatabase

    fun openAccount(email: String): KlafRoomDatabase
}

interface SelectedAccountStore {

    fun read(): String?

    fun write(email: String?)
}

class ManualSyncInProgressException : IllegalStateException(
    "Account edits are paused during manual synchronization",
)

interface RoomDatabaseSource {

    val databases: Flow<KlafRoomDatabase>

    fun current(): KlafRoomDatabase

    suspend fun <R> transaction(block: suspend () -> R): R
}

class StaticRoomDatabaseSource(private val database: KlafRoomDatabase) : RoomDatabaseSource {

    override val databases: Flow<KlafRoomDatabase> = flowOf(database)

    override fun current(): KlafRoomDatabase = database

    override suspend fun <R> transaction(block: suspend () -> R): R = database.performInTransaction(block)
}

/** Owns opened databases; callers must close it when the application scope ends. */
class ActiveLocalRoomDatabase(
    private val factory: LocalRoomDatabaseFactory,
    private val accountStore: SelectedAccountStore,
    private val selectionObserver: LocalRoomSelectionObserver? = null,
    private val beforeSelectionChange: suspend () -> Unit = {},
) : RoomDatabaseSource {

    private val switchMutex = Mutex()
    private var syncingAccountEmail: String? = null
    private val initialEmail = normalizeEmail(accountStore.read())
    private val openedDatabases = mutableMapOf<String?, KlafRoomDatabase>()
    private val mutableSelection = MutableStateFlow(
        LocalRoomSelection(initialEmail, openDatabase(initialEmail).also { openedDatabases[initialEmail] = it }),
    )

    val selection: StateFlow<LocalRoomSelection> = mutableSelection
    override val databases: Flow<KlafRoomDatabase> = selection.map { it.database }.distinctUntilChanged()

    override fun current(): KlafRoomDatabase = selection.value.database

    suspend fun selectAccount(email: String?) {
        val normalizedEmail = normalizeEmail(email)
        switchMutex.withLock {
            val previous = selection.value
            if (normalizedEmail == previous.accountEmail) return@withLock
            if (syncingAccountEmail != null) throw ManualSyncInProgressException()
            val existing = openedDatabases[normalizedEmail]
            val nextDatabase = existing ?: openDatabase(normalizedEmail)
            try {
                nextDatabase.deckDao().getAllDecks()
                beforeSelectionChange()
                accountStore.write(normalizedEmail)
            } catch (failure: Throwable) {
                if (existing == null) nextDatabase.close()
                throw failure
            }
            openedDatabases[normalizedEmail] = nextDatabase
            val current = LocalRoomSelection(normalizedEmail, nextDatabase)
            mutableSelection.value = current
            selectionObserver?.onSelectionChanged(previous, current)
        }
    }

    override suspend fun <R> transaction(block: suspend () -> R): R = switchMutex.withLock {
        current().performInTransaction(block)
    }

    suspend fun beginManualSyncAttempt(accountEmail: String) = switchMutex.withLock {
        check(selection.value.accountEmail == accountEmail) { "Selected account changed" }
        check(syncingAccountEmail == null) { "Manual synchronization is already running" }
        syncingAccountEmail = accountEmail
    }

    suspend fun endManualSyncAttempt(accountEmail: String) = switchMutex.withLock {
        check(syncingAccountEmail == accountEmail) { "Manual synchronization account changed" }
        syncingAccountEmail = null
    }

    fun requireAccountEditAllowed(accountEmail: String) {
        if (syncingAccountEmail == accountEmail) throw ManualSyncInProgressException()
    }

    internal suspend fun <R> withGuestAndAccount(
        email: String,
        block: suspend (guest: KlafRoomDatabase, account: KlafRoomDatabase) -> R,
    ): R = switchMutex.withLock {
        check(selection.value.accountEmail == null) { "Guest database must be selected for transfer" }
        val normalizedEmail = requireNotNull(normalizeEmail(email)) { "Account email is required" }
        val account = openedDatabases.getOrPut(normalizedEmail) { openDatabase(normalizedEmail) }
        account.deckDao().getAllDecks()
        block(selection.value.database, account)
    }

    fun close() {
        openedDatabases.values.forEach(KlafRoomDatabase::close)
        openedDatabases.clear()
    }

    private fun openDatabase(email: String?): KlafRoomDatabase = if (email == null) {
        factory.openGuest()
    } else {
        factory.openAccount(email)
    }

    private fun normalizeEmail(email: String?): String? = email?.trim()?.lowercase()?.also {
        require(it.isNotEmpty()) { "Account email is required" }
    }
}
