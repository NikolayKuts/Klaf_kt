# Audio Transcription Status

## Current Focus

- Storage/voice integration implements explicit account-transition cancellation:
  cancel active local transcription and clear retained results, rotate the device
  session namespace, signal cancellation only over an existing connection, and
  reject late results/notification taps. Ordinary disconnect/reconnect retention
  remains unchanged. Combined tests and Android/Desktop compilation passed;
  validation details are tracked in `klaf-server-status.md`;
  no merged physical-device or real-inference check is claimed.

- Theme consistency follow-up (2026-09-28) is implemented and tested: server log
  UI and source-detail components consume central light/dark palettes. The
  pre-integration full-suite total was 116 passing tests, including seven new
  theme regressions;
  details are in server and transcript-vocabulary status documents. This does
  not change the explicit killed-process recovery deferral below.
- The 2026-09-28 audit of staged/unstaged/new changes on
  `mnemonic-voice-dictation` is complete; remaining implementation/manual gates
  below are not claimed as finished.
- Explicitly deferred by the user (2026-09-28): killed-process/notification
  recovery is not required for the current commit. Request
  IDs and originating client namespaces are runtime-only; an FCM tap carries a
  source ID but does not restore a request ID or fetch its retained response.
  Same-process navigation and reconnect are distinct, implemented paths.
- Also not implemented: selected-file duration display and disconnected failure
  push. Do not treat the historical "Completed" headings below as proof of these.
- Manual Android picker/notification/background checks, real Whisper inference,
  large/long-file validation and CPU-only Linux benchmarking were not run in this
  audit. No deployment, paid-provider call or working-data/backup edit.
- The actual local AgentDriver checkout and Klaf catalog use
  `0.10.12-SNAPSHOT`. Its external Gradle plugin, JVM and root metadata
  publications were built/published to Maven local under the existing user
  authorization. AgentDriver source was not changed.

## Final Commit Privacy Check (2026-09-28)

- Scanned 662 current text files and separately scanned the Git index for
  personal paths, email addresses, non-loopback IPv4 addresses, private keys,
  provider keys, JWTs, resource URLs, and hard-coded secret/account/device IDs.
  Remaining matches were Kotlin labels, synthetic email fixtures, and loopback
  or wildcard-bind defaults. This is an automated source check, not a guarantee
  about old Git history or embedded metadata in existing media.
- Removed 15 previously tracked local/generated files from the index: Gradle
  and Kotlin caches, JVM diagnostics, and the presentation local configuration.
  Every file was preserved on disk and verified ignored. No tracked ignored
  files or tracked cache/config/database candidates remain.
- Added a neutral presentation configuration template and fresh-checkout
  instructions. Replaced one machine-specific documentation path and two
  IP-bearing test fixture lines with neutral values.
- The privacy fixes and explicit killed-process deferral were staged first,
  preserving unrelated staging during the audit. The complete current snapshot
  now includes all implementation edits and new theme files/tests under the
  user's subsequent explicit authorization to commit the branch changes.
- Re-ran `:klaf-server:test`: 34 tests, zero failures/errors. Both working-tree
  and index whitespace checks pass. No commit or history rewrite was performed;
  values previously committed are not removed from historical commits.

## Audit Fixes And Regression Coverage (2026-09-28)

- Strict binary request-ID/UTF-8 validation; exact declared upload size,
  oversize-before-write rejection, incomplete/duplicate-start handling and
  temporary-file cleanup.
- Preserve start/chunk/completion ordering. Cancel queued work without resetting
  another client's active recognition; reset canceled active recognition while
  owning its slot. Fake recognition tests cover timestamps, M4A format,
  missing terminal results and disconnected incomplete uploads.
- Handshake cleanup no longer waits for a reader while holding its lifecycle
  lock. Connection-local readiness and upload ownership prevent stale-reader
  cleanup from touching a newer connection. Terminal results use suspending
  delivery rather than a lossy progress send.
- Resolve pending registry waiters even when its scope is already canceled;
  cancel all waiters on stop; transcription-only result retention is 15 minutes.
  Unexpected errors sent to clients do not expose exception/provider details.
