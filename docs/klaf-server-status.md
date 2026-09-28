# Klaf Server Status

This file is a temporary status log for the Klaf server implementation.
It should be removed or replaced by permanent documentation after the full implementation is finished.

## Current Phase

MVP implementation is in local testing/review.

## Completed

- Created initial requirements document.
- Created initial status document.
- Captured the first high-level concept:
  - separate Gradle module;
  - Ktor server;
  - Koin DI;
  - AgentDriver SDK dependency from local Maven;
  - multiple feature-specific AgentDriver/Codex sessions;
  - SDK changes likely needed for `developerInstructions` support.
- Implemented AgentDriver SDK code changes for Codex thread-level instructions:
  - removed old shared `systemInstruction` behavior config;
  - added Codex-only nullable `baseInstructions` and `developerInstructions`;
  - pass those fields through Codex app-server `thread/start`;
  - keep `turn/start` prompts plain, without a repeated system-instruction wrapper;
  - updated SDK unit/fake-server tests at code level.
- Ran AgentDriver reference scans for old `systemInstruction` usage.
- Ran `git diff --check` in AgentDriver; only line-ending warnings were reported.
- Added initial Klaf server Gradle modules:
  - `:klaf-server-contract` for shared Kotlinx Serialization DTOs;
  - `:klaf-server` for the JVM/Compose Desktop server app.
- Added the MVP Klaf WebSocket contract:
  - `server.ready`;
  - `wordInsights.generate`;
  - `wordInsights.generated`;
  - structured `request.error`.
- Added the initial Klaf server app:
  - minimal Compose Desktop window;
  - automatic Ktor WebSocket server startup at `/ws`;
  - Koin module;
  - LoKdroid console logging;
  - local ignored `SecretConstants.kt` plus committed example file.
- Added the first Word Insights server path:
  - starts a feature-specific internal AgentDriver/Codex session lazily;
  - sends shared Klaf context through `baseInstructions`;
  - sends Word Insights rules through `developerInstructions`;
  - sends only a word-specific prompt and response schema per request;
  - recreates the feature session and retries once after request/session failure.
- Verified `:klaf-server-contract:compileKotlinMetadata` successfully.
- Fixed the Klaf server Ktor engine type after the first compile attempt.
- Bumped AgentDriver SDK version to `0.10.10-SNAPSHOT` in `C:\Users\<user>\StudioProjects\AgentDriver`.
- Updated Klaf version catalog to consume AgentDriver `0.10.10-SNAPSHOT`.
- Published AgentDriver `0.10.10-SNAPSHOT` to Maven local with `publishSdkToMavenLocal`.
- Verified `:klaf-server:compileKotlin --refresh-dependencies --no-configuration-cache` successfully.
- Added Klaf client-side WebSocket session in `data` for the new Klaf server protocol.
- Added Android/Desktop Ktor WebSocket client factories.
- Switched Android/Desktop `IWordMeaningInsightsRepository` DI binding from direct KlafServer to `KlafServerWordMeaningInsightsRepository`.
- Added local client config fields under ignored `data` `SecretConstants.KlafServer`.
- Verified:
  - `:data:compileKotlinMetadata`;
  - `:data:compileKotlinDesktop :di:compileKotlinDesktop`;
  - `:data:compileReleaseKotlinAndroid :di:compileReleaseKotlinAndroid`.
- Added a temporary Room downgrade migration from schema version `8` to `7` after stashing
  the Vocabulary Source work. This drops only the Vocabulary Source tables created by the
  stashed feature and preserves the existing deck/card tables.
- Extended the Klaf WebSocket contract for mnemonic generation:
  - `mnemonic.association.generate`;
  - `mnemonic.association.generated`;
  - `mnemonic.image.generate`;
  - `mnemonic.image.generated`.
- Added the Klaf server mnemonic path:
  - one feature-specific internal AgentDriver/Codex session for mnemonic text and image;
  - mnemonic text/image rules sent through session-level `developerInstructions`;
  - per-turn prompts contain only request-specific word/comment/selection data;
  - mnemonic text response parsing and validation preserved from the old client implementation;
  - mnemonic image bytes returned to the app as Base64 over WebSocket.
- Added client-side Klaf server repositories:
  - `KlafServerMnemonicAssociationRepository`;
  - `KlafServerMnemonicImageRepository`;
  - `KlafServerConnectionManager`.
- Switched Android/Desktop mnemonic text and image DI bindings from direct KlafServer to Klaf server repositories.
- Removed old client-side direct KlafServer networking/repository/session implementation from `data`.
- Removed the `agentdriver-client` dependency from the `data` module. The AgentDriver SDK is now used by `:klaf-server`, not by the Android/Desktop client feature path.
- Verified:
  - `:klaf-server-contract:compileKotlinMetadata`;
  - `:klaf-server:compileKotlin`;
  - `:data:compileKotlinMetadata`;
  - `:data:compileReleaseKotlinAndroid`;
  - `:di:compileReleaseKotlinAndroid`;
  - `:data:compileKotlinDesktop`;
  - `:di:compileKotlinDesktop`.
