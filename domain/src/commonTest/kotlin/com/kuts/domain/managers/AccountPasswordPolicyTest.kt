package com.kuts.domain.managers

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountPasswordPolicyTest {

    @Test
    fun `signup password policy matches the server bounds without normalizing input`() {
        assertTrue(AccountPasswordPolicy.isValid("long-secret-phrase"))
        assertFalse(AccountPasswordPolicy.isValid(""))
        assertFalse(AccountPasswordPolicy.isValid("fourteen-chars"))
        assertFalse(AccountPasswordPolicy.isValid("long secret phrase"))
        assertFalse(AccountPasswordPolicy.isValid("long-secret-phrase\n"))
        assertFalse(AccountPasswordPolicy.isValid("long-secret-phrase\u0000"))
        assertFalse(AccountPasswordPolicy.isValid("long-secret-phrase\uD800"))
        assertFalse(AccountPasswordPolicy.isValid("a".repeat(129)))
        assertTrue(AccountPasswordPolicy.isValid("🎓".repeat(15)))
    }
}