- Process-owned transcription coordinator survives screen disposal and retains
  the draft result for 15 minutes; explicit Cancel still cancels. A successful
  explicit Save clears only the corresponding completed operation. Results over
  the source text limit preserve existing text and show failure.
- Push tokens are indexed by the originating client namespace, not the last
  registered phone. Missing routing skips push. Duplicate notification calls
  wait for confirmed delivery; failures allow retry, with bounded deduplication
  retention. Push diagnostics no longer print payloads or credential paths.
- Desktop picker uses the current callback and disposes its dialog; an unknown
  Android file size is not invented as one byte. Android TTS initialization is
  deferred to the main thread and ignores callbacks after shutdown.
- Source state/save races, empty new analyses and analysis results for changed
  text are covered by ViewModel tests. Temporary Room tests cover 8/9/10/11 -> 12,
  atomic rollback, ignored-rule uniqueness and preservation of current ADDED
  card/deck links.
- Foreign phrases normalize internal whitespace. Log-viewer capture caps a
  pending line at 16 KiB and history at 5,000 entries without truncating console
  output; capture installation is idempotent and autoscroll keeps working once
  history is full.
- Secret/credential/email-pattern and Git-ignore scans found no sensitive
  literals in the change set. No temporary databases, build outputs or local
  configuration files need to be added. Existing staging was preserved.

## Accepted Decisions

- First release accepts local WAV, MP3, and M4A audio files selected on Android.
- Inputs are limited to 60 minutes and 250 MB.
- First release recognizes English only.
- Linux production must work on CPU without requiring a GPU.
- Accuracy is preferred over shortest CPU execution time; the initial model
  candidate is quantized English Whisper `small.en`, pending Windows/Linux
  benchmarks.
- The first release does not identify or split speakers.
- A recognized transcript is inserted without rewriting wording, capitalization,
  or punctuation; only technical whitespace may be normalized, and the existing
  Cleaned mode remains a separate transformation.
- Initial Linux deployment launches Klaf Server as a regular terminal process,
  not a Docker container.
- FFmpeg is provisioned automatically by a pinned, checksum-verified Gradle
  task; a failed provisioning preflight gives an actionable manual-path
  fallback.
- Android uploads the selected file to Klaf Server; Klaf Server invokes
  AgentDriver.
- Completing transcription does not start vocabulary analysis automatically.
- File selection and transcription are separate explicit actions on an existing
  Vocabulary Source detail screen.
- A successful transcription replaces that source's current Original transcript
  text without confirmation or merging.
- The replacement remains unsaved until the user invokes the existing Save
  action.
- An active upload or recognition operation has an explicit cancellation action
  and cancellation leaves the source transcript unchanged.
- The first release displays `Uploading audio` and `Transcribing` phases, not a
  synthetic percentage.
- Transcription continues server-side through Android backgrounding or a
  WebSocket drop; FCM and reconcile recover its completed result.
- A foreground service accompanies upload and transcription while the process
  is alive, following the later Vocabulary Source implementation requirements.
  Once the server
  acknowledges full receipt of the file, subsequent Android backgrounding,
  screen locking, or connection loss does not cancel transcription; its result
  is retained until reconcile or timeout.
- A completed transcription result is retained for 15 minutes or until the
  client reconciles it; this TTL must not unintentionally alter other server
  operations.
- An upload interrupted before server acknowledgement is discarded and must be
  started again; offset-based upload resume is deferred.
- Uploaded source audio is temporary and deleted by the server on success,
  failure, or cancellation.
- A Klaf Server restart does not preserve active or unreconciled transcription
  work in the first release.
- A failed operation without an active client sends FCM to its source; that
  screen presents the actionable failure and permits a new attempt.
- Only one transcription can be active for a Vocabulary Source at a time.
- Klaf Server executes recognition for one source at a time and queues other
  accepted transcription operations.
- AgentDriver speech-to-text exposes optional timestamp output per request;
  it returns `SpeechToTextResult` with a full transcript and optional segments.