- Added internal AgentDriver session diagnostics for Klaf Server:
  - server state subscription;
  - server metrics subscription;
  - provider diagnostics subscription;
  - client connection state subscription;
  - connected session details: provider, model, reasoning effort, capabilities, capability details, reconnect grace period;
  - explicit logs for preflight, internal server start, and client connect.
- Verified `:klaf-server:compileKotlin`.
- Switched Klaf Server internal Codex sessions to explicit `gpt-5.5` model with `low` reasoning effort.
- Verified `:klaf-server:compileKotlin`.
- Fixed Klaf client connection status after server migration:
  - `KlafServerConnectionManager` no longer reports `Ready` by default;
  - connection state now comes from the shared `KlafServerSession`;
  - app startup triggers an initial Klaf Server connection attempt;
  - `Ready` is emitted only after the WebSocket receives `server.ready`;
  - failed connection attempts emit `Error`, so the drawer can show Retry instead of false readiness.
- Verified:
  - `:data:compileKotlinMetadata`;
  - `:data:compileReleaseKotlinAndroid`;
  - `:di:compileReleaseKotlinAndroid`;
  - `:data:compileKotlinDesktop`;
  - `:di:compileKotlinDesktop`.
- Completed a first code-review pass over the Klaf Server migration and documented the main follow-up risks.
- Updated project documentation and changelog for the current Klaf Server migration state.
- Renamed public client/domain/UI/data connection concepts from AgentDriver naming to Klaf Server naming where the app now talks to the Klaf Server wrapper instead of directly to AgentDriver.
- Removed the stale direct AgentDriver client secret/config block from the local ignored `data` `SecretConstants.kt`, leaving only the Klaf Server host/port config for the client path.
- Corrected docs/changelog wording so AgentDriver remains the name of the internal SDK/assistant bridge, while Klaf Server remains the public app/server protocol.
- Updated the Klaf AgentDriver SDK dependency target to `0.10.11-SNAPSHOT` for the WSL2 data-root readiness fix in AgentDriver preflight. This requires publishing the matching AgentDriver snapshot to Maven Local before rebuilding `:klaf-server`.
- Verified after rename:
  - `:domain:compileKotlinMetadata`;
  - `:data:compileKotlinMetadata`;
  - `:presentation:compileKotlinMetadata`;
  - `:di:compileKotlinMetadata`;
  - `:data:compileReleaseKotlinAndroid`;
  - `:di:compileReleaseKotlinAndroid`;
  - `:presentation:compileReleaseKotlinAndroid`;
  - `:data:compileKotlinDesktop`;
  - `:di:compileKotlinDesktop`;
  - `:presentation:compileKotlinDesktop`.

## Decisions

- The previous Klaf Vocabulary Source work is saved in Git stash:
  `WIP vocabulary source feature before Clav server app`.
- The Klaf server application will be implemented as a separate Gradle module inside the existing `Klaf_kt` project.
- The Gradle module name will be `:klaf-server`.
- The server is intended to run on a remote computer/server machine, with local localhost debugging also supported.
- The MVP transport direction is WebSocket-style communication, similar to the current AgentDriver client/server usage.
- Any local secrets/config files must be ignored by Git.
- The Klaf server will expose a Klaf-specific WebSocket protocol and use AgentDriver internally.
- The Klaf app will use one persistent WebSocket connection to the Klaf server for the whole app, with request IDs for multiplexing operations.
- Feature-specific AgentDriver/Codex sessions will be created lazily on first use for MVP.
- Failed feature sessions should be recreated automatically, with one retry for the failed request.
- Word Insights was the first end-to-end feature.
- Word Insights will reuse the existing client-side prompt/response contract at first.
- No fallback to the old direct AgentDriver path should be added.
- The Klaf server will use a local ignored `SecretConstants.kt` file for MVP secrets/configuration, plus a committed example/template file.
- Klaf app to Klaf server authentication can be temporarily disabled for first local/LAN MVP testing.
- Security/authentication must be implemented before committing or using the server outside the trusted local development setup.
- AgentDriver SDK should be modified first to support session-level Codex instructions before building `:klaf-server`.
- A small shared Klaf app context may use Codex `baseInstructions` if useful; feature-specific rules should use `developerInstructions`.
- AgentDriver SDK behavior config should expose explicit `baseInstructions` and `developerInstructions` fields.
- AgentDriver SDK should remove `systemInstruction` and stop wrapping every prompt with `<system_instruction>`.
- Codex provider should send `baseInstructions` and `developerInstructions` through `thread/start`.
- Codex instruction fields should be optional nullable config; unset values should be omitted from `thread/start`.
- Keep the current `Assistant(AssistantType.Codex) { ... }` construction style while exposing provider-specific builder options.
- Add AgentDriver SDK tests for Codex `thread/start` instruction fields.
- Publish AgentDriver SDK to local Maven after SDK changes; `:klaf-server` should depend on that artifact.
- `:klaf-server` will be JVM-only.
- `:klaf-server` will include a Compose Multiplatform desktop UI window.
- Server startup will be automatic on desktop app launch.
- Initial UI will be minimal: icon/text only, without an in-app log panel.
- Logs should go to console/output and show server/session state plus request activity.
- Server logs should use the existing Klaf app logging library/style, not raw `println`.
- `:klaf-server-contract` will be Kotlin Multiplatform.
- WebSocket messages will use JSON with Kotlinx Serialization.
- A shared `:klaf-server-contract` module will hold serializable protocol DTOs.
- `:klaf-server-contract` will be a Kotlin Multiplatform module.
- Protocol DTOs will stay separate from pure domain entities.
- The Klaf protocol will send command names plus business inputs, not full AgentDriver prompts.
- The first WebSocket command will be `wordInsights.generate`.
- MVP text-generation features will preserve the current one-request/one-final-response behavior.
- Feature failures will be returned as structured per-request WebSocket errors.
- Logs should use short summaries by default; raw assistant responses only in explicit debug mode.
- Internal AgentDriver session logs should include observable state changes and connected session
  details, not only "started" messages.
