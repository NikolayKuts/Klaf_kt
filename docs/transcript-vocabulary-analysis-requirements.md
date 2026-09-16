# Transcript Vocabulary Analysis Requirements

This document captures product and technical requirements for transcript-based
vocabulary extraction in Klaf.

## Agreed Requirements

### P0 - Feature Naming

The persistent source object should be named `VocabularySource` in code. The UI
can use the Russian label "Источник слов" / "Источники слов".

### P0 - Vocabulary Source Navigation

The feature should have a dedicated vocabulary source list screen, similar in
spirit to the existing deck list. From that screen, the user can create a new
source with a plus action. Source creation collects at least `title` and
`description`. Opening a source navigates to a detail screen where the user can
provide transcript text, trigger AI analysis, review detected items, and add
items to decks.

The creation flow should not collect transcript text. Transcript text is entered
or edited on the source detail screen.

The source list should show item counters for each source, including pending,
added, and ignored counts.

The source list should show useful timestamps such as creation date, update date,
or last analysis date.

Deleting a `VocabularySource` should delete its `VocabularySourceItem` records
but must not delete cards that were previously created from those items.

The user should be able to edit source `title`, `description`, and transcript
text after creation. Editing transcript text follows the reanalysis confirmation
rules when the user runs analysis again.

After an analysis has been created, the user may edit the transcript text and run
analysis again. Reanalysis must require confirmation because it can replace or
recompute existing analysis items.

On reanalysis, items already linked to created cards should be preserved. Other
items can be replaced by the new AI result. The user may later adjust item status
manually when needed.

### P0 - Manual Transcript Input

The MVP accepts text pasted manually by the user. It does not integrate with
subtitle websites, REST APIs, YouTube APIs, or file import.

### P0 - English To Russian Analysis

The first supported analysis direction is English input with Russian meanings.

### P0 - Words And Expressions

The analyzer should extract both individual words and useful multi-word
expressions, including phrasal verbs and stable phrases.

### P0 - Two Result Groups

The analysis result should separate items into at least two groups:

- new words or expressions that are not present in the user's vocabulary;
- possible new meanings for words or expressions that already exist in the
  user's vocabulary.

### P0 - Result Item Data

Each result item should include:

- foreign word or expression;
- Russian meaning;
- source example from the transcript;
- short explanation for the detected meaning;
- timestamp when the pasted transcript contains one.

### P0 - Review Before Adding

The user must be able to review analysis results before adding anything to a
deck. The review UI should support selecting, deselecting, and removing items
from the result list.

### P0 - Create Cards From Results

Selected result items can be converted into cards. The generated card should
include at least:

- `foreignWord`;
- `nativeWord`.

IPA is not required for this feature. Other card fields may use existing default
or empty values.

### P0 - Add To Existing Or New Deck

When moving selected result items to a deck, the user should be able to choose an
existing deck or create a new deck from the deck selection flow. The same
capability should be considered for the existing card transfer flow.

### P0 - Persistent Transcript Source

Transcript analysis should be stored as a persistent object rather than as a
runtime-only result. The object should store the original transcript text and the
analysis items returned by the AI.

The MVP uses a flat list of vocabulary sources. A source has a user-provided
`title` and free-form `description`. It does not model series, seasons,
episodes, folders, or nested grouping.

The original pasted text should be stored in Room as part of the source record.
The MVP does not store transcript text in separate files.

Analysis items should be stored in a separate `VocabularySourceItem` table linked
to `VocabularySource` by `sourceId`, rather than as a JSON blob inside the source
record.

Each analysis item should have a status:

- `pending` - visible and not added yet;
- `added` - added to a deck/card;
- `ignored` - intentionally hidden or removed by the user from the active review
  list.

Each analysis item should also store its result category:

- `new` - the word or expression is not present in the user's vocabulary;
- `possibleNewMeaning` - the word or expression exists, but the transcript may
  use a new meaning.

For `possibleNewMeaning` items, store a snapshot of known meanings from existing
cards when available, for example "обвинение; плата". This helps explain why the
item is considered a possible new meaning.

