package com.kuts.klaf.networking.klafServer

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val MAX_PROTECTED_SESSION_BYTES = 32_768
private val DESKTOP_SESSION_PROCESS_MUTEX = Mutex()
private val DESKTOP_REFRESH_PROCESS_MUTEX = Mutex()

/** Account/session-scoped encrypted bundles; filenames reveal neither email nor tokens. */
internal class DesktopProtectedSessionStore(
    root: Path,
    private val protector: DesktopDeviceKeyProtector = desktopSystemProtector(),
) : ProtectedAuthSessionStore {

    private val directory = root.toAbsolutePath().normalize().resolve("security/sessions")
    private val lockFile = directory.resolve("sessions.lock")
    private val json = Json { ignoreUnknownKeys = false }

    override suspend fun read(origin: String, email: String): ProtectedAuthSession? = locked {
        val file = sessionFile(origin, email)
        if (!Files.exists(file, NOFOLLOW_LINKS)) return@locked null
        if (!Files.isRegularFile(file, NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) {
            throw ProtectedSessionUnavailableException("Protected session path is invalid")
        }
        val protected = Files.readAllBytes(file)
        if (protected.size !in 1..MAX_PROTECTED_SESSION_BYTES) {
            throw ProtectedSessionUnavailableException("Protected session size is invalid")
        }
        val raw = try {
            protector.unprotect(protected)
        } catch (failure: Exception) {
            throw ProtectedSessionUnavailableException("Cannot unlock protected account session", failure)
        }
        try {
            json.decodeFromString<ProtectedAuthSession>(raw.decodeToString()).also { session ->
                if (session.serverOrigin != origin || session.accountEmail != email ||
                    session.deviceId.isBlank() || session.accessToken.isBlank() || session.refreshToken.isBlank()
                ) throw ProtectedSessionUnavailableException("Protected account session does not match its profile")
            }
        } catch (failure: ProtectedSessionUnavailableException) {
            throw failure
        } catch (failure: Exception) {
            throw ProtectedSessionUnavailableException("Protected account session is malformed", failure)
        } finally {
            raw.fill(0)
        }
    }

    override suspend fun write(session: ProtectedAuthSession): Unit = locked {
        require(session.deviceId.isNotBlank() && session.accessToken.isNotBlank() && session.refreshToken.isNotBlank())
        val file = sessionFile(session.serverOrigin, session.accountEmail)
        val raw = json.encodeToString(session).encodeToByteArray()
        var temporary: Path? = null
        try {
            temporary = Files.createTempFile(directory, "session-", ".tmp")
            Files.write(temporary, protector.protect(raw), StandardOpenOption.TRUNCATE_EXISTING)
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { it.force(true) }
            Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (failure: Exception) {
            throw ProtectedSessionUnavailableException("Cannot persist protected account session", failure)
        } finally {
            raw.fill(0)
            temporary?.let(Files::deleteIfExists)
        }
    }

    override suspend fun remove(origin: String, email: String): Unit = locked {
        val file = sessionFile(origin, email)
        if (Files.isSymbolicLink(file)) throw ProtectedSessionUnavailableException("Protected session path is invalid")
        Files.deleteIfExists(file)
    }

    override suspend fun <T> withRefreshLock(origin: String, email: String, action: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            DESKTOP_REFRESH_PROCESS_MUTEX.withLock {
                if (Files.isSymbolicLink(directory)) {
                    throw ProtectedSessionUnavailableException("Protected session directory cannot be a symbolic link")
                }
                Files.createDirectories(directory)
                val refreshLock = sessionFile(origin, email).resolveSibling(
                    sessionFile(origin, email).fileName.toString() + ".refresh.lock",
                )
                if (Files.isSymbolicLink(refreshLock)) {
                    throw ProtectedSessionUnavailableException("Protected refresh lock cannot be a symbolic link")
                }
                FileChannel.open(refreshLock, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                    channel.lock().use { action() }
                }
            }
        }

    private suspend fun <T> locked(action: () -> T): T = withContext(Dispatchers.IO) {
        DESKTOP_SESSION_PROCESS_MUTEX.withLock {
            if (Files.isSymbolicLink(directory)) {
                throw ProtectedSessionUnavailableException("Protected session directory cannot be a symbolic link")
            }
            Files.createDirectories(directory)
            if (Files.isSymbolicLink(lockFile)) {
                throw ProtectedSessionUnavailableException("Protected session lock cannot be a symbolic link")
            }
            FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use { action() }
            }
        }
    }

    private fun sessionFile(origin: String, email: String): Path {
        requireSafeAuthOrigin(origin)
        require(email.isNotBlank())
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((origin + "\u0000" + email).toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return directory.resolve("$name.bin")
    }
}
