package com.kuts.klaf.di

import kotlin.test.Test
import kotlin.test.assertEquals

class KlafServerEndpointConfigTest {

    @Test
    fun secureDefaultPortUsesOnePublicOrigin() {
        val endpoint = KlafServerEndpointConfig("example.test", 443, isSecure = true)

        assertEquals("https://example.test", endpoint.restBaseUrl())
    }

    @Test
    fun nonDefaultPortsRemainExplicit() {
        assertEquals(
            "https://example.test:8443",
            KlafServerEndpointConfig("example.test", 8443, isSecure = true).restBaseUrl(),
        )
        assertEquals(
            "http://127.0.0.1:8090",
            KlafServerEndpointConfig("127.0.0.1", 8090).restBaseUrl(),
        )
    }
}
