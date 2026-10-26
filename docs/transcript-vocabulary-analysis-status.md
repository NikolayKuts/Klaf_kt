# Transcript Vocabulary Analysis Status

Working status file for the `VocabularySource` feature.

## Current Focus

- Revised-policy live validation is still pending: first real attempt failed with
  operator-reported provider `serverOverloaded`; one unchanged retry failed after
  902.5 seconds with operator-confirmed `RequestTimeout`. No analysis was returned
  in either attempt, so no standalone controls were evaluated. Bounded server
  text-timeout override(default15min, next experiment30min) is implemented;
  independent review/test verification passed. Await remote update and full
  process restart with the explicit30min override before another real run.
- Implemented agreed whole-text completeness policy for standalone content words
  and an opt-in six-word coverage evaluation. Independent offline tests/build and
  review passed. The old real report fails all six controls as expected; a fresh
  real-model GREEN requires remote deployment. No new model run claimed yet.
- Simplified comparison: local opt-in authenticated Desktop test collects both
  remote Analyze responses. Operator only changes the remote mode/restarts;
  transcript/result transfers and remote paths are optional standalone fallback.
  Independent offline verification passed (23 passed, 1 remote opt-in skipped).
  First attempt failed during authenticated connection setup with account HTTP
  502 (`HTTP_ERROR`). After operator startup, the 2026-10-26 real chunked run
  succeeded: 379 items, 490 occurrences, 1,149.5 seconds request time.
  A declared whole-text attempt was stopped after operator observed chunk1/11
  and chunk2/11: the serving process was still chunked. After correcting the
  remote environment setting, a valid whole-text run completed: 162 items,
  272 occurrences, 374.8 seconds. Operator confirmed the whole-text start log.
  Comparison is complete for this transcript: 3.07x faster, but a much narrower
  final vocabulary selection. Default remains chunked; no automatic mode change.
  The protocol still does not advertise effective mode before Analyze.
- Server-only chunked/whole-text comparison, opt-in JSON reports and standalone
  file-input startup are implemented. Independent code review has no remaining
  findings; independent post-fix test/build verification passed.
  Initial real remote comparison is complete; revised-policy evaluation pending.
  Source snapshot verified at `C:/Users/kutsn/.codex/experiments/klaf-vocabulary-comparison-2026-10-25`:
  14,189 text characters, 382 items, 463 occurrences. Phone is no longer required.

- Global semantic reconciliation and retry are implemented using request-scoped
  read-only AgentDriver data tools and in-memory checkpoints. Independent final
  review and regression/build verification passed; no database cleanup/migration.
- Local direct-owner smoke remains unavailable because `KLAF_CODEX_OWNER_HOME`
  is not configured. Remote authenticated tests above verified the 14,189-character
  input; larger-input/provider capacity is not established by those samples.

- User reports completing Android cleanup and manual synchronization. Prepare
  the normal client update for installation over the existing app.
- Mandatory test-first development is now recorded in root/server AGENTS.md.
- Device cleanup/sync completion is user-reported; the agent has not inspected
  the working phone database. The normal Clear analysis action remains source-only.
- Manually verify Total/Ignored counter wrapping at narrow widths on a device.
- Manually check Ignore/Restore enabled states for pending, ignored, and mixed
  selections on a device.
- Manually verify the pinned item review toolbar with a long list on Android,
  including scrolling back to the source fields and both light/dark themes.
- Manually smoke-test item target selection on Android with inflected words,
  repeated words, and discontinuous expressions.
- The user reports that external YouTube opening works in a manual check;
  platform-specific Android/Desktop checks were not separately recorded.
  Verify URL-field layout at narrow widths and with larger text scaling.
  iOS and timed playback remain deferred.
- 2026-09-28 staged/unstaged audit complete. Domain/data/presentation regression
  tests and Android/Desktop compilation pass. Full audit details and remaining
  audio lifecycle gaps are recorded in `audio-transcription-status.md`.
- Manual Android smoke test of the target-deck chooser with enough decks to
  require scrolling.

## Completed

