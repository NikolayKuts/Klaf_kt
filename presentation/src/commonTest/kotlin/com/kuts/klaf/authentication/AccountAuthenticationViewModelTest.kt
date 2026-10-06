package com.kuts.klaf.authentication

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.managers.AccountSignInResult
import com.kuts.domain.managers.AccountSignUpResult
import com.kuts.domain.managers.AccountEnrollmentKind
import com.kuts.domain.managers.AccountEnrollmentStatus
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.AccountFailure
import com.kuts.klaf.common.UiText
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.account_error_connection
import com.kuts.klaf.presentation.resources.account_error_timeout
import com.kuts.klaf.presentation.resources.account_error_not_found
import com.kuts.klaf.presentation.resources.account_error_exists
import com.kuts.klaf.presentation.resources.account_error_device
import com.kuts.klaf.presentation.resources.account_error_server
import com.kuts.klaf.presentation.resources.account_error_invalid_response
import com.kuts.klaf.presentation.resources.account_error_pending_signup
import com.kuts.klaf.presentation.resources.account_error_sign_in_required
import com.kuts.klaf.presentation.resources.account_error_invalid_credentials
import com.kuts.klaf.presentation.resources.account_error_approval_pending
import com.kuts.klaf.presentation.resources.account_error_device_proof
import com.kuts.klaf.presentation.resources.account_error_invalid_request
import com.kuts.klaf.presentation.resources.account_error_unknown
import com.kuts.klaf.presentation.resources.account_error_throttled
import com.kuts.klaf.presentation.resources.account_error_throttled_wait
import com.kuts.domain.managers.IAccountSession
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class AccountAuthenticationViewModelTest {

    @Test
    fun `password reset validates input and stays signed out after success`() = runTest(dispatcher) {
        val account = FakeAccountSession()
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")
        viewModel.resetPassword("token", "short", "short")
        advanceUntilIdle()
        assertEquals(0, account.resetCalls)

        viewModel.resetPassword("token", "replacement-passphrase", "mismatch-passphrase")
        advanceUntilIdle()
        assertEquals(0, account.resetCalls)

        viewModel.resetPassword("token", "replacement-passphrase", "replacement-passphrase")
        advanceUntilIdle()
        assertEquals(1, account.resetCalls)
        assertEquals(true, viewModel.passwordResetCompleted.value)
        assertIs<LoadingState.Non>(viewModel.screenLoadingState.value)
    }

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sign in requires a password before requesting device approval`() = runTest(dispatcher) {
        val account = FakeAccountSession()
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")

        viewModel.signIn()
        advanceUntilIdle()
        assertEquals(0, account.signInCalls)
        assertFalse(viewModel.isPasswordless)

        viewModel.updatePassword("long-secret-phrase")
        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(1, account.signInCalls)
        assertEquals("long-secret-phrase", account.lastSignInPassword)
        assertEquals("alice@example.test", viewModel.pendingDeviceRegistrationEmail.value)
        assertIs<LoadingState.Non>(viewModel.screenLoadingState.value)
        assertEquals(0, account.registerCalls)

        viewModel.registerDevice()
        advanceUntilIdle()

        assertEquals(1, account.registerCalls)
        assertNull(viewModel.pendingDeviceRegistrationEmail.value)
        assertEquals(
            AuthenticationAction.SIGN_IN,
            (viewModel.screenLoadingState.value as LoadingState.Success<*>).data,
        )
    }

    @Test
    fun `sign up rejects blank short whitespace and mismatched passwords`() = runTest(dispatcher) {
        val account = FakeAccountSession()
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())

        viewModel.updateEmail("not-an-email")
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(0, account.signUpCalls)

        viewModel.updateEmail("alice@example.test")
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(0, account.signUpCalls)

        viewModel.updatePassword("short")
        viewModel.updatePasswordConfirmation("short")
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(0, account.signUpCalls)

        viewModel.updatePassword("long secret phrase")
        viewModel.updatePasswordConfirmation("long secret phrase")
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(0, account.signUpCalls)

        viewModel.updatePassword("long-secret-phrase")
        viewModel.updatePasswordConfirmation("different-password")
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(0, account.signUpCalls)

        viewModel.updatePasswordConfirmation("long-secret-phrase")
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(1, account.signUpCalls)
        assertEquals("long-secret-phrase", account.lastSignUpPassword)
        assertIs<LoadingState.Non>(viewModel.screenLoadingState.value)
        assertEquals("pending-id-123456789", viewModel.pendingApprovalRequestId.value)
        assertEquals("", viewModel.typingState.value.passwordHolder.text)
        assertEquals("", viewModel.typingState.value.passwordConfirmationHolder?.text)
    }

    @Test
    fun `new device approval request never reports successful sign in`() = runTest(dispatcher) {
        val account = FakeAccountSession().apply {
            signInResult = AccountSignInResult.PendingApproval("pending-id-123456789")
        }
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")
        viewModel.updatePassword("long-secret-phrase")

        viewModel.signIn()
        advanceUntilIdle()

        assertIs<LoadingState.Non>(viewModel.screenLoadingState.value)
        assertEquals("pending-id-123456789", viewModel.pendingApprovalRequestId.value)
        assertEquals("", viewModel.typingState.value.passwordHolder.text)
        assertEquals(0, account.registerCalls)
    }

    @Test
    fun `approved enrollment needs password reentry before completing account selection`() = runTest(dispatcher) {
        val account = FakeAccountSession()
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")
        viewModel.updatePassword("long-secret-phrase")
        viewModel.updatePasswordConfirmation("long-secret-phrase")
        viewModel.signUp()
        advanceUntilIdle()

        viewModel.checkPendingApproval()
        advanceUntilIdle()
        assertEquals(AccountEnrollmentStatus.APPROVED, viewModel.pendingApprovalStatus.value)
        viewModel.completePendingApproval()
        advanceUntilIdle()
        assertEquals(0, account.completionCalls)

        viewModel.updatePassword("long-secret-phrase")
        viewModel.completePendingApproval()
        advanceUntilIdle()
        assertEquals(1, account.completionCalls)
        assertEquals(AuthenticationAction.SIGN_UP,
            (viewModel.screenLoadingState.value as LoadingState.Success<*>).data)
    }

    @Test
    fun `sign in failures show specific messages and release loading state`() = runTest(dispatcher) {
        val expected = mapOf(
            AccountFailure.CONNECTION to Res.string.account_error_connection,
            AccountFailure.TIMEOUT to Res.string.account_error_timeout,
            AccountFailure.ACCOUNT_NOT_FOUND to Res.string.account_error_not_found,
            AccountFailure.ACCOUNT_EXISTS to Res.string.account_error_exists,
            AccountFailure.DEVICE_REGISTRATION to Res.string.account_error_device,
            AccountFailure.SERVER to Res.string.account_error_server,
            AccountFailure.INVALID_RESPONSE to Res.string.account_error_invalid_response,
            AccountFailure.PENDING_SIGNUP to Res.string.account_error_pending_signup,
            AccountFailure.SIGN_IN_REQUIRED to Res.string.account_error_sign_in_required,
            AccountFailure.INVALID_CREDENTIALS to Res.string.account_error_invalid_credentials,
            AccountFailure.APPROVAL_PENDING to Res.string.account_error_approval_pending,
            AccountFailure.DEVICE_PROOF to Res.string.account_error_device_proof,
            AccountFailure.INVALID_REQUEST to Res.string.account_error_invalid_request,
            AccountFailure.THROTTLED to Res.string.account_error_throttled,
            AccountFailure.UNKNOWN to Res.string.account_error_unknown,
        )
        for ((failure, message) in expected) {
            val account = FakeAccountSession().apply { exception = AccountOperationException(failure) }
            val viewModel = AccountAuthenticationViewModel(account, contextProvider())
            viewModel.updateEmail("alice@example.test")
            viewModel.updatePassword("long-secret-phrase")
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.eventMessage.first() }
            viewModel.signIn()
            advanceUntilIdle()
            assertEquals(message, (event.await().text as UiText.Resource).resource)
            assertIs<LoadingState.Error<*>>(viewModel.screenLoadingState.value)
        }
    }

    @Test
    fun `throttled sign in tells the user when to retry`() = runTest(dispatcher) {
        val account = FakeAccountSession().apply {
            exception = AccountOperationException(AccountFailure.THROTTLED, retryAfterSeconds = 30)
        }
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")
        viewModel.updatePassword("long-secret-phrase")
        val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.eventMessage.first() }

        viewModel.signIn()
        advanceUntilIdle()

        val message = assertIs<UiText.Resource>(event.await().text)
        assertEquals(Res.string.account_error_throttled_wait, message.resource)
        assertEquals(listOf(30), message.args)
    }

    @Test
    fun `sign up and registration also use specific errors and can retry`() = runTest(dispatcher) {
        val account = FakeAccountSession().apply { exception = AccountOperationException(AccountFailure.ACCOUNT_EXISTS) }
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")
        viewModel.updatePassword("long-secret-phrase")
        viewModel.updatePasswordConfirmation("long-secret-phrase")
        val duplicate = async(start = CoroutineStart.UNDISPATCHED) { viewModel.eventMessage.first() }
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(Res.string.account_error_exists, (duplicate.await().text as UiText.Resource).resource)
        account.exception = null
        viewModel.signIn()
        advanceUntilIdle()
        account.exception = AccountOperationException(AccountFailure.CONNECTION)
        val offline = async(start = CoroutineStart.UNDISPATCHED) { viewModel.eventMessage.first() }
        viewModel.registerDevice()
        advanceUntilIdle()
        assertEquals(Res.string.account_error_connection, (offline.await().text as UiText.Resource).resource)
        assertEquals("alice@example.test", viewModel.pendingDeviceRegistrationEmail.value)
        account.exception = null
        viewModel.registerDevice()
        advanceUntilIdle()
        assertIs<LoadingState.Success<*>>(viewModel.screenLoadingState.value)
    }

    @Test
    fun `cancellation is not shown as an account error and clears loading`() = runTest(dispatcher) {
        val account = FakeAccountSession().apply { exception = CancellationException("screen closed") }
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")
        viewModel.updatePassword("long-secret-phrase")
        viewModel.signIn()
        advanceUntilIdle()
        assertIs<LoadingState.Non>(viewModel.screenLoadingState.value)
        assertNull(viewModel.pendingDeviceRegistrationEmail.value)
    }

    @Test
    fun `dismissing new device prompt does not register or sign in`() = runTest(dispatcher) {
        val account = FakeAccountSession()
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")
        viewModel.updatePassword("long-secret-phrase")
        viewModel.signIn()
        advanceUntilIdle()

        viewModel.cancelDeviceRegistration()
        viewModel.registerDevice()
        advanceUntilIdle()

        assertEquals(0, account.registerCalls)
        assertNull(viewModel.pendingDeviceRegistrationEmail.value)
        assertIs<LoadingState.Non>(viewModel.screenLoadingState.value)
    }

    private fun contextProvider() = object : ICoroutineContextProvider {
        override val io: CoroutineContext = dispatcher
    }

    private class FakeAccountSession : IAccountSession {
        override val selectedAccountEmail: Flow<String?> = MutableStateFlow(null)
        var signInCalls = 0
        var signUpCalls = 0
        var registerCalls = 0
        var exception: Exception? = null
        var lastSignInPassword: String? = null
        var lastSignUpPassword: String? = null
        var signInResult: AccountSignInResult = AccountSignInResult.DeviceRegistrationRequired("alice@example.test")
        var completionCalls = 0
        var resetCalls = 0

        override suspend fun signUp(email: String, password: String): AccountSignUpResult {
            lastSignUpPassword = password
            signUp(email)
            return AccountSignUpResult.PendingApproval("pending-id-123456789")
        }

        override suspend fun signIn(email: String, password: String): AccountSignInResult {
            lastSignInPassword = password
            return signIn(email)
        }

        override suspend fun signUp(email: String) {
            signUpCalls++
            exception?.let { throw it }
        }

        override suspend fun signIn(email: String): AccountSignInResult {
            signInCalls++
            exception?.let { throw it }
            return signInResult
        }

        override suspend fun registerDevice(email: String) {
            registerCalls++
            exception?.let { throw it }
        }

        override suspend fun checkPendingEnrollment(): AccountEnrollmentStatus = AccountEnrollmentStatus.APPROVED

        override suspend fun completePendingEnrollment(password: String): AccountEnrollmentKind {
            completionCalls++
            return AccountEnrollmentKind.ACCOUNT
        }

        override suspend fun signOut() = Unit

        override suspend fun resetPassword(email: String, token: String, newPassword: String) {
            resetCalls++
            exception?.let { throw it }
        }
    }
}
