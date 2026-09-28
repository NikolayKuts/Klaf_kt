# Transcript Vocabulary Analysis Status

Working status file for the `VocabularySource` feature.

## Current Focus

- 2026-09-28 staged/unstaged audit complete. Domain/data/presentation regression
  tests and Android/Desktop compilation pass. Full audit details and remaining
  audio lifecycle gaps are recorded in `audio-transcription-status.md`.
- Manual Android smoke test of the target-deck chooser with enough decks to
  require scrolling.

## Completed

- Theme consistency (2026-09-28): source detail colors moved to the central
  `Color.kt` vocabulary-source palette wired into both `MainTheme` variants.
  Disabled controls, save/edit markers, occurrence highlights, category/CEFR
  badges, audio card, icons and secondary text no longer define local colors.
  Dialogs retain Material theme colors and now have both light/dark previews;
  preview backgrounds also come from the central configuration. Shared
  checkbox and scrollbar defaults use central tokens.
- Added three source-theme tests for palette wiring, disabled tokens and badge
  contrast. Tests caught and fixed an initialization cycle in the first palette
  factory placement; the factory now belongs to the palette's Theme object.
  Full validation: 116 tests, zero failures/errors; Android compile and Desktop
  DI compile pass. Visual previews/device smoke checks were not run.
- Audit: one authoritative editable StateFlow prevents rapid edits being lost;
  Save persists its snapshot without clearing newer edits; duplicate Save is
  blocked; an empty new analysis no longer displays old persisted items.
- Audit: analysis against earlier text stays visibly stale, including after
  Save. ADDED rows keep their current card/deck links rather than being overwritten
  by stale draft copies. Foreign phrases normalize repeated internal whitespace.
- Added temporary real-Room tests for migrations 8/9/10/11 -> 12, transactional
  rollback, ignored-rule uniqueness and ADDED-link preservation. Added ViewModel
  tests for fast edits, concurrent Save, analysis staleness, transcription-limit
  rejection and result recovery after ViewModel recreation.
- Made the existing-deck list in the target-deck chooser a height-bounded,
  scrollable `LazyColumn`; the new-deck controls remain above it. Verified
  `:presentation:compileDebugKotlinAndroid`.
- Center-aligned the foreign word and transcription/text-to-speech unit within
  a shared `FlowRow` line. Verified `:presentation:compileDebugKotlinAndroid`.
- Replaced the item title's fixed row with `FlowRow`: the foreign word and the
  combined transcription/text-to-speech unit now wrap independently, so the
  unit moves intact below the word when horizontal space is insufficient.
  Verified `:presentation:compileDebugKotlinAndroid`.
- Starting a new analysis now clears the previous occurrence highlight, pending
  locate request, and transcript selection before the background operation is
  started. Verified `:presentation:compileDebugKotlinAndroid`.
- Added a compact character counter in the transcript field's top-right
  outline. It follows the visible Original or Cleaned text and remains visible
  for empty text. Verified `:presentation:compileDebugKotlinAndroid`.
- Created requirements document:
  `docs/transcript-vocabulary-analysis-requirements.md`.
- Created nice-to-have document:
  `docs/transcript-vocabulary-analysis-nice-to-have.md`.
- Created ordered implementation plan:
  `docs/transcript-vocabulary-analysis-implementation-plan.md`.
- Added domain entities for `VocabularySource`, `VocabularySourceItem`, status,
  category, confidence, part of speech, and occurrences.
- Added `IVocabularySourceRepository`.
- Added initial vocabulary source CRUD/observe use cases.
- Added Room entities, DAOs, repository implementation, mappers, database v8
  registration, platform migrations, and Koin data bindings.
- Verified `:data:compileDebugKotlinAndroid` and `:domain:compileKotlinMetadata`
  successfully after Room changes.
- Added `TranscriptTextCleaner` with text length validation and heuristic
  SRT/VTT cleanup.
- Added common tests for transcript cleanup and validation.
- Verified `:domain:allTests` successfully.
- Added AI extraction domain contract, strict response schema prompt factory,
  Agent Driver repository, and DI binding.