- New-policy live attempt reports preserved: first provider overload after293.4s
  (`remote-whole-text-b2e66e7b-c4a7-4896-8911-dc7a5123d762.json`), then confirmed
  timeout after902.5s (`remote-whole-text-915d7fa3-f978-4bfb-83d8-865fe7c57829.json`).
  Logs `klaf-remote-vocabulary-standalone-live-20261026.log` and
  `klaf-remote-vocabulary-standalone-retry1-live-20261026.log`. SDK code confirms
  provider failures trigger a best-effort `turn/interrupt`, so the operator's
  additional JSON-RPC-32600 interrupt error may be cleanup fallout, not the root
  provider failure. No automatic/unbounded retry or model-quality claim.
- Configurable account-runtime one-shot text deadline added test-first. Command
  `:klaf-server:test --tests '*KlafServerConfigTest*TextTimeout*' --console=plain`
  RED:3tests,2expected failures for ignored override/invalid values; then
  `:klaf-server:test --tests '*KlafServerConfigTest*' --tests
  '*ManagedAccountCodexRuntimeFactoryTest*' --tests '*AccountCodexRuntimePoolTest*'
  :klaf-server:assemble --continue --console=plain` GREEN36s. Logs
  `klaf-text-timeout-config-{red,green}.log`. Default15minutes retained; explicit
  env override1..60validated in config/direct constructor and wired to SDK total.
  Effective numeric deadline is logged on factory construction. Independent final
  review has no findings; scoped server105 discovered,104 passed,1 live smoke
  skipped, zero failures/errors; server assembly passed52s. Exact filters covered
  config/factory/pool/profiles/vocabulary/controlled routing; log
  `klaf-text-timeout-independent-final.log`. The actual30-minute-budget model run
  still requires deployment; no model-completeness improvement is claimed yet.
- Whole-text standalone coverage policy now explicitly requires all independently
  meaningful content-word senses, including basic/frequent vocabulary, independent
  words inside compositional phrases, and full transcript coverage. It states
  explicit grammatical/name/noise/opaque-idiom exceptions and contains no corpus-
  specific target words. Chunked instructions, SDK, schema and default unchanged.
- TDD evaluator RED/GREEN: `:di:desktopTest --tests
  '*StandaloneVocabularyCoverageTest*' --rerun --console=plain` failed expected
  missing evaluator behavior (2 failures), then passed after implementation.
  Logs `klaf-standalone-coverage-evaluator-{red,green}.log` in host TEMP.
  Before instruction changes, `:di:desktopTest --tests
  '*StandaloneVocabularyCoverageTest.evaluateRecordedRealResponseForStandaloneWordCoverage'
  --rerun --console=plain`, with `KLAF_VOCABULARY_COVERAGE_REPORT` set to the
  preserved real whole-text report, failed all six real controls. This was a
  replay evaluation, not another provider call (`klaf-standalone-real-baseline-red.log`).
- Review regression RED/GREEN: the focused punctuation and wrong-POS tests failed
  before the evaluator fix (`klaf-standalone-review-red.log`) and full DI passed
  afterward (`klaf-standalone-review-green.log`). Recorded baseline was rechecked
  after normalization: still expected RED all six (`klaf-standalone-real-baseline-recheck-red.log`).
  Evaluation tolerates boundary punctuation/whitespace, rejects larger phrases,
  and reports wrong POS distinctly. Semantic meaning accuracy remains manual.
- Independent final verification: DI31 discovered (29 passed,2 opt-in skips),
  server102 discovered (101 passed,1 live smoke skip), zero failures/errors;
  server assembly passed. Final review has no remaining findings. Logs
  `klaf-standalone-policy-independent-final.log` and
  `klaf-standalone-policy-independent-review-fix.log`. Full DI was actually rerun;
  server filters covered vocabulary, config, profiles, account runtime and routing.
  The semantic acceptance of the revised model is still pending the next remote
  request; offline GREEN is not proof of NLP completeness.
