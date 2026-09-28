# Vocabulary Source Audio Transcription - Implementation Requirements

**Status:** planned only. This document authorizes no production implementation
by itself; begin implementation only after the user explicitly asks for it.

This is a self-contained handoff for the next agent. It describes the next
feature for the Klaf `VocabularySource` detail screen and the required Klaf
Server bridge to AgentDriver speech-to-text. Read this document together with
`audio-transcription-requirements.md`; when the documents disagree about
timestamps for the Vocabulary Source UI, this document wins.

## Goal

An existing Vocabulary Source can store an optional source URL and can receive
a timestamped transcript from a user-selected local audio file. The transcript
is placed into the existing editable Original transcript field; it is not
saved or analyzed automatically.

The Android app uploads the selected audio to Klaf Server. Klaf Server owns the
AgentDriver client call. AgentDriver owns FFmpeg, Whisper, and all generic audio
recognition behavior.

## Existing Code Map

- Detail UI: `presentation/src/commonMain/kotlin/com/kuts/klaf/vocabularySource/VocabularySourceDetailScreen.kt`.
- Detail state and mutations: `VocabularySourceDetailViewModel.kt` in the same
  package. Its current `VocabularySourceDetailState` holds `title`,
  `description`, `rawText`, and `cleanText`.
- Persistent source domain type:
  `domain/src/commonMain/kotlin/com/kuts/domain/entities/VocabularySource.kt`.
- Room source type and mapping:
  `data/src/commonMain/kotlin/com/kuts/klaf/room/entities/RoomVocabularySource.kt`
  and `data/src/commonMain/kotlin/com/kuts/klaf/room/Mapper.kt`.
- Current Klaf client/server WebSocket contract:
  `klaf-server-contract/.../KlafServerProtocol.kt`.
- Current app-side WebSocket session and pending-response routing:
  `data/src/commonMain/kotlin/com/kuts/klaf/networking/klafServer/KlafServerSession.kt`.
- Existing Vocabulary Source analysis bridge:
  `KlafServerVocabularySourceAnalysisRepository.kt`; use it as the style
  reference, not as a reason to route audio through the analysis feature.
- Current server feature/session:
  `klaf-server/.../features/vocabularysource/VocabularySourceService.kt` and
  `VocabularySourceAgentSession.kt`.
- AgentDriver public client API from Maven Local version `0.10.12-SNAPSHOT`:
  `IAssistantClient.transcribeSpeechProgressively(request)` returns
  `Flow<SpeechToTextEvent>`.

## Persisted Source URL

1. Add an optional `url: String = ""` field to `VocabularySource`. It is a
   separate user-facing property, never an alias for `description`.
2. Add the corresponding non-null Room column with an empty-string default,
   then update all Room database schema versions and migrations for every
   supported platform. Update Room/domain mappers and repository insert/update
   paths.
3. Preserve URL values in all source create, observe, update, and save flows.
   Existing persisted rows migrate to an empty URL.
4. Extend `VocabularySourceDetailState` with the editable URL and include it in
   unsaved-change/savable-content calculation. The existing Save action is the
   only action that persists it.
5. Add a dedicated URL text field to the detail screen. It is placed with the
   source metadata fields and must not replace, relabel, or overload
   Description.
6. Add a Copy icon button attached to that URL field. It copies the current
   editable URL value, including an unsaved value. Disable or hide the button
   when the URL is blank. Use the existing platform clipboard mechanism if one
   exists; otherwise introduce a small common clipboard interface with Android
   and Desktop implementations. Do not make the copy action save the source.
7. URL validation beyond ordinary text storage is out of scope for this pass:
   do not block saving because a string is not a syntactically valid URL.

## Audio Picker And Android Permissions

1. Add a platform abstraction from the common detail UI/ViewModel to choose an
   audio document. Implement Android first with the system document picker
   (`OpenDocument` or equivalent), allowing WAV, MP3, and M4A.
2. Selecting a file only changes transient screen state. It does **not** start
   transcription and does not save anything to Room.
3. Display selected filename, byte size when available, and a Replace action.
   The selected URI/file is transient and must not become a persisted
   Vocabulary Source field.