- The Vocabulary Source path sends `includeTimestamps = true`; timestamp segments
  are formatted as SRT, with plain full transcript fallback when absent.
- The selected file is displayed with name, size, duration, and a replace
  action before transcription starts.

## Validation

- Audit final command succeeded:
  `:domain:desktopTest :klaf-server-contract:jvmTest :data:desktopTest
  :presentation:desktopTest :klaf-server:test :di:compileKotlinDesktop
  :Android:compileDebugKotlin` (Gradle `--continue`, 2026-09-28).
- JUnit XML reports: domain 37, contract 10, data 7, presentation 21, server 34:
  **109 tests, zero failures/errors**. Room tests use bundled SQLite in temporary
  directories; session tests use temporary loopback WebSocket servers.
- Regression tests first reproduced invalid binary IDs/UTF-8, bad upload sizes,
  duplicate upload replacement, canceled registry scope, exception-detail leaks,
  source-state/save races, empty-draft fallback, inconsistent phrase normalization,
  stale ADDED-link overwrite and unbounded log capture before their fixes.
- `git diff --check HEAD` passed (Git reports CRLF normalization warnings only).
  Existing Gradle/Compose/Firebase deprecation warnings remain; iOS and real
  Android/Linux runtime verification were not performed.

- Added AgentDriver tests for the structured response, protocol serialization,
  client propagation, binary-transfer propagation, and suppression of segments
  when timestamps were not requested.
- Added focused JVM tests for local result assembly and for Codex-session
  capability advertisement/delegation without provider turns.
- `git diff --check` passed for the AgentDriver changes.
- JVM validation has been run directly in AgentDriver. The focused acknowledged-window client
  upload and STT server tests pass, as do `:sdk:domain:jvmTest`,
  `:sdk:protocol:jvmTest`, `:sdk:client:jvmTest`, and `:sdk:speech-to-text:jvmTest`.
  A full `:sdk:server:jvmTest` run executed 228 tests and recorded 15 failures across Claude
  response limits, process-environment isolation, preflight, storage, and legacy server-integration
  contracts. One was a stale direct-dispatch STT test harness; it was corrected and its focused
  test now passes. The remaining full server suite has not yet been rerun, so it is not yet a clean
  project-wide gate.
- Progressive speech-to-text reporting is covered by focused JVM tests for
  progress-model validation, protocol serialization, client event ordering, and
  server dispatcher forwarding. A fresh full `:sdk:client:jvmTest` and
  `:sdk:speech-to-text:jvmTest` run passed; `:sdk:domain:jvmTest`,
  `:sdk:protocol:jvmTest`, the targeted server forwarding test, and
  `:apps:androidApp:compileDebugKotlin` also passed after the API change.
- `:apps:androidApp:generateLocalClientConfig` and
  `:apps:androidApp:compileDebugKotlin` pass with
  `--configuration-cache --configuration-cache-problems=fail`; a repeated
  generator invocation reuses the stored configuration cache entry.
- The AgentDriver Android demo now uses the public progressive client SDK directly. It
  selects WAV/MP3/M4A documents through `OpenDocument`, waits for the server to
  advertise `SpeechToText`, calls `transcribeSpeechProgressively` with the
  current manual test setting `includeTimestamps = true`, renders upload and
  recognition progress, and then renders the returned transcript.
  This is intentionally independent of Klaf UI/background orchestration.
- The current manual-test configuration is `clientApp.connectionMode=lan`. The
  Android demo and server demo now read the gitignored `clientApp.lanServerHost` /
  `clientApp.lanServerPort` configuration. LAN mode binds the server to that
  IP, uses unencrypted WebSocket transport, and deliberately omits both
  Cloudflare and client-token authentication for this temporary local test.
- The speech-runtime installer task types were moved out of the Gradle script
  into `buildSrc` after Gradle rejected script-local task classes as non-static
  inner classes. Their file extraction now uses injected Gradle services rather
  than `Task.project`, and the already-installed checks run inside the task
  action so that Gradle configuration cache remains supported. This requires
  rerunning the server demo validation.
