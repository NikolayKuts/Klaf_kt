# Klaf Server Status

This file is a temporary status log for the Klaf server implementation.
It should be removed or replaced by permanent documentation after the full implementation is finished.

## Current Phase

Local branch integration is complete (2026-09-29): `remote-storage-server`
(`2e00cee`) is merged into `mnemonic-voice-dictation`, following the requirements
checkpoint `101d11c`. No push or deployment is included.

- Combined textual conflicts, retaining both storage and voice functionality.
  Client Room schema is version 15. Explicit integration migrations handle both
  historical branch layouts without destructive fallback; a preserved voice-v12
  schema fixture supports regression coverage alongside the storage schemas.
- Added account-scoped Ignored Words, complete guest source/item/rule transfer
  and source-only retry support. The two new ownership tests were first run red,
  demonstrating missing transfer behavior before its implementation.
- Added device-session end signaling, server operation/upload cancellation,
  namespace rotation and late push/notification rejection on account transitions.
  Ordinary disconnections retain existing transcription behavior.
- Final combined Gradle run succeeded: 515 tests total, 514 passed, zero failures
  or errors, and one optional imported-baseline smoke test skipped. Module totals:
  data 136, domain 46, contract 11, presentation 73, DI 18, server 231 (one skipped).
  Android `compileDebugKotlin` and Desktop `compileKotlin` both succeeded.
  The run included all six test suites with `--continue --max-workers=2`.
- Verification follow-up: fixed an Android push map-receiver compile error,
  normalized the voice branch's missing source URL SQL default while preserving
  its rows, and added online session reconnection coverage. Account changes
  reconnect only a previously ready connection using the new session namespace;
  offline logout still makes no connection attempt. Both branch-layout migration
  tests and guest ownership/transfer regressions passed in the final run.
- Earlier verification found that the existing clean-close replay test
  exceeded its aggregate five-second window across cold client initialization
  plus reconnect. A focused run reproduced this: the request was sent and the
  connection closed, but the outer deadline cancelled it before replay. The test
  now connects initially before measuring replay, without changing production
  timeouts or relaxing the replay deadline. It passed in the final combined run.
- Follow-up review prevents ended requests from surfacing generic failures in
  the next account: request failures and transcription events are converted to
  cancellation when their originating namespace has changed. The online logout
  regression asserts cancellation specifically; logout during the initial
  handshake is also covered. All six client lifecycle regressions passed.
- Android application compilation exposed a module boundary issue in notification
  routing: the activity referenced the data-layer session interface directly.
  A domain-only `IClientSessionScope` now exposes the rotating namespace; common
  DI binds the existing session to it. The default-binding regression asserts
  both interfaces resolve to the same instance. DI tests and final Android
  compilation verified this fix. A JUnit return-type error in the new server
  logout test was also fixed; the final server suite passed.

- Compared clean branch heads `mnemonic-voice-dictation` (`a0b44a3`) and
  `remote-storage-server` (`2e00cee`), with common base `e6f6297`. A `merge-tree`
  simulation found 19 conflicted paths without changing branches or the index.
  Main overlaps: divergent Room schema versions, dependency wiring, server
  lifecycle, Android notification navigation and card-insights behavior.
- Vocabulary Source/voice data may start empty, but deck/card/sync data and
  immutable backups must remain protected.
- User chose cancellation on explicit logout rather than retaining unfinished
  feature results across account changes. Logout ends only the originating
  device's session/work; other devices of the account are unaffected. Offline
  logout is immediate and cancels local work; undeliverable cancellation may
  leave server work running, but its results are rejected locally. No queued
  logout is introduced. Integration now uses a rotating originating device
  session namespace for cancellation/results, with automated lifecycle coverage.
- User confirmed Vocabulary Sources and their associated persisted feature data
  are part of account-owned content. Sign-up must copy/verify/transfer them with
  decks/cards, preserving links and supporting source-only guest databases;
  sign-in still leaves the guest database untouched. Transfer implementation has
  been extended; recovery/isolation tests passed in the combined verification.
- User agreed to cross-device source-data synchronization as a separate phase
  immediately after the merge, with its own conflict rules/tests. The current
  storage protocol continues to contain deck/card operations only.
- No original backup, real account database, sibling project or live device was
  modified. Runtime/device testing remains separate from disposable test fixtures.

### Remaining Verification and Next Phase

- No merged physical-device acceptance, notification-tap/visual smoke check,
  real AI inference, or deployment was performed. Historical device results
  below belong to the pre-integration branches.
- The optional `ManualImportedDesktopSmokeTest` was not enabled against a real
  imported baseline. Automated migration/REST tests used disposable fixtures.
- iOS remains unverified with the known unresolved LoKdroid native publication
  issue; Android/Desktop success does not resolve it.
- Implement cross-device Vocabulary Source synchronization as the next separate
  phase, after agreeing its conflict rules and test scenarios. Source-to-card
  additions already use the deck/card outbox and respect review/sync edit gates.

### Prior Branch History (Before Integration)

The entries below describe validation and decisions on the mnemonic branch
before the current merge; their test totals are not combined-branch results.

MVP implementation is in local testing/review.

- Commit preparation (2026-09-28): user authorized a local snapshot of all
  current branch changes, including audited implementation/tests, both UI themes
  and privacy cleanup. Last full validation: 116 passing tests and Android/Desktop
  compilation; the final server theme follow-up also passes. The diagnosed
  LoKdroid iOS publication issue below is deliberately included as a known open
  issue, not fixed or hidden. No push, merge or sibling-project changes requested.

