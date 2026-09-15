# Changelog

## Unreleased

### Added

- Add a new local/LAN Klaf Server path:
  - shared `:klaf-server-contract` protocol module;
  - JVM/Compose Desktop `:klaf-server` wrapper around AgentDriver SDK;
  - Ktor WebSocket endpoint at `/ws`;
  - Word Insights, mnemonic association, and mnemonic image commands;
  - console diagnostics for internal AgentDriver server/client state, provider
    diagnostics, selected model, reasoning effort, and capabilities.
- Keep Android mnemonic text and image generation visible to the operating
  system through a shared foreground service.
- Show success or failure notifications when generation finishes while Klaf is
  in background, and route a notification tap back to the matching card flow.
- Add shared KMP contracts for background-generation lifecycle handling, with
  an Android implementation and no-op Desktop and iOS implementations.
- Add lifecycle, connection, request, service, process, power, and network
  diagnostics for long-running mnemonic requests.

### Changed

- Route Android/Desktop Word Insights and mnemonic generation through the new
  Klaf Server WebSocket protocol instead of direct client-side AgentDriver SDK
  sessions.
- Move feature prompt construction, response-schema selection, assistant
  parsing, and assistant validation into `:klaf-server`.
- Use session-level Codex `baseInstructions` and `developerInstructions`
  through AgentDriver `thread/start`; per-request prompts now carry only
  request-specific data.
- Use AgentDriver SDK `0.10.11-SNAPSHOT` from Maven Local for Klaf Server,
  including the WSL2 preflight readiness fix.
- Configure Klaf Server internal Codex sessions to use `gpt-5.5` with `low`
  reasoning effort.
- Keep Android foreground-service handling for long mnemonic operations while
  routing the actual assistant request through Klaf Server.

### Fixed

- Show Klaf Server readiness from the real WebSocket connection state. The
  drawer now becomes ready only after receiving `server.ready` from Klaf Server
  instead of reporting readiness by default.
- Add a temporary Room `8 -> 7` downgrade migration for local devices that
  opened the stashed Vocabulary Source schema during development.
- Show a failed initial Klaf Server connection as an error instead of leaving
  the drawer on `Connecting` when no automatic retry loop is active.

### Removed

- Remove the Android/Desktop direct AgentDriver client feature path and bind
  migrated features to Klaf Server repositories.
- Remove the Android partial wake lock experiment. A vendor process freezer can
  disable the lock, while SDK-level session resume already provides the required
  recovery behavior.