4. Android must not request broad storage permission, `MANAGE_EXTERNAL_STORAGE`,
   or a download permission. The system picker supplies a read URI grant.
5. Because the upload may be handed to a foreground service after the Activity
   returns, retain the picker URI permission with
   `ContentResolver.takePersistableUriPermission(...)` when the provider grants
   it. Handle providers that do not provide a persistable grant by keeping the
   operation in the current process and showing a clear failure if the URI can
   no longer be opened.
6. Desktop picker behavior is not required in this implementation unless the
   agent can reuse an existing platform picker abstraction without adding a
   separate feature. Do not fake a desktop implementation.

## UI State And User Flow

1. Add a separate `Transcribe audio` action. It is enabled only when an audio
   document is selected, the source exists, and no transcription is active for
   that source.
2. The screen must model at least these mutually exclusive operation states:
   `Idle`, `Uploading(uploadedBytes, totalBytes)`,
   `Transcribing(completedChunks, totalChunks)`, and `Failed(userMessage)`.
   A finished result returns the UI to `Idle` after applying the draft text.
3. Render truthful phase-specific progress:
   - Uploading may render acknowledged bytes and a byte percentage.
   - Transcribing renders completed Whisper chunks out of total chunks.
   - Do not invent a single combined `0..100` percent for the whole operation.
4. Show an explicit Cancel action during Uploading and Transcribing. It targets
   the current transcription request. Cancellation leaves the existing editable
   transcript unchanged.
5. A failure shows an actionable error in the existing event-message/error
   pattern, clears the active operation state, and leaves the selected file
   available for a retry when possible. It must not replace `rawText`.
6. On success, replace the current editable Original transcript. Mark the
   source as having unsaved changes and stale analysis using the existing
   `onRawTextChanged` behavior. Do not auto-save, do not auto-run Analyze, do
   not create or save Vocabulary Source items, and do not alter URL/Title/
   Description.

## Timestamped Transcript Format

1. Klaf Server must call AgentDriver with
   `AssistantClientSpeechToTextRequest(includeTimestamps = true)`.
2. The final AgentDriver result is
   `SpeechToTextResult(transcript, segments)`, where every
   `SpeechToTextSegment` has `startMillis`, `endMillis`, and `text`.
3. Do not discard `segments`. Convert them on the app side, or in a shared
   Klaf formatter, into an SRT-compatible text representation and insert that
   representation in the Original field. For each segment, use an index, an
   `HH:MM:SS,mmm --> HH:MM:SS,mmm` range, its text, and a blank separator.
4. This format intentionally works with the existing `TranscriptTextCleaner`:
   Original preserves timestamps and Cleaned removes SRT indexes/time ranges
   for vocabulary analysis. If no segments arrive, insert the final
   `result.transcript` rather than failing a successful operation.
5. Timestamp granularity is current Whisper PCM-chunk granularity (about 29
   seconds), not word-level timestamps. Do not promise word or sentence
   alignment in UI copy.

## Klaf Protocol And Server Bridge

1. Keep the public Android-to-Klaf protocol independent from AgentDriver. Do
   not expose `AssistantClientSpeechToTextRequest`, AgentDriver request IDs, or
   AgentDriver WebSocket messages to the app.
2. Extend `KlafServerProtocol.kt` with a dedicated Vocabulary Source
   transcription operation. It needs:
   - a start message carrying the Klaf request ID, source ID, filename, audio
     format, and declared byte size;
   - binary upload framing tied to that request ID, with an explicit transfer
     complete message;
   - server-to-client progress messages for acknowledged upload bytes and
     completed recognition chunks; and
   - a terminal success message containing transcript plus timestamp segments,
     or the existing typed `KlafServerErrorMessage` / cancellation response.
3. Binary audio must use WebSocket binary frames and a bounded temporary file
   on Klaf Server. Do **not** Base64 encode audio inside JSON messages and do
   not hold a 250 MB upload entirely in an Android or server JSON object.
