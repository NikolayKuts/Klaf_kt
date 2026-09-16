# Transcript Vocabulary Analysis Implementation Plan

Ordered implementation plan for the `VocabularySource` feature.

## 1. Domain Model And Contracts

- Add domain entities for `VocabularySource`, `VocabularySourceItem`, item status,
  item category, confidence, unit type/part of speech, and item occurrence.
- Add repository contracts for sources and source items.
- Add use cases for creating, updating, deleting, observing, and fetching
  vocabulary sources and items.

## 2. Room Persistence

- Add Room entities for `VocabularySource` and `VocabularySourceItem`.
- Add DAOs for source list, source detail, item updates, and cascade-like source
  deletion behavior.
- Add mappers between Room and domain models.
- Add Room migration and schema update.
- Keep storage local-only for the MVP.

## 3. Transcript Cleanup

- Add local transcript cleanup logic that produces `cleanText` from `rawText`.
- Remove common pasted-subtitle noise such as line numbers, timestamps, and
  simple markup.
- Validate text length during input/paste before analysis starts.
- Add unit tests for cleanup and length validation.

## 4. AI Extraction Contract

- Add a strict JSON schema contract for transcript vocabulary extraction.
- Use the existing Agent Driver SDK session.
- Return normalized vocabulary units, original text, meaning, examples,
  explanation, confidence, CEFR, unit type, and occurrences.
- Add parsing and validation tests.

## 5. Local Categorization

- Build a runtime vocabulary index from cards across all decks.
- Categorize AI-extracted items as `new` or `possibleNewMeaning`.
- Store known-meaning snapshots for possible new meanings.
- Recalculate category after item edits.
- Add unit tests for categorization.

## 6. Source List UI

- Add a shared Compose screen for the flat list of vocabulary sources.
- Show title, description, timestamps, and counters for pending/added/ignored
  items.
- Add create, edit, and delete source actions.

## 7. Source Detail UI

- Add source detail screen with editable title, description, raw text, clean text
  preview, analysis actions, save action, and one analysis item list.
- Support switching between original and cleaned text views.
- Show stale-state warning when text changes after analysis.
- Show unsaved-draft leave confirmation with existing Klaf dialog patterns.

## 8. Analysis Draft Flow

- Run analysis against current editable text, not only saved Room state.
- Keep analysis results as runtime draft until explicit save.
- Preserve existing persisted items until a new draft is saved.
- On reanalysis, preserve items linked to existing cards and replace the rest
  when the new draft is saved.

## 9. Item Review And Editing

- Add runtime selectable item holders for checkbox selection.
- Display status markers, category, original text, confidence, CEFR, part of
  speech, example, explanation, and known meanings snapshot when applicable.
- Add item edit dialog/bottom sheet for `foreignWord` and `nativeWord`.
- Add actions to ignore items and restore ignored items to pending.

## 10. Add Items To Deck

- Reuse existing deck selection patterns where possible.
- Add ability to choose an existing deck or create a new deck from the selection
  flow.
- Confirm batch card creation with target deck and selected item count.
- Create cards from selected pending items only.
- Mark successful items as added with linked `cardId`/deck data; leave failed
  items pending and show a summary.
- Make added state follow linked card existence/current deck.

## 11. Navigation And DI

- Add destinations for source list, source creation/editing, source detail, item
  editing, and deck selection/create flow integration.
- Register repositories, use cases, and view models in Koin.
- Keep shared/common implementation where possible; add Android-specific pieces
  only when required.

## 12. Android Verification

- Verify source creation, text save, analysis, draft save, item editing,
  ignoring/restoring, add-to-deck, card deletion state refresh, and source
  deletion manually on Android.
- Do not add UI/instrumentation tests for the MVP.