- Contextual follow-up audit of the full transcript and both item inventories:
  whole-text retains advanced idioms/constructions (`jump through hoops`,
  `from the ground up`, `have a problem licked`, `hold office`, `go to the dogs`)
  and contextual senses (`set` for the sun, `count` as matter). A smaller list
  alone does not establish worse learning value; many differences are basic
  literal words, sound/music annotations or equivalent dictionary forms.
  Useful whole-text gaps remain: `button` meaning campaign badge, `tell` meaning
  discern, `get something done`, indirect request `I don't suppose...`, and
  intentional `mean to`. Both outputs omit transport sense of `take` in
  `take me to the park` (both retain `take sides`). Recommendation, not yet
  implemented: refine whole-text selection for contextual senses/constructions
  and rerun semantic control examples; do not target the chunked item count.
  Whole-text input contains no learner vocabulary/level, so selection is not
  personalized knowledge filtering. Private comparison report updated.
- 2026-10-26 valid real whole-text request passed after remote environment fix:
  same 14,189-character transcript/hash, 162 items, 272 unique occurrence spans,
  374.8 seconds request time (Gradle6m25s). User confirmed
  `Vocabulary whole-text extraction started` and no sanitizer-change log.
  Report `remote-whole-text-4a038759-3ca8-4f15-99d7-83532eeab538.json` is in the
  same private local results directory as the chunked report. Log:
  `klaf-remote-vocabulary-whole-text-retry-20261026.log`. No Room/Save/sync changes.
- Real comparison: whole-text is3.07x faster but returns162 vs379 items and272
  vs490 occurrences. It lacks final units such as `What do you mean?`, `get off`
  and `on the way`; other differences are canonical naming variants. It also
  finds useful units missing from chunked (`cooperation`, `deliver`, `weed`).
  For142 normalized forms shared by both, whole-text has250 vs191 occurrences;
  `science project` has all3 literal mentions vs2 in chunked. Occurrences span
  all text quarters, so this is not simply an output cut off near the beginning.
  Neither output is ground truth. Current one-pass instructions are not equivalent
  in extraction scope; retain normal chunked behavior pending completeness work.
  Private detailed report/metrics saved beside the fixture as `comparison.md` and
  `comparison-metrics.json`; no private transcript/result was committed.
- 2026-10-26 declared whole-text attempt was invalidated by operator-observed
  chunk logs. Local test worker41164 was stopped intentionally after3m11s, not a
  spontaneous build/runtime failure; server stop was requested to cancel its work.
  Log `klaf-remote-vocabulary-whole-text-live-20261026.log`. No whole-text report
  collected; the first successful chunked report is preserved. Investigation of
  effective remote environment configuration is pending.
- 2026-10-26 real remote chunked collection passed via the existing Desktop login:
  14,189-character input SHA-256 matched the preserved source; 379 items and 490
  occurrences (490 unique spans), all structural client checks passed. Operator
  confirmed server chunk processing (9/11 during the run); protocol itself does
  not echo mode. Client request took 1,149.5 seconds; Gradle run 19m23s. Report:
  `C:/Users/kutsn/.codex/experiments/klaf-vocabulary-comparison-2026-10-25/remote-results/remote-chunked-26d8586b-7f9e-4a7a-ab00-5c5396f0a1dd.json`.
  Command `:di:desktopTest --tests
  '*RemoteVocabularyComparisonTest.collectRemoteAnalysisUsingExistingDesktopLogin'
  --rerun --console=plain`, opt-in true, declared mode chunked; log
  `klaf-remote-vocabulary-chunked-live-20261026.log`. No Room/Save/sync changes.
  Semantic quality comparison remains pending the whole-text sample.
- Added opt-in local `RemoteVocabularyComparisonTest` using the standard Desktop
  protected-session signer and existing selected account; it does not open Room,
  save items or synchronize. Diagnostic recorder RED failed before implementation
  (`klaf-remote-vocabulary-report-red.log`); GREEN and independent actual rerun
  `:di:desktopTest --rerun --console=plain` passed 23 tests with one remote skip
  (`klaf-remote-vocabulary-report-green.log`,
  `klaf-remote-vocabulary-independent-final.log`). Independent review has no
  remaining findings. Invalid/empty/error responses are retained before assertions;
  tokens are excluded. Test-only JSON dependency added to DI desktop tests.