4. Validate declared size, actual received size, accepted type/signature, and
   the existing AgentDriver input bounds before recognition. Delete the
   temporary upload after success, failure, or cancellation.
5. Extend `KlafServerSession` so it can send the new start/complete messages,
   send binary frames, route progress messages to the correct pending
   transcription operation, and route terminal response/error messages. Its
   existing `request(message): KlafServerMessage` shape is insufficient for a
   progressive binary operation; add a dedicated typed transcription API rather
   than weakening unrelated one-shot request behavior.
6. Add an app-side repository/use case specifically for vocabulary-source
   transcription. It converts the platform URI into upload bytes/streams and
   exposes the Klaf progress/result states to the ViewModel. Do not put Android
   `Uri` or `ContentResolver` into common domain contracts.
7. On Klaf Server, add a dedicated speech-to-text session/service or extend a
   carefully named existing owner. It creates/uses an internal AgentDriver
   client session that advertises `SpeechToText`, invokes
   `transcribeSpeechProgressively`, and maps:
   - `SpeechToTextEvent.Uploading` -> Klaf upload progress;
   - `SpeechToTextEvent.Transcribing` -> Klaf transcription progress;
   - `SpeechToTextEvent.Completed(result)` -> Klaf terminal success.
8. AgentDriver failures terminate the Flow as exceptions. Translate them at the
   Klaf Server boundary into the existing Klaf error taxonomy without exposing
   raw provider paths, tokens, audio bytes, or transcript content in logs.
9. Add the new operation to `KlafServerOperationRegistry` so a completed result
   can be replayed after a reconnect. Follow the established transcription
   lifecycle in `audio-transcription-requirements.md`: server-owned recognition
   continues after acknowledged upload; result retention is 15 minutes or until
   reconcile; no offset-resume before acknowledgement; and disconnected
   completion/failure uses the existing FCM/deep-link pattern.
10. Upgrade `KLAF_SERVER_PROTOCOL_VERSION` and keep old-message handling
    explicit. Update client/server serialization tests together.

## Concurrency And Cancellation

- At most one active transcription per `VocabularySource`.
- Klaf Server serializes recognition globally because the local Whisper model is
  shared and CPU-heavy; later requests queue server-side.
- Cancelling while queued removes the queued operation. Cancelling while active
  cancels the AgentDriver Flow collection/request and deletes temporary audio.
- Repeating the Transcribe action while active is disallowed. Never start both
  AgentDriver `transcribeSpeech(...)` and
  `transcribeSpeechProgressively(...)` for one selected file.

## Test-First Plan

Write the tests before production handlers where practical.

1. Domain/Room tests: URL defaults to empty, maps round-trip, migrates existing
   rows, and participates in save/unsaved state without changing Description.
2. ViewModel/presentation tests: blank URL cannot copy; nonblank editable URL
   copies; selection alone does not transcribe; each operation state renders;
   success replaces only draft Original text and marks it unsaved/stale; failure
   and cancellation preserve the draft text.
3. Formatter tests: segments produce deterministic SRT-compatible timestamps,
   including millisecond padding and hour rollover; an empty segment list falls
   back to `transcript`.
4. Klaf contract tests: JSON messages serialize/deserialize, binary transfer is
   request-bound, malformed sizes/types fail safely, and new terminal/progress
   messages are routed by request ID.
5. Klaf Server tests with a fake AgentDriver client: `includeTimestamps` is
   true; upload and recognition events map faithfully; completed result is
   retained/replayed; failure/cancellation clean temporary files; one global
   recognition task and one task per source are enforced.
6. Android instrumentation/manual checks: system picker returns a readable
   WAV/MP3/M4A URI without broad storage permission; URI remains readable by
   the foreground upload path; progress changes on-device; Cancel works;
   background/reconnect and notification navigation recover a completed result.

## Explicit Non-Goals

- Downloading audio from the stored URL.
- YouTube extraction, URL scraping, or media acquisition.
- Automatically saving the URL or transcript.
- Automatically starting vocabulary analysis after transcription.
- Speaker diarization or word-level timestamp alignment.
- Persisting uploaded audio in Room or keeping it after terminal completion.
- Desktop audio picking in this first Android-focused pass.

