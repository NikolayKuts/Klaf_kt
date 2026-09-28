package com.kuts.klaf.di

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientStorageModeTest {

    @Test
    fun `normal build defaults to Room without a mode opt in`() {
        listOf(null, "", "  ").forEach { mode ->
            assertTrue(useAccountScopedStorage(mode, isolatedTestIdentity = false, legacyDatabaseExists = false))
        }
    }

    @Test
    fun `default Room launch refuses an existing legacy database`() {
        assertFailsWith<IllegalStateException> {
            useAccountScopedStorage(null, isolatedTestIdentity = false, legacyDatabaseExists = true)
        }
    }

    @Test
    fun `explicit legacy override remains available for recovery`() {
        assertFalse(useAccountScopedStorage("legacy", isolatedTestIdentity = false, legacyDatabaseExists = false))
        assertFalse(useAccountScopedStorage(" LEGACY ", isolatedTestIdentity = false, legacyDatabaseExists = true))
    }

    @Test
    fun `ordinary Room mode requires a clean database path`() {
        assertTrue(useAccountScopedStorage("room", isolatedTestIdentity = false, legacyDatabaseExists = false))
        assertFailsWith<IllegalStateException> {
            useAccountScopedStorage("room", isolatedTestIdentity = false, legacyDatabaseExists = true)
        }
    }

    @Test
    fun `isolated test identity stays scoped even with a legacy test database`() {
        assertTrue(useAccountScopedStorage(null, isolatedTestIdentity = true, legacyDatabaseExists = true))
    }

    @Test
    fun `unknown mode is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            useAccountScopedStorage("other", isolatedTestIdentity = false, legacyDatabaseExists = false)
        }
    }
}
