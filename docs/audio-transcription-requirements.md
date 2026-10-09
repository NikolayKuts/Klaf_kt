# Audio Transcription Requirements

## Account Integration (2026-09-28)

Explicit logout/account-context changes cancel the originating device session's
unfinished work and clear retained client transcription results. This overrides
retention/reconnect behavior for explicit logout only; ordinary backgrounding
and transient connection loss still retain work. Offline logout remains immediate
and rejects late results, even when server cancellation cannot be delivered.
Other devices' work is unaffected. See `klaf-server-requirements.md` for agreed
branch integration rules; cross-device source synchronization is a later phase.

This document records agreed requirements for importing a local audio file and
producing an English transcript for Klaf.

When server-side transcription fails, the server log must identify the exception
types and relevant code locations (including nested causes) without logging
audio content, transcript text, credentials, or arbitrary exception messages.
The client continues to receive a generic failure response.

The later `vocabulary-source-audio-transcription-implementation-requirements.md`
is authoritative for current Klaf UI timestamps and foreground-service scope.
The demo gate below describes the earlier SDK verification phase, not a current
instruction to stop reviewing the already implemented Klaf path.

## AgentDriver Demo Verification Gate

Before resuming Klaf Server or Klaf Android work, validate the SDK independently:

- The AgentDriver demo server run task provisions the local Whisper and FFmpeg runtime and advertises `SpeechToText` through the normal session handshake.
- The AgentDriver Android demo selects a local WAV, MP3, or M4A document through the Android system picker.
- The demo calls the public `IAssistantClient.transcribeSpeechProgressively(...)` API with `includeTimestamps = true`, renders upload and recognition progress, and displays the terminal transcript and timestamp segments.
- The demo keeps file selection and transcription as explicit, separate user actions. It is a smoke-test app, not a Klaf UI, and has no Klaf background, FCM, save, or analysis behavior.
- The server demo diagnostics must expose actual local-recognition stages without logging audio data or transcript text: FFmpeg decode start/completion, then start and completion of each Whisper PCM chunk with its measured chunk-based percentage. FFmpeg must not report a synthetic percentage.
- During the current AgentDriver-only Android demo validation, an M4A selection is remuxed into a valid temporary preview containing its first 10% of media duration before upload. This is an explicitly temporary test-speed setting, not the Klaf production upload behavior. The preview is a media-duration crop, not a raw byte truncation.
- A connected session with an active request must not be closed by the session's client-idle timeout. Once no request is active, normal client-idle timeout behavior still applies.
- The current Windows-only demo verification route uses `clientApp.connectionMode=lan`. The local IP and port are gitignored `clientApp.lanServerHost` / `clientApp.lanServerPort` values. In this explicitly opted-in demo mode, the server binds to that LAN address and Android uses unencrypted `ws` without Cloudflare or client-token authentication. It is not a production deployment mode.

Only after this manual end-to-end verification passes may Klaf-specific upload,
operation retention, and UI work resume.

## Interview Decisions

### P0 - Supported Input

The first release accepts a local audio file selected in the Android app.
Supported formats are WAV, MP3, and M4A. An input must be no longer than 60
minutes and no larger than 250 MB. Video files and acquiring media from a URL
are explicitly outside this release.

### P0 - Recognition Language

The first release recognizes English speech only.

### P0 - Compute Environment

The Linux production server must support speech recognition on CPU alone. A GPU
may be used as an optional future acceleration, but it is not a deployment
requirement.

### P0 - Recognition Quality Baseline

Recognition accuracy is preferred over the shortest possible CPU processing
time. The initial offline ASR candidate is a quantized English Whisper
`small.en` model. Before the feature is accepted, its processing time and
memory use must be measured on both Windows development and CPU-only Linux.

### P0 - No Speaker Diarization

The first release returns one transcript without identifying or separating
speakers. Speaker diarization is out of scope.

### P0 - Production Deployment

Klaf Server is initially launched as a regular process from a terminal command
on Linux, not in Docker. Audio-decoding dependencies must therefore support a
regular host installation.

### P0 - Self-Provisioned FFmpeg

FFmpeg must not require a separate manual installation on Windows development
or Linux production. A Gradle setup task checks for a locally provisioned,
pinned platform-specific FFmpeg binary and downloads it once when absent before
the server run task starts.

The implementation must verify the downloaded artifact using a pinned checksum
before making it executable. If automatic provisioning fails or the platform is
unsupported, the server preflight must return an actionable diagnostic that
names the missing dependency and documents an explicit manual executable-path
override.

### P0 - File Transfer Boundary

The Android app selects the file through the system file picker and uploads it
to Klaf Server. Klaf Server then invokes the AgentDriver speech-to-text
capability; the user must not manually place the file on the server machine.

AgentDriver server SDK owns the generic speech-to-text capability: audio
validation, decoding, FFmpeg, offline Whisper runtime, model provisioning, and
the typed result contract. Klaf Server owns only Klaf-specific orchestration:
the source identifier, Android-facing upload transport, operation replay, FCM,
and reconciliation. It must not contain Whisper- or FFmpeg-specific behavior.

### P0 - Analysis Is Explicit

Completing audio transcription must not automatically start Vocabulary Source
analysis or create cards. Analysis remains a separate explicit user action.

### P0 - Vocabulary Source Detail Flow

Audio transcription is started from the detail screen of an existing
`VocabularySource`, not from a separate creation flow. That screen provides:

1. an action to select an audio file through the Android system file picker;
2. a separate explicit action to submit the selected file for transcription.

Selecting a file alone must not start transcription.

After selection and before submission, the detail screen displays the file
name, size, and duration, and gives the user an action to replace the selected
file.