- IDE import dependency check (2026-09-28): `help` and all five KMP
  `resolveIdeDependencies` tasks complete, but their JSON reports contain 12
  unresolved iOS dependencies in data/di/presentation. LoKdroid `0.2.0-alpha`
  requests `LoKdroid:core-iosarm64:unspecified` and
  `LoKdroid:core-iossimulatorarm64:unspecified`. Android/Desktop build success
  does not validate these iOS artifacts. Latest IDE sync is recorded as
  successful despite these dependency errors; the earlier AgentDriver plugin
  lookup failure is not reproduced now. No library/configuration fix or
  publication was performed; correcting the sibling library publication needs
  separate authorization.

- Theme follow-up (2026-09-28): server/client colors centralized and both
  palettes covered by tests. Manual visual smoke checks remain; no live server
  or device was launched for this change.

## Uncommitted-worktree Audit (2026-09-28)

- All staged/unstaged/new work was reviewed relative to HEAD on
  `mnemonic-voice-dictation`. Existing staging/user edits were preserved; no
  commit, merge, deployment or paid-provider request was performed.
- Fixed binary/upload validation, start ordering, temporary-file lifetime,
  cancellation/queue isolation, pending registry completion and transcription TTL.
- Fixed connection cleanup lock inversion and covered cancellation before Ready,
  close before Ready and pending-request replay through a temporary loopback server.
- Push registration routes by originating client session; a desktop request can
  never fall back to the last registered phone. Delivery deduplication records
  confirmed success only and has bounded retention. Diagnostic errors avoid
  credential paths, push payloads and exception details sent to clients.
- Log capture is bounded per pending line (16 KiB) and history (5,000 entries);
  console output remains intact. Installation is idempotent; full-history
  autoscroll follows content, not the now-constant entry count.
- Actual SDK/plugin version is `0.10.12-SNAPSHOT`, published locally from the
  sibling checkout under existing user authorization without SDK source edits.
- Server 34 tests and contract 10 tests pass. The final cross-module run passes
  all 109 tests plus `:di:compileKotlinDesktop` and `:Android:compileDebugKotlin`.
- Remaining audio gaps and manual verification are in `audio-transcription-status.md`:
  selected-file duration display, disconnected failure push, real phone/provider/Linux
  checks. Killed-process reconciliation is explicitly deferred by the user for
  the current commit. These are not completed by a compile/test-only audit.

## Completed

- UI theme consistency (2026-09-28): server window/log styles now consume
  centralized light/dark `ServerColors`; initial selection follows the system,
  with an in-window toggle. Inputs, caret, selection, search borders/highlights,
  logo and actions follow the active palette. Styled log caching includes the
  palette, while entries and search text survive a toggle. No dependency on
  the client presentation module was added.
- Seven new palette/style regressions cover light/dark selection, every log
  part, unchanged text/search ranges, readable contrast, source badge colors,
  disabled tokens and direct palette initialization. The full cross-module run
  passed 116 tests plus Android and Desktop DI compilation. See the source
  feature status for client colors and previews. No commit was performed.
- Final commit privacy/hygiene check (2026-09-28): removed tracking for 15
  local/generated files without deleting them, added a neutral presentation
  config template, sanitized documentation/test examples, and checked both
  current text and the index. Server 34 tests pass after fixture sanitization.
  Details and the old-history/staging caveats are in `audio-transcription-status.md`.
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
- Bumped AgentDriver SDK version to `0.10.10-SNAPSHOT` in the sibling `AgentDriver` checkout.
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
- Bumped AgentDriver to `0.10.12-SNAPSHOT` for the progressive speech-to-text
  client API and configuration-cache-safe local config tasks. Published the
  server/client SDK sets to Maven Local, updated the Klaf version catalog, and
  verified `:klaf-server:compileKotlin --refresh-dependencies --no-configuration-cache`.
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
- AgentDriver's external plugin and JVM/root metadata were published locally
  under the user's explicit authorization; no SDK source was modified by this audit.
- `:klaf-server` currently compiles against locally published `0.10.12-SNAPSHOT`.
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

## Review Findings Rechecked

- Connection cleanup lock inversion and clean-close pending-request hangs are
  fixed and covered by loopback lifecycle regression tests.
- Mnemonic text/image still share an internal session, now with both text and
  image contracts in its developer instructions. Alternating real provider turns
  were not tested here; the old text-only-instruction finding is not proof of a
  current defect. This pre-existing policy was not changed by the audit.
- AgentDriver docs were updated for the new `baseInstructions` / `developerInstructions` API
  in the main architecture notes and Ralph planning docs.

## Next

- Address the audio implementation gaps listed in `audio-transcription-status.md`
  without including the user-deferred killed-process recovery in the current scope.
- Run manual end-to-end checks:
  - set Klaf server bind host to the computer LAN/hotspot IP;
  - set client `SecretConstants.KlafServer.HOST` to the same IP;
  - start `:klaf-server`;
  - open the app and request Word Insights from the screen;
  - generate mnemonic association text;
  - generate mnemonic image.
- Keep AgentDriver naming only in `:klaf-server` internals and documentation when referring to the actual SDK/assistant bridge.
