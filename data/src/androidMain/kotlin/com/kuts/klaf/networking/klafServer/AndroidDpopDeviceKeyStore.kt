package com.kuts.klaf.networking.klafServer

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val INSTALLATION_KEY_ALIAS = "klaf_dpop_installation_p256_v1"

/** The private device key never leaves Android Keystore. */
internal class AndroidDpopDeviceKeyStore(
    private val alias: String = INSTALLATION_KEY_ALIAS,
    private val storageRoot: File? = null,
) {

    private val mutex = Mutex()

    suspend fun loadOrCreate(): DpopDeviceKey = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (!keyStore.containsAlias(alias)) {
                    val sessions = storageRoot?.toPath()?.toAbsolutePath()?.normalize()?.resolve("security/sessions")
                    if (sessions != null && Files.exists(sessions, NOFOLLOW_LINKS)) {
                        if (!Files.isDirectory(sessions, NOFOLLOW_LINKS) || Files.isSymbolicLink(sessions)) {
                            throw DeviceKeyUnavailableException("Android session directory is invalid")
                        }
                        if (Files.list(sessions).use { files ->
                                files.anyMatch { it.fileName.toString().endsWith(".bin") }
                            }
                        ) throw DeviceKeyUnavailableException("Android device key is missing while account sessions exist")
                    }
                    KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                        initialize(KeyGenParameterSpec.Builder(alias,
                            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                            .setDigests(KeyProperties.DIGEST_SHA256)
                            .setUserAuthenticationRequired(false)
                            .build())
                    }.generateKeyPair()
                }
                val privateKey = keyStore.getKey(alias, null) as? PrivateKey
                    ?: throw DeviceKeyUnavailableException("Android device key is missing")
                val publicKey = keyStore.getCertificate(alias)?.publicKey as? ECPublicKey
                    ?: throw DeviceKeyUnavailableException("Android device certificate is missing")
                require(privateKey.encoded == null && publicKey.params.curve.field.fieldSize == 256)
                AndroidDpopDeviceKey(privateKey, publicKey)
            } catch (failure: DeviceKeyUnavailableException) {
                throw failure
            } catch (failure: Exception) {
                throw DeviceKeyUnavailableException("Android Keystore device key is unavailable", failure)
            }
        }
    }
}

private class AndroidDpopDeviceKey(
    private val privateKey: PrivateKey,
    publicKey: ECPublicKey,
) : DpopDeviceKey {

    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override val publicJwk = buildJsonObject {
        put("kty", "EC")
        put("crv", "P-256")
        put("x", encoder.encodeToString(fixedCoordinate(publicKey.w.affineX)))
        put("y", encoder.encodeToString(fixedCoordinate(publicKey.w.affineY)))
    }

    override suspend fun sign(input: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val der = Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
            update(input)
        }.sign()
        EcdsaDerSignature.toJoseRaw(der)
    }

    private fun fixedCoordinate(number: BigInteger): ByteArray {
        val source = number.toByteArray()
        require(source.size in 1..33)
        val unsigned = if (source.size == 33) source.copyOfRange(1, 33) else source
        return ByteArray(32).also { unsigned.copyInto(it, 32 - unsigned.size) }
    }
}