- Verified `:domain:compileKotlinMetadata`, `:data:compileDebugKotlinAndroid`,
  and `:di:compileDebugKotlinAndroid` successfully after AI contract changes.
- Added local `VocabularySourceItemCategorizer` and tests for new words,
  possible new meanings, exact known meanings, and occurrence order.
- Verified `:domain:allTests` successfully after categorization changes.
- Added shared Compose scaffold for vocabulary source list, creation dialog, and
  detail screen.
- Added navigation destinations and drawer entry for vocabulary sources.
- Added presentation Koin bindings for vocabulary source ViewModels.
- Added separate `alreadyExists` item marker so existing vocabulary is not
  confused with source-created `added` state.
- Added runtime selection holders for source items on the detail screen.
- Added basic selected-item actions: ignore selected and restore selected.
- Added item edit dialog for manual `foreignWord` and `nativeWord` correction
  before saving.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after item editing changes.
- Added replacement persistence for new analysis drafts: pending/ignored source
  items are replaced while added items are preserved.
- Changed local card/deck insert contracts to return inserted ids so source
  items can keep `createdCardId`/`targetDeckId` linkage.
- Added use case for adding selected saved source items to an existing deck.
- Added basic target deck chooser dialog on the source detail screen.
- Added create-new-deck option inside the target deck chooser.
- Verified `:domain:allTests`, `:data:compileDebugKotlinAndroid`,
  `:presentation:compileDebugKotlinAndroid`, and `:di:compileDebugKotlinAndroid`
  successfully after add-to-deck and new-deck changes.
- Replaced immediate long-press source deletion with confirmation dialog.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after source deletion
  confirmation changes.
- Added source list counters for pending/added/ignored items.
- Verified `:domain:allTests`, `:data:compileDebugKotlinAndroid`,
  `:presentation:compileDebugKotlinAndroid`, and `:di:compileDebugKotlinAndroid`
  successfully after source counters changes.
- Added Compose previews for vocabulary source list, detail, creation dialog,
  delete confirmation, item editing dialog, and deck chooser dialog.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after preview additions.
- Replaced separate raw/clean transcript fields on the source detail screen
  with one shared text field and an Original/Clean mode switch.
- Added expand/close behavior for the transcript text field: collapsed mode
  shows 10 lines, expanded mode lets the full text field grow.
- Replaced the text mode buttons with an animated segmented switch whose
  selected background slides between Original and Cleaned.
- Replaced text expand/close action with an icon button inside the transcript
  text field; the icon changes between expand and collapse states.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after transcript field changes.
- Added debug/error logs for vocabulary source Agent Driver responses, JSON
  parsing, contract validation, domain mapping, Room occurrence parsing, save,
  analysis/categorization, and add-to-deck actions.
- Verified `:data:compileDebugKotlinAndroid`,
  `:presentation:compileDebugKotlinAndroid`, and
  `:di:compileDebugKotlinAndroid` successfully after logging changes.
- Disabled transcript text mode switching per mode availability: Original is
  enabled only when raw text exists, Cleaned is enabled only when cleaned text
  exists, and invalid selected modes auto-return to a valid mode.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after text mode availability
  changes.
- Cleared transcript text field focus before changing Original/Cleaned modes to
  avoid Android keyboard flicker when switching away from an active text field.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after the focus/keyboard fix.
- Made the transcript Original/Cleaned switch compact instead of full-width,
  improved its text contrast/readability, and moved the expand/collapse action
  into a top-right overlay on the transcript text field so it no longer centers
  vertically inside expanded text.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after transcript UI refinements.
- Reworked vocabulary source item display from a dense text block into a
  structured card with separate foreign/native text, badges, labeled example,
  explanation, known meaning, and added dedicated light/dark Compose previews
  for item cards.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after item display previews.
- Moved CEFR level out of the general item badge rows and into a compact
  colored marker next to the foreign word, reusing the Word Insights CEFR color
  palette.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after CEFR marker changes.
