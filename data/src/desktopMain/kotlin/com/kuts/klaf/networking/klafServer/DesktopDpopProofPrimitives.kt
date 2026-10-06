package com.kuts.klaf.networking.klafServer

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal class DesktopDpopProofPrimitives : DpopProofPrimitives {

    private val random = SecureRandom()

    override fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1_000L

    override fun nextJti(): String = ByteArray(24).also(random::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

    override fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)
}
