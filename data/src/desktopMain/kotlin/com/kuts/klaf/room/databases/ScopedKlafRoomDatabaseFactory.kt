package com.kuts.klaf.room.databases

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.Dispatchers

private const val GUEST_DATABASE_NAME = "klaf_guest.db"
private const val ACCOUNT_DATABASE_PREFIX = "klaf_account_"
private const val HEX_DIGITS = "0123456789abcdef"

/** Opens caller-owned Room instances; the legacy singleton database is not changed. */
class ScopedKlafRoomDatabaseFactory(
    private val directory: File = File(System.getProperty("user.home"), ".klaf_kt"),
) : LocalRoomDatabaseFactory {

    override fun openGuest(): KlafRoomDatabase = open(GUEST_DATABASE_NAME)

    override fun openAccount(email: String): KlafRoomDatabase = open(accountDatabaseName(email))

    private fun open(fileName: String): KlafRoomDatabase {
        require(directory.isDirectory || directory.mkdirs()) { "Cannot create local database directory" }
        val path = File(directory, fileName).absolutePath
        return Room.databaseBuilder<KlafRoomDatabase>(name = path)
            .addMigrations(Migrations.from6To7, Migrations.from7To8, DeckCardIntegrityMigration, SyncIdentityMigration)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    private fun accountDatabaseName(email: String): String {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        require(normalizedEmail.isNotEmpty()) { "Account email is required" }
        val digest = MessageDigest.getInstance("SHA-256").digest(normalizedEmail.toByteArray(Charsets.UTF_8))
        val hash = buildString(digest.size * 2) {
            digest.forEach { byte ->
                val value = byte.toInt() and 0xff
                append(HEX_DIGITS[value ushr 4])
                append(HEX_DIGITS[value and 0x0f])
            }
        }
        return "$ACCOUNT_DATABASE_PREFIX$hash.db"
    }
}
