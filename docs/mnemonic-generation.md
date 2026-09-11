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
3. `AgentDriverSession` records the request as active before sending it through
   the client-side Agent Driver SDK.
4. When Klaf enters background, an idle socket is closed deliberately. A socket
   with an active request is left to normal SDK reconnect handling.
5. If Android or the network drops the WebSocket, the client SDK resumes the
   logical session and keeps the same one-shot request ID. The server-side Agent
   Driver SDK continues provider work and buffers the terminal result.
6. The result updates the existing view-model state. If Klaf is not visible, a
   success or failure notification is also shown.
7. Tapping that result notification removes it and opens the matching card
   creation or editing mnemonic screen.

User cancellation cancels the view-model job. The client SDK sends
`CancelRequest` immediately when connected, or after resume when cancellation
happens during a disconnect. Finishing the final generation handle stops the
foreground service. Cancelled work does not show a result notification.

## Android Permissions

`FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` are install-time
permissions. `POST_NOTIFICATIONS` is requested through the existing runtime
notification permission flow on supported Android versions. No wake-lock
permission is used.

## Limits

- The foreground service improves process priority but cannot prevent every
  vendor-specific process freeze or operating-system kill.
- WebSocket continuity is not required for one-shot text and image operations;
  SDK session resume provides recovery after a transport drop.
- Recovery state is in memory. A Klaf process death loses the waiting UI job,
  and a server process restart loses the retained logical session and result.
- The server advertises a reconnect grace period, currently 10 minutes by
  default. Recovery after that deadline is not guaranteed.
- Streaming text is not resumable because replay of already delivered chunks is
  not defined.
- Navigating in a way that clears the card-management view model cancels its
  active generation. This flow is background-safe, not durable scheduled work.

## Diagnostics

Useful Android log messages include:

- `Agent Driver SDK connection monitor failed` when the client detects loss.
- `Agent Driver SDK reconnect starting` when automatic recovery begins.
- `AgentDriver connecting: resuming the previous session` for resume handshake.
- `Agent Driver SDK request send started` and `response received` with request
  IDs for correlation.
- `Mnemonic foreground service ...` for service start, stop, and destruction.
- `Mnemonic diagnostics: generation ...` for process, power, and network state.

The server application can correlate the same request ID in its request and
provider diagnostics. Resume tokens must never be logged.

## Verification

Run shared request-lifecycle tests and build the Android app:

```powershell
.\gradlew.bat :data:testDebugUnitTest :Android:assembleDebug
```

The final device check should start text and image generation, lock the screen,
wait for completion, unlock it, and verify both state restoration and result
notification routing.
