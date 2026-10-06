package com.kuts.klaf.networking.klafServer

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val SESSION_AES_ALIAS = "klaf_account_sessions_aes_v1"
private const val MAX_ENCRYPTED_SESSION_BYTES = 32_768
private val SESSION_FILE_HEADER = "KLAFS1".toByteArray(Charsets.US_ASCII)
private val ANDROID_SESSION_PROCESS_MUTEX = Mutex()
private val ANDROID_REFRESH_PROCESS_MUTEX = Mutex()

/** Keystore AES-GCM protects access/refresh credentials in app-private profile files. */
internal class AndroidProtectedSessionStore(
    root: File,
    private val keyAlias: String = SESSION_AES_ALIAS,
) : ProtectedAuthSessionStore {

    private val directory = root.toPath().toAbsolutePath().normalize().resolve("security/sessions")
    private val lockFile = directory.resolve("sessions.lock")
    private val json = Json { ignoreUnknownKeys = false }

    override suspend fun read(origin: String, email: String): ProtectedAuthSession? = locked {
        val file = sessionFile(origin, email)
        if (!Files.exists(file, NOFOLLOW_LINKS)) return@locked null
        if (!Files.isRegularFile(file, NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) {
            throw ProtectedSessionUnavailableException("Android session path is invalid")
        }
        val encrypted = Files.readAllBytes(file)
        if (encrypted.size !in SESSION_FILE_HEADER.size + 12 + 16..MAX_ENCRYPTED_SESSION_BYTES ||
            !encrypted.copyOfRange(0, SESSION_FILE_HEADER.size).contentEquals(SESSION_FILE_HEADER)
        ) throw ProtectedSessionUnavailableException("Android session bundle is malformed")
        val key = loadKey(create = false)
        val offset = SESSION_FILE_HEADER.size
        val iv = encrypted.copyOfRange(offset, offset + 12)
        val raw = try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                updateAAD(profileAad(origin, email))
            }.doFinal(encrypted, offset + 12, encrypted.size - offset - 12)
        } catch (failure: Exception) {
            throw ProtectedSessionUnavailableException("Cannot unlock Android account session", failure)
        }
        try {
            json.decodeFromString<ProtectedAuthSession>(raw.decodeToString()).also { session ->
                if (session.serverOrigin != origin || session.accountEmail != email ||
                    session.deviceId.isBlank() || session.accessToken.isBlank() || session.refreshToken.isBlank()
                ) throw ProtectedSessionUnavailableException("Android account session profile mismatch")
            }
        } catch (failure: ProtectedSessionUnavailableException) {
            throw failure
        } catch (failure: Exception) {
            throw ProtectedSessionUnavailableException("Android account session is malformed", failure)
        } finally {
            raw.fill(0)
        }
    }

    override suspend fun write(session: ProtectedAuthSession): Unit = locked {
        require(session.deviceId.isNotBlank() && session.accessToken.isNotBlank() && session.refreshToken.isNotBlank())
        val file = sessionFile(session.serverOrigin, session.accountEmail)
        val key = loadKey(create = true)
        val raw = json.encodeToString(session).encodeToByteArray()
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, key)
                updateAAD(profileAad(session.serverOrigin, session.accountEmail))
            }
            val iv = cipher.iv.also { require(it.size == 12) }
            val encrypted = SESSION_FILE_HEADER + iv + cipher.doFinal(raw)
            val temporary = Files.createTempFile(directory, "session-", ".tmp")
            try {
                Files.write(temporary, encrypted, StandardOpenOption.TRUNCATE_EXISTING)
                FileChannel.open(temporary, StandardOpenOption.WRITE).use { it.force(true) }
                Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(temporary)
            }
        } catch (failure: Exception) {
            throw ProtectedSessionUnavailableException("Cannot persist Android account session", failure)
        } finally {
            raw.fill(0)
        }
    }

    override suspend fun remove(origin: String, email: String): Unit = locked {
        val file = sessionFile(origin, email)
        if (Files.isSymbolicLink(file)) throw ProtectedSessionUnavailableException("Android session path is invalid")
        Files.deleteIfExists(file)
    }

    override suspend fun <T> withRefreshLock(origin: String, email: String, action: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            ANDROID_REFRESH_PROCESS_MUTEX.withLock {
                if (Files.isSymbolicLink(directory)) {
                    throw ProtectedSessionUnavailableException("Android session directory cannot be a symbolic link")
                }
                Files.createDirectories(directory)
                val refreshLock = sessionFile(origin, email).resolveSibling(
                    sessionFile(origin, email).fileName.toString() + ".refresh.lock",
                )
                if (Files.isSymbolicLink(refreshLock)) {
                    throw ProtectedSessionUnavailableException("Android refresh lock cannot be a symbolic link")
                }
                FileChannel.open(refreshLock, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                    channel.lock().use { action() }
                }
            }
        }

    private fun loadKey(create: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing
        if (!create || Files.list(directory).use { files ->
                files.anyMatch { it.fileName.toString().endsWith(".bin") }
            }
        ) throw ProtectedSessionUnavailableException("Android session key is missing")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }

    private suspend fun <T> locked(action: () -> T): T = withContext(Dispatchers.IO) {
        ANDROID_SESSION_PROCESS_MUTEX.withLock {
            if (Files.isSymbolicLink(directory)) {
                throw ProtectedSessionUnavailableException("Android session directory cannot be a symbolic link")
            }
            Files.createDirectories(directory)
            if (Files.isSymbolicLink(lockFile)) {
                throw ProtectedSessionUnavailableException("Android session lock cannot be a symbolic link")
            }
            FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use { action() }
            }
        }
    }

    private fun sessionFile(origin: String, email: String): Path {
        requireSafeAuthOrigin(origin)
        require(email.isNotBlank())
        val digest = MessageDigest.getInstance("SHA-256").digest(profileAad(origin, email))
        val name = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return directory.resolve("$name.bin")
    }

    private fun profileAad(origin: String, email: String): ByteArray =
        (origin + "\u0000" + email).toByteArray(Charsets.UTF_8)
}