- Real opt-in command used `:di:desktopTest --tests
  '*RemoteVocabularyComparisonTest.collectRemoteAnalysisUsingExistingDesktopLogin'
  --rerun --console=plain` with local fixture paths and declared chunked mode.
  It failed HTTP 502 in account authorization setup (9s), not in model extraction;
  log `klaf-remote-vocabulary-chunked-live.log`. No remote quality result is claimed.
- Comparison experiment TDD: tests were written before behavior. Minimal API
  scaffolding allowed executable RED assertions: mode parsing/whole-text routing/
  exports failed (3/4) in `klaf-vocabulary-experiment-red.log`; file-input startup
  failed (2/3) in `klaf-vocabulary-file-experiment-red.log`; variable-form span
  retention failed (1/1) in `klaf-vocabulary-variants-red.log` (host TEMP logs).
  Commands were `:klaf-server:test --tests '*VocabularyAnalysisExperimentTest*'`,
  `:klaf-server:test --tests '*VocabularyAnalysisFileExperimentTest*'`, and
  `:klaf-server:test --tests
  '*VocabularyAnalysisExperimentTest.wholeTextPreservesDifferentInflectedOccurrenceSpans'`.
  Each passed after implementation. Final scoped command passed:
  `:klaf-server:test --tests '*VocabularyAnalysis*ExperimentTest*' --tests
  '*VocabularySource*Test*' --tests '*AccountCodexInstructionsTest*' --tests
  '*KlafServerConfigTest*' --console=plain` (`klaf-vocabulary-experiment-final-green.log`).
  Run Configuration instructions: [vocabulary-analysis-comparison.md](vocabulary-analysis-comparison.md).
- Review follow-up RED: `:klaf-server:test --tests
  '*VocabularyAnalysisExperimentTest.wholeTextRepairsOffsetAgainstExactOccurrenceSurfaceInsteadOfAcceptingUnrelatedWord'
  --tests '*VocabularyAnalysisFileExperimentTest.bothCloseAttemptsRunAndOriginalAnalysisFailureRemainsPrimary'`
  failed both assertions (`klaf-vocabulary-review-fixes-red.log`). Whole-text now
  requires per-occurrence exact `surface_text`, and cleanup always attempts both
  pool/factory closure while preserving primary errors. The same scoped suite
  passed (`klaf-vocabulary-review-fixes-green.log`); independent review rechecked
  both fixes and reports no remaining actionable findings.
- Independent post-fix command: `:klaf-server:test --tests
  '*VocabularyAnalysis*ExperimentTest*' --tests '*VocabularySource*Test*' --tests
  '*AccountCodexInstructionsTest*' --tests '*KlafServerConfigTest*' --tests
  '*AccountCodexRuntimePoolTest*' --tests '*ManagedAccountCodexRuntimeFactoryTest*'
  --tests '*ControlledSiwcFeatureRoutingTest*' --tests
  '*VocabularyDataToolsLiveSmokeTest*' :klaf-server:assemble --continue --console=plain`.
  Passed in 51s: 102 discovered, 101 passed, zero failures/errors, one opt-in
  real-model smoke skipped. All 10 new experiment tests passed. Log:
  `klaf-vocabulary-experiment-independent-final.log`. No real provider requests,
  device access or working database changes; remote comparison remains pending.
- Global reconciliation receives the full transcript and every valid chunk item
  in one model pass, without host-side grouping. The host validates complete,
  unique source-item coverage and reconstructs all original occurrences.
  Genuine different meanings remain independent by prompt instruction.
- Failed reconciliation retains successful extraction in bounded server RAM
  (30-minute TTL, until restart). Generic Retry analysis reuses extraction only
  for the same authenticated account, source and exact text. Successful Clear
  analysis removes client retry state. Failures preserve previous analysis.
- Test-first verification: semantic merge/coverage, checkpoint binding/expiry/
  capacity/concurrency, protocol token handling, repository retry mapping, and
  ViewModel retry/text-change/reset tests failed for the missing behavior before
  implementation and passed afterward. Valid RED logs are in the host TEMP
  directory: `klaf-reconciliation-red.log`,
  `klaf-reconciliation-retry-valid-red.log`,
  `klaf-analysis-repository-red.log`, `klaf-reconciliation-retry-red.log`,
  `klaf-no-premerge-red.log`, and `klaf-clear-retry-red.log`.
