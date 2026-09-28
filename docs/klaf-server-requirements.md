# Klaf Server Requirements

This file is a temporary working document for the Klaf server implementation.
It should be removed or replaced by permanent documentation after the full implementation is finished.

## UI Theme Consistency (2026-09-28)

- Uncommitted server log UI and client screens/dialogs/components must support
  both light and dark themes. Keep actual color values in centralized theme
  configuration, not in screen files or log text builders.
- Server log severity, search highlights, inputs and actions must use the active
  palette. Changing theme must rebuild styled log text without losing searches
  or log entries. The server uses a module-local theme configuration rather than
  depending on the client presentation module.

## Branch Integration Decisions (2026-09-28)

- Merge preparation targets `mnemonic-voice-dictation`, bringing in
  `remote-storage-server`. Review conflicts and clarify material behavior before
  performing the merge.
- Existing Vocabulary Source, subtitle and voice-feature data need not be
  preserved or migrated; these feature tables may start empty. This permission
  does not cover decks, cards, account/synchronization data or original backups.
- Explicit logout must cancel unfinished feature work from the user context
  being left, rather than retain it for later delivery. When the server receives
  the logout/cancellation signal, it must stop the affected work and release its
  associated resources/sessions without stopping unrelated work. Late results
  must not be applied or opened in the next account or guest context.
- Logout ends the originating device's session and its unfinished work only.
  Other devices signed into the same account must keep their sessions and work.
  Cleanup must target the session being left, not a later session on that device.
- Logout remains an immediate local action, including when offline: cancel local
  work from the session being left and reject its later results/notification
  navigation. When connected, send server cancellation for that device session.
  Without connectivity, server processing may finish because cancellation cannot
  be delivered; its result must not be adopted after logout. Do not wait for
  server confirmation or create a queued logout for later guest-mode delivery.
- Vocabulary Sources and their associated persisted feature data are an integral
  part of the user's data, scoped to the selected account/guest database rather
  than a store shared across users. This includes saved text, source items and
  the feature's Ignored Words records.
- Sign-up transfers all such guest feature data along with decks/cards into the
  new account's local database, including when no guest decks/cards exist.
  Preserve source/item/deck/card links and verify the complete copied data before
  removing transferred guest records. Retry/interruption must not lose data or
  duplicate it. Sign-in to an existing account still leaves guest data untouched.
- Permission to reset pre-merge Vocabulary Source/voice data is a one-time
  integration permission, not permission to drop newly created feature data
  during later sign-up/account changes.
- Cross-device synchronization of Vocabulary Sources and associated feature data
  is a separate follow-up immediately after the merge, not part of integration.
  Integration provides account ownership and local sign-up transfer; the storage
  protocol continues to synchronize decks/cards only. Source synchronization
  needs its own agreed conflict rules and tests before implementation.
- Shared server-wide AgentDriver sessions are not automatically user-owned;
  the cleanup design must distinguish shared infrastructure from affected work.

## Goal

Create a separate Gradle module for a Klaf server application inside the existing `Klaf_kt` Gradle project.

The server will be a wrapper around the AgentDriver SDK. It will use Ktor for HTTP/server transport and Koin for dependency injection, following the style already used in the Klaf Kotlin Multiplatform project.

## Current Concept

The server should create and manage several active AgentDriver/Codex sessions. Each session represents one functional area and should receive its own developer instructions.

The server should be similar in spirit to the AgentDriver demo app: a wrapper process around the AgentDriver SDK, but with Klaf-specific behavior and feature-specific session instructions.

Initial planned sessions and current MVP status:

- Word Insights session: implemented.
- Mnemonic text/image session: implemented as one shared session for now.
- Vocabulary source / subtitle analysis session: planned later.
- Standalone image generation session: planned later if needed outside mnemonic image generation.

Review note: keeping mnemonic text and mnemonic image in one session creates a possible instruction conflict because text-generation rules include JSON/tool prohibitions while image generation needs the image tool path. Splitting mnemonic text and mnemonic image into separate feature sessions is the preferred follow-up if image generation behaves inconsistently.

## AgentDriver SDK Changes

AgentDriver SDK needs support for passing session-level developer instructions into Codex app-server `thread/start`, instead of wrapping the same instruction into every `turn/start` prompt.

This has been implemented in the local AgentDriver working tree. Klaf Server currently targets `0.10.12-SNAPSHOT`, published to Maven Local for the local integration build.

The current SDK shape is:

- Codex config exposes nullable `baseInstructions`.
- Codex config exposes nullable `developerInstructions`.
- Both fields are sent in Codex `thread/start` when configured.
- `turn/start` receives only the actual request prompt and optional response schema.
- The old shared `systemInstruction` wrapper was removed from the request prompt path.

Klaf Server uses `baseInstructions` for a short shared Klaf application context and `developerInstructions` for feature-specific behavior.

