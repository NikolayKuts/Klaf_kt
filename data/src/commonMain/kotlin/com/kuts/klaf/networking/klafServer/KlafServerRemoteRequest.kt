package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.io.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException

/** Translate this request's timeout, never cancellation imposed by its caller. */
internal suspend fun <T> klafServerRequest(timeoutMillis: Long, block: suspend () -> T): T = klafServerRequest(
    timeoutMillis, operation = null, block = block,
)

/** Operation labels must be fixed strings, without account details, URLs with queries or payloads. */
internal suspend fun <T> klafServerRequest(
    timeoutMillis: Long,
    operation: String?,
    block: suspend () -> T,
): T = try {
    withTimeout(timeoutMillis) { block() }
} catch (failure: TimeoutCancellationException) {
    currentCoroutineContext().ensureActive()
    throw requestFailure(AccountFailure.TIMEOUT, failure, operation)
} catch (failure: CancellationException) {
    throw failure
} catch (failure: HttpRequestTimeoutException) {
    throw requestFailure(AccountFailure.TIMEOUT, failure, operation)
} catch (failure: ConnectTimeoutException) {
    throw requestFailure(AccountFailure.TIMEOUT, failure, operation)
} catch (failure: SocketTimeoutException) {
    throw requestFailure(AccountFailure.TIMEOUT, failure, operation)
} catch (failure: AccountOperationException) {
    if (operation == null || failure.failure != AccountFailure.TIMEOUT) throw failure
    // Token renewal can time out inside request signing, before HTTP sends the sync/image request.
    throw AccountOperationException(
        failure = failure.failure,
        message = "$operation: ${failure.failure.name}",
        cause = failure,
        retryAfterSeconds = failure.retryAfterSeconds,
    )
} catch (failure: SerializationException) {
    throw requestFailure(AccountFailure.INVALID_RESPONSE, failure, operation)
} catch (failure: IOException) {
    throw requestFailure(AccountFailure.CONNECTION, failure, operation)
}

private fun requestFailure(
    failure: AccountFailure,
    cause: Throwable,
    operation: String?,
): AccountOperationException = AccountOperationException(
    failure = failure,
    message = if (operation == null) failure.name else "$operation: ${failure.name}",
    cause = cause,
)
