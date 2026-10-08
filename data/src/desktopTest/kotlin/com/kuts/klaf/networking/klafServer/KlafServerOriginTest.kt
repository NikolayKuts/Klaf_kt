package com.kuts.klaf.networking.klafServer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KlafServerOriginTest {

    @Test
    fun secureDefaultPortHasOneOriginForRestAndWebSocketProof() {
        val origin = klafServerHttpOrigin("example.test", 443, isSecure = true)

        assertEquals("https://example.test", origin)
        assertEquals("wss://example.test", klafServerWebSocketBaseUrl(origin))
    }

    @Test
    fun localAndCustomPortsRemainExplicit() {
        assertEquals("http://127.0.0.1:8090", klafServerHttpOrigin("127.0.0.1", 8090, isSecure = false))
        assertEquals("https://example.test:8443", klafServerHttpOrigin("example.test", 8443, isSecure = true))
        assertEquals("ws://127.0.0.1:8090", klafServerWebSocketBaseUrl("http://127.0.0.1:8090"))
    }

    @Test
    fun hostCannotContainSchemeOrPath() {
        assertFailsWith<IllegalArgumentException> {
            klafServerHttpOrigin("https://example.test", 443, isSecure = true)
        }
        assertFailsWith<IllegalArgumentException> {
            klafServerHttpOrigin("example.test/path", 443, isSecure = true)
        }
    }
}
