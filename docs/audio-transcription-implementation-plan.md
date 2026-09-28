# Audio Transcription Implementation Plan

Ordered implementation plan for local audio transcription. This plan becomes
active only after explicit user approval.

## 1. Test-First Executable Specification

- Write failing AgentDriver contract tests for `SpeechToTextResult`, optional
  timestamp segments, request validation, and protocol serialization before
  changing production contracts.
- Write failing unit tests for recognition queueing, per-source exclusivity,
  cancellation, terminal replay, and the 15-minute transcription-only TTL.
- Write Klaf Server tests for bounded binary upload, incomplete-upload cleanup,
  FCM/reconcile success and failure, and source-result application before their
  production handlers are added.
- Use fake decoder/recognizer ports for deterministic lifecycle tests. Keep
  real FFmpeg, model download, and actual model inference for explicit
  integration/preflight tests rather than faking their behavior into unit
  tests.

## 2. AgentDriver Speech-To-Text Contract

- Replace the current `String` result of `transcribeSpeech(...)` with public
  `SpeechToTextResult(transcript, segments)`.
- Add `includeTimestamps` to the request; return an empty segment list when it
  is false.
- Keep the one-shot method and add `transcribeSpeechProgressively(...)` as a
  cold `Flow` that emits acknowledged upload bytes, completed Whisper chunks,
  and one terminal complete result. Do not invent an overall percentage.
- Update protocol messages, client implementation, dispatcher, tests, and
  compatibility-sensitive callers.
- This is a user-approved replacement of the prior AgentDriver direction that
  limited STT to a plain provider transcript. The generic STT implementation is
  SDK-owned, not Klaf Server application code.

## 3. Offline Recognition Runtime

- Add a properly named AgentDriver JVM module for offline speech recognition;
  do not extend the temporary TTS module indefinitely.
- Reuse the sherpa-onnx Java/native runtime pattern for Windows and Linux.
- Add a checksum-verified Gradle task that downloads and installs the pinned,
  quantized English Whisper `small.en` model once into the AgentDriver data
  directory before `:klaf-server:run` starts, not only before an AgentDriver
  sample-server run task.
- Provide a shared CPU recognizer guarded by one execution queue. It returns a
  transcript and optional timestamp segments, has no speaker diarization, and
  is advertised as the production `SpeechToText` capability.

## 4. Audio Decoding Runtime

- Add a separate checksum-verified Gradle task that provisions a pinned,
  platform-specific FFmpeg executable once for Windows and Linux before
  `:klaf-server:run` starts.
- Resolve an explicit manual executable-path override first, then the
  provisioned binary; preflight must explain a missing or unusable dependency.
- Decode accepted WAV, MP3, and M4A inputs to the PCM format required by the
  recognizer. Validate file signature, 250 MB size, and 60-minute duration on
  the server before recognition.

## 5. Klaf Server Upload And Operation Flow

Before starting this Klaf-specific step, implement and manually verify an
AgentDriver demo vertical slice: the demo server advertises local
`SpeechToText`, and the demo Android client selects WAV/MP3/M4A through the
system picker, invokes the progressive public client SDK with
`includeTimestamps = true`, and displays progress plus the returned transcript
and segments. This isolates SDK/model/decoder failures from Klaf orchestration.

- Add binary upload messages and a bounded temporary-file receiver to the
  Klaf Server protocol. Bind every operation to its `VocabularySource`; relay
  accepted audio to AgentDriver without duplicating SDK media processing.
- Start recognition only after the full upload has been acknowledged and
  validated. Discard incomplete uploads on interruption.
- Use a server-side queue for one recognition task globally and one active
  operation per source. Cancellation removes queued work or cancels active
  work and always deletes temporary audio.
- Invoke AgentDriver with `includeTimestamps = true` for this UI; format SRT
  segments with a plain transcript fallback.
- Extend operation-result retention to support a 15-minute transcription TTL
  without changing existing operation retention. A server restart is terminal
  failure in this release.
- Deliver disconnected success and failure through existing FCM/deep-link and
  reconcile mechanics; retain terminal responses until reconcile or TTL.

## 6. Android Vocabulary Source Flow

- Add an Android system document picker for WAV, MP3, and M4A and retain the
  URI permission needed by a foreground upload service.
- On the source detail screen, show the selected file's name, size, duration,
  replace action, and a separate explicit Transcribe action.
- Accompany upload and transcription with a foreground service while the app
  process is alive;
  recognition remains server-owned after full acknowledgement.
- Show `Uploading audio`, `Queued`, or `Transcribing` as applicable, with
  Cancel. Do not display synthetic percentages.
- On result, replace the editable Original transcript text without saving it,
  starting analysis, merging text, or rewriting recognition punctuation and
  capitalization. Mark existing analysis stale according to current transcript
  edit behavior.
- On FCM navigation or reconnect, reconcile the operation and restore its
  result or actionable failure on the correct source detail screen.

## 7. Verification

Audit checkpoint (2026-09-28): the deterministic regression suite and
Android/Desktop compilation are covered in `audio-transcription-status.md`.
Persisting/reconciling requests across process death is intentionally deferred
by the user (2026-09-28). Selected-file duration, disconnected failure pushes
and real Android/Linux validation remain open.

- Add unit tests for the AgentDriver contract/result model, recognition queue,
  cancellation, timestamp option, and model/dependency preflight.
- Add Klaf Server tests for upload bounds, validation, cancellation, queueing,
  result replay/15-minute expiry, and FCM/reconcile success and failure paths.
- Add presentation tests where existing patterns allow; manually verify Android
  file selection, background upload, reconnect after acknowledged upload,
  notification navigation, cancellation, and unsaved transcript behavior.
- Benchmark the selected model's memory and processing time on Windows and
  CPU-only Linux before accepting the feature.
