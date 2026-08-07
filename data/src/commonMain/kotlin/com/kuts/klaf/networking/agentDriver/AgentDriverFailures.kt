package com.kuts.klaf.networking.agentDriver

import org.agentdriver.project.ktorclient.external.AssistantClientDisconnectCause
import org.agentdriver.project.ktorclient.external.KtorAssistantClientException

private const val MAX_MESSAGE_LENGTH = 80
private const val MAX_DETAIL_LENGTH = 60

/** A short, human-readable reason for the UI, taken from the failure's own type. */
internal fun Throwable.toShortAgentDriverMessage(): String = when (this) {
    is KtorAssistantClientException.NotConnected -> "Not connected"
    is KtorAssistantClientException.ConnectionClosed -> "Connection closed"
    is KtorAssistantClientException.ServerStopping -> "Server is stopping"
    is KtorAssistantClientException.SessionStartFailed -> "Session unavailable"
    is KtorAssistantClientException.UnsupportedRequestOption -> "Unsupported by this server"
    is KtorAssistantClientException.RequestCancelled -> "Cancelled"
    is KtorAssistantClientException.RequestFailed -> "Generation failed"

    // The only type that says nothing on its own. "Connection failed" is true of a name that would
    // not resolve, a gateway answering 502, and a certificate the device would not trust alike, and
    // those need different things done about them -- so the detail comes along.
    is KtorAssistantClientException.TransportFailure ->
        firstDetailMessage(maxLength = MAX_DETAIL_LENGTH)
            ?.let { detail -> "Connection failed: $detail" }
            ?: "Connection failed"

    else -> firstDetailMessage(maxLength = MAX_MESSAGE_LENGTH) ?: "Request failed"
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
