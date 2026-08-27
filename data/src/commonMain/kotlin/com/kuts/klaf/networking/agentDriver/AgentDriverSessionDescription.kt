package com.kuts.klaf.networking.agentDriver

import org.agentdriver.project.domain.model.AssistantCapability
import org.agentdriver.project.ktorclient.external.AssistantClientEndpoint
import org.agentdriver.project.ktorclient.external.AssistantClientSession
import org.agentdriver.project.protocol.ProtocolSpecification
import org.agentdriver.project.protocol.SessionCapabilityDetails
import org.agentdriver.project.protocol.StructuredOutputSupport

/** The capabilities Klaf actually asks a session for, checked against what it advertises. */
private val capabilitiesUsedByKlaf = listOf(
    AssistantCapability.TextGeneration,
    AssistantCapability.ImageGeneration,
)

/**
 * Everything the SDK knows about an open session, as one readable block.
 *
 * A session says on opening which provider and model answer for it and exactly what it can do, and
 * none of that appears anywhere else: a request refused for a capability the session never
 * advertised fails with the same typed error as one the provider simply could not fulfil. Printed
 * once per connection, this turns that guess into a lookup.
 */
internal fun AssistantClientSession.describeForLog(
    endpoint: AssistantClientEndpoint,
): String = buildString {
    val scheme = if (endpoint.secure) "wss" else "ws"

    appendLine("AgentDriver session opened")
    appendLine("  endpoint: $scheme://${endpoint.host}:${endpoint.port}")
    appendLine(
        "  protocol: v${ProtocolSpecification.VERSION} " +
            "(${ProtocolSpecification.REQUIRED_SUBPROTOCOL})",
    )
    appendLine("  provider: $provider")
    appendLine("  model: $modelId")
    appendLine("  reconnect grace period: ${reconnectGracePeriodSeconds}s")
    appendLine("  capabilities: ${capabilities.describeCapabilities()}")
    appendLine("  required by Klaf: ${capabilities.describeRequiredCapabilities()}")
    append(capabilityDetails.describeForLog())
}.trimEnd()

private fun Set<AssistantCapability>.describeCapabilities(): String {
    if (isEmpty()) return "none advertised"

    return map(AssistantCapability::wireName).sorted().joinToString(separator = ", ")
}

private fun Set<AssistantCapability>.describeRequiredCapabilities(): String =
    capabilitiesUsedByKlaf.joinToString(separator = ", ") { capability ->
        val availability = if (capability in this) "yes" else "MISSING"

        "${capability.wireName}=$availability"
    }

private fun SessionCapabilityDetails.describeForLog(): String = buildString {
    textGeneration?.let { details ->
        appendLine("  text generation:")
        appendLine("    structured output: ${details.structuredOutput.describe()}")
    }

    imageGeneration?.let { details ->
        appendLine("  image generation:")
        appendLine("    output formats: ${details.outputFormats.joinToString(separator = ", ")}")
        appendLine("    size support: ${details.sizeSupport}")
    }

    textToSpeech?.let { details ->
        appendLine("  text to speech:")
        appendLine("    output formats: ${details.outputFormats.joinToString(separator = ", ")}")
        appendLine(
            "    voices: ${details.voices.joinToString(separator = ", ") { voice -> "${voice.id}" }}",
        )
    }

    speechToText?.let { details ->
        appendLine("  speech to text:")
        appendLine(
            "    accepted formats: " +
                details.acceptedInputFormats.joinToString(separator = ", "),
        )
        appendLine("    maximum input: ${details.maximumInputBytes} bytes")
        appendLine("    language hint supported: ${details.languageHintSupported}")
    }
}.ifEmpty { "  capability details: none reported" }

private fun StructuredOutputSupport.describe(): String = when (this) {
    StructuredOutputSupport.Unsupported -> "unsupported"
    is StructuredOutputSupport.JsonSchema -> "JSON schema, up to $maximumSchemaBytes bytes"
}
