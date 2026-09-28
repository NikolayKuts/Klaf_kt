package com.kuts.klaf.authentication

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.managers.AccountSignInResult
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
import com.kuts.klaf.presentation.resources.account_error_invalid_request
import com.kuts.klaf.presentation.resources.account_error_unknown
import com.kuts.domain.managers.IAccountSession
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun `sign in needs explicit device confirmation and no password`() = runTest(dispatcher) {
        val account = FakeAccountSession()
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(1, account.signInCalls)
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
    fun `sign up accepts blank password but rejects invalid email`() = runTest(dispatcher) {
        val account = FakeAccountSession()
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())

        viewModel.updateEmail("not-an-email")
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(0, account.signUpCalls)

        viewModel.updateEmail("alice@example.test")
        viewModel.signUp()
        advanceUntilIdle()
        assertEquals(1, account.signUpCalls)
        assertEquals(
            AuthenticationAction.SIGN_UP,
            (viewModel.screenLoadingState.value as LoadingState.Success<*>).data,
        )
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
            AccountFailure.INVALID_REQUEST to Res.string.account_error_invalid_request,
            AccountFailure.UNKNOWN to Res.string.account_error_unknown,
        )
        for ((failure, message) in expected) {
            val account = FakeAccountSession().apply { exception = AccountOperationException(failure) }
            val viewModel = AccountAuthenticationViewModel(account, contextProvider())
            viewModel.updateEmail("alice@example.test")
            val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.eventMessage.first() }
            viewModel.signIn()
            advanceUntilIdle()
            assertEquals(message, (event.await().text as UiText.Resource).resource)
            assertIs<LoadingState.Error<*>>(viewModel.screenLoadingState.value)
        }
    }

    @Test
    fun `sign up and registration also use specific errors and can retry`() = runTest(dispatcher) {
        val account = FakeAccountSession().apply { exception = AccountOperationException(AccountFailure.ACCOUNT_EXISTS) }
        val viewModel = AccountAuthenticationViewModel(account, contextProvider())
        viewModel.updateEmail("alice@example.test")
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

        override suspend fun signUp(email: String) {
            signUpCalls++
            exception?.let { throw it }
        }

        override suspend fun signIn(email: String): AccountSignInResult {
            signInCalls++
            exception?.let { throw it }
            return AccountSignInResult.DeviceRegistrationRequired(email)
        }

        override suspend fun registerDevice(email: String) {
            registerCalls++
            exception?.let { throw it }
        }

        override suspend fun signOut() = Unit
    }
}
