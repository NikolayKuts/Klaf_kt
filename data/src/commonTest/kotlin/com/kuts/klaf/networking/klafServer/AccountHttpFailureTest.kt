package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountHttpFailureTest {

    @Test
    fun `secure authentication failures produce actionable account errors`() {
        assertEquals(AccountFailure.INVALID_CREDENTIALS, accountHttpFailure(401, "INVALID_CREDENTIALS"))
        assertEquals(AccountFailure.SIGN_IN_REQUIRED, accountHttpFailure(401, "SIGN_IN_REQUIRED"))
        assertEquals(AccountFailure.APPROVAL_PENDING, accountHttpFailure(409, "REGISTRATION_NOT_APPROVED"))
        assertEquals(AccountFailure.APPROVAL_PENDING, accountHttpFailure(403, "DEVICE_NOT_APPROVED"))
        assertEquals(AccountFailure.DEVICE_PROOF, accountHttpFailure(401, "INVALID_DEVICE_PROOF"))
        assertEquals(AccountFailure.SERVER_BUSY, accountHttpFailure(429, "AUTH_BUSY"))
    }
}