- All current Klaf Server internal Codex sessions should use one shared default model/effort config:
  `gpt-5.5` and `low`.
- Do not add an in-app log panel for MVP.
- The current implementation/debugging phase will run the Klaf server on this same development computer and should support physical Android device testing over local LAN/hotspot.
- Do not create Git commits during implementation.
- Server bind host and port will be configurable through the local ignored server `SecretConstants.kt`.
- The MVP WebSocket path will be `/ws`.
- MVP local/LAN testing will use plain non-TLS `ws://`.
- AgentDriver Gradle tests/publish were not run yet because the AgentDriver repository contract says not to run Gradle from Codex.
- `:klaf-server` previously compiled against AgentDriver `0.10.10-SNAPSHOT`; the current target is
  `0.10.11-SNAPSHOT` and needs the matching local Maven publish before the next rebuild.
- Android/Desktop Word Insights and mnemonic text/image now use the Klaf server path.
- Direct AgentDriver client/server interaction has been removed from the Android/Desktop app feature path.
- The domain/UI connection concept has been renamed from AgentDriver naming to Klaf Server naming:
  `KlafServerConnectionState`, `IKlafServerConnectionManager`,
  `ObserveKlafServerConnectionStateUseCase`, and `RetryKlafServerConnectionUseCase`.
- Drawer readiness must reflect the Klaf Server WebSocket state, not internal AgentDriver readiness.
  The client is considered ready only after the server sends `server.ready`.
- The Room `8 -> 7` migration exists only because the local development device may already have
  opened the stashed Vocabulary Source schema. When Vocabulary Source is restored, schema version
  `8` and the normal `7 -> 8` migration should come back with that feature.
- The current implementation keeps mnemonic text and mnemonic image in one internal Codex session,
  but this should be revisited because text rules and image rules can conflict.
- No Git commits should be created by Codex during this implementation phase.

## Review Findings To Fix

- `KlafServerSession.disconnect()` can deadlock by holding `lifecycleMutex` while waiting for
  `readerJob.cancelAndJoin()`. The reader job also needs `lifecycleMutex` in `finally`.
- `KlafServerSession.ensureConnected()` has the same cleanup pattern in its failure path.
- Pending Klaf Server client requests are failed only when `readMessages()` catches an exception.
  If the WebSocket incoming loop ends normally, a pending `response.await()` can hang forever.
- Mnemonic text and image generation share one `MnemonicAgentSession`. The session-level text
  developer rules include JSON-only and no-tool constraints, which can conflict with image
  generation.
- AgentDriver docs were updated for the new `baseInstructions` / `developerInstructions` API
  in the main architecture notes and Ralph planning docs.

## Next

- Fix review findings above before treating the migration as stable.
- Add focused tests for `KlafServerSession` connection lifecycle:
  - timeout before `server.ready`;
  - clean close during a pending request;
  - disconnect during reader shutdown;
  - retry after failed initial connection.
- Run manual end-to-end checks:
  - set Klaf server bind host to the computer LAN/hotspot IP;
  - set client `SecretConstants.KlafServer.HOST` to the same IP;
  - start `:klaf-server`;
  - open the app and request Word Insights from the screen;
  - generate mnemonic association text;
  - generate mnemonic image.
- Keep AgentDriver naming only in `:klaf-server` internals and documentation when referring to the actual SDK/assistant bridge.
