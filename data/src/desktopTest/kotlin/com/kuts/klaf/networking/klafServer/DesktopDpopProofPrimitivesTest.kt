package com.kuts.klaf.networking.klafServer

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopDpopProofPrimitivesTest {

    @Test
    fun `proof identifiers have CSPRNG length and SHA-256 matches provider`() {
        val primitives = DesktopDpopProofPrimitives()
        val ids = (1..100).map { primitives.nextJti() }
        assertEquals(100, ids.toSet().size)
        assertTrue(ids.all { it.length in 16..128 })
        assertContentEquals(MessageDigest.getInstance("SHA-256").digest("access".encodeToByteArray()),
            primitives.sha256("access".encodeToByteArray()))
    }
}