## Transport

The Klaf app should communicate with the Klaf server through WebSocket-style communication, similar to the current AgentDriver client/server usage. REST should not be the MVP direction.

Long-running operations, such as subtitle analysis, should therefore be handled through the WebSocket protocol rather than long blocking HTTP requests.

The Klaf server should expose its own Klaf-specific WebSocket protocol. The Android app should send business-level commands such as vocabulary analysis or word insights, not raw AgentDriver prompts. AgentDriver remains an internal dependency of the server.

The Klaf app should keep one persistent WebSocket connection to the Klaf server for the whole app. Multiple feature requests should be multiplexed over this connection using request IDs.

For the MVP, the Klaf server can own feature-specific AgentDriver `Assistant` instances and start their AgentDriver servers on internal loopback ports. The Klaf server then talks to those internal sessions through the AgentDriver client SDK. This keeps the public Android-to-Klaf protocol separate from AgentDriver, while still using the SDK's current public server/client API.

WebSocket messages should use JSON with Kotlinx Serialization.

For MVP local/LAN testing, the WebSocket endpoint should be plain non-TLS WebSocket at `/ws`, for example `ws://<LAN_IP>:<PORT>/ws`. Secure WebSocket can be added later when needed.

Protocol DTOs should be separate from pure domain entities. A shared `:klaf-server-contract` module should define serializable WebSocket request/response/event DTOs used by both the Android app and `:klaf-server`.

The protocol should preserve existing feature business inputs where possible. The main migration change is that the Android app should stop sending full AgentDriver prompts; instead, it should send a command name plus the same business inputs as before. The Klaf server owns prompt construction, session instructions, response schema selection, parsing, and validation.

The first command should be `wordInsights.generate`, with the same business input as today: `word`.
Mnemonic MVP commands should be:

- `mnemonic.association.generate`, with `word`, optional `comment`, and already-used sound anchors.
- `mnemonic.image.generate`, with selected mnemonic data and optional image comment.

For MVP, preserve the current non-streaming behavior for text generation features. If a feature currently receives one complete text/result response, the Klaf server protocol should also return one complete response message rather than streaming partial text.

Feature request failures should be returned as structured protocol error messages tied to the original request ID. Invalid assistant JSON, invalid contract data, request validation failures, and KlafServer failures should not close the WebSocket connection unless the connection itself is broken or unauthorized.

Logging should use short summaries by default. Full raw assistant responses should only be logged in an explicit debug mode because future requests may contain user text or subtitle content.

Connection readiness is defined by the public Klaf Server WebSocket connection, not by internal AgentDriver sessions. The client is ready only after it receives `server.ready` from the server.

## Secrets

If the implementation uses a secrets file or local sensitive configuration, it must be added to `.gitignore` and must not be committed to the repository.

For MVP, the Klaf server should use a local `SecretConstants.kt` file, similar to the existing `data` module pattern. The real file must be ignored by Git, and a committed example/template file should show the expected structure without real secrets.

For first local/LAN MVP testing, Klaf app to Klaf server authentication can be temporarily disabled. Security/authentication must be implemented before committing or using the server outside this trusted local development setup.

## Open Questions

- Production security model before using the server outside trusted local development.
- Whether mnemonic text and mnemonic image should remain in one shared AgentDriver/Codex session or be split into separate sessions to avoid instruction conflicts.
- Whether the client WebSocket session should include explicit request timeouts for very long operations or keep relying on caller-level cancellation/loading state.

## Known Risks From Review

- `KlafServerSession.disconnect()` and the connection-failure cleanup path can deadlock because they cancel/join the reader job while holding the same lifecycle mutex that the reader job needs in `finally`.
- Pending client requests can wait forever if the WebSocket incoming loop ends normally without throwing, because pending responses are failed only in the exception path.
- The shared mnemonic session currently combines text-only JSON/tool restrictions and image-generation rules in one `developerInstructions` block.
- AgentDriver documentation has been updated for the new `baseInstructions` /
  `developerInstructions` API in the main architecture notes and Ralph planning docs.

## Decisions

