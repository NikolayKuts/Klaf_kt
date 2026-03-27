package com.kuts.klaf.networking.wordInsights

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.klaf.common.WordMeaningInsightsPayload
import com.kuts.klaf.common.toDomainEntity
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal class CodexObserverHotSession private constructor(
    private val session: WebSocketSession,
    private val model: String?,
) {

    companion object {

        private const val REQUEST_TIMEOUT_MS = 90_000L
        private const val CLIENT_NAME = "klaf-kt"
        private const val CLIENT_TITLE = "Klaf"
        private const val CLIENT_VERSION = "1.0.0"
        private const val APPROVAL_POLICY = "never"
        private const val SANDBOX_MODE = "read-only"
        private const val REASONING_EFFORT = "low"
        private const val SERVICE_NAME = "klaf-word-meaning-insights"
        private const val METHOD_INITIALIZE = "initialize"
        private const val METHOD_INITIALIZED = "initialized"
        private const val METHOD_ACCOUNT_READ = "account/read"
        private const val METHOD_THREAD_START = "thread/start"
        private const val METHOD_TURN_START = "turn/start"
        private const val METHOD_AGENT_MESSAGE_DELTA = "item/agentMessage/delta"
        private const val METHOD_ITEM_COMPLETED = "item/completed"
        private const val METHOD_TURN_COMPLETED = "turn/completed"
        private const val JSON_RPC_METHOD_NOT_FOUND = -32601
        private val UNSUPPORTED_OUTPUT_SCHEMA_KEYWORDS = setOf("uniqueItems")

        suspend fun connect(
            client: HttpClient,
            serverUrl: String,
            model: String?,
        ): CodexObserverHotSession {
            val normalizedServerUrl = serverUrl.trim()
            require(value = normalizedServerUrl.isNotBlank()) {
                "Codex App server URL is not configured."
            }

            val webSocketSession = client.webSocketSession(urlString = normalizedServerUrl)
            val hotSession = CodexObserverHotSession(
                session = webSocketSession,
                model = model?.trim()?.ifBlank { null },
            )

            try {
                hotSession.prepare()
                return hotSession
            } catch (throwable: Throwable) {
                hotSession.close(reasonMessage = "codex observer session initialization failed")
                throw throwable
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val queuedNotifications = ArrayDeque<JsonObject>()
    private var nextRequestId = 1L
    private var threadId: String? = null

    suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights = withTimeout(
        timeMillis = REQUEST_TIMEOUT_MS,
    ) {
        val requestedWord = word.trim()
        require(value = requestedWord.isNotBlank()) { "Word must not be blank." }

        val preparedThreadId = threadId
            ?: throw IllegalStateException("Codex Observer session is not initialized.")
        val prompt = WordMeaningInsightsPromptFactory.build(word = requestedWord)
        val rawResponse = startWordMeaningTurn(
            threadId = preparedThreadId,
            userPrompt = prompt.userPrompt,
            outputSchema = json.parseToJsonElement(prompt.responseJsonSchema)
                .removeUnsupportedOutputSchemaKeywords(),
        )

        val payload = json.decodeFromString(
            deserializer = WordMeaningInsightsPayload.serializer(),
            string = rawResponse.unwrapMarkdownCodeFence(),
        )
        payload.throwIfInvalidForRequestedWord(requestedWord = requestedWord)

        val parsedInsights = payload.toDomainEntity()
        require(value = parsedInsights.isValidForContract(expectedWord = requestedWord)) {
            "Codex App server returned payload that does not match the expected contract."
        }

        parsedInsights
    }

    suspend fun close(reasonMessage: String) {
        session.close(
            reason = CloseReason(
                code = CloseReason.Codes.NORMAL,
                message = reasonMessage,
            ),
        )
    }

    private suspend fun prepare() = withTimeout(timeMillis = REQUEST_TIMEOUT_MS) {
        initialize()
        ensureAuthenticatedAccount()
        threadId = startThread(
            model = model,
            systemInstruction = WordMeaningInsightsContract.systemInstruction,
            developerInstructions = buildDeveloperInstructions(),
        )
    }

    private suspend fun sendMessage(payload: JsonObject) {
        session.send(Frame.Text(payload.toString()))
    }

    private suspend fun readMessage(): JsonObject {
        while (true) {
            when (val frame = session.incoming.receive()) {
                is Frame.Text -> {
                    return json.parseToJsonElement(frame.readText()).jsonObject
                }

                is Frame.Close -> {
                    throw IllegalArgumentException(
                        "Codex App server websocket closed unexpectedly."
                    )
                }

                else -> Unit
            }
        }
    }

    private suspend fun pollMessage(): JsonObject {
        return queuedNotifications.removeFirstOrNull() ?: readMessage()
    }

    private suspend fun rejectServerRequest(message: JsonObject) {
        val method = message.requireString(name = "method")
        sendMessage(
            payload = buildJsonObject {
                put(key = "id", element = message.requireElement(name = "id"))
                put(
                    key = "error",
                    element = buildJsonObject {
                        put(key = "code", value = JSON_RPC_METHOD_NOT_FOUND)
                        put(
                            key = "message",
                            value = "Unsupported server request for non-interactive word insights flow: $method",
                        )
                    },
                )
            },
        )
    }

    private suspend fun sendNotification(method: String) {
        sendMessage(
            payload = buildJsonObject {
                put(key = "method", value = method)
            },
        )
    }

    private suspend fun sendRequest(
        method: String,
        params: JsonElement? = null,
    ): JsonObject {
        val requestId = nextRequestId++
        sendMessage(
            payload = buildJsonObject {
                put(key = "id", value = requestId)
                put(key = "method", value = method)
                params?.let { put(key = "params", element = it) }
            },
        )

        while (true) {
            val message = readMessage()

            when {
                message.isResponseFor(requestId = requestId) -> {
                    return message.requireObject(name = "result")
                }

                message.isErrorFor(requestId = requestId) -> {
                    val error = message.requireObject(name = "error")
                    val code = error.requireInt(name = "code")
                    val errorMessage = error.requireString(name = "message")
                    throw IllegalArgumentException(
                        "Codex App server request failed. method=$method, code=$code, message=$errorMessage"
                    )
                }

                message.isNotification() -> queuedNotifications.addLast(message)
                message.isRequest() -> rejectServerRequest(message = message)
            }
        }
    }

    private suspend fun initialize() {
        sendRequest(
            method = METHOD_INITIALIZE,
            params = buildJsonObject {
                put(
                    key = "clientInfo",
                    element = buildJsonObject {
                        put(key = "name", value = CLIENT_NAME)
                        put(key = "title", value = CLIENT_TITLE)
                        put(key = "version", value = CLIENT_VERSION)
                    },
                )
                put(key = "capabilities", element = JsonNull)
            },
        )
        sendNotification(method = METHOD_INITIALIZED)
    }

    private suspend fun ensureAuthenticatedAccount() {
        val result = sendRequest(
            method = METHOD_ACCOUNT_READ,
            params = buildJsonObject {
                put(key = "refreshToken", value = true)
            },
        )

        val account = result["account"]
        val hasAccount = !account.isMissingOrNull()

        require(value = hasAccount) {
            "Codex App server is not authenticated. Complete Codex login on the remote server before requesting word insights."
        }
    }

    private suspend fun startThread(
        model: String?,
        systemInstruction: String,
        developerInstructions: String,
    ): String {
        val result = sendRequest(
            method = METHOD_THREAD_START,
            params = buildJsonObject {
                model?.let { put(key = "model", value = it) }
                put(key = "approvalPolicy", value = APPROVAL_POLICY)
                put(key = "sandbox", value = SANDBOX_MODE)
                put(key = "serviceName", value = SERVICE_NAME)
                put(key = "baseInstructions", value = systemInstruction)
                put(key = "developerInstructions", value = developerInstructions)
                put(key = "ephemeral", value = true)
                put(key = "experimentalRawEvents", value = false)
                put(key = "persistExtendedHistory", value = false)
            },
        )

        return result.requireObject(name = "thread").requireString(name = "id")
    }

    private suspend fun awaitTurnCompletion(
        threadId: String,
        turnId: String,
    ): String {
        val collectedMessages = linkedMapOf<String, StringBuilder>()

        while (true) {
            val message = pollMessage()

            when {
                message.isRequest() -> rejectServerRequest(message = message)
                message.isNotification(method = METHOD_AGENT_MESSAGE_DELTA) -> {
                    val params = message.requireObject(name = "params")
                    if (!params.matchesTurn(threadId = threadId, turnId = turnId)) continue

                    val itemId = params.requireString(name = "itemId")
                    val delta = params.requireString(name = "delta")
                    collectedMessages.getOrPut(key = itemId) { StringBuilder() }
                        .append(delta)
                }

                message.isNotification(method = METHOD_ITEM_COMPLETED) -> {
                    val params = message.requireObject(name = "params")
                    if (!params.matchesTurn(threadId = threadId, turnId = turnId)) continue

                    val item = params.requireObject(name = "item")
                    if (item.requireString(name = "type") != "agentMessage") continue

                    val itemId = item.requireString(name = "id")
                    val text = item.requireString(name = "text")
                    collectedMessages[itemId] = StringBuilder(text)
                }

                message.isNotification(method = METHOD_TURN_COMPLETED) -> {
                    val params = message.requireObject(name = "params")
                    if (params.requireString(name = "threadId") != threadId) continue

                    val turn = params.requireObject(name = "turn")
                    if (turn.requireString(name = "id") != turnId) continue

                    when (turn.requireString(name = "status")) {
                        "completed" -> {
                            val finalMessage = collectedMessages.values
                                .joinToString(separator = "") { messageBuffer ->
                                    messageBuffer.toString()
                                }
                                .trim()

                            require(value = finalMessage.isNotBlank()) {
                                "Codex App server returned an empty response."
                            }

                            return finalMessage
                        }

                        "interrupted" -> {
                            throw IllegalArgumentException(
                                "Codex App server interrupted the turn before completion."
                            )
                        }

                        "failed" -> {
                            val error = turn.requireObject(name = "error")
                            val messageText = error.requireString(name = "message")
                            val details = error.optionalString(name = "additionalDetails")
                            val detailsSuffix = details
                                ?.let { value -> " Details: $value" }
                                .orEmpty()
                            throw IllegalArgumentException(
                                "Codex App server failed to generate insights. $messageText$detailsSuffix"
                            )
                        }

                        else -> {
                            throw IllegalArgumentException(
                                "Codex App server returned unexpected turn status."
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun startWordMeaningTurn(
        threadId: String,
        userPrompt: String,
        outputSchema: JsonElement,
    ): String {
        val turnResult = sendRequest(
            method = METHOD_TURN_START,
            params = buildJsonObject {
                put(key = "threadId", value = threadId)
                put(
                    key = "input",
                    element = buildJsonArray {
                        add(
                            element = buildJsonObject {
                                put(key = "type", value = "text")
                                put(key = "text", value = userPrompt)
                                put(key = "text_elements", element = buildJsonArray { })
                            },
                        )
                    },
                )
                put(key = "effort", value = REASONING_EFFORT)
                put(key = "outputSchema", element = outputSchema)
            },
        )

        val turnId = turnResult.requireObject(name = "turn").requireString(name = "id")
        return awaitTurnCompletion(
            threadId = threadId,
            turnId = turnId,
        )
    }

    private fun buildDeveloperInstructions(): String = """
        Never use tools, shell commands, file reads, web search, MCP, plugins, or any external actions.
        Answer directly from the model.
        Return only the final JSON object that satisfies the provided output schema.
    """.trimIndent()

    private fun JsonObject.isNotification(method: String): Boolean {
        return isNotification() && optionalString(name = "method") == method
    }

    private fun JsonObject.isNotification(): Boolean {
        return containsKey(key = "method")
            && !containsKey(key = "id")
            && !containsKey(key = "result")
            && !containsKey(key = "error")
    }

    private fun JsonObject.isRequest(): Boolean {
        return containsKey(key = "method")
            && containsKey(key = "id")
            && !containsKey(key = "result")
            && !containsKey(key = "error")
    }

    private fun JsonObject.isResponseFor(requestId: Long): Boolean {
        return containsKey(key = "result")
            && this["id"]?.jsonPrimitive?.longOrNull == requestId
    }

    private fun JsonObject.isErrorFor(requestId: Long): Boolean {
        return containsKey(key = "error")
            && this["id"]?.jsonPrimitive?.longOrNull == requestId
    }

    private fun JsonObject.requireObject(name: String): JsonObject {
        return requireElement(name = name).jsonObject
    }

    private fun JsonObject.requireString(name: String): String {
        return requireElement(name = name).jsonPrimitive.content
    }

    private fun JsonObject.optionalString(name: String): String? {
        return this[name]?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.requireInt(name: String): Int {
        return requireElement(name = name).jsonPrimitive.intOrNull
            ?: throw IllegalArgumentException("Expected integer field '$name' in Codex response.")
    }

    private fun JsonObject.requireElement(name: String): JsonElement {
        return this[name]
            ?: throw IllegalArgumentException("Missing field '$name' in Codex response.")
    }

    private fun JsonElement?.isMissingOrNull(): Boolean {
        return this == null || this is JsonPrimitive && contentOrNull == null
    }

    private fun JsonElement.removeUnsupportedOutputSchemaKeywords(): JsonElement {
        return when (this) {
            is JsonObject -> JsonObject(
                content = entries
                    .asSequence()
                    .filterNot { (key, _) -> key in UNSUPPORTED_OUTPUT_SCHEMA_KEYWORDS }
                    .associate { (key, value) ->
                        key to value.removeUnsupportedOutputSchemaKeywords()
                    },
            )

            is JsonArray -> JsonArray(
                content = map { element -> element.removeUnsupportedOutputSchemaKeywords() },
            )

            else -> this
        }
    }

    private fun JsonObject.matchesTurn(
        threadId: String,
        turnId: String,
    ): Boolean {
        return requireString(name = "threadId") == threadId
            && requireString(name = "turnId") == turnId
    }
}