- The demo's stale, user-specific WSL Codex executable path was replaced with
  a `serverApp.codexExecutable` value generated from gitignored
  `local.properties`. The current WSL2 environment has `bubblewrap` and Codex
  installed; the SDK's isolated provider profile still needs its own Codex
  login before provider-backed capabilities can pass preflight.
- The Android STT smoke-test transcript field uses a fixed height rather than
  `Modifier.weight`, avoiding an Android Compose dependency-resolution error;
  Android demo compilation still needs rerunning by the project controller.
- Android and server-side STT diagnostics now log the non-sensitive request
  path: selected format, byte count, client WebSocket event/request ID, and
  server recognition phase with elapsed time. They intentionally exclude audio
  bytes, local file paths, and transcript text.
- The first LAN smoke test isolated an Android-only upload defect: Ktor's OkHttp
  bridge queued the whole 21.9 MB payload, crossed OkHttp's fixed 16 MiB send
  queue, and closed the socket with `1001` before the upload completed. The SDK
  now uses a 2 MiB acknowledged upload window: the client sends one window,
  waits until the server confirms the bytes were written, then sends the next.
  This keeps the queue bounded for files up to the 250 MB policy limit. The
  demo also prioritizes the selected filename extension over an inconsistent
  Android MIME type, so a `.m4a` file is sent as `Mp4` rather than `Mp3`.
- The first build of the acknowledged-window implementation exposed a Kotlin
  scoping error in the server receiver: a nested state data class tried to use
  an outer receiver property as a default argument. The window value is now
  supplied explicitly when that state is created; server and Android demo
  compilation still require controller validation.
- The first successful long-file upload reached local recognition. Its native
  Whisper runtime reported that input chunks must be strictly shorter than 30
  seconds, while the SDK had produced exactly 30-second chunks. The chunk size
  is now 29 seconds; it preserves sequential coverage of the decoded PCM while
  avoiding native truncation at that boundary. This needs a fresh server/demo
  run to validate receipt of the terminal `SpeechTranscribed` response.
- Runtime diagnostics now report FFmpeg start/completion with source duration
  and elapsed time, plus completion of each Whisper PCM chunk with an exact
  chunk-based percentage. They deliberately do not expose audio bytes, paths,
  or recognized text beyond the pre-existing aggregate diagnostics. A fresh
  server demo build is required to observe them.
- The long M4A validation reached all 47 Whisper chunks, but the generic
  session client-idle timer closed the active connection at ten minutes. That
  caused the client to fail the pending request and cancelled the operation
  before it could emit `SpeechTranscribed`; no transcript existed to render.
  The idle monitor now defers closure while a request is active and resets the
  idle period when its terminal delivery completes. A regression test covers
  that lifecycle. The Android demo also now remuxes the first 10% of an M4A
  into a temporary valid preview before upload, reducing this manual test from
  47 Whisper chunks to roughly 5. This is temporary demo-only behavior.
- Timestamp-enabled responses already traverse the Android request, protocol,
  and server recognizer without being suppressed. The demo previously rendered
  only `SpeechToTextResult.transcript`, hiding its returned `segments`; it now
  renders each segment with its coarse recognizer-chunk time range. This needs
  a fresh Android demo run to validate on-device.
- Review/refactoring tightened three non-functional boundaries: the downloaded
  FFmpeg and Whisper archives are now gitignored; generated Kotlin config
  values correctly escape Windows paths, quotes, dollar signs, and whitespace;
  and the configured STT result-length policy is enforced and regression-tested.
  The request completion hook now refreshes session activity before clearing
  the active-request flag, closing the idle-timeout race for long operations.
- The media receiver now rejects a declared audio size above its configured
  input limit before allocating a temporary transfer file; the focused JVM test
  verifies both the typed rejection and the absence of a created file.
- Review also aligned the default STT timeout with the recognizer's advertised
  60-minute input ceiling: it is now 45 minutes rather than 10. The prior
  10-minute value could cancel a valid long CPU transcription despite the
  inactivity monitor correctly keeping its session open.

## Klaf End-to-End Implementation Completed

