package com.kuts.klaf.networking.agentDriver

import org.agentdriver.project.domain.error.AssistantError
import org.agentdriver.project.ktorclient.external.AssistantClientDisconnectCause
import org.agentdriver.project.ktorclient.external.KtorAssistantClientException

private const val MAX_MESSAGE_LENGTH = 80
private const val MAX_DETAIL_LENGTH = 60

/** A short, human-readable reason for the UI, taken from the failure's own type. */
internal fun Throwable.toShortAgentDriverMessage(): String = when (this) {
    is KtorAssistantClientException.NotConnected -> "Not connected"
    is KtorAssistantClientException.ConnectionClosed -> closeCode
        ?.let { code -> "Connection closed ($code)" }
        ?: "Connection closed"
    is KtorAssistantClientException.ConnectionLost -> "Connection lost"
    is KtorAssistantClientException.ServerStopping -> "Server is stopping"
    is KtorAssistantClientException.SessionStartFailed -> "Session unavailable"
    is KtorAssistantClientException.UnsupportedRequestOption ->
        "Unsupported by this server (${option.name})"
    is KtorAssistantClientException.RequestCancelled -> "Cancelled"

    // The server says why in a typed payload the exception's own message does not repeat: without
    // reading `error`, every provider-side refusal -- a busy session, a rate limit, a denied
    // permission -- reaches the user and the log as the same three words.
    is KtorAssistantClientException.RequestFailed -> "Generation failed: ${error.toShortMessage()}"

    // The only type that says nothing on its own. "Connection failed" is true of a name that would
    // not resolve, a gateway answering 502, and a certificate the device would not trust alike, and
    // those need different things done about them -- so the detail comes along.
    is KtorAssistantClientException.TransportFailure ->
        firstDetailMessage(maxLength = MAX_DETAIL_LENGTH)
            ?.let { detail -> "Connection failed: $detail" }
            ?: "Connection failed"

    else -> firstDetailMessage(maxLength = MAX_MESSAGE_LENGTH) ?: "Request failed"
}

/** The provider's own reason for refusing a request, in one short phrase. */
internal fun AssistantError.toShortMessage(): String =  when (this) {
    is AssistantError.PermissionDenied -> "permission denied (${action.name})"
    is AssistantError.SessionBusy -> "session is busy with another request"
    is AssistantError.RateLimitExceeded -> "rate limit, retry in ${retryAfter.value} ms"
    is AssistantError.InvalidMediaType -> "invalid media (${issue.name})"
    is AssistantError.UnsupportedRequestOption -> "unsupported option (${option.name})"
    is AssistantError.ProviderOperationFailed -> "${operation.wireName} failed (${reason.name})"
    is AssistantError.RequestTimeout -> "${operation.wireName} timed out"
    AssistantError.Unknown -> "provider reported an unknown error"
}

/** The same, for a connection that is down rather than a request that failed. */
internal fun AssistantClientDisconnectCause.toShortAgentDriverMessage(): String = when (this) {
    AssistantClientDisconnectCause.NeverConnected -> "Not connected"
    AssistantClientDisconnectCause.ClosedByClient -> "Switched off"
    AssistantClientDisconnectCause.ClosedByServer -> "Server closed the connection"
    AssistantClientDisconnectCause.SessionInactive -> "Session timed out"
    AssistantClientDisconnectCause.ServerStopping -> "Server is stopping"

    is AssistantClientDisconnectCause.ConnectionLost -> closeCode
        ?.let { code -> "Connection lost ($code)" }
        ?: "Connection lost"

    is AssistantClientDisconnectCause.OpenFailed -> failure.toShortAgentDriverMessage()
}

/**
 * Whether this is worth putting in front of the user as an error.
 *
 * Two causes are not: nobody has connected yet, and the user switched the assistant off. Both are
 * the connection being absent on purpose, not a fault to report.
 */
internal val AssistantClientDisconnectCause.isFault: Boolean
    get() = this != AssistantClientDisconnectCause.NeverConnected &&
        this != AssistantClientDisconnectCause.ClosedByClient

/**
 * A throwable with the whole chain behind it.
 *
 * `TransportFailure` alone says only that the socket did not open; what actually went wrong -- a
 * name that would not resolve, a handshake the server refused, a certificate the device would not
 * trust -- is in the exception it wraps, sometimes several levels down.
 */
internal fun Throwable.describeChainForLog(): String = generateSequence(this, Throwable::cause)
    .take(n = 5)
    .joinToString(separator = " <- ") { link -> "${link::class.simpleName}: ${link.message}" }

/**
 * The first message in the chain that actually says something.
 *
 * The outermost exception is often the one carrying no message at all -- a wrapper whose whole
 * content is its type -- while the sentence naming the gateway, the host, or the certificate sits
 * one or two levels below it.
 */
private fun Throwable.firstDetailMessage(maxLength: Int): String? =
    generateSequence(this, Throwable::cause)
        .take(n = 5)
        .mapNotNull { link ->
            link.message
                ?.lineSequence()
                ?.firstOrNull()
                ?.trim()
                ?.ifBlank { null }
        }
        .firstOrNull()
        ?.take(n = maxLength)

/**
 * The fullest description a log line can carry, whatever the failure's type.
 *
 * Interpolating the throwable itself prints only its class and `message`, and the SDK's request
 * failures keep their reason in a typed field instead -- so a plain `"$throwable"` reduces every
 * one of them to "Request failed.".
 */
internal fun Throwable.describeAgentDriverFailureForLog(): String = when (this) {
    is KtorAssistantClientException -> describeForLog()
    else -> describeChainForLog()
}

internal fun KtorAssistantClientException.describeForLog(): String = when (this) {
    is KtorAssistantClientException.ConnectionClosed ->
        "${this::class.simpleName}(code=$closeCode)"

    // These three carry their whole content in a typed payload rather than in `message`, so
    // logging the message alone loses the only part worth reading.
    is KtorAssistantClientException.RequestFailed ->
        "${this::class.simpleName}(requestId=$requestId, error=$error)"

    is KtorAssistantClientException.ProtocolFailure ->
        "${this::class.simpleName}(error=$error)"

    is KtorAssistantClientException.InvalidMediaTransfer ->
        "${this::class.simpleName}(requestId=$requestId, reason=$reason)"

    else -> "${this::class.simpleName}: ${message ?: "no message"}"
}
