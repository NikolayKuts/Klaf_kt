# Mnemonic Generation Lifecycle

This document describes the current text and image generation flow in Klaf.

## Shared KMP Boundary

`IMnemonicGenerationBackgroundManager` is declared in `domain/commonMain`.
`CardManagementViewModel` starts a handle before it launches a mnemonic request
and finishes that handle with `Succeeded`, `Failed`, or `Cancelled` when the
request job ends.

Android provides `AndroidMnemonicGenerationBackgroundManager` in
`data/androidMain`. Desktop and iOS bind
`NoOpMnemonicGenerationBackgroundManager`, so shared presentation code does not
depend on Android services or notifications. This is an interface-based KMP
boundary, not an `expect`/`actual` declaration.

## Android Flow

1. Starting text or image generation registers an active operation and starts
   `MnemonicGenerationForegroundService`.
2. One foreground service and one ongoing notification are shared by all active
   mnemonic operations. The service stops only after the final handle finishes.
3. The presentation layer checks the shared connection state. For Android and
   Desktop, this state now represents the Klaf Server WebSocket connection.
4. The repository sends a Klaf Server protocol command over the shared
   `KlafServerSession`:
   - `mnemonic.association.generate` for mnemonic text;
   - `mnemonic.image.generate` for mnemonic image bytes.
5. Klaf Server owns prompt construction, response-schema selection, assistant
   parsing, and validation. The Android app sends business data, not full
   AgentDriver prompts.
6. Inside Klaf Server, the mnemonic feature lazily starts an internal
   AgentDriver/Codex session. Session-level Klaf/mnemonic instructions are sent
   through AgentDriver/Codex `thread/start`; each request sends only
   request-specific word/comment/selection data.
7. The result updates the existing view-model state. If Klaf is not visible, a
   success or failure notification is also shown.
8. Tapping that result notification removes it and opens the matching card
   creation or editing mnemonic screen.

User cancellation cancels the view-model job. The current Klaf Server protocol
does not yet expose a per-request cancel command. Finishing the final generation
handle stops the foreground service. Cancelled work does not show a result
notification.

## Android Permissions

`FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` are install-time
permissions. `POST_NOTIFICATIONS` is requested through the existing runtime
notification permission flow on supported Android versions. No wake-lock
permission is used.

## Limits

- The foreground service improves process priority but cannot prevent every
  vendor-specific process freeze or operating-system kill.
- WebSocket continuity is currently required for receiving the final Klaf
  Server response. If the app process dies, the waiting UI job is lost. If the
  Klaf Server process dies, the internal AgentDriver session and in-flight
  feature request are lost.
- The drawer readiness reflects the Klaf Server WebSocket state. It becomes
  ready only after the client receives `server.ready`.
- Streaming text is not resumable because replay of already delivered chunks is
  not defined.
- Navigating in a way that clears the card-management view model cancels its
  active generation. This flow is background-safe, not durable scheduled work.
- The current Klaf Server implementation keeps mnemonic text and mnemonic image
  in one internal Codex session. This is convenient for MVP, but text-only
  developer rules can conflict with image generation rules; splitting them into
  separate sessions is a known follow-up.

## Diagnostics

Useful Android log messages include:

- `Klaf Server connection opening` when the app tries to open the WebSocket.
- `Klaf Server ready` when the app receives the server readiness message.
- `Klaf Server mnemonic association request failed` for client-side transport or
  protocol failure.
- `Klaf Server mnemonic association received` when mnemonic text returns.
- `Klaf Server mnemonic image received` when image bytes return.
- `Mnemonic foreground service ...` for service start, stop, and destruction.
- `Mnemonic diagnostics: generation ...` for process, power, and network state.

Useful Klaf Server console logs include:

- `Mnemonic association request received/completed`.
- `Mnemonic image request received/completed`.
- `[mnemonic AgentDriver server] state=...`.
- `[mnemonic AgentDriver server] sessions active=...`.
- `[mnemonic AgentDriver client] [Connected] provider=... model=...`.
- `[mnemonic provider] ...` for provider diagnostics.

The server application can correlate the same request ID in its request logs and
provider diagnostics. Secrets and future authentication tokens must never be
logged.

## Verification

Run shared request-lifecycle tests and build the Android app:

```powershell
.\gradlew.bat :data:compileReleaseKotlinAndroid :di:compileReleaseKotlinAndroid
```

The final device check should start text and image generation, lock the screen,
wait for completion, unlock it, and verify both state restoration and result
notification routing.