- **Persistence & Migration:**
  - Added `url: String = ""` to `VocabularySource` and `RoomVocabularySource`.
  - Room database migrated from v11 to v12 (`MIGRATION_11_12`) across Android, Desktop, and iOS with schema exported.
- **Domain Layer:**
  - Added `SpeechToTextSegment`, `TranscriptTimestampFormatter` (deterministic SRT formatting), and unit tests (`TranscriptTimestampFormatterTest`).
  - Added `IVocabularySourceTranscriptionRepository` and `TranscribeVocabularySourceAudioUseCase`.
- **Protocol & Networking:**
  - Protocol version 7 with `AudioUploadFrame` & `AudioUploadFrameCodec` binary framing.
  - Streaming binary frames via `KlafServerSession` and `KlafServerVocabularySourceTranscriptionRepository`.
- **Server Service & STT Bridge:**
  - `VocabularySourceTranscriptionService` with bounded temp file storage, chunk progress emission, cancel cleanup, and global Whisper mutex serialization.
  - `SpeechToTextAgentSession` bridge to AgentDriver headless recognition.
  - Tested with `VocabularySourceTranscriptionServiceTest`.
- **UI & File Picker:**
  - Multiplatform `AudioFilePickerLauncher` (Android `OpenDocument` with persistable permission, Desktop `FileDialog`, iOS stub).
  - `VocabularySourceDetailViewModel` state management (`TranscriptionUiState`), replace/clear audio, progress tracking, and formatting with timestamped segments.
  - `VocabularySourceDetailScreen` audio section with progress bar, cancellation, replace/remove buttons, and URL input with clipboard copy.
  - Desktop & Android compilation verified cleanly; all domain, protocol, and server tests passed.

## Lifecycle Implementation History (Not Full Process-Death Recovery)

- **SDK version correction from the audit:** the earlier `0.10.14-SNAPSHOT`
  publication claim did not match disk. The current checkout/catalog and local
  Maven publication are `0.10.12-SNAPSHOT`; SDK source/tests were not reworked in
  this audit.
- **Phase 2 (Klaf Server Multi-User & Lifecycle Isolation):**
  - Added `sourceTitle` and `clientSessionId` to `VocabularySourceTranscribeStartRequest`.
  - Refactored `VocabularySourceTranscriptionService` to key active uploads/transcriptions by unique `requestId` and composite `SourceSessionKey(clientSessionId, sourceId)`.
  - Added stale-request superseding/cancellation: a new upload for the same source and client automatically aborts previous stale transcription work and cleans temporary files.
  - Added FCM push notification trigger in `KlafServer.kt` (`notifyVocabularySourceTranscriptionCompleted`) when transcription completes while the client WebSocket is disconnected.
  - Updated unit tests in `VocabularySourceTranscriptionServiceTest.kt` to verify multi-user isolation and stale-request preemption.
- **Phase 3 (Klaf Android Foreground Service & Reconnect Recovery):**
  - Added `IVocabularySourceTranscriptionBackgroundManager` domain contract and Android foreground service implementation (`VocabularySourceTranscriptionForegroundService`, `AndroidVocabularySourceTranscriptionBackgroundManager`).
  - Added notification channels and localized strings for progress, success, and failure notifications.
  - Implemented `VocabularySourceTranscriptionNotifier` handling foreground notification updates and completion/failure push notifications with pending intent navigation.
  - Handled `vocabulary_source_transcription_completed` push messages in `KlafFirebaseMessagingService`.
  - Background work is now owned by a singleton transcription coordinator;
    detail ViewModels observe it rather than canceling it on screen disposal.
    This recovers results only while the app process survives.
  - Added seamless WebSocket reconnect recovery in `KlafServerSession`: if WebSocket drops during the Whisper recognition phase (after upload), the request remains active, and upon reconnection (via `AndroidKlafServerForegroundReconnecter` on foreground return or backoff), `VocabularySourceTranscribeCompleteRequest(requestId)` is automatically resent to retrieve the completed result from `KlafServerOperationRegistry` without failing the client flow.