### P0 - Completed Transcript Replaces Source Text

When transcription completes successfully, its transcript replaces the current
Original transcript text of that `VocabularySource`. This happens without a
confirmation dialog and without appending or merging text: requesting
transcription for an existing source is an explicit intention to replace its
source transcript.

The replacement is not saved to Room automatically. It is an ordinary unsaved
edit on the detail screen and is persisted only when the user invokes the
existing Save action.

The transcript is inserted as recognized. Only safe normalization of technical
whitespace is allowed before insertion; the model's wording, capitalization,
and punctuation are not silently rewritten. The existing local `Cleaned` mode
must handle this ASR transcript format as a separate transformation.

### P0 - Cancellation

While a file is uploading or speech recognition is running, the detail screen
shows an explicit Cancel action. Cancelling stops the current operation and
must not replace or otherwise modify the source transcript text. The user may
then select a different file and start a new transcription.

### P0 - Progress Presentation

The first release shows two truthful operation phases: `Uploading audio` and
`Transcribing`. Both phases expose the Cancel action. It does not display a
synthetic percentage because recognition cannot provide a reliable overall
progress value.

### P0 - Background Completion And Recovery

User decision (2026-09-28): recovery after process death is intentionally out of
the current implementation/commit. The background and reconnect behavior here
applies while the Android process survives. Persistent request routing and
killed-process reconciliation remain future work.

Audio transcription is a server-owned operation. It must continue when the
Android app moves to the background or its WebSocket connection drops.

The Android app uses a foreground service during upload and transcription while
its process is alive. Once Klaf Server has fully received and acknowledged that file, a
subsequent app backgrounding, screen lock, internet interruption, or WebSocket
drop must not cancel the server-owned transcription.

If connection loss occurs before Klaf Server acknowledges the complete upload,
the first release does not resume from a byte offset. The incomplete temporary
upload is discarded and the user starts the upload again.

If transcription completes without an active client connection, Klaf Server
sends an FCM notification that opens the specific originating
`VocabularySource`. When the app returns, it must also reconcile with Klaf
Server and obtain a completed result even if the FCM message was delayed or was
not delivered. Klaf Server retains the completed result until it is reconciled
by the client or its 15-minute retention timeout expires.

If transcription fails without an active client connection, Klaf Server sends
an FCM notification that opens the originating `VocabularySource`. The detail
screen shows the actionable failure and lets the user start again.

### P0 - Source Audio Retention

Uploaded source audio is temporary and must not be persisted in Room or retained
on Klaf Server. The server deletes its temporary audio file after success,
failure, or cancellation.

### P0 - Server Restart

The first release does not persist active transcription work or completed
unreconciled results across a Klaf Server restart. A restart interrupts the
operation; the client receives a clear failure and the user may start it again.

### P0 - Per-Source Concurrency

Only one audio transcription may be active for a given `VocabularySource`.
While it is active, the UI presents its current operation and Cancel action and
does not permit starting a second transcription for that source.

### P0 - Global Recognition Concurrency

Klaf Server executes one speech-recognition task at a time across all sources.
Additional accepted transcription operations wait in a server-side queue. This
protects CPU-only execution and the shared loaded model from competing heavy
work.

### P0 - Optional Timestamp Output In AgentDriver

The AgentDriver client speech-to-text request must let its caller request
timestamped output through an explicit request field or flag. Timestamp
generation is optional per request; callers that do not need it can request a
plain transcript.

The speech-to-text response contract must support both the complete transcript
and timestamped segments when requested. It returns one structured result type
containing `transcript` and `segments`; `segments` is empty when timestamps
were not requested.

The initial local recognizer reports timestamp segments at its PCM recognition
chunk granularity (approximately 29 seconds), not word- or sentence-level
alignment. The AgentDriver Android demo must render returned segments with
their start and end times when it requests timestamps, so this response shape
can be manually verified.

Klaf Server and the AgentDriver server must enforce the configured maximum
transcript character count before returning a speech-to-text result to a
client. A recognizer result exceeding that limit is rejected as an invalid
provider result rather than being serialized unboundedly.

The local recognizer accepts audio up to 60 minutes. Its default operation
timeout is 45 minutes to allow sequential CPU recognition to finish; a timeout
is an operational failure, not a normal way to end a still-running recording.

For the initial Vocabulary Source UI, Klaf Server sends
`includeTimestamps = true`. The Vocabulary Source UI formats returned segments
as SRT; when segments are absent it uses the full transcript.

### P0 - Progressive AgentDriver Client API

Alongside the existing one-shot `IAssistantClient.transcribeSpeech(...)` method,
AgentDriver exposes `transcribeSpeechProgressively(...)` as a cold Kotlin
`Flow`. One collection starts exactly one upload and recognition request; an
application must not call both methods for the same audio input.

The flow emits typed lifecycle events rather than partially populated
`SpeechToTextResult` values:

- `Uploading(uploadedBytes, totalBytes)` is based on server acknowledgements of
  persisted upload bytes;
- `Transcribing(completedChunks, totalChunks)` is emitted by the server after
  each local Whisper chunk; and
- `Completed(result)` contains the final complete `SpeechToTextResult`.

Recognition errors terminate the flow with the existing typed exception path;
they are not represented as a nullable result or an error event. Cancelling the
collector cancels the client request using the existing request-cancellation
mechanism.

The API does not define a synthetic universal `0..100` percentage. A UI may
show phase-specific progress directly or map the two truthful phases into its
own visual presentation.

## Deferred

- Timestamped segments in the Vocabulary Source UI.
- Speaker diarization.
- Offset-based resume of a partially uploaded file.
- Persisting active or unreconciled transcription work across a server restart.