- Replaced the confusing `NOT SAVED` item badge with a compact card-link
  indicator shown for every item: red dot means no linked created card yet,
  orange dot means linked to the interim deck, and green dot means linked to any
  normal deck.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after updated card-link
  indicator colors.
- Made the card-link indicator easier to see by increasing the dot size, using
  a stronger orange for interim deck links, adding a dot outline, and reordering
  item preview samples so red, orange, and green states are visible near the top.
- Made vocabulary source item card containers more transparent to improve white
  text readability on the screen background.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after card indicator visibility
  and item background changes.
- Moved each item part-of-speech marker next to the foreign word and CEFR level,
  using the same compact rounded form with the existing gray badge background.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after moving the part-of-speech
  marker.
- Removed the visible `PENDING`/`ADDED`/`IGNORED` status badge from vocabulary
  source item cards. Card linkage is now represented by the card icon dot, while
  ignored items show a dimmed card icon without a dot.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after removing the visible item
  status badge.
- Made vocabulary source item cards, part-of-speech markers, and gray item
  badges more transparent to improve readability against the screen background.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after item and badge transparency
  adjustments.
- Reduced vocabulary source item card and gray badge background opacity again
  after visual review.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after reducing item and badge
  opacity again.
- Reduced vocabulary source item card and gray badge background opacity one more
  step for a lighter item UI.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after the latest opacity
  reduction.
- Increased spacing between the item foreign word and the CEFR/part-of-speech
  markers for better visual separation.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after marker spacing adjustment.
- Replaced the textual confidence badge (`LOW`/`MEDIUM`/`HIGH`) with compact
  monochrome confidence icons near the CEFR and part-of-speech markers: alert
  triangle for low, question circle for medium, and check circle for high.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after replacing the confidence
  indicator with icons.
- Updated vocabulary source item preview data so high, medium, and low
  confidence icons are all visible near the top of the preview.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after updating confidence preview
  data.
- Replaced the main vocabulary source `Save` text button with a compact save
  icon button. The button uses the normal primary color before saving and turns
  green after a successful save until the source or items are edited again.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after replacing the main save
  action with the save icon button.
- Changed the main vocabulary source save action to use the shared circular
  `RoundButton` style used by primary app action buttons.
- Verified `:presentation:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` successfully after switching the save action
  to `RoundButton`.
- Added chunked Agent Driver vocabulary source analysis for long transcripts:
  long cleaned text is split into smaller natural-boundary chunks, each chunk is
  analyzed separately, occurrence offsets are shifted back to full transcript
  offsets, and exact duplicate vocabulary items are merged.
- Verified `:data:compileDebugKotlinAndroid` successfully after chunked
  analysis changes.
- Reduced Agent Driver vocabulary source chunk size from about 4500 characters
  to about 1500 characters and capped each chunk response to 30 items after the
  provider still timed out on a 4261-character chunk.
- Verified `:data:compileDebugKotlinAndroid` successfully after smaller chunk
  and per-chunk item limit changes.
- Made vocabulary source AI response handling tolerant of bad occurrence data:
  simple occurrence issues are repaired, missing occurrences are inferred from
  source text when possible, and only unrecoverable occurrences/items are
  dropped instead of failing the whole chunk.
- Reduced per-chunk response cap from 30 items to 20 items after a
  1152-character chunk still produced a large 15382-character response.
- Verified `:data:compileDebugKotlinAndroid` successfully after tolerant
  response sanitization changes.
- Verified `:domain:allTests`, `:data:compileDebugKotlinAndroid`,
  `:presentation:compileDebugKotlinAndroid`, and `:di:compileDebugKotlinAndroid`
  successfully after selection/status persistence changes.
- Verified `:domain:allTests`, `:data:compileDebugKotlinAndroid`,
  `:presentation:compileDebugKotlinAndroid`, and `:di:compileDebugKotlinAndroid`
  successfully after UI scaffolding.
- Restored the Vocabulary Source feature from stash after the Klaf Server
  integration work.
- Removed the old direct app-side Agent Driver analysis repository and moved
  prompt/schema/chunking/parsing/sanitizing logic into the `klaf-server` module.