## Multi-User Isolation And Conflict Handling

1. Audio upload and transcription lifecycle must be indexed and tracked by unique
   `requestId`, not raw client integer `sourceId`. Raw `sourceId` values are
   local SQLite keys unique only on a single device, so two concurrent users with
   `sourceId = 5` must never conflict or block each other.
2. If a new transcription start request arrives for the same source from the same
   client session, the server preempts and cancels any previous active or queued
   transcription for that source, cleans up old temporary uploads, and starts the
   new request cleanly.

## Background Execution, Foreground Service, And Disconnected Push

### Explicit deferral (2026-09-28)

Recovery after the Android app process has been killed is intentionally deferred
by the user for now. Do not persist request routing or implement process-death
reconciliation in the current commit. Same-process screen navigation and
WebSocket reconnect remain supported. The recovery description below applies
to the surviving process; killed-process recovery is future work.

1. Android audio transcription must be coordinated by an
   `IVocabularySourceTranscriptionBackgroundManager` backed by an Android
   Foreground Service (`AndroidVocabularySourceTranscriptionBackgroundManager`),
   displaying a user-facing notification while uploading and transcribing.
2. If the user navigates away or the WebSocket connection drops while Whisper is
   processing on Klaf Server:
   - Klaf Server continues processing in `KlafServerOperationRegistry`.
   - When transcription finishes, if the client WebSocket is disconnected,
     Klaf Server sends an FCM notification (`pushNotifier.notifyVocabularySourceTranscriptionCompleted`).
   - Tapping the notification or returning to the source detail screen reconnects
     to the server with the active `requestId` and retrieves the completed transcript.
   - If the user is actively viewing the screen when transcription completes,
     the transcript updates the draft text directly and the push notification is
     suppressed.

## Suggested Implementation Order

### Uncommitted-worktree audit (2026-09-28)

- Review the entire staged/unstaged change set, refactor confirmed defects and
  cover them with deterministic tests before treating the work as ready.
- An upload must receive exactly its declared byte count. Reject oversize chunks
  before writing them and reject incomplete completion. Duplicate starts must
  not replace an existing upload or leak its temporary file.
- Keep start/chunk/completion ordering, discard unfinished uploads on connection
  loss, and preserve acknowledged server-owned recognition for reconnect.
- Connection failure/cancellation must not wait for the reader while holding a
  mutex that the reader needs for shutdown. Terminal results must not be dropped
  when progress buffers fill.
- Tests use fake recognition and temporary databases; no paid provider calls,
  real account/backup edits or phone/server deployment are required for this audit.
- A push must target the originating client session, never whichever phone last
  registered its token. Missing routing information means no push, not a fallback
  to an unrelated device.
- A duplicate push attempt must not report successful delivery merely because
  another attempt is in flight. Only confirmed delivery suppresses a retry.
- Final commit privacy check: exclude real credentials, personal paths, LAN
  addresses and user/device/resource identifiers from tracked files and the
  index. Preserve local configuration on disk; use neutral templates and
  synthetic test data instead. Do not commit generated caches or crash dumps.

1. Add URL persistence, migration, detail-state/UI field, and clipboard action. (Done)
2. Add shared transcription domain/UI state and timestamp formatter with tests. (Done)
3. Extend the Klaf contract and app WebSocket session for bounded binary upload,
   progress, terminal result, and cancellation. (Done)
4. Update AgentDriver SDK with cooperative cancellation (`ensureActive`) and
   `cancelAndJoin` synchronization. (Done)
5. Implement Klaf Server multi-user isolation (key by requestId and clientSessionId),
   stale-request superseding, and FCM push notification on disconnected completion. (Done)
6. Add Android `AndroidVocabularySourceTranscriptionBackgroundManager` (Foreground
   Service) and screen reconnect recovery. Same-process recovery is implemented;
   killed-process recovery is intentionally deferred by the user (2026-09-28).
7. JVM regression tests and Android/Desktop compilation are verified by the audit;
   real provider/phone and CPU-only Linux smoke/benchmark checks remain required.
