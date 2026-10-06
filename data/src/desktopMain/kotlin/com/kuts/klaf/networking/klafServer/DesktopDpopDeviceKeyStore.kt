package com.kuts.klaf.networking.klafServer

import com.sun.jna.platform.win32.Crypt32Util
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardOpenOption
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val MAX_PROTECTED_DEVICE_KEY_BYTES = 16_384
private val DEVICE_KEY_PROCESS_MUTEX = Mutex()

internal interface DesktopDeviceKeyProtector {
    fun protect(data: ByteArray): ByteArray
    fun unprotect(data: ByteArray): ByteArray
}

private object WindowsDeviceKeyProtector : DesktopDeviceKeyProtector {
    override fun protect(data: ByteArray): ByteArray = Crypt32Util.cryptProtectData(data)
    override fun unprotect(data: ByteArray): ByteArray = Crypt32Util.cryptUnprotectData(data)
}

/** A missing or locked Secret Service must never fall back to a plaintext device key. */
internal fun desktopSystemProtector(): DesktopDeviceKeyProtector = when {
    System.getProperty("os.name").lowercase(Locale.ROOT).startsWith("windows") -> WindowsDeviceKeyProtector
    else -> throw DeviceKeyUnavailableException("Desktop OS device-key protection is unavailable")
}

internal class DesktopDpopDeviceKeyStore(
    private val root: Path,
    private val protector: DesktopDeviceKeyProtector = desktopSystemProtector(),
) {

    private val directory = root.toAbsolutePath().normalize().resolve("security")
    private val keyFile = directory.resolve("device-key.bin")
    private val lockFile = directory.resolve("device-key.lock")

    suspend fun loadOrCreate(): DpopDeviceKey = withContext(Dispatchers.IO) {
        DEVICE_KEY_PROCESS_MUTEX.withLock {
            if (Files.isSymbolicLink(directory)) {
                throw DeviceKeyUnavailableException("Device-key directory cannot be a symbolic link")
            }
            Files.createDirectories(directory)
            if (Files.isSymbolicLink(lockFile)) {
                throw DeviceKeyUnavailableException("Device-key lock cannot be a symbolic link")
            }
            FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use {
                    if (Files.exists(keyFile, NOFOLLOW_LINKS)) return@withLock readExisting()
                    val sessions = directory.resolve("sessions")
                    if (Files.exists(sessions, NOFOLLOW_LINKS)) {
                        if (!Files.isDirectory(sessions, NOFOLLOW_LINKS) || Files.isSymbolicLink(sessions)) {
                            throw DeviceKeyUnavailableException("Protected session directory is invalid")
                        }
                        if (Files.list(sessions).use { files ->
                                files.anyMatch { it.fileName.toString().endsWith(".bin") }
                            }
                        ) throw DeviceKeyUnavailableException("Device key is missing while account sessions exist")
                    }
                    val pair = KeyPairGenerator.getInstance("EC").apply {
                        initialize(ECGenParameterSpec("secp256r1"))
                    }.generateKeyPair()
                    val raw = encode(pair)
                    var temporary: Path? = null
                    try {
                        temporary = Files.createTempFile(directory, "device-key-", ".tmp")
                        Files.write(temporary, protector.protect(raw), StandardOpenOption.TRUNCATE_EXISTING)
                        Files.move(temporary, keyFile, ATOMIC_MOVE)
                    } catch (failure: Exception) {
                        throw DeviceKeyUnavailableException("Cannot persist device key", failure)
                    } finally {
                        raw.fill(0)
                        temporary?.let(Files::deleteIfExists)
                    }
                    StoredDesktopDpopKey(pair)
                }
            }
        }
    }

    private fun readExisting(): DpopDeviceKey {
        if (!Files.isRegularFile(keyFile, NOFOLLOW_LINKS) || Files.isSymbolicLink(keyFile)) {
            throw DeviceKeyUnavailableException("Device key is not a regular protected file")
        }
        val protected = Files.readAllBytes(keyFile)
        if (protected.size !in 1..MAX_PROTECTED_DEVICE_KEY_BYTES) {
            throw DeviceKeyUnavailableException("Protected device key has an invalid size")
        }
        val raw = try {
            protector.unprotect(protected)
        } catch (failure: Exception) {
            throw DeviceKeyUnavailableException("Cannot unlock existing device key", failure)
        }
        return try {
            StoredDesktopDpopKey(decode(raw))
        } catch (failure: Exception) {
            throw DeviceKeyUnavailableException("Existing device key is malformed", failure)
        } finally {
            raw.fill(0)
        }
    }

    private fun encode(pair: KeyPair): ByteArray {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return (encoder.encodeToString(pair.public.encoded) + ":" +
            encoder.encodeToString(pair.private.encoded)).toByteArray(Charsets.US_ASCII)
    }

    private fun decode(raw: ByteArray): KeyPair {
        val parts = raw.toString(Charsets.US_ASCII).split(':')
        require(parts.size == 2)
        val decoder = Base64.getUrlDecoder()
        val factory = KeyFactory.getInstance("EC")
        val public = factory.generatePublic(X509EncodedKeySpec(decoder.decode(parts[0]))) as ECPublicKey
        require(public.params.curve.field.fieldSize == 256)
        val private = factory.generatePrivate(PKCS8EncodedKeySpec(decoder.decode(parts[1])))
        return KeyPair(public, private)
    }
}

private class StoredDesktopDpopKey(private val pair: KeyPair) : DpopDeviceKey {

    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override val publicJwk = (pair.public as ECPublicKey).let { public ->
        buildJsonObject {
            put("kty", "EC")
            put("crv", "P-256")
            put("x", encoder.encodeToString(fixedCoordinate(public.w.affineX)))
            put("y", encoder.encodeToString(fixedCoordinate(public.w.affineY)))
        }
    }

    override suspend fun sign(input: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        Signature.getInstance("SHA256withECDSAinP1363Format").apply {
            initSign(pair.private)
            update(input)
        }.sign().also { require(it.size == 64) }
    }

    private fun fixedCoordinate(number: BigInteger): ByteArray {
        val source = number.toByteArray()
        require(source.size in 1..33)
        val unsigned = if (source.size == 33) source.copyOfRange(1, 33) else source
        return ByteArray(32).also { unsigned.copyInto(it, 32 - unsigned.size) }
    }
}