- Focused RED/GREEN commands used `:klaf-server:test --tests
  '*VocabularySourceReconciliationTest*'`, `:data:desktopTest --tests
  '*VocabularyAnalysisRetryRepositoryTest*'`, `:klaf-server-contract:jvmTest
  --tests '*VocabularyAnalysisRetryProtocolTest*'`, and
  `:presentation:desktopTest --tests '*VocabularySourceDetailViewModelTest*'`.
  RED runs used the pre-implementation behavior; tests failed on the expected
  behavioral assertions. Compilation/setup failures were corrected and were
  not counted as valid RED results.
- Targeted GREEN command: `:klaf-server:test --tests '*VocabularySource*Test*'
  --tests '*AccountCodexInstructionsTest*' :presentation:desktopTest
  --tests '*VocabularySourceDetailViewModelTest*' --continue --console=plain`
  passed after review fixes (`klaf-reconciliation-review-fixes-green.log`).
  Earlier targeted contract/data/server/presentation suites also passed
  (`klaf-reconciliation-targeted-green.log`). Independent code review rechecked
  both fixes and reported no remaining actionable findings.
- Independent final SDK JVM regressions passed: 424 tests, zero failures/errors/
  skips. Independent Klaf regressions and Android debug unit/build checks passed
  in 9m14s: domain 53, data 233, presentation 91, contract 15, Android 6, server
  513 discovered (911 total), zero failures/errors. Three server tests skipped:
  opt-in real-model data-tools smoke, Linux synthetic secret-service integration,
  and opt-in manual desktop import smoke. Combined: 1,332 passed, 3 skipped.
  `:Android:assembleDebug` passed; no phone/emulator installation, working database
  access, remote publication or deployment was performed by verification.
- Independent final Gradle command (live-model flag false):
  `:domain:desktopTest :data:desktopTest :presentation:desktopTest
  :klaf-server-contract:jvmTest :Android:testDebugUnitTest :Android:assembleDebug
  :klaf-server:test --continue --console=plain`. APK:
  `apps/Android/build/outputs/apk/debug/Android-debug.apk`.
- Removed temporary startup cleanup, receipt/backup handlers, maintenance UI,
  temporary dependencies and maintenance-only tests after user confirmation.
  Permanent source review behavior and server submodule revision are unchanged;
  Room remains version16. Existing private backup/receipt files are inert.
- Test-first removal: isolated emulator startup regression failed before removal
  with `Launching the app must preserve analyzed items expected:<1> but was:<0>`.
  Command: `:Android:connectedRemoteStorageTestAndroidTest
  -PklafAndroidTestBuildType=remoteStorageTest
  -Pandroid.testInstrumentationRunnerArguments.class=com.kuts.klaf.VocabularySourceStartupPersistenceTest`.
  The same command passed after removal: 1 test, no failures/skips, covering
  launch and Activity recreation. Independent final verification passed data
  Desktop231, Android unit6, and startup instrumentation1: zero failures/errors/
  skips. Independent review found no actionable issues. Normal debug APK built;
  signature matches maintenance APK, ZIP alignment verified, package
  com.kuts.klaf1.5/code15. Its21 DEX entries contain no temporary cleanup classes
  or paths. Tests used an isolated read-only emulator; working phone untouched.
- Corrected Clear analysis to preserve global Ignored Words; one-time legacy
  cleanup permission is not a permanent global-clear feature. Independent review
  found no issues; data Desktop (231) and presentation Desktop (89) tests and
  Android/DI compilation passed. No device UI check or global cleanup performed.
- Persist all valid analyzed items, including ignored and exact Card matches.
  Saved ignored items retain source metadata; globally saved Ignore/Restore is
  keyed by language/word/meaning. Different meanings keep the new-meaning badge.
- Live Card/ignored-rule observation updates candidate visibility across sources;
  deleted Cards release ADDED, moved/edited linked Cards retain provenance,
  and stale/repeated Add selections cannot create duplicate Cards.