Klaf should assign item categories locally by comparing AI-extracted vocabulary
items with the user's card database. If a meaning comparison is ambiguous, Klaf
may send a compact set of ambiguous cases to the AI for resolution.

The local comparison should use cards from all decks, not only a target deck.

Analysis results should not be saved automatically. After AI extraction and
local categorization, results live as a runtime draft until the user explicitly
saves them. Before saving, the user can remove or adjust items. Only the
user-approved result is persisted as `VocabularySourceItem` records.

If the user tries to leave the detail screen with an unsaved analysis draft, the
app should show a confirmation dialog using existing Klaf dialog patterns.

The detail screen should have one save action that persists the current source
state. If there are no analysis items, saving persists source fields such as
title, description, raw text, and clean text. If there are analysis items or an
analysis draft, saving persists those items and their current states as well.
Saving source state must not create decks or cards. Creating cards from selected
items is a separate explicit action.

Analysis should use the current editable text on the detail screen, even if that
text has not yet been saved to Room. Saving later persists the same current
source state.

If transcript text changes after analysis items were produced, the UI should
mark existing items as potentially stale until the source is analyzed again.

Adding selected items to a deck is separate from saving the source. After cards
are created successfully, the corresponding items should update their current
state and links to the created deck/card.

Selection checkbox state is runtime UI state only and should not be persisted in
Room. It can be modeled with a holder similar to the existing card transfer
selection holder.

Item processing status such as `pending`, `added`, and `ignored` is persistent
state and should be stored in Room.

Ignored items should have an action that restores them back to `pending`.

The MVP should not provide a manual action that changes `added` back to
`pending` while the linked card still exists. Added state should follow the
linked card state.

If a linked card is moved to another deck, the item should reflect the card's
current deck when displaying where it was added.

When adding selected items to a deck, partial success should be handled
gracefully: successfully created cards are marked as added, failed items remain
pending, and the user receives an error or summary.

Before creating cards from selected items, show a confirmation dialog with the
selected item count and target deck name.

Batch add should create cards only from selected `pending` items. Selected
`ignored` or `added` items should be skipped with a short message or summary.

The MVP detail screen can show analysis items as one list with visual state
markers instead of status/category filters.

The MVP item list should keep active review items above completed or ignored
items. Within each state group, items should follow their first occurrence order
in the source text when available.

When an item's original transcript form differs from its normalized
`foreignWord`, the UI should display the original form as supporting text.

The user should be able to edit an item's `foreignWord` and `nativeWord` before
creating cards from it. Editing the AI explanation is not required for the MVP.

Item editing should use a dialog or bottom sheet rather than inline editing in
the list.

After editing an item's `foreignWord` or `nativeWord`, Klaf should recalculate
the item's category against the current vocabulary database when applicable.

The MVP does not need to store separate original AI-suggested values after the
user edits an item.

`VocabularySource.title` is required. Transcript text may be empty so the user
can create a source first and fill it later.

`VocabularySource.title` does not need to be unique.

The MVP stores vocabulary sources and their items locally in Room only. Remote
sync is out of scope.

Implementation should live in shared/common modules where possible, including
domain and shared Compose UI. MVP verification and any required platform-specific
implementation should target Android first.

AI analysis should use the existing Agent Driver SDK session. The MVP should not
introduce a second AI provider or provider switch.

AI transcript analysis should use a strict JSON schema contract, following the
same general approach as the existing word meaning insights contract.

Unit tests should cover transcript cleanup, local item categorization, and AI
response parsing/validation.

UI/instrumentation tests are out of scope for the MVP because the visual flow is
still expected to evolve.

When a source already has saved items, a new analysis should produce a separate
runtime draft. Existing persisted items must remain unchanged until the user
explicitly saves the new draft.

AI analysis should expose a loading/progress state and should allow cancellation
when feasible.

If AI analysis fails, the source and transcript text should remain available, and
no analysis draft should be saved automatically.

The MVP should enforce a configurable soft limit for transcript text length and
show a clear message when the text is too large for one analysis request.