- Added `vocabularySource.analyze` / `vocabularySource.analyzed` websocket
  protocol messages to `klaf-server-contract`.
- Added app-side `KlafServerVocabularySourceAnalysisRepository` so
  `AnalyzeVocabularySourceTextUseCase` now talks to the project Klaf Server.
- Added server-side `VocabularySourceAgentSession` and
  `VocabularySourceService`; the service keeps the previous chunked analysis
  behavior, occurrence repair, offset shifting, and exact item merge logic.
- Wired Vocabulary Source handling into `KlafServer`, `KlafServerModule`, and
  client response dispatch.
- Resolved stash conflicts by keeping Room schema version 8 and restoring the
  `7 -> 8` vocabulary source table migration.
- Verified `:klaf-server:compileKotlin`,
  `:domain:compileKotlinMetadata`, `:data:compileKotlinMetadata`,
  `:presentation:compileKotlinMetadata`, `:di:compileKotlinMetadata`,
  `:Android:compileDebugKotlin`, and `:domain:allTests` successfully after
  adapting the feature to Klaf Server.
- Added global Ignored Words persistence for Vocabulary Sources:
  - `Ignore` remains reversible in an unsaved runtime draft;
  - `Save` atomically stores ignored word/meaning pairs, removes those source
    items, and preserves added items;
  - an ignored record stores the server-provided analysis language; and
  - later analysis filters exact normalized pairs from cards and Ignored Words.
- Added `IGNORED · NEW MEANING` for a different meaning of a word known only
  through Ignored Words, using a muted red badge.
- Added Room database migration `10 -> 11` for item language and the global
  Ignored Words table on Android, Desktop, and iOS.
- Added categorizer coverage for exact card filtering, normalized ignored-pair
  filtering, and ignored-word new meanings.
- Verified `:domain:allTests`, `:data:compileDebugKotlinAndroid`,
  `:data:compileKotlinDesktop`, `:presentation:compileDebugKotlinAndroid`, and
  `:di:compileDebugKotlinAndroid` successfully.

## Active Decisions

- MVP accepts pasted text and the separately specified audio transcription path;
  subtitle fetching and subtitle/text file import remain deferred.
- MVP stores data locally in Room only.
- `VocabularySource` is the persistent source object.
- Source list is flat: no folders, seasons, or hierarchy.
- Source has `title`, `description`, `rawText`, and `cleanText`.
- Analysis items are stored separately as `VocabularySourceItem`.
- The app talks to the project Klaf Server; Klaf Server owns the AgentDriver
  session and uses strict JSON schemas for assistant responses.
- AI extracts vocabulary; Klaf performs local categorization against all cards
  from all decks.
- Analysis results are runtime drafts until explicit save.
- `Save` persists source state and item state but does not create cards/decks.
- Adding selected items to a deck is a separate explicit action.
- Selection checkbox state is runtime-only and not persisted.
- `pending` and `added` are persistent source-item states. `ignored` is a
  runtime state until save, after which it becomes a global Ignored Words rule
  and the source item is removed.
- Ignored Words are global across Vocabulary Sources and include the analysis
  language, foreign word, and Russian meaning. Exact normalized pairs are
  filtered from later analysis results.
- Added state follows the linked card's current existence and deck.
- MVP verification targets Android.

## Deferred / Moved Out Of MVP

- API-based subtitle fetching.
- File import for `.srt`, `.vtt`, and `.txt`.
- Remote sync/server-side storage.
- UI/instrumentation tests.
- Item filters by status/category/level.
- Ignored Words management screen for viewing and removing global rules.

## Open Questions

- No blocking open questions. Continue implementation according to the ordered
  implementation plan.

## Next Steps

- Manual Android smoke test of the full source flow is still useful.

## Implementation Notes

- Reuse existing Klaf dialog/button/list patterns where practical.
- Reuse the Klaf Server feature shape from Word Insights and Mnemonic:
  websocket contract message, app repository, server service, and server
  AgentDriver session.
- Watch existing duplicate-card prevention behavior when adding items to decks.