- Added confirmed Clear analysis: clears only current source items, preserving
  global ignored rules. Source fields/transcript/URL, Cards/Decks and other sources'
  items are preserved. Existing Room schema suffices; no destructive migration.
- Added ignored-rule removal operations and deletion deltas across client/server
  sync, with conflicts for stale concurrent Add/Remove decisions.
- The two-device regression exposed a pre-existing source-only account bootstrap
  bug: a populated vocabulary store was treated as empty and bypassed deletion
  deltas. Coordinator/applier now require empty source/item/ignored tables for
  initial snapshots. Established source-only accounts use incremental sync.
- Independent review found and verified fixes for orphan ADDED rows surviving
  empty reanalysis and stale inherited ignores resurrecting restored rules.
  Final review has no remaining actionable findings.
- Passed domain Desktop (53), data Desktop (231), presentation Desktop (89),
  shared contract JVM (14) and Android debug unit (6) tests; Android app/data/DI
  compilation and server compilation pass. The full 505-test server run found
  the source-only bootstrap bug above (two unrelated tests skipped). After its
  fix, the failing Restore roundtrip passed individually, then all 119 affected
  source/manual-coordinator/protocol/contract server tests passed. Unrelated
  server suites were not repeated. Final independent review has no findings.
- Added Ignored alongside Total in the pinned review toolbar, counting ignored
  items independently of selection. Independent review found no actionable
  issues; Android/Desktop presentation compilation and 87 Desktop tests passed.
  Device rendering/wrapping checks were not performed.
- Ignore now requires a selected pending item; Restore requires a selected
  ignored item. Mixed selections enable both, empty selections enable neither;
  analysis locking is preserved. Independent review found no actionable issues.
  Android/Desktop presentation compilation and `:presentation:desktopTest`
  passed (87 tests), including the selection-state regression matrix. Device UI
  checks were not performed.
- Pinned the complete item review toolbar within the list viewport using a
  sticky section header with an opaque themed background. Independent review
  found no actionable issues; `:presentation:compileDebugKotlinAndroid` and
  `:presentation:desktopTest` passed (86 tests, no failures). Device scrolling
  and visual checks were not performed.
- Vocabulary Source detail now shows `Total` above `Selected` for the review
  list. Independent review found no issues; `:presentation:desktopTest` and
  `:presentation:compileDebugKotlinAndroid` passed. Manual UI check is pending.
- Item target offsets are now server-owned and validated against `original_text`,
  the actual surface form in the cleaned transcript, never `foreign_word` (the
  normalized dictionary form). Ambiguous or missing surface matches are omitted;
  the client uses returned offsets directly, with no saved-item relocalization.
  The server prompt requires the full surface span for discontinuous expressions.
  Focused server regression tests and Desktop/Android client compilation pass;
  independent review found no remaining actionable issues. Android device
  smoke check remains pending.
- Added an open-video action beside the source URL field for Android/Desktop.
  It uses the current unsaved URL, is disabled for invalid HTTPS YouTube links,
  and preserves the existing copy action. URL validation tests passed on
  Desktop; `:presentation:desktopTest`, `:Desktop:compileKotlin`, and
  `:Android:assembleDebug` passed. A follow-up review added coverage and
  support for explicit HTTPS port `:443`, while rejecting other ports.
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
- Initial global Ignored Words implementation (superseded by persistent ignored
  items and live review state described above):
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
- Data is stored locally in Room and uses the existing manual account sync.
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
- `pending`, `added`, and `ignored` are persistent source-item states. Saving
  ignored items retains them in their source; global Ignore/Restore rules and
  current Cards determine effective review state.
- Ignored Words are global across Vocabulary Sources and include the analysis
  language, foreign word, and Russian meaning. Exact ignored pairs remain in
  later analysis results as ignored items; other meanings stay independent.
- Added state follows the linked card's current existence and deck.
- MVP verification targets Android.

## Deferred / Moved Out Of MVP

- API-based subtitle fetching.
- File import for `.srt`, `.vtt`, and `.txt`.
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