Transcript length should be validated during input or paste before analysis is
started. The app should prevent launching analysis for text that exceeds the
configured limit.

If pasted text exceeds the configured limit, the paste should be rejected rather
than silently truncated.

The app should preserve both the original pasted text (`rawText`) and a cleaned
version (`cleanText`) used for AI analysis. Occurrence offsets should be relative
to `cleanText`. The UI may let the user switch between original and cleaned text
views.

Klaf should perform local transcript cleanup before sending text to the AI.
The MVP can use heuristic cleanup for pasted subtitle text, such as removing
line numbers, timestamps, and simple markup, rather than a full SRT/VTT parser.

Source text should be stored separately from analysis items. `rawText` and
`cleanText` belong to `VocabularySource`; extracted vocabulary items belong to
`VocabularySourceItem`.

The AI extraction should skip pure function words such as articles,
prepositions, and conjunctions when they do not form part of a useful expression.
Basic content words should still be considered because they can have multiple
important meanings.

AI extraction should store the normalized vocabulary unit separately from the
surface form found in the transcript. For example, `foreignWord = "go"` and
`originalText = "went"`.

When a phrase or phrasal verb is the meaningful vocabulary unit, the item should
represent the whole expression rather than separate non-useful component words.
Component words may still appear as separate items only when they have their own
important use elsewhere in the transcript.

Repeated occurrences of the same word or expression with the same meaning should
be represented as one item. If timestamps are available, the item may store a
list of timestamps where the unit occurs.

The same word or expression used with different meanings should create separate
items.

Each item may store occurrence data as JSON, including timestamp, start offset,
end offset, and source sentence. For discontinuous expressions such as
`turn it on`, the offset span may cover from the start of the first word to the
end of the last word, while `originalText` preserves the actual surface form.

Each item should store and display AI confidence when available, for example
`high`, `medium`, or `low`.

Each item should store and display part of speech or unit type when available,
for example `verb`, `noun`, `adjective`, `phrasalVerb`, or `phrase`.

Each item should store and display a CEFR proficiency level when available.

When an item is marked as `added`, it should store the target deck id and the
created card id when those values are available.

The added state must reflect the current card state, not historical actions. If
the linked card is deleted later, the vocabulary source item should no longer be
shown as added.

After an item creates a card, its added state should be based on the linked
`cardId`. Editing the card text later should not reset the item state while the
card still exists.

If the same word/meaning is added manually elsewhere in the app, the source item
should be able to show that the vocabulary already exists even when it was not
created from this source item. This should be treated separately from the
source-created `added` state, for example with an `alreadyExists` flag.

Ignored items should not disappear permanently. They may be shown with a disabled
or semi-transparent visual style, and the screen may provide filters to include
or exclude them.

### P0 - Added State Tracking

Analysis items should show whether they have already been added to a deck. When
possible, the item should retain enough information to identify the target deck
and/or created card.

## Deferred / Nice-To-Have

See `docs/transcript-vocabulary-analysis-nice-to-have.md`.

## Draft Data Model

### VocabularySource

- `id`
- `title`
- `description`
- `rawText`
- `cleanText`
- `analysisVersion`
- `createdAt`
- `updatedAt`
- `lastAnalyzedAt`

### VocabularySourceItem

- `id`
- `sourceId`
- `foreignWord`
- `nativeWord`
- `originalText`
- `partOfSpeech`
- `cefrLevel`
- `confidence`
- `category`
- `status`
- `sourceExample`
- `explanation`
- `knownMeaningsSnapshot`
- `alreadyExists`
- `occurrencesJson`
- `createdCardId`
- `targetDeckId`
- `firstOccurrenceOrder`
- `createdAt`
- `updatedAt`

`createdCardId` and `targetDeckId` are nullable. When displaying added state,
Klaf should validate the linked card against the current card database.

### VocabularySourceItemOccurrence

Stored in `occurrencesJson` for the MVP.

- `timestamp`
- `startOffset`
- `endOffset`
- `sentence`
