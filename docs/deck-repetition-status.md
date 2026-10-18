# Deck Repetition Status

## Current Focus

- Live device/visual and audio playback verification remains. iOS compilation was not available on Windows.

## Completed

- Blocked image holders show a crossed-out eye. Native → Foreign reveals the image when the foreign side is shown; manual hide/reveal remains available.
- Removed word-tap pronunciation and added an accessible speaker button above the flip button, enabled on the foreign side during repetition.
- Independent scoped code review found no actionable issues. `git diff --check` passed.
- Independent `:presentation:desktopTest` and `:presentation:compileDebugKotlinAndroid` verification passed: Desktop and Android compiled; 86 tests in 18 suites passed without failures, errors, or skips.
- Replaced the pronunciation microphone icon with the existing speaker drawable; independent review and Android compilation passed again after this icon-only change.
- Ordered screen functions by visibility (public → internal → private), placing the private extension below the internal screen entry point, and recorded the convention in `AGENTS.md`.
- Final independent review of the complete diff found no actionable issues after reordering. Desktop tests (86 in 18 suites) and Android compilation passed again on the final source.
