# Changelog

## Unreleased

### Added

- Keep Android mnemonic text and image generation visible to the operating
  system through a shared foreground service.
- Show success or failure notifications when generation finishes while Klaf is
  in background, and route a notification tap back to the matching card flow.
- Add shared KMP contracts for background-generation lifecycle handling, with
  an Android implementation and no-op Desktop and iOS implementations.
- Add lifecycle, connection, request, service, process, power, and network
  diagnostics for long-running mnemonic requests.

### Changed

- Use Agent Driver client SDK `0.10.7-SNAPSHOT` so one-shot text and image calls
  can resume after an unexpected WebSocket disconnect.
- Close an idle Agent Driver connection when Klaf enters background, but keep a
  session with an active request available for automatic resume.

### Removed

- Remove the Android partial wake lock experiment. A vendor process freezer can
  disable the lock, while SDK-level session resume already provides the required
  recovery behavior.