- The Klaf server application will be implemented as a separate Gradle module inside the existing `Klaf_kt` project.
- The Gradle module name will be `:klaf-server`.
- The normal target environment is a remote computer/server machine. Development flow may be: commit locally, push, pull on server machine, run the Klaf server there.
- For the current implementation/debugging phase, the Klaf server will be run on this same development computer and should be reachable from a physical Android device over the local LAN/hotspot network.
- Later, the target environment can be a remote computer/server machine. Development flow may be: commit locally, push, pull on server machine, run the Klaf server there.
- Local debugging should use a configurable bind host. For physical Android device testing over local Wi-Fi/LAN/hotspot, bind to the computer's LAN IP. For same-computer testing, `127.0.0.1` can be used.
- The MVP transport direction is WebSocket-style communication, not REST.
- The Klaf server will expose a Klaf-specific WebSocket protocol, not the raw AgentDriver protocol.
- The Klaf server may use internal loopback AgentDriver server/client sessions per feature because the AgentDriver SDK public API currently exposes `Assistant.startServer(...)`, not direct in-process text generation.
- The Klaf app will use one persistent WebSocket connection to the Klaf server for the whole app.
- WebSocket requests/results should use request IDs so multiple operations can share the same connection.
- Feature-specific AgentDriver/Codex sessions should be created lazily on first use for MVP, not eagerly on server startup.
- If a feature session fails or the Codex thread dies, the Klaf server should recreate that session and retry the same request once. If the retry fails, the server should return an error to the app.
- The first end-to-end feature should be Word Insights.
- Word Insights should reuse the existing prompt/response contract from the current Klaf client code for the first server implementation.
- The Android app should move to the Klaf server path without fallback to the old direct AgentDriver connection. Do not add fallback flags or parallel old/new routing.
- The Klaf server should use a local ignored `SecretConstants.kt` file for MVP secrets/configuration, plus a committed example/template file.
- Klaf app to Klaf server authentication can be temporarily disabled for first local/LAN MVP testing.
- Security/authentication must be implemented before committing or using the server outside the trusted local development setup.
- AgentDriver SDK should be modified first to support session-level Codex instructions before building `:klaf-server`.
- A small shared Klaf app context may be passed through Codex `baseInstructions` if useful, while feature-specific rules should live in `developerInstructions`.
- AgentDriver SDK should expose explicit behavior configuration fields for `baseInstructions` and `developerInstructions`, matching Codex app-server concepts.
- AgentDriver SDK should remove the existing `systemInstruction` behavior field and stop wrapping every prompt with `<system_instruction>`.
- Codex provider should pass `baseInstructions` and `developerInstructions` in `thread/start`, while `turn/start` should receive only the actual request prompt plus any per-turn `outputSchema`.
- Codex `baseInstructions` and `developerInstructions` should be optional nullable fields. If not explicitly set, AgentDriver should omit the corresponding fields from Codex app-server `thread/start`.
- AgentDriver should keep the current construction style, for example `Assistant(AssistantType.Codex) { ... }`, but expose provider-specific builder options inside the typed configuration lambda.
- AgentDriver SDK tests should verify that Codex `thread/start` includes `baseInstructions` and `developerInstructions` when configured and omits them when null.
- After AgentDriver SDK changes, publish the SDK to local Maven and make `:klaf-server` consume it as a normal dependency.
- `:klaf-server` should be a JVM-only Gradle module because it runs as a server process on a computer/server.
- `:klaf-server` should include a Compose Multiplatform desktop UI window. It should not be only a headless console process.
- The Klaf server should start automatically when the desktop app launches.
- The initial UI should be minimal: essentially an app/server icon and text indicating that this is the Klaf server. It should not include advanced controls or visible logs for MVP.
- Logs should be written to the console/output, not displayed in the Compose UI.
- Console logs should show useful server/session state and request activity, similar to the AgentDriver demo app.
- Console logs should use the same logging library/style already used by the Klaf app, not raw `println`.
- `:klaf-server-contract` should remain Kotlin Multiplatform for shared client/server DTOs.
- WebSocket messages should use JSON with Kotlinx Serialization.
- Create a shared `:klaf-server-contract` module for serializable WebSocket DTOs.
- `:klaf-server-contract` should be a Kotlin Multiplatform module, with shared DTOs in `commonMain`.
- Protocol DTOs should be separate from pure domain entities, with mapping between DTOs and domain models where needed.
- The Android app should not send full AgentDriver prompts through the Klaf server protocol.
- Feature commands should keep the same business inputs where possible.
- The first WebSocket command will be `wordInsights.generate`.
- Mnemonic text and mnemonic image generation will also use Klaf server WebSocket commands.
- MVP text-generation features should use one request and one complete final response, not streaming partial text.
- Feature failures should return structured per-request error messages over WebSocket.
- Logs should use short summaries by default; full raw assistant responses should only be logged in explicit debug mode.
- Logs should be console/output logs only; do not add an in-app log panel for MVP.
- Server logs should use the existing Klaf app logging library/style, not raw `println`.
- Do not create Git commits during implementation. The user wants to inspect diffs manually.
- Server bind host and port should be configurable through the local ignored server `SecretConstants.kt`.
- The MVP WebSocket path will be `/ws`.
- MVP local/LAN testing will use plain `ws://`, not TLS `wss://`.
- Android/Desktop client feature code should not keep fallback or old direct AgentDriver server paths after migration.
- AgentDriver SDK should be used inside `:klaf-server`; the app should talk to the Klaf-specific server protocol.
