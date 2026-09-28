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
internal suspend fun <T> klafServerRequest(timeoutMillis: Long, block: suspend () -> T): T = try {
    withTimeout(timeoutMillis) { block() }
} catch (failure: TimeoutCancellationException) {
    currentCoroutineContext().ensureActive()
    throw AccountOperationException(AccountFailure.TIMEOUT, cause = failure)
} catch (failure: CancellationException) {
    throw failure
} catch (failure: HttpRequestTimeoutException) {
    throw AccountOperationException(AccountFailure.TIMEOUT, cause = failure)
} catch (failure: ConnectTimeoutException) {
    throw AccountOperationException(AccountFailure.TIMEOUT, cause = failure)
} catch (failure: SocketTimeoutException) {
    throw AccountOperationException(AccountFailure.TIMEOUT, cause = failure)
} catch (failure: SerializationException) {
    throw AccountOperationException(AccountFailure.INVALID_RESPONSE, cause = failure)
} catch (failure: IOException) {
    throw AccountOperationException(AccountFailure.CONNECTION, cause = failure)
}
