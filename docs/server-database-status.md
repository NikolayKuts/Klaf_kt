# Server Database — Implementation Status

## Phases

| # | Phase | Status | Notes |
|---|-------|--------|-------|
| 0 | Requirements gathering | ✅ Complete for MVP | Deferred features have separate follow-up decisions. |
| 0.5 | Test-first scenario suite | ✅ Focused executable coverage | The 31 setup-only client umbrella cases and obsolete in-place migration placeholders were mapped to real Room/REST/domain tests and retired. Desktop/server regression passes; device acceptance remains under phase 7. |
| 1 | Database technology & schema design | 🔄 In Progress | Room Multiplatform on server; client schema v16/server v7 now include synchronized source/item identities and feature revisions alongside review summaries and outbox/checkpoint/conflicts. |
| 2 | Server-side DB module & repository layer | 🔄 In Progress | Account/device, deck/card, revision, accepted-change history, and payload fingerprints are persisted. |
| 3 | REST synchronization protocol | 🔄 In Progress | Server push/pull, recent-history REST read, shared Android/Desktop HTTP transport, local outbox/checkpoint, durable conflicts, safe disjoint deck-add partial apply with an account-scoped edit pause, bulk and per-conflict deck/card edit resolution, server-side `/sync-events`, and a shared client event observer exist. The conflict route runs in ordinary Room/REST mode by default and in isolated account test mode; explicit legacy recovery remains available. |
| 4 | Client-side integration (replace Firestore) | 🔄 In Progress | Scoped Room/reminders, passwordless account UI, and Room/WebSocket sync are now the ordinary Android/Desktop defaults and remain available in isolated test configurations. Existing legacy-database installations still require a guarded clean cutover. |
| 5 | Direct Firestore migration | ⏸ Deferred | A manual Android Room export bootstrap creates the initial server data. |
| 6 | Authentication & security | ⬜ Not Started | |
| 7 | Testing & verification | 🔄 In Progress | Desktop Room and live server tests run; isolated Android account flows, two-client rename and mixed deck/card conflicts, offline manual retry, recent-history status UI, structural-conflict choices, both reviewed-destination retarget choices, and both review-versus-card-removal choices were checked against disposable local servers. Ordinary-app Android account switching, isolated Desktop account UI sign-in/registration, first pull, offline restart, and rendered data separation passed. Final regression and shared-screen action-label tests passed; default-launch rollout and primary-phone cutover remain separate. |

## Current Focus

- 2026-10-08 Linux Tunnel client cutover implemented locally. Android and
  Desktop now share the existing ignored client
  `SecretConstants.KlafServer`; Linux Server uses its separate ignored
  `SecretConstants.Server.PUBLIC_ORIGIN`. A common origin builder omits default
  HTTPS port 443 and supplies the REST/DPoP origin and WSS URLs consistently.
  RED-then-GREEN endpoint tests, Android/Desktop compilation, focused server
  configuration tests, and a separate full `:klaf-server:test` run passed.
  An earlier duplicate concurrent full run was cancelled to avoid competing
  test workers. Git-tracked files and both Git histories did not contain the
  real hostname. The current public HTTPS route validates TLS but returns 502,
  so target-Linux Tunnel and end-to-end checks remain blocked. Push remains
  operator-controlled. The later data move must preserve Klaf account/deck/card/image/
  sync content; Codex internal history is excluded, and re-sign-in is
  acceptable.

- 2026-10-07 post-import review complete: removed the temporary
  existing-account Android-backup importer, its Gradle task, storage hook and
  importer-only tests. Kept the compact display of the persisted import
  history. Reviewed the uncommitted auth-recovery diff, extended its UI test
  through password reentry, and applied the local Kotlin declaration-wrapping
  rule to changed code. `:klaf-server:test :data:desktopTest
  :presentation:desktopTest` passed: 732 tests, zero failures/errors, two
  skipped. `git diff --check` passed in both repositories. Recovery backups
  and live data were not changed. The operator subsequently authorized local
  commits; push remains pending.

- 2026-10-07: completed a one-time legacy Android backup import into the current
  password-authenticated account. The operator supplied a path with an extra
  space; the matching protected `klaf_backup` contains the known 35-file
  Room/WAL/SHM export and 19 saved PNGs. All 35 original files still match
  the 2026-09-28 SHA-256 manifest; no review DataStore file is present. The
  live server has one registered/authenticated account and device, one empty
  interim deck, zero cards and images, and revision 1 before import. The old
  bootstrap creates a new account and was not used. A new guarded offline
  existing-account importer passed synthetic TDD coverage, the full server
  test suite (83 suites, zero failures), and a disposable snapshot trial with
  all 57 decks, 1093 cards and 19 PNGs. The server was closed normally; a
  full pre-import copy of its storage was SHA-256 checked outside the repo.
  Live import produced revision 2, 57 decks, 1093 cards and 19 image files;
  SQLite integrity and foreign keys passed, image hashes matched, and the
  existing auth account and device remain. Server restarted successfully.
  Android manual sync advanced the device's last-confirmed revision from 1 to
  2, and imported decks appeared in the Android list. A particular saved PNG
  has not yet been visually opened on the device; server copies were hash-
  verified. The import history UI was bounded with a test-first regression;
  that display change requires a new client build to appear on the device.

- 2026-09-29 pre-commit review: corrected stale source conflict snapshots after
  later accepted operations in one batch; added update/deletion regressions and
  refactored source/item mapping to named arguments. Review regression: 555
  total, 554 passed, one optional smoke skipped, no failures/errors. Scoped
  staged privacy/whitespace checks pass; user authorized the local snapshot.
  Details are in `klaf-server-status.md`; no live data/backups were modified.

- 2026-09-29: Vocabulary Source cross-device synchronization implemented using
  TDD. Source/analysis aggregates and Ignored Words now use transactional outbox,
  manual REST push/pull/bootstrap, revisions and durable conflict choices.
  Existing local sources survive migration and become pending first uploads;
  upgraded clients also receive a missing feature baseline even when the global
  revision was already confirmed. Portable links and deletion cleanup are
  covered by real Room/live REST regressions. Final regression: 553 total,
  552 passed, one existing optional smoke skipped, no failures/errors. Android
  debug APK and Desktop compilation passed; details and deployment instructions
  are maintained in `klaf-server-status.md`. Real device acceptance
  remains separate; no protected backup or live account was modified.

- Branch integration into `mnemonic-voice-dictation` is complete and validated
  locally (2026-09-29). Client schema v15 migrates both historical branch
  layouts, preserving deck/card/review/sync data. Guest sign-up now transfers
  sources/items/Ignored Words as well; sign-in does not transfer guest data.
  Device-session cancellation and source-to-card edit restrictions are integrated.
  Current validation/merge state is recorded in `klaf-server-status.md`; the
  device checks below belong to the earlier storage branch, not a merged build.
  Cross-device source synchronization was subsequently implemented in the
  follow-up above; merge-only validation remains a historical checkpoint.

- 2026-09-28 expanded pre-commit privacy audit completed: inspected all 840
  indexed paths / 780 text files, including all 17 Markdown files and README.
  Removed remaining personal Windows home paths, the physical-device serial
  and ordinary account DB identifier from five documents, replacing them with
  generic placeholders. Original machine-specific notes were copied only to
  ignored `.local-notes/precommit-privacy/`; none are indexed. README documents
  the local-only configuration policy. No personal email, private key, provider
  token, credential URL, non-placeholder home path or LAN address remains in
  the audited staged text. Three credential values read from actual ignored
  local config were additionally checked for accidental copying: no matches.
  Broad matches were manually reviewed: 16 email-like Kotlin constructs,
  10 UI variable/method assignments and 16 password UI labels are not secrets;
  321 synthetic email occurrences use reserved test/example domains. Actual
  `local.properties`, Firebase Android JSON and server service-account JSON
  are ignored, untracked and absent from reachable history at those paths.
  Required public certificates, test fixtures and Room schema history remain.
  No cache/database/build/credential-container files or private notes are in
  the index. All new/modified files are text; 60 unchanged legacy binary assets
  were not content-audited, and complete historical content was not rescanned
  or rewritten. Staged whitespace validation passes. Runtime config, protected
  backups and live account were unchanged. No product-code edits/test rerun,
  commit, push, merge or deployment was performed.
- 2026-09-28 pre-commit preparation completed: removed 14 tracked generated
  files from the index (six Kotlin/JVM cache/crash files and eight additional
  Gradle-home cache files), keeping all local copies ignored. Added nested
  Kotlin-cache/JVM-crash ignore rules; removed the three already-retired
  staged-only test additions. Staged the reviewed current implementation,
  schema history, tests and documentation; preserved AGENTS.md instructions
  while removing its final redundant blank line. Removed the personal account
  email from two documents without changing runtime settings/account data.
  Credential-pattern audit covered all 840 indexed paths (780 text files;
  60 binary files excluded from content scanning): no private-key/provider
  token matches. Ten broad assignment candidates were inspected and are UI
  variables/method references, not embedded credentials. Real Firebase config,
  `klaf-server/secrets/firebase-service-account.json` and runtime SecretConstants
  files are ignored and absent from the index. Checked sensitive paths also
  have no entries in reachable local Git history. Synthetic test
  Google-services JSON is retained; the three
  tracked PEM files are public certificates, not private keys. No generated
  cache, database, APK, crash log or local.properties remains in the index;
  staged whitespace validation passes. This is an index/text-pattern audit,
  not a full historical or binary-archive secret audit. No production code
  changed, so previous passing regression remains applicable; tests were not
  rerun for Git/documentation-only cleanup. No commit, push, merge, deployment
  or protected backup/live-account modification was performed.
- 2026-09-28 full uncommitted-worktree review completed in
  `Klaf_kt_remote-storage` / `remote-storage-server`: staged, unstaged and
  untracked work relative to HEAD included persistence/migrations, account and
  guest isolation, REST/WebSocket/retry/conflicts, image lifecycle, client
  UI/reminders, DI/build configuration and coverage. Five defects reproduced
  by failing tests and fixed: preserve a moved card's local ID before its old
  deck is cascade-deleted; reject a delta retaining a card in a deleted parent;
  reconnect after the WebSocket connector's own timeout without swallowing
  caller cancellation; discard saved image bytes if the account changes during
  disk read; reject unsafe local image import IDs; and attempt manual Room/REST
  sync even without an Android active network (USB reverse can be reachable).
  The deleted-parent validation is part of the identity/ordering fix. Eight
  regression tests added, including a real Room/live-REST move-and-delete case.
  Small refactors moved the DeckListViewModel companion to the class top and
  made RoomSyncStatusObserver's coroutine opt-in explicit. Instrumented
  conflict/reminder tests now refuse ordinary-package execution before activity
  launch/test-body writes. Final regression: 122 data + 64 presentation + 191
  server + 18 DI + 30 domain + 6 Android unit tests pass (431, zero failures;
  one existing opt-in server smoke skipped). Android debug assembly and Desktop
  compilation pass; three production conflict-screen tests also pass on the
  secondary physical RMX2001 in the isolated package (434 tests including
  device checks). Reminder acceptance tests compiled but were not rerun.
  All 35 protected backup files still match the saved SHA-256/length manifest.
  Real Android Firebase config and server secrets are ignored; the isolated
  Google-services fixture contains dummy credentials. Scoped whitespace check
  is clean; HEAD-wide check only flags an existing AGENTS.md EOF blank line,
  left unchanged to preserve user instructions. No commit, ordinary APK
  deployment, ordinary server restart/account mutation or primary-phone cutover
  was performed. Image conflict previews remain deferred. Review details and
  commands are in `docs/server-database-test-coverage.md`.
- 2026-09-28 user reports the image appeared on Android after synchronization.
  Image-specific conflict details and local/server previews are explicitly
  deferred for future implementation. No code or conflict-resolution behavior
  changed; whole-card choices remain. Independent per-field resolution is not
  agreed. This report confirms Android display, not every remaining two-device
  or conflict acceptance scenario.
- 2026-09-28 new-image synchronization and actionable errors implemented.
  Shared Android/Desktop manual sync uploads referenced saved images before
  publishing pending card metadata; drafts are excluded and duplicate IDs in
  a batch upload once. Account/device-scoped REST PUT validates identifiers,
  image signatures and the 16 MiB limit. Asset IDs are immutable: equal-byte
  retries succeed, different bytes are rejected without replacing the file.
  Failed uploads preserve the outbox and do not publish the metadata batch;
  a real Room/live-server test verifies retry after a lost successful reply
  and download through a second device's offline-persistent cache. Missing
  legacy files preserve their references. Unreferenced uploaded files are
  retained; remote image deletion/garbage collection remains deferred.
  Account sign-in/sign-up/registration and REST sync now distinguish connection,
  timeout, missing/duplicate account, device registration, invalid request or
  response, server failure and pending sign-up. Request timeouts release UI
  loading; caller cancellation remains cancellation without an error message.
  Logs contain error categories/types rather than raw account/request values.
  Test-first account tests initially failed on absent failure types/resources.
  Final regression: 117 data + 62 presentation + 190 server + 18 DI + 30 domain
  + 6 Android unit tests passed (423 executed, zero failures; one existing
  opt-in server smoke skipped). Android debug assembly and Desktop compilation
  passed. Ordinary server restarted with GET/PUT routes (launch session 71540);
  all 19 live image downloads match the working-backup hashes. Invalid PUT
  is rejected with HTTP 400 without modifying the account. APK installed with
  `adb install -r` and launched on secondary RMX2001 (`<test-device-serial>`),
  preserving app data and the USB port-8090 reverse mapping. Read-only checks:
  57 decks, 1093 cards, revision 8, SQLite quick_check OK; all 19 server images
  and all 35 protected originals in `C:\Users\<user>\Desktop\klaf_backup` are
  hash-identical. Primary phone untouched. Desktop must be restarted; ordinary
  rendered-image and user-generated-image two-device acceptance remains pending.
- 2026-09-28 existing mnemonic image display implemented for Android/Desktop.
  Account-scoped read-only `/api/v1/images` checks registered devices, current
  card references, safe identifiers, image signatures, file links and a 16 MiB
  size limit. The shared repository lazily downloads missing images into a
  SHA-256-account-scoped cache; cached files work offline, missing/offline images
  leave cards usable, cancellation propagates, and account switches discard
  stale display results. Existing local images and existing UI remain in use;
  network/file work runs on the IO context. Cache imports write a temporary
  file before replacement rather than deleting an existing image first.
  Six new cache/file tests and three live REST tests cover caching/restart,
  concurrent loads, guest/account separation, offline/missing/corrupt responses,
  cancellation, account changes, failed replacement, exact REST bytes, device
  checks, unreferenced files, path rejection and oversized/non-image files.
  Final regression: 114 data + 183 server + 18 DI tests passed (315 executed;
  one existing opt-in server smoke skipped). Android debug assembly and Desktop
  compilation passed. Initial test-first run failed on the absent cache class.
  All 19 PNGs were copied from the hash-verified working backup to
  `C:\Users\<user>\.klaf-server\accounts\<account-database-id>.db.images`;
  all 35 originals remain hash-identical. The ordinary server was restarted
  with the new route (launch session 99876); read-only live downloads of all
  19 referenced images matched working-backup hashes exactly. No account
  creation, DB import, device registration or revision mutation was performed
  by this image deployment. APK installed successfully with `adb install -r`
  on secondary RMX2001 (`<test-device-serial>`), without clearing app data. Android
  and Desktop rendered-image acceptance is still pending with the user;
  Desktop must be restarted to load the new code. At this earlier checkpoint,
  new-image uploads were deferred; the extension above now implements them.
  Remote deletion/garbage collection remains deferred.
  User reports ordinary Desktop/Android sync, restart persistence, rename
  conflict resolution and delete-deck/edit-card resolution passed; offline
  acceptance was paused and is not claimed complete.
- 2026-09-28 ordinary-server bootstrap completed for the user-approved account
  (personal email omitted from the commit).
  All 35 source files in `C:\Users\<user>\Desktop\klaf_backup` were copied
  to `C:\Users\<user>\Desktop\klaf_import_account_20260928\source-copy`;
  SHA-256 manifest is saved beside the copy. SQLite was opened only on copies;
  a checkpointed Room export includes the nonempty WAL. Import created 57 decks
  and 1093 cards at revision 0 in `C:\Users\<user>\.klaf-server`, account DB
  `accounts\<account-database-id>.db`. Registry/account integrity
  checks passed, with zero orphan cards or deck-count mismatches. Card content,
  deck schedules and counts match the checkpointed source; the legacy interim
  deck ID is remapped and its card references follow it. Explicit no-review
  mode was used because this source has no review-info DataStore; latest-review
  summaries are absent, not schedules/counts. All 35 original file hashes were
  checked again after import and are unchanged. Ordinary server is left running
  at `127.0.0.1:8090` (launch session 19469; observed PID 42052). No device is
  registered for this account yet. Next: user signs in through ordinary Desktop,
  confirms device registration if prompted, and manually synchronizes. Do not
  sign up again or rerun the import. Primary phone remains untouched; its clean
  cutover and final two-device acceptance remain pending.
- 2026-09-28 ordinary Desktop clean installation prepared. With no running
  Desktop client and no reparse points, the complete `.klaf_kt` directory was
  moved to
  `C:\Users\<user>\Desktop\klaf_desktop_legacy_backup_20260928_before_room\legacy-original`.
  All four files (DB/WAL/SHM/lock) match their pre-move SHA-256 values; the
  empty mnemonic-images/drafts/saved directory structure was preserved too.
  The backup README contains hashes and the recovery sequence. SQLite was
  opened only on a separate `verification-copy`: `quick_check` passed, legacy
  schema 7, one deck and zero cards. Original backup hashes were checked again
  after new-client startup and remained identical. Nothing was deleted.
  A fresh ordinary `C:\Users\<user>\.klaf_kt` was created, and `:Desktop:run`
  without mode/test-directory flags successfully started the production app.
  Its new guest Room file has schema 14, one interim deck, zero cards, zero
  outbox rows and no sync checkpoint; read-only `quick_check` passed. There is
  no legacy DB or selected account in the new directory. Startup was verified
  through process/log/Room evidence, not a new visual UI acceptance test.
  Desktop is left open for the user (launch session 19788; observed PID 45516).
  Local server was not started; connection-refused logging is expected in
  this offline guest setup. No sign-in, account creation, import or sync was
  performed. Primary phone, `klaf_backup`, and server accounts were untouched.
  Remaining deployment work is the primary-phone/account cutover and final
  ordinary-client two-device acceptance.
- 2026-09-28 ordinary-launch rollout implemented: Android's generated default
  and Desktop's runtime fallback now choose account-scoped Room/REST without
  a mode flag. Explicit legacy recovery and the old-database startup guard
  remain; no migration or deletion was introduced. Configuration tests were
  updated first and failed under the old default, then passed. All 18 DI
  Desktop tests and six Android unit tests passed; ordinary Android assembly
  and Desktop compilation passed without `-PklafClientStorageMode`. A new
  real-DI/Room Desktop test uses ordinary configuration in a temporary root,
  proves shared scoped bindings and atomic review-save repository selection,
  guest/account isolation, guest edits without account outbox entries, and
  account edits with a pending `AddDeck` at base revision 0. It does not open
  the Desktop GUI or the user's real Desktop database.
  The no-flag ordinary APK was installed in place on secondary RMX2001 without
  clearing data. Offline launch retained the existing test-account decks,
  local revision 11 and zero pending uploads. Explicit REST sync against the
  retained disposable server showed green 11/11, devices/history, and zero
  pending; cold restart retained the same decks and synchronized badge.
  Server stayed at revision 11 with one card and 14 accepted history rows;
  SQLite `quick_check` passed. Test server stopped and USB reversal removed.
  The full no-mode-flag regression rerun passed 401 executed tests (server
  180, domain 30, data 108, presentation 59, DI 18, Android unit 6), zero
  failures; one optional manual imported-Desktop smoke was skipped. Ordinary
  Android assembly and Desktop compilation passed again. The actual Desktop
  directory then still contained legacy `klaf_kt.db`; it was inspected by name
  only and left untouched in that rollout turn. Its subsequent recoverable
  clean-install preparation is recorded above. Source backup was untouched.
- 2026-09-28 final review found inconsistent bulk versus per-item keep-local
  eligibility. Five regression tests first failed for moved/unrelated cards,
  deleted/unrelated decks, mismatched local card payload IDs, deleted parents
  in a mixed batch, and blank rename payloads. Shared UI predicates now apply
  the same checks in both modes; structural restoration stays separate.
  A forced rerun passed server (180 passed, 1 optional imported-Desktop smoke
  skipped), domain (30), data (108), DI (15), and presentation (59) tests,
  with zero failures; Desktop compilation and ordinary Room-mode Android
  assembly passed. Three new lightweight Compose instrumentation tests passed
  on physical secondary RMX2001 (`<test-device-serial>`) in the isolated package,
  then passed again after switching to the nondeprecated v2 test runner.
  They render the production conflict screen, verify all eight exact action
  labels and bulk callback dispatch, existing/new retarget choices (including
  blank-name prevention), disabled resolution buttons while working, and
  complete per-item decisions with their correct operation IDs. These are
  screen-level tests with synthetic models, not new end-to-end REST conflict
  runs. The earlier real Room/REST and physical structural-choice checks
  remain the separate integration evidence. No server was started; ordinary
  app data, primary phone, and source backup were untouched.
- 2026-09-28 Android-specific unit suite was also checked. It initially failed
  to compile because the review regression still used the removed split-save
  constructor dependencies. The fixture now uses `SaveCompletedDeckReviewUseCase`
  and explicitly verifies no completed-review result after two of four answers.
  Two transfer tests still assumed reviewed destinations and a new card ID;
  their success fixture now has an unreviewed target, the fake implements an
  ID-preserving upsert, and assertions require the original ID. An additional
  ViewModel test rejects a reviewed target without changing decks/cards/save
  version and verifies negative feedback. All six active Android unit tests
  passed. Commented-out historical tests were not counted as coverage.
  Final block-1 review/regression is complete: 401 executed tests passed
  across these suites and the three device UI tests; one optional manual
  imported-Desktop smoke test was skipped. Existing Gradle/KMP deprecation
  warnings remain; no rollout defaults or primary-phone cutover were changed.
- 2026-09-28 explicit reviewed-deck addition feedback implemented. The
  transaction now raises a typed reviewed-deck rejection; the shared addition
  ViewModel maps it to a message explaining that cards must go into a deck
  with no reviews, while unrelated failures retain their general message.
  Entered data is not cleared. Tests were added first (initially red for
  missing types/helper/resource), then full `:domain:desktopTest`,
  `:data:desktopTest`, `:presentation:desktopTest` and ordinary Room-mode
  Android debug assembly passed. The APK was installed without clearing
  secondary RMX2001 data. A save in the once-reviewed deck rendered the new
  explanation, kept the `test`/`pro` input fields, and retained zero cards,
  local revision 11, and zero pending uploads. Both menu routes use that same
  addition ViewModel; only the deck-list route was physically exercised for
  this message. Server stayed stopped; primary phone and backup were untouched.
- 2026-09-28 reviewed-deck local-write audit passed. Two new real
  account-scoped Room tests invoke the production domain use cases and verify
  that rejected card creation and a stale move into a reviewed destination
  preserve deck/card data, local save version, and the empty account outbox.
  The focused account-outbox suite and full `:data:desktopTest` passed.
  On secondary Android RMX2001, the reviewed deck's Add cards form remained
  accessible, but save attempts with nonempty word fields left card count
  zero, local server version 11, and pending uploads zero. The error event
  itself was not captured; code inspection shows a generic addition error,
  not a specific explanation of the review restriction. Clearer entry-point
  feedback remains a UI follow-up, not a persistence failure. No production
  code, server data, primary phone, or source backup was changed.
- 2026-09-28 reviewed-destination picker corrected after ordinary Android
  RMX2001 exposed a mismatch with the explicit MVP rule. The once-reviewed
  `ordinary_regression_20260927` appeared as a target; choosing it produced
  a generic move error, while the transactional domain guard kept the card
  and outbox unchanged. A focused test was added first and failed on the
  missing selection helper. The transfer ViewModel now offers only other
  decks with `reviewCount == 0`; the transactional guard remains. Focused
  picker tests (2/2), full `:presentation:desktopTest`, and ordinary
  `:Android:assembleDebug -PklafClientStorageMode=room` passed. The APK was
  installed in place on the secondary phone without clearing data. Its
  rendered picker showed only `interim deck` and `guest_switch_smoke`, not
  the reviewed deck. The card remained in `remote_pull_regression_20260927`,
  local server version stayed 11, and pending uploads stayed zero. The
  server, primary phone, and source backup were untouched.
- 2026-09-28 ordinary Android existing-card move acceptance passed on
  secondary RMX2001 against disposable account B. The rendered transfer UI
  moved `guestword`/`testnve` from unreviewed `guest_switch_smoke` to
  unreviewed `remote_pull_regression_20260927`. Before explicit sync, the
  local list showed source/target counts 0/1 and one pending change, while
  server revision 10 still held the card in its source deck. Manual sync
  reached synchronized 11/11 with zero pending. Server Room/SQLite then held
  exactly one card with the original sync ID and word data in the target,
  source/target counts 0/1, and one accepted `MoveCard`. Repeat sync stayed
  at revision 11 with one card and one move; `quick_check` passed. A
  recoverable pre-move server copy is in Local Temp under
  `klaf-card-move-backup-20260928`. No code change was needed. Primary phone
  and source backup were untouched.
- 2026-09-28 ordinary Android deck rename/delete acceptance passed on
  secondary RMX2001 against disposable account B. The previously uploaded
  empty `offline_retry_20260928` deck was renamed through the rendered UI.
  Before manual sync, only the local list changed and one upload was pending;
  server revision stayed 8. Manual sync changed the same server deck to
  `offline_renamed_20260928` at revision 9 with one accepted `EditDeck`.
  Deleting that deck through its confirmation dialog again changed only the
  local list until explicit sync; server revision 9 still held it. Manual
  sync reached synchronized 10/10 with zero pending, removed that one deck,
  and recorded one `DeleteDeck`. Repeat sync stayed at revision 10, with zero
  matching decks and one accepted deletion. The server DB passed SQLite
  `quick_check`; a recoverable pre-delete copy is in Local Temp under
  `klaf-deck-delete-backup-20260928`. No code change was needed. Primary
  phone and source backup were untouched.
- 2026-09-28 ordinary Android offline retry acceptance passed on secondary
  RMX2001 and disposable account B. With the test server stopped and USB
  reverse removed, the rendered app created `offline_retry_20260928`; a
  process restart retained the deck and one pending upload at local revision
  7. The status showed the server unavailable. An explicit offline sync failed
  without clearing the pending change; after the server returned, the error
  remained visible until another explicit attempt. That manual retry reached
  synchronized 8/8 with zero pending changes. Server Room/SQLite held exactly
  one matching deck and one accepted `AddDeck`; `quick_check` passed. Another
  explicit sync left revision 8 and the row counts unchanged. No code change
  was needed. The primary phone and source backup were untouched.
- 2026-09-28 ordinary Android reviewed-card edit/delete acceptance passed on
  secondary RMX2001 against the disposable `mujg1gn4` server. Editing
  `fruit`→`fruit2` in a once-reviewed deck queued two `EditCard` operations:
  the editor saved valid automatic Word Insights, then the explicit text edit.
  Manual sync accepted both at server revision 6, preserved one card with
  `fruit2` and the insights, and cleared the outbox. A repeated manual sync
  stayed at revision 6 with one card and two accepted edits. The rendered UI
  then deleted that existing card, showing `1r/0c` and one pending change.
  Manual sync reached version 7/7, zero pending; the server retained the
  one-review deck with zero cards and exactly one accepted `DeleteCard`.
  Repeated sync stayed at revision 7 with no duplicate deletion. Server
  SQLite `quick_check` passed. Before deletion, recoverable test-account DB
  copies were saved under `klaf-reviewed-card-delete-backup-20260928` in
  Local Temp. Primary phone and source backup were untouched.
- 2026-09-28 Desktop open-list refresh audit passed on new disposable copies
  of the earlier 60-deck client/server fixtures. A remote REST add advanced
  server 4→5; the already open Desktop window showed a pending remote change
  but no automatic pull. Clicking `Synchronize now` reached local/server 5/5,
  zero pending operations. Without restart or navigation, scrolling the same
  `LazyColumn` to its end showed `list_audit_tail_20260928` as the last row.
  Server and client each held exactly one matching deck; both SQLite files
  passed `quick_check`. The focused live Room/REST observed-list test passed.
  The earlier apparent absence is consistent with the appended row being
  off-screen in a long list; no reproducible refresh failure and no production
  code change. Test app/server were stopped; source fixtures, backup, and
  phones were untouched.
- 2026-09-27 ordinary Android single-card review acceptance passed on secondary
  RMX2001. The rendered app added `apple` → `fruit` to
  `ordinary_regression_20260927`, and explicit sync stored one card at revision
  4. The review screen's Start/difficulty buttons were moved fully inside the
  physical viewport. A focused single-card position test exposed and fixed an
  `IndexOutOfBoundsException` after choosing Good; Good then Easy completed
  the first review, leaving one durable pending operation and local `1r/1c`.
  The initial upload got HTTP 400 because the isolated test server required an
  explicit null `lastIterationDate` while Android omitted it. A test now
  reproduces that JSON mismatch; the contract accepts absence as null, and
  isolated-server JSON settings match production. A manual diagnostic request
  had already accepted the operation at server revision 5 in the old JSON
  format, so its one test-only payload fingerprint was corrected after saving
  a database backup in `klaf-review-diagnostic-20260927`. The phone's own
  retry then succeeded: rendered status `Synchronized`, versions 5/5, zero
  pending; server has one matching card, one accepted review, `1r/1c`, and
  SQLite `quick_check=ok`. Full `:klaf-server:test` and
  `:presentation:desktopTest` passed. The APK was installed in place without
  clearing phone data. Primary phone and source backup were untouched.
- 2026-09-27 ordinary Android manual-sync regression passed on secondary
  RMX2001, using its already clean Room-mode `com.kuts.klaf` installation
  and the retained disposable `mujg1gn4` server root. In signed-in test
  account B, the rendered app created `ordinary_regression_20260927` and
  showed one pending upload at revision 1. A process restart preserved both
  the deck and pending operation. Explicit sync advanced both revisions to 2,
  cleared the outbox, and a repeat sync left revision 2 unchanged; the server
  contained exactly one row for that deck. A separate registered test device
  then posted `remote_pull_regression_20260927` to the server at revision 3.
  The open Android status dialog changed to local 2 / server 3 with no
  automatic download, and the deck was absent from the open list. Clicking
  `Synchronize now` brought the client to 3/3, zero pending uploads, and
  rendered the new deck in that same open list without restarting. The server
  account held exactly one row for each test deck, four decks total, and its
  SQLite file plus registry passed `quick_check`. The isolated server was
  stopped and USB reverse removed. Only disposable server/account data and
  the secondary test phone were changed; source backup and primary phone
  were untouched. This does not yet cover an ordinary-app card edit/review
  cycle or the earlier one-off Desktop list-refresh observation.
- 2026-09-27 ordinary-launch readiness audit (no cutover): the default
  `local.properties` has no client storage/server override, so a normal
  Android build still selects legacy storage. The ordinary Desktop directory
  still contains `klaf_kt.db`; the Room switch correctly refuses to open
  account-scoped storage there. Android has the same shared policy guard,
  checking its package-local legacy database before selecting the scoped
  Room source. The isolated test identities bypass that guard by design.
  Configuration tests, ordinary `:Android:assembleDebug`, and
  `:Desktop:compileKotlin` passed (`:di:desktopTest` also passed). The guarded
  clean-install test path is ready, but default cutover is not done: broader
  ordinary-client regression and the one-off rendered deck-list refresh
  observation remain to be checked before using the primary phone. Also,
  Desktop reads `local.properties` at runtime by walking from `user.dir` to
  the repository root, so this developer switch is not a dependable setting
  for a packaged Desktop app launched outside the project. Decide and verify
  packaged-client configuration before any general release. No app, server,
  phone, backup, or installed database was changed during this audit.
- 2026-09-27 rendered Desktop offline-upload acceptance passed on fresh copies
  of disposable account/server databases. The signed-in app started with its
  server offline; creating `render_offline_upload_smoke` through the deck UI
  produced 61 local decks, checkpoint 4, and exactly one durable pending
  operation. After the isolated server started, the UI showed connected
  `Changes waiting to sync`, local/server versions 4/4, and one upload still
  pending. A REST bootstrap confirmed server revision 4, 60 decks, and no
  test deck: reconnect alone had not uploaded it. Clicking `Synchronize now`
  advanced client and server to revision 5, cleared the outbox, and stored
  the deck exactly once. A second manual sync without changes left revision
  5 and the deck count unchanged. All five copied SQLite files passed
  `quick_check`. Test app/server were stopped; source fixtures, source backup,
  and both phones were untouched. No production code change was needed.
- 2026-09-27 rendered Desktop reconnect acceptance passed on copies of the
  retained disposable server/client data. The window showed account revision
  1/1 and 57 decks, then 1/2 pending after a remote REST deck addition. With
  the server stopped it showed `Server connection unavailable`; after restart
  it reconnected to 1/2 without downloading. Direct read of the test Room
  file still found 57 decks, no new row, and checkpoint 1. Clicking
  `Synchronize now` downloaded the deck and reached 2/2, 58 decks, zero
  pending operations. On the first attempt the open list did not visibly show
  that appended row until the client reopened. Two further manual pulls,
  including one after another server restart, displayed newly added decks in
  the still-open list immediately. A focused real Room/REST test now checks
  that an already observed deck Flow emits after manual pull. The first UI
  observation did not reproduce; the 2026-09-28 open-window audit above found
  the newly appended deck off-screen at the end of a long list. No unproven
  production fix was made. All five copied client
  and server SQLite files passed `quick_check`; the copied account ended at
  60 decks, revision 4, zero pending changes. Full regression passed: domain
  30/30, presentation 48/48, DI 15/15, data 106/106, server 179 passed with
  one expected opt-in skip (379 reported, zero failures). Room-mode Android
  assembly and Desktop compilation passed. Test app/server are stopped;
  original fixture directories, source backup, and both phones were untouched.
- 2026-09-27 real WebSocket/Room restart check passed. A disposable Ktor
  server was restarted while an account-scoped Room client observed its actual
  event channel. After a remote REST write, the reconnected client saw server
  revision 1 and yellow status, but still had no downloaded deck and retained
  local revision 0. Only explicit REST synchronization downloaded the deck
  and restored green status. A local deck was then queued; after a second
  restart/reconnect, the server remained at revision 1 and the operation ID
  stayed pending until explicit REST synchronization uploaded it at revision
  2 and restored green status. The focused live class passed, followed by full
  regression: domain 30/30, presentation 48/48, DI 15/15, data 106/106,
  server 178 passed with one expected opt-in skip (378 reported, zero
  failures). Room-mode Android debug assembly and Desktop Kotlin compilation
  passed. Source backup and both phones were untouched. Rendered full-app
  reconnect remains a separate manual acceptance check.
- 2026-09-27 contract-suite audit complete: all 31 setup-only `ClientSyncContractTest`
  cases were mapped individually in `server-database-test-coverage.md` to
  executable Room, live Ktor/REST, domain, or UI-model tests. Added live tests
  proving that a remote revision event does not download until manual sync
  and that status reconnection does not retry a failed upload. Added a timeout
  test proving the account edit pause is released. Focused server and data
  test classes passed. Retired the deliberately failing `ClientSyncFixture`
  and umbrella test rather than implementing a fake synchronization adapter.
  Complete regression passed: domain 30/30, presentation 48/48, DI 15/15,
  data Desktop 106/106, and server 177 passed with one expected opt-in skip
  (377 reported tests in total, zero failures). Ordinary Room-mode
  `:Android:assembleDebug` and `:Desktop:compileKotlin` passed. Case 2
  password-field/menu visibility remains code-reviewed,
  not UI-automation-covered; full-app reconnect remains a manual acceptance
  check.
- 2026-09-27 isolated Desktop sign-in and first-sync UI check passed with
  user-assisted authentication. Against retained disposable server root
  `klaf-remote-storage-test-server-mujg1gn4`, the fresh Desktop directory
  `klaf-remote-storage-test-desktop-auth-ui-20260927` started as guest and
  showed server `Ready`. The user completed the passwordless sign-in and
  new-device confirmation for `backup-check-mujg1gn4@example.test`; the
  rendered drawer then showed that account, and the server database contained
  a new `Desktop` device registration with no confirmed revision. Before
  sync, the status dialog showed local version 0, known server version 1,
  zero local uploads, and `Never synchronized`. Clicking `Synchronize now`
  downloaded 57 decks/1093 cards, including `first_sync_smoke`; the dialog
  turned `Synchronized` at versions 1/1 with zero pending uploads, and the
  server recorded Desktop's confirmed revision 1 without advancing its own
  revision or duplicating rows. After closing the app and stopping the server,
  an offline restart restored the selected account and decks with local
  version 1, zero pending changes, and `Server connection unavailable`.
  Client account/guest and server SQLite files passed `quick_check`; client
  account held 57 decks/1093 cards/zero outbox, guest held one empty interim
  deck, and server remained at revision 1. The test app/server were stopped;
  the ordinary Desktop directory, source backup, and primary phone were not
  touched. The device-registration prompt itself was handled by the user,
  not automated or independently captured.
- 2026-09-27 isolated Desktop rendered data-separation check passed. A
  disposable copy of the previously verified imported-account Room directory
  opened in the real Desktop window with its 57 decks/1093 cards. Clicking
  Log out while the server was offline switched to guest; a new
  `desktop_guest_smoke` deck appeared there only. With the app closed, the
  disposable selected-account test fixture was set to B (not a UI sign-in);
  the reopened window showed no guest/imported decks, and a new
  `desktop_b_smoke` deck appeared only for B. Closing/reopening retained B.
  Returning the fixture selection to A restored the imported list without
  either test deck. Logging out A again restored the guest test deck. The
  three isolated Room files passed SQLite `quick_check`: guest 2 decks/0
  cards, A 57/1093, B 2/0; test-deck names were confined to their own files.
  Three `:Desktop:run` executions exited successfully. The test app was
  closed; the ordinary `~/.klaf_kt` directory, original imported Desktop
  fixture, source backup, and primary phone were not changed. This fixture
  selection did not prove the Desktop sign-in UI; a subsequent user-assisted
  sign-in/device-registration check is recorded above. Windows UI tooling
  did not automate authentication dialogs.
- 2026-09-27 rendered ordinary-app account-switch acceptance check passed on
  secondary physical RMX2001. Signing out of imported account A displayed
  only the guest deck; sign-up of test account B transferred a newly created
  guest deck/card and removed them from guest. Before B's first manual sync,
  the UI correctly showed three pending operations despite both revisions
  being 0. After sync, A retained 57 decks/1093 cards and B held two decks/
  one card on the server. Switching A → B through the UI restored only each
  account's decks and synchronized revision-1 status. With the server stopped,
  an app restart restored B locally and showed the unavailable-server badge,
  without automatic sync. Read-only copies of all three on-device Room files
  passed SQLite `quick_check`: guest 1/0, A 57/1093, B 2/1 decks/cards, all
  with zero pending operations. The primary phone and source backup were not
  touched; source DB/WAL/SHM SHA-256 hashes remained unchanged. The disposable
  server was stopped and USB reverse removed.
- 2026-09-27 autocomplete follow-up: the Room-mode Android binding still
  created the Firestore autocomplete repository, and a Firestore cache file
  appeared during test-card entry. Account-scoped mode now binds an inert
  repository without constructing the Firestore provider. The use case
  exposes availability, and the card editor skips the autocomplete request
  and clears its suggestions when unavailable; legacy mode retains its
  existing binding. The desktop/iOS no-op repositories also report the
  feature unavailable. All 30 domain and 15 DI desktop tests passed,
  including both new selection tests. The ordinary Room-mode Android APK
  assembled and was installed over the test phone's existing app. After
  restart with the server stopped, the selected B account still displayed its
  own test deck/card and
  the unavailable-server badge. This does not remove Firebase Crashlytics or
  prove the cache file came from autocomplete rather than SDK initialization.
- 2026-09-27 test-harness follow-up: `data` cannot directly use the real
  `klaf-server` fixture without a dependency cycle, so the 31 deliberately
  failing `ClientSyncContractTest` cases were not made green with a fake sync
  implementation. Instead, a new `LiveServerAccountSessionTest` interrupts
  guest transfer immediately after copying the account data. It verifies
  that guest rows and the persisted sign-up attempt survive, then retries
  the same server account, confirms one local deck/card and two pending
  operations, and manually uploads exactly one deck/card without duplicates.
  A second live account-session test uploads a guest deck with one completed
  review and an existing card in the same first batch; the server accepts
  both and preserves the review count. The focused account-session class
  passed (7/7); a clean rerun of the complete `:klaf-server:test` suite
  passed (176/176). The umbrella scaffold
  remains unresolved; continue moving its distinct missing checks to live
  Room+REST or rendered-app tests, then reassess it. The rendered Android
  account-switch flow was subsequently checked as recorded above.
- 2026-09-27 first-sync status false positive fixed: a connected account
  without a durable checkpoint now shows pending, not synchronized, even
  when client/server both report revision 0. The Room observer test first
  failed on the old behavior, then passed (3/3) after the fix; the ordinary
  Room-mode Android APK built successfully. On secondary RMX2001, an in-place
  APK update retained the imported account data. Renaming the test copy's
  `first` deck to `first_sync_smoke` produced one pending operation and a
  yellow indicator while the disposable server still held `first`. The
  local rename survived a process restart. Manual sync then advanced both
  status versions and the server revision to 1, cleared the outbox, kept
  57 decks/1093 cards, and showed the renamed deck after another restart.
  The server database passed SQLite `quick_check`; the test account and its
  renamed deck remain as test data. The full `:data:desktopTest` run had
  105 passing tests and 31 known `ClientSyncContractTest` failures, all from
  `newClientSyncFixture()`'s still-unimplemented real adapter, not from this
  fix. The source backup DB/WAL/SHM hashes remained unchanged. The test
  server was stopped and USB reverse removed; no primary-phone action was
  taken. Next: cover the umbrella fixture's distinct missing cases with
  real Room+REST or rendered-app tests before primary-phone cutover.
- 2026-09-27 test-only ordinary-client cutover requested with the source
  backup now at `C:\Users\<user>\Desktop\klaf_backup`. Read-only inventory
  found Room v7 `.db` with nonempty WAL/SHM, no matching review DataStore,
  and 19 saved mnemonic PNG files. The source Room/WAL/SHM were copied to a
  new temp workspace; SQLite `.backup` on that copy produced a standalone
  `checkpointed.db` with `quick_check=ok`, 57 decks, 1093 cards, and zero
  orphan cards. Source files and the primary phone were not touched. The
  copied database was imported in the approved explicit no-review mode into
  new root `C:\Users\<user>\AppData\Local\Temp\klaf-remote-storage-test-server-mujg1gn4`
  for `backup-check-mujg1gn4@example.test` at revision 0. Server SQLite
  `quick_check=ok`, 57 decks, 1093 cards, zero orphans, devices, and accepted
  history; SHA-256 checks show the source DB/WAL/SHM unchanged. A one-off
  ordinary Android debug APK was built with `-PklafClientStorageMode=room`;
  generated BuildConfig confirms Room mode, `127.0.0.1:8090`, and package
  `com.kuts.klaf`. The isolated server ran on loopback with USB reverse
  targeting only secondary RMX2001 `<test-device-serial>`. Its previous ordinary
  `com.kuts.klaf` installation contained a legacy database. Oppo firmware
  denied `pm clear`; after verifying the exact device and package, the
  user-authorized test-phone app was uninstalled and the Room-mode APK was
  installed cleanly. This removed that test phone's previous ordinary-app
  data, not the primary phone's. The app created `klaf_guest.db` without a
  legacy database, signed in to the imported test account, explicitly
  registered RMX2001, and manually synchronized. Visible source decks
  appeared; a diagnostic copy of the account Room database passed SQLite
  `quick_check` and contains exactly 57 decks, 1093 cards, 0 orphan cards,
  57/1093 distinct deck/card sync IDs, 0 pending operations, and checkpoint
  revision 0. The server still contains 57 decks and 1093 cards. After app
  restart, the status UI shows Synchronized, versions 0/0, zero pending
  changes, and the device's last confirmed version 0. Revision 0 is expected
  for the imported baseline; this is not evidence of a new server write.
  Image files were not transferred (deferred MVP scope), and the absent
  review DataStore means no review summary was imported. Neither the primary
  phone nor the source backup was modified; SHA-256 of the source DB/WAL/SHM
  was rechecked after the sync and remained unchanged. The next step is a
  separate ordinary-client mutation/restart/sync regression pass before any
  primary-phone cutover. The disposable server was stopped and USB reverse
  removed after this check; its server data root and the test phone's
  synchronized account database were retained. Immediately after device
  registration but before its first manual pull, the status dialog had
  incorrectly displayed "Synchronized" at versions 0/0 while the device row
  said "Never synchronized". This was corrected in the follow-up above.
- 2026-09-27 guarded ordinary-build cutover switch implemented. Git-ignored
  `local.properties` now accepts `klaf.client.storage.mode=room`; absent or
  `legacy` keeps the old source. Android and Desktop select their existing
  account-scoped Room/REST bindings only in Room mode, and fail fast before
  opening them if that ordinary installation has `klaf_kt.db`. The isolated
  Android package/disposable Desktop directory remain scoped independently.
  New policy and Desktop configuration tests pass (`:di:desktopTest` 13/13);
  normal Android Kotlin and Desktop Kotlin compilation pass. Read-only checks
  found legacy database files in both ordinary installations, so neither can
  be switched safely in place. No local property was changed, no ordinary
  APK was installed, and no app database or backup was modified. The user-
  prepared clean-install cutover and first ordinary-client sync remain open.
- 2026-09-27 OS-scheduled account reminder passed on physical RMX2001 in the
  isolated package. `AndroidScopedDeckReminderActions` scheduled an alarm
  five seconds ahead; `DeckReviewReceiver` published a notification with the
  selected account tag and expected deck ID at or after that time. The test
  cancelled its alarm, notification, and navigation PendingIntent afterward.
  The focused delivery test passed (1/1), then the complete reminder
  instrumentation class passed (5/5). This covers actual alarm delivery,
  while the separate shade-tap test below covers rendered navigation. The
  ordinary app and its database were untouched.
- 2026-09-27 notification navigation acceptance check passed on physical
  RMX2001 in the isolated account package. The test temporarily inserted an
  unreviewed deck and card into the selected test account's Room database,
  published an account-scoped notification, opened Android's notification
  shade, tapped that notification, and observed the rendered review screen
  with the correct deck name, review statistics, and Start control. It
  removed the notification, tap PendingIntent, and temporary deck/card
  afterward. The complete isolated reminder instrumentation class passed
  (4/4). OS alarm delivery was subsequently verified as recorded above.
  Ordinary app data was untouched.
- 2026-09-27 account-scoped Android notification test passed on physical
  RMX2001 in the isolated `.remote.storage.test` package. With the same
  local deck ID, a mismatched account published nothing; the selected
  account's notification appeared with its own navigation PendingIntent.
  After the account changed, the old notification was dismissed, a stale
  callback did not recreate it, and the new account's notification used a
  distinct PendingIntent. Android notification posting/removal is asynchronous,
  so the test waits for the observed state. The full reminder test class
  passed (3/3) after rebuilding and installing only the isolated test APK.
  Test notifications and tap intents were cleaned up. The ordinary app and
  database were untouched. The tap-action/rendered-navigation check and
  OS-scheduled delivery are now recorded above.
- 2026-09-27 the isolated Android reminder instrumentation now covers the
  real `ActiveLocalRoomDatabase` selection observer and Android alarm actions
  together. Three in-memory Room databases with the same local deck ID were
  switched guest → Alice → Bob → Alice; each switch cancelled the previous
  alarm and scheduled only the selected database's alarm. Both instrumentation
  tests passed on physical RMX2001 (2/2). Room/SQLite were added only to the
  `androidTest` compile classpath. The synthetic alarms were cancelled and
  the in-memory databases closed; saved app databases were not opened by the
  new test. Full notification delivery/navigation and ordinary-app cutover
  remain unverified.
- 2026-09-27 account-scoped reminder instrumentation passed on physical
  RMX2001 (1/1) against the separate `com.kuts.klaf.remote.storage.test`
  package. Gradle now accepts opt-in `-PklafAndroidTestBuildType=remoteStorageTest`
  while the default remains `debug`. The isolated androidTest APK assembled;
  its target identity was verified before installation. The connected Gradle
  task could not start offline because its Android Test Plugin host dependency
  was not cached, so the already-built test APK was installed and run directly
  with `adb shell am instrument`. The test proved that two accounts with the
  same local deck ID have distinct alarm PendingIntents, that cancelling one
  retains the other, and that the matching legacy alarm is removed. It did
  not test the complete app-driven scheduling flow or notifications after the
  ordinary-app cutover; those remain pending. No ordinary app package or
  database was installed, launched, or cleared for this check.
- 2026-09-27 follow-up on the earlier Android pull failure: two new live
  Room+REST regression tests pass after review/removal resolution. They cover
  a later remote deck/card addition and a remote addition followed by deletion
  before pull; both advance the local checkpoint correctly. On RMX2001, the
  previously failing account pulled its saved server delta from revision 3 to
  5 after an isolated-app reinstall without data clearance. A later remote
  rename also pulled from revision 5 to 6 after switching accounts twice in
  the same process. The initial generic failure could not be reproduced after
  restart, so no product-code fix is claimed. Keep the intermittent first-
  attempt failure under observation; capture its exception if it recurs. The
  full `LiveManualRoomSyncCoordinatorTest` class passed (30/30), the isolated
  Android APK built, and the final APK without temporary diagnostic output
  was installed without clearing app data. The disposable server was stopped
  and `adb reverse tcp:8090` removed; its data root was retained.
- 2026-09-27 physical review-versus-card-removal checks passed on RMX2001
  with the isolated `remoteStorageTest` package and a disposable loopback
  server. In two separate test accounts, Android synchronized a deck with one
  card at revision 1, deleted the card locally through Transfer cards, and a
  registered second device completed that deck's review at revision 2. Manual
  sync showed the deleted card's readable name, the remote review and device,
  and all three choices. Choosing **Keep card removal and review schedule**
  in the first account produced server revision 3: card absent, review count
  1, original next-review date retained, and Android showed synchronized.
  Choosing **Keep card removal; make deck due now** in the second account
  produced server revision 3: card absent, review count 1, next-review date
  changed to the resolution time, and Android showed `1r`, `0c`, and
  synchronized. The ordinary Android package and backup were untouched.
  During setup, one first sync attempt displayed the generic failure dialog;
  retry succeeded and both server and Android Room confirmed revision 1.
  Separately, a REST-created/deleted deck on the first account initially
  produced a generic pull failure. A later run successfully pulled the saved
  delta, as recorded above. Test data is retained for
  follow-up; the temporary server was stopped and `adb reverse tcp:8090`
  removed. No ordinary-app cutover was performed.
- 2026-09-26 physical review-versus-card-removal UI check paused at the
  device lock screen. `:Android:assembleRemoteStorageTest` and server classes
  built successfully; the updated isolated package was installed on RMX2001,
  and a new loopback-only disposable server started with root
  `C:\Users\<user>\AppData\Local\Temp\klaf-remote-storage-test-server-review-20260926213540`.
  The phone then showed its secure pattern prompt, so no account, deck,
  review, removal, or conflict action was exercised. The server was stopped
  and `adb reverse tcp:8090` removed; its root was retained. The isolated
  package's existing data, ordinary app, and Android backup were not cleared
  or modified. This paused check resumed and completed on 2026-09-27 above.
- 2026-09-26 completed conflict-name fallback for deleted local rows. The
  UI model now uses names from the saved server conflict/delta when a locally
  deleted card or deck has no Room row; local names retain precedence when
  present, and unknown IDs remain visible as a last resort. Added card and
  deck regression cases; the focused `ConflictResolutionUiModelTest` suite
  passed (17/17), and normal Android `:Android:compileDebugKotlin` passed.
  No resolution semantics changed. The deleted-card label was subsequently
  verified on RMX2001 on 2026-09-27.
- 2026-09-26 live account-isolation test passed. Alice receives a guest deck
  and card on sign-up with two pending operations. After sign-out, new guest
  deck/card data remains untouched when signing in to pre-existing Bob;
  Bob's local Room database and outbox are empty. Returning to Alice restores
  only Alice's deck/card and the two pending operations; returning to guest
  restores only the guest deck/card. Bob's server stays at revision 0. The
  focused `LiveServerAccountSessionTest` suite passed (5/5). This is test
  coverage for agreed behavior, not an ordinary-app cutover or physical UI
  verification.
- 2026-09-26 filled two executable coverage gaps. A real account-scoped Room
  test confirms conflict display names come from the selected account and
  cannot read another account's names after a switch; the focused five-test
  `RoomSyncConflictSnapshotTest` suite passed. A live Room+REST session test
  confirms transferred guest deck/card edits stay pending after sign-up and
  later sign-in: the server remains at revision 0 until explicit manual sync,
  after which both items appear on the server and the local outbox clears.
  The focused `LiveServerAccountSessionTest` suite passed. No product behavior
  or ordinary-app binding changed; this work adds evidence for existing MVP
  requirements. The setup-only client umbrella cases and remaining physical
  checks are still open.
- 2026-09-26 conflict-screen readability follow-up: the isolated account
  route now resolves local Room deck/card sync IDs to visible names for move,
  deletion, review, and server-change descriptions. When a row is already
  absent, the technical ID remains a fallback instead of guessing a name.
  The lookup stays in the data module, preserving the DI/Room dependency
  boundary. Three focused UI-model cases cover a moved card with source and
  destination names, a remotely deleted source deck, and card removal.
  `:presentation:desktopTest` and `:di:desktopTest` passed. This improves
  labels only; no conflict-resolution semantics or ordinary-app binding
  changed. The 31 setup-only client umbrella cases remain unresolved.
- 2026-09-26 physical-device reviewed-destination follow-up passed on
  RMX2001 with the rebuilt isolated `remoteStorageTest` APK and a new
  disposable loopback-server account. The Android UI moved card A from
  Source into Target at revision 4; a second registered device completed
  Target's review at revision 5. Android's manual sync showed the concrete
  conflict and offered an alternate deck. Tapping Alternate advanced the
  account to revision 7: Android showed synchronized/zero pending changes;
  server bootstrap retained card A's sync ID, image filename, one card in
  Alternate, and Target's completed review. A separate card B was locally
  moved into SecondTarget at revision 9 while the server reviewed it at
  revision 10. The same conflict screen offered a new-deck name; tapping
  Create deck and move card created exactly one FreshPhysical deck and moved
  card B there at revision 11. Android again showed synchronized/zero
  pending changes. Server bootstrap retained both card IDs and image
  filenames, with two cards total and the reviewed destinations empty.
  Opening card A in the editor before the first transfer also saved fetched
  Word Insights, so that run had a separate accepted EditCard at revision 6;
  the second run had only a pending MoveCard before resolution. The ordinary
  Android package/database were untouched. The disposable server root was
  retained; the server process stopped and `adb reverse` was removed.
- 2026-09-26 completed the stale-move-into-reviewed-destination path.
  A manual conflict action now lets the user choose another existing
  unreviewed deck or create a new one. The account-scoped Room resolution
  applies the server change first, then rehomes the same card with its ID,
  content/image reference, and review duration intact; it queues fresh
  AddDeck/MoveCard operations against the confirmed revision. Existing
  destination eligibility is rechecked transactionally. Real Room+REST
  tests passed for both choices, a subsequent remote review of the chosen
  deck, local rejection without changing the saved conflict, and a lost
  response followed by a non-duplicating retry. The planner now includes
  the moved card's review duration in MoveCard; server input validation
  rejects a negative value. The conflict UI explains the reviewed target
  and offers a destination picker; legacy MoveCard operations missing that
  duration do not offer a potentially lossy retarget. Focused server,
  presentation, data outbox, and DI tests passed, as did normal Android
  `:Android:compileDebugKotlin`. The final full regression run passed:
  server 171 tests (zero failures, one expected opt-in skip), presentation
  42/42, and DI 7/7. The physical-device UI flow was subsequently verified
  as recorded above; ordinary app bindings still use the legacy path. The 31
  setup-only client umbrella failures remain independent of this work.
  A subsequent focused UI-model test passed for an older MoveCard payload:
  it still explains the reviewed destination but hides unsafe retargeting.
- 2026-09-26 implemented the review-versus-card-removal choices test-first.
  The original saved conflict is applied in one Room transaction; keeping
  the removal creates a fresh, durable delete operation against the current
  revision. The due-now choice also queues a replay-safe schedule operation.
  Server time replaces only the current next-review date; review count,
  pass dates, interval/duration fields, and the completed review summary stay
  intact. The conflict UI explains why the completed review and deletion
  disagreed and offers both choices. Live tests passed for each choice, a
  lost response after commit followed by an idempotent retry, and rejection
  of due-now on an unreviewed deck. Full verification: domain 30/30,
  presentation 40/40, server 165 tests with zero failures and one expected
  opt-in skip. Data Desktop: 135 tests, 31 failures solely in the known
  unimplemented client umbrella fixture; the other 104 passed. Normal
  Android `:Android:compileDebugKotlin` passed. No physical UI/device check
  of these new controls was performed; ordinary app bindings remain unchanged.
  Additional focused checks passed after the full run: a second unrelated
  deletion conflict does not show review-specific choices, two pending card
  removals of one reviewed deck produce only one due-now schedule operation,
  and the new schedule preserves review dates, interval, durations, and summary.
  The final full rerun after those checks passed: presentation 41/41 and
  server 166 tests, zero failures, one expected opt-in skip. Normal Android
  `:Android:compileDebugKotlin` also passed again after the final UI label edit.
- 2026-09-26 placeholder-suite audit in progress. The 31 client-contract
  scenarios must be mapped to executable Room, live Room+Ktor, and UI-state
  tests before their nonfunctional umbrella fixture can be retired. The
  `data` test module cannot embed `klaf-server` without a dependency cycle;
  integrated cases belong in `klaf-server` tests. The three old migration
  placeholders demand an in-place DataStore-to-account transfer that is not
  part of the agreed clean-install bootstrap. Real Room-v8 foreign-key/orphan
  and sync-identity migrations already have executable tests. One genuine
  gap identified: the promised review-completion versus card-removal choice
  between retaining the schedule and making the remaining deck due now has
  no executable end-to-end test yet. No placeholder has been suppressed to
  make the suite green.
- The three obsolete in-place migration placeholders were retired after their
  invariants were mapped to executable Room-v8 migration and server-bootstrap
  tests in `docs/server-database-test-coverage.md`. They were not marked green
  or replaced by a fake adapter. The 31 client placeholders remain failing
  until the uncovered real-client paths are checked. Focused real
  `CardDeckForeignKeyMigrationTest`, `RoomSyncIdentityMigrationTest`, and
  `BootstrapImportContractTest` suites passed together after the retirement.
- Added a live account-scoped Room-to-REST test for creating a deck and card
  with an image reference in mnemonic JSON. Both operations commit as one
  server revision; the filename survives locally and in the server snapshot,
  the outbox empties; file bytes remain outside the data-sync protocol. The full
  `LiveManualRoomSyncCoordinatorTest` suite passed 16/16. The first assertion
  assumed a revision per operation; the test revealed the existing protocol's
  one-revision-per-accepted-batch rule, and the expectation was corrected.
  Full verification afterward: `:klaf-server:test` 159 tests, zero failures,
  one expected opt-in skip; `:data:desktopTest` 135 tests, 31 failures, all
  confined to the still-unimplemented `ClientSyncContractTest` adapter. The
  other 104 data tests passed. No production app cutover was performed.
- A second live Room+REST test verifies that the status remains gray while the
  event channel is disconnected, yet an explicit REST synchronization still
  downloads a remote deck and advances the local checkpoint. The complete
  `LiveManualRoomSyncCoordinatorTest` suite passed 17/17 after this addition.
- A third live test covers local card removal against a remote completed
  review. The client retains its removal and operation ID, leaves its
  checkpoint at the pre-review revision, saves the conflict, and the server
  retains both the card and its completed review schedule. The live suite
  passed 18/18. The actual two resolution choices (keep the review schedule
  or make the remaining deck due now) remain unimplemented and must not be
  mistaken for covered behavior. Final full server verification: 161 tests,
  zero failures, one expected opt-in skip. A final focused rerun also passed
  after the live case was strengthened to assert the complete review-summary
  snapshot remains on the server while the conflict is unresolved.
- 2026-09-26 imported-account Desktop follow-up passed through an opt-in
  integration test using the real Desktop account session, persistent device
  identity, scoped Room database, and REST sync client against the disposable
  server on loopback. A fresh device received `DEVICE_NOT_REGISTERED`, was
  explicitly registered, and then performed its first manual download with
  an auto-created empty interim deck already queued locally. Local Room has
  exactly 57 decks and 1093 cards. Every deck's name, creation date, review
  count, review/scheduled date lists, interval, duration fields, success flag,
  summary, and card count matched the server snapshot; every card's parent,
  words, IPA, mnemonic JSON, and parsed Word Insights matched. The outbox was
  empty afterward, and a second manual sync left the server at revision 0.
  The opt-in test passed 1/1 with zero skips. The full server suite passed
  158 tests with zero failures and one expected skip (this smoke test without
  its explicit environment), and `:di:desktopTest` passed 7/7. The user's
  normal Desktop directory, normal Android package, and Android recovery
  files were not touched. The same isolated Desktop Room directory was then
  opened in the real Desktop window: imported decks rendered, one imported
  card opened for viewing without starting or completing a review, and the
  sync dialog showed version 0, zero pending uploads, and `Synchronized`.
  Windows UI automation does not allow authentication dialogs, so the actual
  Desktop sign-in screen was not manually exercised.
  The 31 client-contract and three version-8 migration placeholder tests
  remain red by design pending their real adapters; this smoke test does not
  silently replace or disable them.
- Code review of the first-download route exposed one unsafe pre-first-sync
  corner case: unrelated local edits could otherwise be uploaded to a
  revision-0 imported account while its baseline remained invisible. A new
  focused test failed first; the coordinator now reads the initial snapshot
  when there is no checkpoint and rejects that specific unsafe state without
  sending or clearing local operations. A later-revision lost-response retry
  still uses incremental sync. The focused Room tests pass 8/8, the live
  coordinator tests pass, and the imported Desktop smoke test passed again.
  Final full run: server 158 tests, zero failures, one expected opt-in skip;
  Desktop DI 7/7; Desktop data 138 tests with 34 failures solely in the two
  already-known placeholder suites (`ClientSyncContractTest` 31 and
  `RoomMigrationContractTest` three). Those adapter suites are not claimed as
  implemented or green. The 31-test fixture currently lives in `data`, which
  cannot depend on the real Ktor server module without a dependency cycle;
  implementing that fixture requires moving the contract into the server test
  source set or using an external server process. The three version-8 migration
  contracts need separate disposition in light of the agreed clean-install
  cutover; they were not suppressed merely to turn the suite green. The
  isolated server process was stopped after testing;
  no listener remains on port 8090. The disposable imported root remains for
  future checks. After the extra Desktop window check, both that Desktop
  process and the restarted isolated server were stopped again. The isolated
  Desktop Room directory under the temp folder was retained for reproducible
  visual inspection; no ordinary Desktop directory was used.
- 2026-09-26 physical first-download check found and resolved a revision-0 blind spot:
  after explicit device registration, the client marked an imported account
  synchronized but showed no decks because incremental sync has no history
  before revision 0. A test-first client fix now requests the complete REST
  bootstrap snapshot when the selected account database is empty or contains
  only its auto-created empty interim deck, with no unrelated pending edits or
  saved conflict. It merges the server interim deck, clears only the matching
  queued local interim-add, applies the snapshot and checkpoint atomically in
  Room, then confirms that revision to the server. Seven focused desktop tests
  pass, including the auto-created interim case and a second incremental sync.
  Physical retest on the isolated `com.kuts.klaf.remote.storage.test` package
  now shows the imported decks and the interim deck's 92 cards; the indicator
  is green, the local/server versions are 0, and the outbox is empty. A repeat
  manual sync stays green with no new revision. The ordinary app was untouched.
  After the check, the disposable server process was stopped and the ADB port
  reverse was removed; the imported disposable server root remains available
  for another test run.
  The wider
  `:data:desktopTest` task still includes 34 known red placeholder-contract
  tests (31 unimplemented client-adapter cases and three version-8 migration
  adapter cases); other data tests, including the new focused tests, passed.
  `:klaf-server:test` passed 157/157 and `:di:desktopTest` passed 7/7;
  `:Android:assembleRemoteStorageTest` succeeded.
- 2026-09-26 source-specific no-review import decision implemented. The user
  confirmed that the exported source installation has no separate
  `DeckRepetitionInfo` file and approved importing without it. The importer
  now requires an explicit `bootstrapNoReviewBackup=true` mode when no review
  file is supplied; a missing specified path still fails. A new synthetic
  Room-v7 test failed before the change and now verifies that normal review
  counts/schedules remain while no latest-review summary is fabricated.
  Running the new mode on a checkpointed copy of the user's Room/WAL backup
  in a fresh disposable server root imported 57 decks and 1093 cards at
  revision 0. SQLite `quick_check` was `ok`: 50 reviewed decks retain their
  review counts (aggregate 5672), zero latest-review summaries as expected,
  zero orphan cards, and the single canonical interim deck owns 92 cards.
  Read-only comparison found zero mismatches in ordinary deck/card fields,
  interim card fields, or raw Word Insights JSON. The original files in
  `klaf_dbs` were unchanged. The full server suite and corrected physical
  first-download check subsequently passed as recorded above.
- 2026-09-26 source-backup clarification and diagnostic import: the folder
  `C:\Users\<user>\Desktop\klaf_dbs` is the recovery export from a different,
  currently disconnected phone; the connected phone is disposable for tests.
  A recursive inventory confirms the folder has Room `.db`/WAL/SHM and three
  unrelated preferences files, but no `deck_repetition_info_file_name`.
  Using a checkpointed copy of Room and an explicitly synthetic **empty**
  review file in a separate disposable server root, the fixed importer
  published 57 decks and 1093 cards at revision 0. SQLite `quick_check` was
  `ok`; there were no orphan cards, exactly one canonical interim deck with
  its 92 cards, and zero imported latest-review summaries. The Room export
  marks 50 decks as previously reviewed, so this diagnostic is not a
  full latest-review-summary migration. The source backup files were not changed and the
  connected test phone was not used in this diagnostic. Explain the missing
  latest-review metadata and obtain a deliberate decision before any final
  cutover; do not present the empty review file as a recovered backup.
- The user subsequently confirmed that this source has no separate review
  file and accepted the no-review path recorded above. Keep the original
  backup and test-only server root separate.
- 2026-09-26 legacy interim-deck import fix completed test-first. The supplied
  Room copy exposed a real importer gap: the special interim deck uses local
  ID `-1` and has 92 cards. A new server test failed against the previous
  positive-ID-only validation. Import now remaps that deck to an unused
  positive server-local ID, gives it the canonical account interim sync ID,
  and remaps its cards' parent IDs; other negative deck IDs remain invalid.
  A synthetic full Room-v7 import test verifies migration, revision-0 publish,
  first-device bootstrap, card ownership, and preservation of the source
  backup. The full `:klaf-server:test` suite passes, 156/156. The real-data
  import is still **not** complete: its matching review DataStore file from
  the source phone is absent. No account was published from the real backup,
  and no ordinary installed app was changed.
- This earlier request for a matching review DataStore file was superseded by
  the user's explicit decision to proceed without it for this source backup.
- 2026-09-26 first real-export inspection stopped before cutover. The files
  supplied at `C:\Users\<user>\Desktop\klaf_dbs` include a Room v7 database
  and its nonempty WAL/SHM but no `deck_repetition_info_file_name`. An isolated
  copy passes SQLite `quick_check`, with 57 decks, 1093 cards, no orphan cards,
  and 50 reviewed decks. A checkpointed disposable copy was prepared without
  changing the source files. A dry-run import failed before account publication
  because the legacy interim deck uses local ID -1, which the then-current
  import validator rejected; the mapping has since been implemented above.
  Separately, the connected phone was **not** the source phone. A review
  DataStore file briefly copied from that wrong device was removed from both
  the supplied-files folder and the disposable inspection folder as soon as
  the user clarified the device mismatch. Do not use that file or combine
  exports from different devices. The disposable import root contains no
  published account, and no installed app or source backup was modified.
- The interim-deck import mapping was subsequently implemented and tested as
  recorded above; the matching source-device review export remains missing.
- 2026-09-26 ordinary-build cutover audit completed without changing the
  installed app or its legacy Room database. Android `com.kuts.klaf` still
  selects `StaticRoomDatabaseSource` and the Firebase authentication/worker
  path; only the separate `.remote.storage.test` package selects
  `ActiveLocalRoomDatabase` and Room/REST. Desktop likewise uses its legacy
  directory/source unless an explicit disposable test directory is supplied.
  This preserves existing data but means the new implementation is not yet
  the default. Before flipping those bindings, obtain and retain the user's
  checkpointed Android Room export **and** matching `DeckRepetitionInfos`
  DataStore export, test a copy with the manual bootstrap against a stopped
  server, verify its account/deck/card/review contents, and only then perform
  the agreed clean-install sign-in/first manual download. A real export has
  not been supplied, so this data cutover remains blocked; do not silently
  point an existing installation at an empty scoped guest/account database.
  Regression verification: `:di:desktopTest` 7/7, `:presentation:desktopTest`
  39/39, and `:klaf-server:test` 153/153 passed; `:Android:compileDebugKotlin`
  and `:Android:assembleRemoteStorageTest` succeeded. No ordinary APK was
  installed or launched. The Android Google Services JSON is ignored and not
  tracked; its contents were not read.
- Next: obtain the user's real checkpointed Room and review-DataStore exports
  when they are ready, dry-run and inspect the manual import on a disposable
  server root, then confirm the reset/cutover sequence before enabling the
  ordinary account-scoped bindings. In-flight lost-response recovery remains
  verified by live automated tests, not a physical fault-injection check.
- 2026-09-26 physical Android/Desktop offline-retry check passed on the
  disposable account. With the server stopped, Android renamed
  `StructureTarget0926` to `StructureTargetOffline0926`; the local edit and one
  pending operation survived a failed manual sync and a forced app restart.
  The retained server still held revision 13 and the old name. After restarting
  that same server, one explicit Android sync committed revision 14 and cleared
  the outbox; a second manual sync left revision 14 unchanged. The isolated
  Desktop client then pulled revisions 12–14: it showed the new deck name,
  no previously deleted `StructureRestore0926` deck, local/known revision 14,
  zero pending operations, and a green status. This validates a real offline
  edit/retry across app and server restarts, not an in-flight lost HTTP
  response. The test Desktop/server processes were stopped and USB port
  reversal removed; disposable data was retained. The ordinary Android
  package and its database were untouched.
- The subsequent ordinary-build audit and regression checks are recorded
  above; the in-flight lost-response path has automated live tests but has
  not been physically forced.
- 2026-09-26 third physical structural-conflict choice passed. On the isolated
  Android package, card B was edited locally at revision 12 while the server
  deleted its two-card deck at revision 13. The conflict screen showed the
  local card edit and the server's cascading deck/card deletion. Choosing
  "Accept server changes" removed the deck on Android, returned the status
  indicator to green, and did not upload or restore either card. Server
  bootstrap remained at revision 13 with neither deck nor cards. This was
  checked with the final rebuilt APK, including the whitespace-only Word
  Insights guard. The isolated server was stopped and USB port reversal
  removed afterward; disposable data was retained. No ordinary Android
  app/database was touched.
- At that point broader two-device retry and ordinary-app cutover checks still
  remained; the offline retry was subsequently checked as recorded above.
- 2026-09-26 physical structural-conflict verification passed in isolated
  Android/Desktop mode against disposable account data. From shared revision
  6, Android moved a card while the server deleted its source deck: Android
  showed both actions, rescued the card, and advanced to revision 8; Desktop
  manually pulled the deleted source and one card in the target. From shared
  revision 9, Android edited a card while the server deleted its deck: Android
  restored the complete two-card deck at revision 11, and Desktop pulled the
  same rows. Desktop Room ended at checkpoint 11 with no outbox or conflict
  snapshot. A separate physical finding was fixed test-first: merely opening
  an Android card editor produced an `EditCard` with unchanged text when the
  Word Insights service returned zero meanings. The server response at
  revision 12 showed only word/language and no meanings. The editor now saves
  auto-fetched insights only when they contain a nonblank translation for the
  current word. After installing the fixed isolated APK, opening the same
  card without saving left Android at revision 12 with zero pending changes;
  the server also stayed at revision 12. Three new insight-save tests and the
  full 39-test presentation Desktop suite passed; the final isolated Android
  APK built. The last additional test guards whitespace-only translations;
  that final guard was installed for the subsequent accept-server test, though
  the no-op editor check was not separately repeated after its installation.
  The ordinary Android package/database were not touched. Disposable data was
  retained.
- 2026-09-25 structural-conflict/offline-recovery audit completed. New live
  tests prove that a lost response after a rescued card is committed preserves
  the same pending operation ID and retries without duplicating the card, and
  that a network failure before restored-deck upload leaves the complete deck
  and cards in Room with three pending operations until an explicit retry.
  No production sync-path change was needed. A new presentation test first
  exposed that the conflict screen attributed every card deleted in a server
  delta to one deleted deck; another showed the same error with an unrelated
  card deletion. The message now says the deck and its cards were deleted
  without an unverified count. Full `:presentation:desktopTest` (36 tests)
  and `:klaf-server:test` (153 tests) passed, zero failures; Android debug
  Kotlin compiled. Physical-device
  structural-conflict choices remain to verify. Ordinary app bindings and
  data were untouched. The Android editor's Word Insights request is separate
  from the disabled Firestore autocomplete; no behavior was changed pending a
  product decision.
- 2026-09-25 mixed-conflict physical-device check passed. On the disposable
  server, Android submitted deck and card edits from revision 3 and reached
  revision 4; Desktop held concurrent deck and card edits from revision 3.
  The Desktop conflict screen showed two independent choices. This exposed
  and fixed a presentation defect: native-text changes had appeared identical
  because only foreign text was shown. A test was red before the fix; all 34
  presentation Desktop tests passed afterward, and the isolated Android APK
  built. In the updated Desktop UI, the card showed both native values;
  selecting the Android card and Desktop deck advanced the server/Desktop to
  revision 5. Desktop Room then had zero pending operations and no conflict
  snapshot. Physical Android manually pulled revision 5 and displayed the
  Desktop deck name with the Android card text, with zero pending changes.
  The Desktop deck edit was seeded transactionally in its isolated Room
  database because the Windows UI controller cannot generate Compose
  long-press; its card edit and both Android edits used the apps. No ordinary
  app data was touched. The disposable server and Desktop process were
  stopped, USB port reversal removed, and isolated data retained.
  Conflict-history rows still show raw device/item IDs; improve their
  readability in a later UX pass.
- Next: verify structural/deletion choices on Android/Desktop against the
  disposable server; ordinary app bindings remain on legacy storage until
  broader verification.
  During this check, opening the Android card editor also sent a Word Insights
  AgentDriver request; its sandbox preflight failed without affecting sync.
  Confirm whether this legacy enrichment request should be disabled in the
  isolated MVP flow.
- 2026-09-25 per-conflict deck/card edit choice completed for the isolated
  account test mode. The screen requires one server/local decision per saved
  conflict. Room applies the delta once, rebases only selected local edits,
  and clears the conflict atomically before the manual REST retry. Missing,
  duplicate, unknown, mismatched, and structural choices fail without
  modifying Room rows; multiple edits to one item are not offered this path.
  Focused Room tests (10), presentation tests (33), DI tests (7), and full
  server tests (151) passed with zero failures. The Android test APK built
  and Desktop compiled. The broad `:data:desktopTest` task still has 34
  pre-existing intentionally incomplete fixture tests throwing
  `IllegalStateException`; targeted resolver tests passed. A physical-device
  mixed-conflict UI check remains to do. Structural/deletion cases retain
  their guarded bulk actions.
- Next: validate mixed-conflict selection in isolated Android/Desktop clients
  against the disposable local server, then review remaining structural
  conflict paths and offline recovery. Ordinary app bindings stay legacy.
- 2026-09-25 recent-history status slice completed. Opening synchronization
  details now makes an account-scoped REST history read without starting data
  sync, then shows the server's latest operations with device display name,
  action, short affected-item identifiers, revision, and time. Empty, loading,
  and failed reads have distinct states; a selected-account change before or
  during the request rejects its result. DI Desktop tests (7) and presentation
  Desktop tests (33) passed with zero failures; the isolated Android APK built
  and Desktop compiled. On the physical RMX2001, history showed revisions
  3/2/1 and the server stayed at revision 3 after opening details. With the
  test server stopped, the dialog showed "Could not load recent changes"
  while keeping local version 3 and the deck visible. The disposable server
  was stopped and USB port reversal removed; no ordinary app data was used.
  Further history UX may use deck/card names instead of short identifiers if
  the history contract later carries or resolves those names.
- 2026-09-25 isolated two-client GUI conflict check passed. The physical
  RMX2001 and Desktop registered/signed in to the same disposable account;
  Desktop manually pulled revision 1 (two decks, one card). Both renamed the
  same deck locally from revision 1: Android to `AndroidConflict0925`, Desktop
  to `RemoteTestDeckNewName`. Android manual sync advanced the server to
  revision 2. Desktop observed remote revision 2 over the status feed, then
  manual sync opened a conflict screen showing both names and the Android
  device's edit. Before resolution, Desktop Room retained its local name,
  one pending operation, and one conflict snapshot at checkpoint 1. Choosing
  "Keep my deck changes" advanced the server and Desktop to revision 3;
  Desktop's pending/conflict rows cleared. Android manually pulled revision 3
  and showed `RemoteTestDeckNewName` with synchronized status. The ordinary
  Android package and legacy database were not touched. Accept-server and
  other conflict shapes remain covered by automated tests, not this GUI pass.
  The test server and Desktop process were stopped, and USB port reversal was
  removed; disposable account data was retained.
- 2026-09-25 post-review verification completed: focused presentation (4),
  DI (4), Room (13), and live coordinator (12) tests passed; then the full
  server suite (150), presentation suite (31), and DI suite (4) passed with
  zero failures. `:Android:assembleRemoteStorageTest` and
  `:Desktop:compileKotlin` succeeded. On the
  physical RMX2001, the separate test package signed in to the retained
  disposable account, displayed its account deck/card and both registered
  devices with platform/last confirmed revision/last sync time, completed a
  manual no-change sync, and returned immediately to the guest-only deck on
  sign-out. REST bootstrap still returned revision 1, two decks, and one
  card. The ordinary `com.kuts.klaf` package remained separately installed;
  the loopback server and USB port reversal were stopped afterward.
- Follow-up from verification: a successful no-change manual sync leaves the
  displayed "Last sync" time unchanged because repeated confirmation of the
  same revision is idempotent on the server. Decide whether this label should
  mean last revision advancement or whether the server should separately
  record every successful no-change sync. The UI-disabled Sign out state
  could not be visually captured during the short network request; Room/live
  tests cover the underlying switch rejection.
- 2026-09-25 source review/refactoring pass completed over the accumulated
  account/guest cutover, isolated Android/Desktop test configuration, and
  recent sync UI/server integration. Regression tests and a separate
  test/build/physical-device verification pass are complete as described
  above. A Desktop GUI two-client/conflict pass remains.
- Review findings addressed in source, awaiting execution: guest interim
  maintenance continues observing later sign-outs after one failed ensure.
  Account switching now rejects a manual sync in
  progress, while the drawer visibly disables Sign out until the attempt
  ends. New/updated unit and live Room/REST regression tests cover these
  boundaries. The disposable Desktop-directory test now verifies its resolved
  temporary target before recursive cleanup. A further status-UI review found
  that device platform, confirmed revision, and last successful sync time were
  dropped from the server event before display; the mapping and scrollable
  details view now retain/show them, with a mapping regression test. The
  review also confirmed that creating the shared interim deck on an account
  startup is intentional: server-side idempotence and two-device tests cover
  duplicate creation, so that offline-first behavior was preserved. No tests,
  builds, or device checks were run during the source-only review turn at
  the user's request; the subsequent validation pass is recorded above.
- 2026-09-25 disposable physical-device cutover check: an Android
  `remoteStorageTest` variant uses package `com.kuts.klaf.remote.storage.test`
  and a separate local Room database. The original installed package
  `com.kuts.klaf` was not modified. A separate Desktop test directory and
  a loopback-only server storage root provide analogous isolation. The real
  Android device reached that server via `adb reverse tcp:8090 tcp:8090`.
  In the test variant, guest deck/card creation, passwordless sign-up with
  guest transfer, manual sync, and REST bootstrap were observed end to end:
  the server reached revision 1 and retained the deck and card. Sign-in to
  the same account restored them without copying guest data.
- Physical testing exposed an empty guest deck list after sign-out because
  sign-up had moved its interim deck into the new account. A test-first
  account-selection observer now recreates the guest interim deck on return
  to guest mode. Its focused Desktop test and Android build passed; a second
  physical sign-out immediately showed only the guest interim deck. Keep
  ordinary launches on legacy storage until remaining two-device/conflict
  checks pass. The one observed transient deck-list error screen during
  navigation was not reproducible after relaunch and still needs diagnosis
  if seen again.
- A second disposable `DESKTOP` device was exercised against the same
  running test server at the REST boundary: sign-in was rejected with
  `DEVICE_NOT_REGISTERED`, explicit registration succeeded, sign-in then
  succeeded, and bootstrap returned revision 1 with both Android-uploaded
  decks and its card. The focused live Desktop account/coordinator suites
  and isolated Desktop configuration tests passed. This is not a Desktop GUI
  sign-in or two-app conflict check; those remain open.
- The isolated Desktop window and loopback test server were stopped after
  verification, and the temporary USB port reversal was removed. Their
  disposable test directories were retained for diagnosis; no existing user
  database or ordinary Android installation was cleared.
- 2026-09-25 account-aware deck-list and conflict route staged but inactive.
  With an account-scoped source, manual sync uses the Room/REST coordinator and
  sign-out uses the account session; it never starts the Firebase worker.
  A `NeedsResolution` result now navigates to the saved per-account Room
  conflict, whose choices call the client resolver and refresh the result.
  The route checks that the selected account still matches before resolving.
  Guest mode requests sign-in; the installed static source keeps the legacy
  worker. Focused deck-list router tests (4/4) and Android/Desktop Kotlin
  compilation and focused conflict-model tests pass. No physical-device
  account sync or conflict UI test yet.
- 2026-09-25 account sync indicator staged but inactive: the deck list now
  subscribes to the Room status observer and a WebSocket event feed only when
  account-scoped DI is selected. The indicator stays visible for a selected
  account and opens details with confirmed/known revisions, pending changes,
  registered devices, manual sync, and conflict resolution. Guest and legacy
  modes hide it. WebSocket observation never starts a sync request, and guest
  mode does not create a device ID or open a socket. Focused Room status,
  WebSocket event, and deck-list router tests pass; Android/Desktop Kotlin
  compilation passes. No visual or physical-device account test was run.
- Current cutover focus: complete remaining isolated Android/Desktop
  acceptance checks, including accept-server and structural conflict shapes,
  before switching the ordinary installed app. Do not touch its legacy
  database.
- Next cutover step: connect the account-scoped Room source, account-aware
  deck-list/authentication state, manual sync control, and conflict route as
  one app path. Keep the static/legacy path active until this can be tested
  with disposable data on Android and Desktop.
- 2026-09-25 account DI preparation complete but inactive. Android/Desktop
  provide the REST account client, persisted device identity, pending sign-up
  store, and `ServerAccountSession`; the shared authentication ViewModel selects
  that path only when the app's `RoomDatabaseSource` becomes account-scoped.
  Device identity is loaded lazily when a request starts. The live account
  session suite and full server suite (150/150) pass; Android and Desktop Kotlin compile. REST and the
  existing server connection now share endpoint settings from developer-local
  `klaf.client.server.host`/`port` (Android build values, Desktop runtime
  values found at the project root when launched from a subdirectory). Those
  keys are absent from this worktree's `local.properties`, so
  localhost remains the fallback; a physical Android device cannot reach the
  computer's server until its LAN host is configured. No physical-device
  account test was run. Legacy UI/storage bindings remain active.
- 2026-09-25 manual sync DI preparation: the same persisted account-device
  provider now supplies IDs to sign-in and manual synchronization. The
  coordinator resolves its device ID only when an explicit attempt starts;
  Android/Desktop bind a shared REST sync transport, outbox, delta applier,
  and coordinator without starting background sync. The focused coordinator
  test, full server suite (150/150), and Android/Desktop compilation pass.
  The deck-list still invokes the legacy worker, so these new bindings are
  deliberately inactive until its account-aware sync and conflict actions
  are connected.
- 2026-09-25 review save boundary implemented. `DeckReviewViewModel` now makes
  one `SaveCompletedDeckReviewUseCase` call. The installed binding intentionally
  delegates to the previous two legacy use cases; a separate
  `RoomDeckReviewResultRepository` obtains the selected account checkpoint and
  uses `RoomReviewResultWriter` for an atomic deck/summary/outbox save (guest
  remains local-only). The Room review suite passes, including a nonzero
  checkpoint. DI now selects the Room result writer and Room review-info reader
  whenever its `RoomDatabaseSource` is account-scoped; the installed static
  source still selects the legacy stores. Android and Desktop Kotlin compile.
  The Room binding is not yet active: switching the source requires the
  account-scoped auth/navigation/sync cutover. Full `:data:desktopTest` still
  has 34 intentionally red migration/client-sync adapter contracts; focused
  new tests pass.
- 2026-09-25 transactional ordinary-write slice: `StorageTransactionRepositoryRoom`
  now captures account-scoped deck/card before-and-after state inside the same
  Room transaction and appends durable operations for create, rename, edit,
  delete, move, and review-state changes. Guest writes add no account operations;
  failed transactions roll back both data and queue, and account switching
  isolates the queues. A whole-deck delete queues only DeleteDeck, not child
  DeleteCard operations. The transfer path exposed a missing protocol field:
  MoveCard now carries both recalculated deck review-pass durations, and the
  server applies them atomically with the card move. Requirements were updated.
  Focused account-outbox Room tests: 7/7 passed; full server suite: 149/149
  passed, including a live REST persistence test for both durations. This is
  staging work only: production DI/navigation still use the legacy path.
  The manual coordinator now pauses account edits for the whole attempt,
  including the network wait; it releases the pause on success, failure, and
  coroutine cancellation. A live Room/REST contract now rejects a late edit
  during the request and accepts it afterward against the new revision. The
  account-attempt Room suite and full 149-test server suite pass; Android debug
  Kotlin compilation passes. Remaining before cutover: check all production
  mutation paths against this recorder, bind account/session/transport/
  coordinator/UI consistently, and verify on disposable app data.
- 2026-09-25 cutover audit detail (addressed in staging): the review screen
  originally saved deck and review details separately through legacy stores.
  It now uses one result-saving boundary, with a Room implementation selected
  automatically when the app source becomes account-scoped. The installed
  static source continues to use the legacy stores. No physical-device data
  was used.
- 2026-09-25 authorized active Room/REST app cutover in progress. DI audit
  found ordinary deck/card use cases still write Room without account outbox
  operations, and Android/Desktop app modules still bind legacy Firebase or
  static Room sources. First add transactional outbox coverage for all ordinary
  mutations; only then switch account/auth/sync bindings and expose the
  conflict screen. Preserve legacy user databases untouched and do not test
  with real device data during this staging work.
- 2026-09-25 conflict-resolution presentation slice: a shared Compose screen
  lists local/server changes, recent device actions, and only currently safe
  bulk choices. A shared action enum dispatches each choice through the tested
  client coordinator; live Room/REST tests exercise the dispatch. Five
  presentation-model tests pass, including mixed conflicts and a reviewed
  destination; Android debug Kotlin compilation passes. The screen is not
  exposed in app navigation yet: installed DI still selects the legacy
  Firebase authentication/sync and static Room source. Activating it safely
  requires the separate account-scoped Room/REST DI cutover and an app host
  that observes the saved conflict, handles loading/errors, and invokes the
  coordinator. Mixed per-conflict manual choices are also not implemented.
- 2026-09-25 all four obsolete server-only conflict-resolution contracts now
  run through real client Room + REST tests. Accept-server drops a conflicting
  card edit; keep-local rebases and uploads it. For source-deck deletion, an
  explicit manual action rescues a moved card into its unreviewed destination;
  another explicit action restores a deleted deck with all its cards when a
  local card edit is kept. Local IDs and image/insight payload paths are
  preserved by the same snapshot-to-sync converters as initial upload.
  Invalid choices roll back atomically and retain the conflict, including a
  destination that was reviewed on the server. Full `:klaf-server:test`: 148
  passed, zero failed. Focused `:data:desktopTest` conflict-resolver suite
  passes. Android debug Kotlin compilation passes after the structural edits;
  the rescue test also verifies that the image reference reaches the server.
  Conflict-resolution UI and general per-conflict manual choices remain.
- 2026-09-25 shared interim-deck identity slice: an account derives one sync ID
  from its normalized email; guest retains a separate ID. Sign-up transfer
  remaps the guest interim deck and preserves its cards; account-local interim
  creation queues its first upload inside the caller's Room transaction.
  Download into a second device retains local navigation ID `-1`. The server
  accepts the second device's AddDeck as an idempotent no-op and merges its
  independent cards, without inserting a duplicate deck. Focused real
  server/Room tests pass, including two first uploads from the same base
  revision on different devices. Full `:klaf-server:test`: 148 tests, 144
  passed, four legacy contracts still invoke the deliberately unimplemented
  server-side conflict resolver. Android debug Kotlin compilation passes.
  Remaining: replace those contracts
  with client-side resolution scenarios and review direct repository-write
  transaction guarantees.
- 2026-09-25 manual-bootstrap contract fixture connected to disposable real
  Android Room v8 exports and companion review DataStore files. All eight
  contracts pass through `AndroidRoomBackupReader` and
  `ManualAccountBootstrap`: image identifier and review fields survive;
  orphan input leaves no account; retry does not duplicate; old local save
  version and Vocabulary Source rows do not enter the server baseline.
  Source-hash assertions cover both immutable backup files. No real user
  backup or physical device was touched. The full server run completed 147
  tests: 141 pass; only the six previously identified server-sync contracts
  remain red. Android debug Kotlin compilation passes.
- 2026-09-25 server contract fixture now uses the real loopback Ktor server,
  REST protocol, `/sync-events`, and temporary per-account SQLite files rather
  than failing at setup. After aligning old assertions with explicit
  post-Room device confirmation and structured conflict reasons, 37 of 43
  server sync contracts pass. Six remain red: four older server-side
  `resolve` calls must be moved to real client-side Room resolution tests,
  and two expose the still-missing shared interim-deck initialization.
  Passwordless account, revisions, retry, history, card/deck changes,
  conflicts, isolation, restart, and event scenarios now exercise production.
  The card-move contract now carries an actual image asset ID inside
  `mnemonicJson`, so its preservation assertion cannot pass on a dropped
  test-only field.
  Added `PATCH` rename and `DELETE` removal for registered devices; a live
  Ktor/Room/WebSocket test passes, including retained account content and
  history. The full server run completed 147 tests: 133 pass, with only the
  six stated server contracts and eight unconnected bootstrap-import
  contracts red. Android debug Kotlin compilation passes. No app DI/UI
  switch or physical-device install was performed.
- 2026-09-25 device sync-position slice complete at the protocol/coordinator
  layer: server Room v6 persists each registered device's last confirmed
  revision and successful-sync time; `POST /api/v1/sync/confirm` rejects stale
  or future positions, is idempotent on retry, and publishes updated device
  state through `/sync-events`. The REST client exposes confirmation and the
  manual coordinator requires it after a conflict-free Room commit. A lost
  confirmation response leaves the local checkpoint intact for a safe manual
  retry; an unresolved partial conflict does not confirm. The real v5→v6
  migration, Desktop coordinator, live REST/WebSocket/coordinator/account
  tests, and Android debug Kotlin compilation pass. App DI/UI binding and
  physical-device verification remain; no device install was performed. A
  subsequent full `:data:desktopTest` run reported 34 expected red contract
  tests: 31 `ClientSyncContractTest` cases still require their real Room+REST
  adapter, and three `RoomMigrationContractTest` cases still require the
  version-8-to-account-scoped adapter. All other 82 Desktop tests passed. A
  full `:klaf-server:test` run has 51 expected red test-first contracts:
  43 `ServerSyncContractTest` cases need the Ktor/SQLite adapter and eight
  `BootstrapImportContractTest` cases need the real export-import adapter;
  the other 95 server tests passed.
- 2026-09-25 Room-backed sync status slice: observable checkpoint, outbox count,
  and conflict queries combine with `/sync-events` and the manual coordinator's
  running/failed state. Guest status is hidden; each account sees only its own
  revision, pending count, conflict, and device list. Conflicts/errors and a
  server revision older than the confirmed local checkpoint show red, an
  unavailable event channel shows gray, and pending or newer remote changes
  show yellow. Connected and equal revisions with no pending changes show
  green. Real Desktop Room tests cover transitions and live invalidation;
  coordinator state, neighboring live Ktor sync tests, and Android debug
  Kotlin compilation pass. This model is not yet bound to the legacy Firebase
  deck screen or app DI; no physical device was touched.
- 2026-09-25 shared client `/sync-events` slice: a Ktor connector and
  account-selected observer expose channel state, server revision, and device
  presence without starting REST synchronization. Guest mode is hidden;
  account switching closes the old socket and clears the prior account's
  state. Dropped or silent connections close and retry, and the next initial
  state refreshes the server revision. Three fake-transport Desktop tests,
  a live Ktor client/server test, four server event tests, and Android debug
  Kotlin compilation pass. App DI/UI binding, device sync-position fields,
  and physical-device verification remain; no device install was performed.
- 2026-09-24 server-side `/sync-events` slice: a registered device receives the
  current revision and account-scoped device presence on connection. Other
  connected clients receive state changes after device registration, connect,
  and disconnect. A newly committed REST sync emits a revision-only event;
  an idempotent retry does not emit another, and another account receives no
  event. Unregistered devices receive no account state. The existing AI `/ws`
  remains separate. Four live Ktor event tests
  pass, as do neighboring REST/AI protocol tests and Android debug Kotlin
  compilation. Device sync-position persistence, reconnecting client/UI
  binding, and broader event cases remain; no physical-device install.
- 2026-09-24 partial-conflict edit pause: after a partial result advances the
  selected account's checkpoint but leaves a saved conflict, new deck/card
  edits recorded through `RoomSyncOutbox` fail inside the Room transaction
  before any local row or outbox write.
  Other accounts remain editable; an unapplied conflict at the same checkpoint
  does not cause this pause. The pause survives database restart and clears
  when the conflict is resolved. Targeted `RoomSyncOutboxTest` and
  `RoomSyncPartialApplierTest`, focused `*RoomSync*Test` Desktop tests, live
  Ktor coordinator tests, and Android debug Kotlin compilation pass. No UI/DI
  switch, physical-device install, or migration was performed. The legacy
  editing repositories are not yet routed through this guard; client binding
  must do that before this pause protects the installed app.
- 2026-09-24 initial resolution/partial-apply slice: client Room can atomically
  accept the server for every saved conflict, applying the full delta while
  retaining independently accepted rows, acknowledging old operation IDs,
  advancing the checkpoint, and clearing the snapshot. Simple conflicting
  deck-name edits can instead keep their local name via a new operation at the
  latest revision. The coordinator immediately repeats REST sync after either
  choice; transport failure after local resolution leaves a retryable outbox.
  For a deliberately narrow, provably disjoint partial batch (accepted new
  decks vs conflicting deck-name edits, with no card/deletion delta), it now
  applies the accepted decks and acknowledges their IDs immediately while
  keeping the conflicted deck and saved response. Unsupported structural or
  overlapping partial results use the earlier safe-retry path. Desktop Room
  and live Ktor tests cover ID preservation, cascade deletion, rollback,
  stale edits, keep-local rebase, partial acceptance, and immediate resync;
  the final focused Room/live-Ktor run and Android debug Kotlin compilation
  pass (resolver 6/6, partial applier 3/3, live coordinator 7/7).
  No conflict UI/DI binding or physical-device migration was performed.
  General per-conflict/manual choices remain open before activation; the user
  subsequently agreed to pause new edits while a partial conflict is unresolved.
- 2026-09-24 durable conflict slice: client Room schema v14 stores the latest
  full conflict response in the selected account's file. The coordinator saves
  it only when the checkpoint and pending operation IDs still match the REST
  request. Saving does not advance the checkpoint or remove operations. A
  successful conflict-free delta clears the snapshot in the same transaction
  as local rows, checkpoint, and acknowledgements; failed apply preserves all.
  Four real Desktop Room behavior tests, a real v13→v14 migration test, and
  six live Ktor/Room coordinator tests pass. Focused `*RoomSync*Test` Desktop
  tests and Android debug Kotlin compilation also pass. Partial-acceptance
  acknowledgement, conflict resolution actions/UI, and persisted UI recovery
  remain to implement; no physical device was touched.
- 2026-09-24 recent-history REST slice: `GET /api/v1/sync/history` and the
  shared HTTP client return the current server revision and latest 20 accepted
  changes, newest revision first, with device/action/affected sync IDs/time
  but no deck content. The server checks the account and registered device
  and reads revision/history consistently. Two new live Ktor tests pass for
  ordering, limit, empty history, access errors, read-only behavior, and
  payload privacy; two neighboring REST transport tests and Android debug
  Kotlin compilation also pass. This is not yet displayed in app UI.
  Partial-response reconciliation needs durable conflict handling and remains
  separate; no physical-device data or app installation was touched.
- 2026-09-24 manual REST coordinator: `ManualRoomSyncCoordinator` now snapshots
  the selected account's confirmed revision and durable outbox, sends protocol
  v2 REST on an explicit call, checks server response IDs/revisions, and applies
  conflict-free deltas through the transactional Room applier. Six tests against
  a live Ktor server and scoped Desktop Room pass: upload retry after a lost
  response, remote-only pull, conflict preservation, partial acceptance with
  idempotent retry, account switch during flight, and a new local edit during
  flight. Focused `*RoomSync*Test` Desktop tests and Android debug Kotlin
  compilation also pass. A response with conflicts currently leaves even its
  independently accepted operations pending locally for safe replay; automatic
  partial acknowledgement and conflict resolution UI remain required to meet
  the full MVP. The coordinator is not yet bound to app UI/DI, and no physical
  device data or app installation was touched in this slice.
- 2026-09-24 conflict-free delta slice: `RoomSyncDeltaApplier` applies server
  deck/card updates by immutable sync ID while preserving local integer IDs,
  handles moves and cascading deck deletions, validates parent references and
  changed deck card counts, and acknowledges accepted outbox IDs with the
  checkpoint in one Room transaction. It rejects conflict responses and stale
  deltas rather than silently overwriting unresolved edits. Six test-first
  Desktop Room tests pass, including new-parent ordering, ID preservation,
  missing-parent rollback, and incorrect-count rollback. All focused
  `*RoomSync*Test` tests and Android debug Kotlin compilation pass. This is
  not yet called by an HTTP sync coordinator; partial-accept/conflict
  reconciliation remains open, so installed app behavior is unchanged.
- 2026-09-24 local sync checkpoint: client Room schema v13 persists the last
  confirmed server revision in each account file. Outbox acknowledgement now
  applies local server changes, advances the checkpoint, and removes accepted
  operation IDs in one Room transaction; a failed apply rolls all three back.
  A local edit rejects a base revision different from that account's checkpoint
  before any row changes. Test-first Desktop Room checks cover real v12→v13
  migration, restart, account isolation, monotonicity, and rollback; 14 focused
  Room sync tests and Android debug Kotlin compilation pass. The REST sync
  coordinator and conflict/delta application are not yet wired to this API.
  The full Desktop data run completed 86 tests: 52 passed and 34 failed only
  in the pre-existing `ClientSyncContractTest` and `RoomMigrationContractTest`
  placeholder adapters (`not implemented yet`). An additional scoped-database
  test initially failed under the new base-revision invariant; its setup now
  establishes each account checkpoint and passes separately and in the full run.
- 2026-09-24 passwordless account UI preparation: a domain account-session
  contract and `AccountAuthenticationViewModel` now handle email-only sign-up,
  sign-in, and an explicit new-device confirmation prompt. The existing form
  supports disabled password fields in this mode. Registration is rejected
  unless the preceding sign-in returned `DEVICE_NOT_REGISTERED`; after the
  confirmed registration, the account database opens without a second sign-in,
  while guest decks remain in the guest database. Three live Ktor + Desktop
  Room session tests and three presentation tests pass; Android debug Kotlin
  compiles. The new view model is deliberately not bound in app DI yet: the
  installed Firebase UI, legacy Room file, and Firestore sync path remain
  active. Before activation, wire scoped repositories/review data and account
  state through DI, replace legacy sync actions, hide Delete account, map
  account errors to UI messages, and verify the full device flow after backup.
  A full `:klaf-server:test` run completed 128 tests: 77 passed and 51 failed
  only in the pre-existing `ServerSyncContractTest` and
  `BootstrapImportContractTest` placeholder fixtures, which deliberately throw
  `not implemented yet`. No other server suite failed; these two fixture
  adapters remain open work before the aggregate task can be green.
- 2026-09-24 physical Android verification: after the RMX2001 was connected,
  `:Android:assembleDebugAndroidTest` and `:Android:assembleDebug` passed. Both
  APKs were installed with `adb -s <test-device-serial> install -r -t` (no uninstall
  or data clearing). The focused `ScopedDeckReminderInstrumentedTest` passed
  1/1 on that phone: accounts with the same local deck ID have distinct alarm
  PendingIntents; canceling one retains the other and removes the matching
  legacy unscoped PendingIntent. The test neither launches the UI nor reads or
  migrates the user's Room data. This does not yet verify real alarm delivery,
  notification navigation, WorkManager, or account-switch UI behavior.
- 2026-09-24 Android reminder-isolation slice: scoped Room selection now
  cancels/dismisses reminders for the old database before scheduling the new
  database's reviewed decks. Android alarm PendingIntents and posted deck
  notifications carry account scope; the receiver and MainActivity reject
  stale scope, including old unscoped alarms/navigation after activation.
  The selected-account preference and scoped-reminder flag commit together.
  Periodic and boot/startup rescheduling read the selected Room database in
  scoped mode, while the installed legacy UI retains its original path.
  Three real Desktop Room switch tests pass, including colliding deck IDs,
  no-op selection, and cancellation of an unscheduled deck's stale alarm;
  `:Android:compileDebugKotlin` passes. Android AlarmManager, WorkManager,
  notification navigation, and sign-in/out still need device or
  instrumentation verification before the new account UI is enabled. Next:
  connect the account session and per-account repositories to app UI, then
  verify reminder behavior on an Android device.
- 2026-09-24 manual bootstrap continuation: a read-only reader migrates a
  disposable copy of an Android Room v8 export to the current schema, merges
  its companion review DataStore, and validates card JSON before publishing a
  server account. The server stages deck/card rows transactionally and registers
  the account only afterward; imported content is revision 0 with no device.
  Seventeen focused real Room/server tests pass, covering first-device download,
  unchanged source bytes, orphan/duplicate IDs, incomplete or unknown review
  data, malformed card JSON, WAL rejection, and reimport protection. The orphan
  failure includes both card and missing-deck IDs; identical duplicate rows
  are caught in raw DataStore JSON before `Set` decoding. The
  `importAndroidBackup` Gradle task is registered; its runbook is in
  `server-database-manual-bootstrap.md`. Android debug Kotlin compiles. No real
  user backup was supplied or imported, and the installed UI still uses the
  legacy account/review path.
- 2026-09-24 review-summary slice: client Room schema v12 stores the missing
  latest-review fields on each deck, with an account-switching Room repository.
  Server account schema v5 and the shared sync payload preserve those fields
  across guest transfer, upload, and bootstrap. Real v11→v12 client and v4→v5
  server migrations passed. A new review writer saves the deck update and
  account outbox operation atomically; guest reviews stay local. A regression
  test exposed stale review snapshots overwriting a newer local deck name, so
  the writer now changes only review fields. The server accepts exact review
  date lists, including an empty completed-iteration list after the first
  pass. Combined post-refactor Room/server tests and Android debug Kotlin
  compilation passed. This writer is not yet called by the installed UI.
- A read-only companion DataStore import preflight is now tested. It merges
  every legacy review field into staged deck rows and rejects a missing or
  malformed backup, orphan/duplicate review entries, and review-count
  mismatches before any target write. Its four focused server tests passed.
  This preflight is now part of the tested manual bootstrap pipeline.
- Next: validate the real Android export when supplied, then connect the tested
  account session, device confirmation, scoped repositories, and atomic review
  writer to app DI/UI and verify the complete flow on a safe Android install.
  Keep the legacy database untouched until migration and switching are verified.
- The outbox now takes `ActiveLocalRoomDatabase` and validates its selected
  account within each write/read/acknowledgement transaction. Wrong-account
  and guest writes are rejected before the local change; the mismatch test
  failed against the old API and then passed. Four outbox tests and three
  scoped-factory/isolation tests pass on Desktop. A shared passwordless
  account REST client now handles sign-up, sign-in, and explicit device
  registration; live Ktor transport tests pass. A separate account session
  now invokes guest transfer, but neither is connected to application
  authentication, scoped app DI, or Android reminders.
- Account-flow review found a bootstrap corner case: uploading an already-
  reviewed guest deck as `AddDeck` followed by ordinary `AddCard` operations
  triggered the server's reviewed-deck insertion guard. A new live test failed
  before the fix and passes after it: the server now permits those cards only
  when their reviewed deck was accepted earlier in the same atomic request;
  later additions remain conflicts, and exact retry does not duplicate rows.
  Guest transfer now enqueues the initial deck/cards in order, but the manual
  sync coordinator still needs to upload them together. The account REST
  transport must not be activated in the UI before review-data migration,
  device-confirm UI, and per-account reminder handling are tested.
- Added a Room-level guest-to-new-account transfer: while guest is selected,
  it copies deck/card rows and ordered initial `AddDeck`/`AddCard` operations
  into the account file in one account transaction, verifies an exact prior
  copy on retry, then removes guest decks/cards in a guest transaction. It
  rejects a nonempty unrelated account file without deleting guest data.
  Three real Room tests pass (reviewed-deck/card transfer, conflicting target
  preservation, and interruption after account copy followed by idempotent
  retry). All active-selection, outbox, scoped-factory tests and Android
  debug Kotlin compilation passed. A new account-session coordinator now
  calls this transfer during sign-up; two live Ktor + Desktop Room session
  tests pass for sign-up/guest view/sign-in and explicit new-device
  registration. It is not connected to app DI/UI and does not migrate
  Android's device-global review DataStore.
- 2026-09-24: added a durable sign-up request ID to the server registry
  (Room schema v2) and account REST request. The same ID with the same email
  and first device replays the original account; a different ID or device
  remains `ACCOUNT_EXISTS`. The new storage and live REST replay tests first
  failed to compile against the old API, then passed. Added Android/Desktop
  stores for a pending sign-up attempt; the session writes the attempt before
  REST and retains it until guest transfer and local account selection both
  finish. A live lost-response recovery test and Android compilation are
  passed alongside the two existing live session tests, and Android debug
  Kotlin compilation passes. A real v1→v2 registry migration test passes:
  legacy accounts remain non-replayable, while fresh accounts can use request
  IDs. No installed app flow is switched to this path yet.
- 2026-09-24: added one persistent per-installation device ID and display
  details for Android/Desktop. A new Desktop persistence test first failed
  because the provider did not exist, then passed after implementation;
  `:Android:compileDebugKotlin` also passed. It remains to connect the
  provider to app DI and the account UI.
- Guest-transfer review found that an empty guest database previously made
  transfer return without checking a nonempty preexisting account file. A
  real Room regression test failed against that behavior. The transfer now
  accepts a nonempty target after guest deletion only if every target
  deck/card is an unsynced initial upload with the exact ordered operation
  queue; unrelated account content is rejected. The combined active-database,
  persistent-device, live account-session, registry, and REST-client suites
  all pass after the fix.
- Review audit: the live `DeckRepetitionInfo` repository is still backed by
  Android's device-global DataStore, while the current `SyncDeck` has only
  part of that detailed summary. Do not activate scoped account databases or
  the new account UI until the missing review fields are stored per account
  in Room, included in server sync/bootstrap and guest transfer, and Android
  reminders are switched by account. This is a data-loss/isolation gap, not
  a mere UI follow-up.
- 2026-09-23 scoped-Room review: a real regression test exposed that interim-
  deck existence was checked before the write transaction. An account switch
  in that gap could skip creation in the newly selected database. The check
  now runs inside the same transaction as creation. The new test failed before
  the fix and passes after it; it also checks that a repeat call does not
  duplicate the deck or reset the local version. All seven active-selection
  tests and all domain Desktop tests pass. Desktop DI and Android debug Kotlin
  compile. No account-switch UI or device test was run.
- Review follow-up before activating scoped databases: the current
  `RoomSyncOutbox` accepts a caller-provided account ID independently of its
  database file, so the new sync coordinator must bind them and reject a
  mismatched account. The still-experimental Vocabulary Source-to-card path
  also does not enforce the unreviewed-destination rule; it remains outside
  Phase 1 per the agreed Vocabulary Source deferral. These paths are not
  claimed to be end-to-end safe by this review.
- MVP requirements and the code re-audit are complete. Room Multiplatform backs
  account/device registration and the new server REST sync endpoint. A shared
  Kotlin HTTP client now talks to those endpoints, but the app does not yet
  call it from its local editing and manual-sync flow. Client
  Room schema v11 enforces card ownership and stable sync IDs, and stores a
  durable outbox; domain transfer preserves card identity. Server sync now
  applies deck/card/review operations
  atomically, accepts independent changes, returns conflicts and incremental
  pull data, and recognizes exact retries. This is **server-side** progress, not
  a working end-to-end client sync. Next: activate scoped local databases from
  the new server account flow, wire edits and manual sync to the outbox,
  implement conflict-resolution actions, review-data migration, and
  `/sync-events`; connect the scenario fixtures to real components as they land.
  The pre-outbox REST review is complete. Android and Desktop can open
  separate guest/account Room files. A persisted active selection and
  switchable local repositories are implemented and tested, but the installed
  DI source still deliberately uses the legacy database. The app must not
  activate scoped files against its old Firebase authentication UI before the
  new server account flow, reminder isolation, and guest transfer are ready.
  No live edit path writes to the outbox yet, and no sync result advances the
  local confirmed revision.
- Keep this status file current as tests, implementation, and verification
  advance. Record each completed change, command/result, known failure, and
  next step so work can resume from the file without relying on chat history.
- On 2026-09-23, published the required AgentDriver `0.10.11-SNAPSHOT` SDK to
  Maven Local from its historical commit `4d539a4` in an isolated worktree.
  The AgentDriver `development` checkout was not modified; temporary worktrees
  were removed after publication. `:klaf-server:compileKotlin --offline`
  succeeded against the published artifacts.
- 2026-09-23 red-suite result: `:domain:desktopTest` ran 30 tests (21 passed,
  nine target-behavior failures); `:data:desktopTest` ran 43 (five passed,
  four target-behavior failures, 34 fixture-setup failures); `:klaf-server:test`
  ran 51 (all fixture-setup failures). Total: 124 tests, 26 passes, 13 failures
  against current local behavior, and 85 cases stopped at fixture setup. The
  last combined command was
  `:domain:desktopTest :data:desktopTest :klaf-server:test --offline --no-daemon --continue`;
  later targeted/full data and server reruns covered the added cases.
- The eight new domain tests cover card creation/move/edit/delete around the
  first deck review. Five fail against current behavior. Four updated transfer
  tests fail because current transfer permits reviewed destinations and
  deletes/recreates the card instead of preserving its identity.
- Nine real desktop Room tests cover card ownership, cascade, unaffected decks,
  updates, stable moves, grouped commit/rollback, and direct foreign-key/index
  checks. Five pass; four fail because the current schema lacks the required
  card-to-deck foreign key/cascade/index.
- 31 client-sync, three Room-migration, 43 server-sync, and eight bootstrap-import
  contract cases compile, but all 85 stop at deliberately unimplemented test-only
  adapters. Their scenario assertions have **not** exercised Room/REST/Ktor or
  import behavior. Connect them to real temporary databases and transports as
  those components are implemented; preserve the assertions rather than
  replacing failures with mocks that only echo expected results.
- Added a separate live-server test harness using the production CIO/Ktor
  server on a temporary loopback port and JDK HTTP/WebSocket clients. The two
  targeted checks ran: existing AI `/ws` returned its real ready message
  (pass); passwordless `POST /api/v1/accounts` reached the real server but
  returned 404 instead of 201 (meaningful red). No fake server, sync route, or
  SQLite backend was created. `klaf-server` test execution now uses installed
  JDK 21 because the locally published AgentDriver dependency has Java 21
  bytecode; main Kotlin compilation still targets JVM 17. Verify the intended
  server runtime/toolchain before deployment.
- Full rerun after adding the live checks:
  `:domain:desktopTest :data:desktopTest :klaf-server:test --offline --no-daemon --continue`
  ran 30/43/53 tests with 9/38/52 failures, respectively. Total: 126 tests,
  27 passes, 14 target-behavior failures (including the real REST 404), and 85
  fixture-setup failures. Server and client synchronization integration remains
  unverified; the live Ktor smoke check does not substitute for the missing
  account database or sync protocol.
- First implementation slice on 2026-09-23 uses Room with `BundledSQLiteDriver`
  in `klaf-server`: one registry file maps normalized email to an opaque account
  database filename; a separate Room file per account holds its first registered
  device. `POST /api/v1/accounts` creates both and returns 201, duplicate email
  returns 409, and malformed email/device input returns 400. No passwords or
  client keys are stored. Room schema JSON was generated for both databases.
  Four real Room storage tests and four live Ktor tests pass, including the
  existing AI `/ws` check. The full three-module rerun now has 30/43/59 tests
  and 9/38/51 failures: 132 total, 34 passes, 13 target-behavior failures,
  and the same 85 setup-only contract failures.
- 2026-09-23: added `POST /api/v1/accounts/sign-in` and
  `POST /api/v1/accounts/devices`. Sign-in checks registration without changing
  the database; an unknown device gets `DEVICE_NOT_REGISTERED`. Explicit
  registration persists the device, identical retries return 200 without
  duplicates, and a reused ID with different details returns 409. Missing
  account and invalid input have separate 404/400 responses. A missing mapped
  account file now raises a storage error instead of silently recreating an
  empty Room database. Five new live Ktor tests and three new Room tests pass;
  the complete targeted account suite is 16/16 green. The latest full command
  `:domain:desktopTest :data:desktopTest :klaf-server:test --offline --no-daemon --continue`
  ran 30/43/67 tests with 9/38/51 failures: 140 total, 42 passes, 13
  target-behavior failures, and the same 85 setup-only contract failures.
  A subsequent safety adjustment retains the account file once registry insert
  begins, because cancellation could leave its commit outcome uncertain;
  the targeted 16 tests were rerun successfully after that change.
- 2026-09-23: client Room schema advanced from v8 to v9. `RoomCard.deckId`
  now has a cascading foreign key to `RoomDeck.id` and a non-unique index;
  deck writes use upsert so changing an existing deck does not delete its
  children. A shared KMP migration preserves valid cards and rejects legacy
  orphan cards with both IDs before changing the old database. Three new
  migration tests pass, including opening a complete exported v8 schema via
  Room v9 and confirming an orphan leaves v8 intact. All nine existing real
  card/deck integrity tests pass (previously five). Android debug Kotlin
  compilation succeeded; iOS compilation was not run on this Windows host.
  The latest full three-module run has 30/47/67 tests with 9/34/51 failures:
  144 total, 50 passes, nine remaining target-behavior failures, and 85
  setup-only contract failures. The larger account migration contract still
  stops at its unimplemented adapter, so review-DataStore migration is not
  verified or implemented.
- 2026-09-23: client Room schema advanced from v9 to v10. Deck and card rows
  gain unique sync IDs generated at first local persistence and
  `lastChangedServerRevision` (initially zero). Ordinary DAO writes preserve
  both fields; a separate guarded DAO operation can record a confirmed server
  revision later. Card writes now abort on a duplicate sync ID rather than
  replacing another card. A KMP migration assigns distinct IDs to existing
  v9 rows without reading the legacy save version; the full v8→v9→v10 Room
  opening test also passes. Four new real identity/migration tests pass, as do
  the earlier integrity tests. Android debug Kotlin compilation succeeded.
  Latest full three-module run: 30/51/67 tests with 9/34/51 failures, or 148
  total, 54 passes, nine existing domain-behavior failures, and 85 contract
  tests still stopped at unimplemented adapters. At that stage, the domain
  card-move use case still deleted/recreated the card; the following review
  slice corrected it. iOS was not compiled here.
- 2026-09-23 code-review/refactoring slice: the transfer use case previously
  deleted and recreated cards, losing local ID, sync ID, mnemonic, and other
  fields. It now loads current Room-backed deck/card state in one transaction,
  rejects moves into a reviewed destination, and changes only the card's deck
  ID. The add-card use case now rejects new cards after the deck's first
  review. A real Room integration test confirms the transfer retains card
  identity and metadata. Existing transfer-test fakes were corrected to update
  by ID like Room instead of allocating a new ID for every write.
- The same review found that a full-row local Room update could overwrite a
  server revision confirmed between its read and write. Partial Room updates
  now write only local deck/card fields, leaving sync ID and server revision
  untouched. A real Room test covers an update based on a stale row after a
  newer revision is recorded; generated SQL was also checked to exclude both
  sync columns. No schema migration was needed.
- Full verification command:
  `:domain:desktopTest :data:desktopTest :klaf-server:test :Android:compileDebugKotlin --offline --no-daemon --continue`.
  Domain: 30/30 pass. Data: 19/53 pass, 34 setup-only contract failures.
  Server: 16/67 pass, 51 setup-only contract failures. Overall: 150 tests,
  65 passes, zero remaining target-behavior failures, 85 expected setup-only
  failures pending production sync/import adapters. Targeted real Room and
  domain suites passed. `:data:compileDebugKotlinAndroid` and
  `:di:compileDebugKotlinAndroid` passed (the latter after adding local-only
  non-secret values to the ignored test-build `SecretConstants.kt`). Full
  `:Android:compileDebugKotlin` did not complete because this worktree lacks
  `apps/Android/google-services.json`; iOS was not compiled on Windows.
- 2026-09-23: reused the client Room deck/card entities, DAOs, and converters
  in each server account database (schema v2); the generated auto-migration
  from v1 preserves registered devices. Three new real Room tests pass for
  separate account files with overlapping sync IDs, persistence after restart,
  orphan/duplicate rejection, parent deletion cascade, and v1→v2 migration.
  The full verification command
  `:Android:compileDebugKotlin :domain:desktopTest :data:desktopTest :klaf-server:test --offline --no-daemon --continue`
  compiled Android successfully. Domain: 30/30 pass; data: 19/53 pass with 34
  setup-only contract failures; server: 19/70 pass with 51 setup-only contract
  failures. Overall: 153 tests, 68 passes, zero target-behavior failures,
  85 tests still awaiting real sync/import adapters. iOS was not compiled.
- The user supplied Android `google-services.json` and a separate server
  Firebase service-account file. Git ignores the Android file already; added
  `/klaf-server/secrets/` to `.gitignore` and verified both files are ignored
  and untracked without reading their contents. The server service account is
  not used by this Room storage slice.
- 2026-09-23: server account schema v3 adds a singleton current-revision record
  and durable accepted-change history with unique operation IDs, base/server
  revisions, device, action, affected sync IDs, and timestamp. A grouped Room
  transaction now applies a caller's accepted data changes, advances the
  revision once, and records every accepted operation together. Failed writes
  roll everything back; an exact retry returns its prior revision without
  reapplying data. The history can be read in full or as a recent limited list.
  Six new real Room tests pass, covering rollback, replay, two operations in
  one revision, account isolation, restart, and v2→v3 migration of existing
  device/deck/card data. Device IDs are normalized consistently. The last full
  three-module run plus Android compile had 30/53/75 tests in domain/data/server:
  73 passes and the same 85 setup-only sync/import failures; Android compiled.
  After the final normalization test, targeted 6/6 passed and a full server
  rerun had 25/76 passes with its same 51 setup-only failures. Combined current
  totals: 159 tests, 74 passes, zero target-behavior failures, 85 pending
  fixture adapters. iOS was not compiled on Windows.
- At that point this was persistence infrastructure, not a working
  `/api/v1/sync` endpoint. The separate low-level batch storage method still
  rejects mixed retries; the new REST sync processor handles them. Event
  notifications and scenario-fixture adapters remain. The old history tests
  alone must not be treated as end-to-end sync tests.
- 2026-09-23: test-first server REST slice added `POST /api/v1/sync` and
  `GET /api/v1/sync/bootstrap` with shared serializable contracts. A request
  compares its base revision to accepted history, applies independent deck,
  card, move, delete, and review operations in one Room transaction, and
  returns structured conflicts for overlapping edits, review/membership
  races, and additions/moves into reviewed decks. Deck deletion cascades its
  cards; different cards added to one unreviewed deck merge and the count is
  recalculated. Empty-operation POST performs a manual incremental pull:
  changed deck/card rows, deleted IDs, and history since the device's base
  revision (including newly accepted changes), not a complete snapshot.
  `/bootstrap` is reserved for initial
  account hydration. Schema v4 adds payload fingerprints and primary entity
  IDs to history so an exact retry is accepted but a reused operation ID with
  changed content is rejected, and deletions can be described in the delta.
  The server updates the deck's last-changed revision when its derived card
  count changes, while independently added cards still merge. Live Ktor/Room
  tests cover these paths, rollback, account registration,
  and v3→v4 migration. Review completion now requires and stores the complete
  existing interval/duration/outcome fields; a focused test confirms they
  survive a REST round trip. This endpoint is not wired into the app's local
  outbox, conflict UI, or WebSocket status channel yet.
- Verification after this slice: `:domain:desktopTest :data:desktopTest
  :klaf-server:test --offline --no-daemon --continue` ran 30/53/90 tests.
  Respectively 30/19/39 passed; the remaining 34 client and 51 server tests
  still fail immediately at their deliberately unimplemented fixture adapters
  (85 total), not at behavioral assertions. The subsequent targeted rerun of
  all 15 live REST checks and seven revision-storage checks passed after adding
  the mixed-retry, derived-revision, and detailed-review assertions. The
  complete suite needs a rerun after these final assertions; its earlier 85
  fixture failures are still expected. `:Android:compileDebugKotlin` also
  passed. No iOS build was run on Windows.
- 2026-09-23: added `KlafServerSyncRestClient` in common client code. It sends
  typed sync operations and bootstrap requests over REST, decodes incremental
  deltas and structured conflicts, preserves server error codes, and bounds
  requests to 15 seconds. Two live Ktor tests use this real Ktor client against
  the production server, including a stale-edit conflict and missing-account
  error. Final combined verification ran `:Android:compileDebugKotlin
  :domain:desktopTest :data:desktopTest :klaf-server:test --offline --no-daemon
  --continue`: Android compiled; domain 30/30, data 19/53, server 43/94 passed.
  All 85 failures are the same deliberately unimplemented sync/import fixture
  adapters, not behavioral assertions. iOS was not compiled.
- 2026-09-23: pre-outbox code review found a silent data-loss path: the
  legacy-tolerant Room insights converter maps malformed server payloads to
  empty insights. Incoming REST card writes now use a strict decoder; a live
  batch test proves malformed insights return 400 and roll back all writes.
  Bootstrap now reads revision, decks, and cards in one Room read transaction.
  Sync history queries read only revisions newer than the client's base, and
  per-operation deck/card lookups use their indexed sync IDs rather than
  repeatedly scanning whole tables. A no-change pull skips deck/card scans.
  Read-only sync now works even at the largest stored revision; revision
  overflow is checked only before a new accepted write. The review/removal
  test now covers both arrival orders, and replay logic was simplified without
  changing its fingerprint check. Final combined run
  `:Android:compileDebugKotlin :domain:desktopTest :data:desktopTest
  :klaf-server:test --offline --no-daemon --continue`: Android compiled,
  domain 30/30, data 20/54, server 46/97 passed. The same 85 tests still stop
  at deliberately unimplemented sync/import fixture adapters. iOS was not
  compiled on Windows.
- Review follow-up before wiring the outbox: the REST request currently has one
  `baseRevision` for its entire batch, while the requirements assign a base
  revision to each pending operation. The client must either send separate
  groups by base revision or the protocol must carry per-operation bases.
  Conflict resolution must create a new operation ID when rebasing a change:
  an exact retry deliberately requires the original base and payload. The
  advertised previous protocol version also lacks a real compatibility suite;
  do not claim backward compatibility until a v1 payload is defined and tested.
- 2026-09-23: client Room schema v11 adds `pending_sync_operations` with a
  unique operation ID, account key, per-operation base revision, and serialized
  immutable payload. `RoomSyncOutbox.recordChange` commits a local change and
  its pending operation together; duplicate IDs or failed local writes roll
  both back. `applyAccepted` applies a server delta and removes only that
  account's accepted operation IDs in one Room transaction, retaining them if
  delta application fails. The outbox survives database reopen and preserves
  insertion order. Test-first red compile was followed by 4/4 outbox, 4/4
  legacy migration, and 6/6 sync-identity desktop tests passing. The final
  API was rerun with the 14/14 desktop tests green, and Android debug Kotlin
  compiled. The generated v11 schema is saved.
  iOS and account-specific database switching were not verified here.
- 2026-09-23: added Android and Desktop factories that open caller-owned Room
  instances in a dedicated guest file or a deterministic SHA-256-named file
  per normalized account email. They leave the existing `klaf_kt.db` file and
  its DI singleton untouched, so opening a scoped file does not silently
  migrate or hide existing app content. Test-first red compile was followed by
  2/2 Desktop factory tests green: guest/two-account deck and outbox isolation,
  persistence across reopen, email-case normalization, and preservation of the
  legacy file. Android debug Kotlin compiled. No Android device test or iOS
  scoped factory was run. These factories alone do not make sign-in/sign-out
  switch visible app data; selection persistence, DI flow switching, reminders,
  and safe guest transfer remain.
- 2026-09-23: added a selected-account store (atomic file replacement on
  Desktop; committed Android preferences) and `ActiveLocalRoomDatabase`.
  It restores the selected email after restart, keeps guest/account Room files
  separate, serializes account switches with local transactions, and rejects a
  corrupt target database before persisting the switch. The deck, card,
  vocabulary, local-version, and transaction repositories now accept a
  database source; their observable Room queries follow source changes.
  Existing direct-database constructors and installed DI still use a static
  source to preserve current app behavior and legacy data. The active source
  is available as a separate lazy Android/Desktop DI binding for the upcoming
  server-account flow. Test-first Desktop suite: 6/6 active-selection cases,
  6/6 existing sync-identity cases, and 2/2 scoped-factory cases pass; the
  corrupt-database test failed before the fix and passed after it. Desktop DI
  and Android debug Kotlin compiled after the final bindings.
  No device test, active UI sign-in/out, reminder isolation, or guest transfer
  is claimed here.
- Account creation writes two Room files. Initialization failures before the
  registry write clean up the new account file, but a registry-write failure
  or process crash can leave an unreferenced file. Define crash recovery before
  considering registration fully atomic; never discard a file whose registry
  write may already have committed.
- Local ignored placeholder `SecretConstants.kt` files were created solely to
  allow compilation; they contain no credentials and are not product changes.
- Test-code review/refactoring on 2026-09-23 replaced map-key counts that
  could not reveal duplicate rows with fixture contracts for direct SQLite row
  counts (client and server). The real Room schema check now verifies the
  foreign-key `ON DELETE CASCADE` action and indexed first column, not an index
  name. Server history checks now assert order and preserve duplicate detection.
  Bootstrap and Room-migration contracts now compare complete legacy review
  summaries; bootstrap also checks that the old save version did not become
  the server revision. WebSocket test waits now have a two-second timeout so a
  missing event cannot hang the suite indefinitely. `:data:compileTestKotlinDesktop`
  and `:klaf-server:compileTestKotlin --offline --no-daemon` succeeded. The
  targeted nine-test Room run still has five passes and four expected schema
  failures. The full three-module rerun preserved the 30/43/51 test totals and
  the same 9/38/51 failure split. No server/client fixture was connected and no
  production sync code was changed.
- Code re-audit found review information in a device-global DataStore and
  experimental `VocabularySource` tables with future deck/card ID links.
  Review data moves into the synced deck row. The Vocabulary Source screen is
  already accessible in this branch; it remains enabled but its tables and
  scenarios stay outside Phase 1 import, sync, and test coverage. The screen
  can create ordinary Card rows, but that source-to-card path is not verified
  for MVP compatibility. The user will separately back up any source data they
  want to keep and clean up existing data before a later phase; a new baseline
  schema is only an option.
- Detailed deck review information is now in Phase 1 sync scope and must move
  from DataStore into the account's Room deck row, reusing existing review
  fields and adding only missing ones. The bootstrap must preserve existing
  DataStore records; a separate history table is out of scope.
- Existing Android synchronization runs through WorkManager with the legacy
  whole-database/version flow; it must not continue running after the new
  account-scoped manual sync replaces it.
- Current card transfer deletes and recreates moved cards with new IDs and
  drops mnemonic fields. Agreed behavior preserves the same card identity and
  all card fields while changing its deck; the old test expectation must be
  replaced during the test-first phase.
- Current transfer UI offers reviewed decks as destinations, and the transfer
  use case does not check `reviewCount`. During implementation, show only
  unreviewed destinations and recheck the stored destination inside the write
  transaction; the server applies the same rule. The existing transfer tests
  have been updated to require this behavior.
- The automatically created interim deck uses local ID `-1` and can hold real
  cards. Each account has one shared synced interim deck; guest mode has a
  separate one. Clients must avoid duplicate server copies on first sync.
- A concurrent card move out of deck A versus deletion of A on another device
  is an explicit conflict. The UI explains both concrete actions and lets the
  user keep the moved card in B while A stays deleted if B is unreviewed,
  choose another unreviewed deck if not, or accept deletion of the card;
  no silent deletion or resurrection.
- Current MVP rule: a deck accepts new cards or moved-in cards only before its
  first review (`reviewCount == 0`); later incoming cards are prohibited.
  Existing cards remain editable and deletable, and moving cards out remains
  allowed. Prior decisions about adding cards to reviewed decks and choosing
  a new schedule were removed from active requirements and retained in
  `server-database-deferred-card-addition.md` only as historical context.
- If another device completes the destination deck's first review before a
  pending card addition or move syncs, show a conflict. Keep the card pending
  and the completed review intact; the user places the card in a new or
  existing unreviewed deck. Preserve card identity/data and do not insert it
  into the reviewed deck.
- Review completion versus card removal/move remains an explicit conflict.
  The UI explains the changed deck composition. For a removed or moved-out
  card, the source deck's completed review record is preserved while the user
  chooses whether its schedule stays or it becomes due again.
- The contract scenarios prioritize cross-device and interruption corner cases,
  including both event orders and invariants against lost or duplicated cards,
  orphan records, cross-account leakage, and silent review schedule changes.
  Those integration assertions remain unverified until their fixtures exist.
- The Android Room schema does not enforce a card-to-deck foreign key; the
  one-time import must abort with a record-level report if orphan cards are
  found, without modifying the backup or partially populating the server.
- A card belongs to only one deck; no many-to-many junction table is needed.
  The new schema adds a foreign key and non-unique index on the existing
  `RoomCard.deckId`; migration must validate pre-existing rows first.
- Android review alarms and periodic reminder work currently identify decks
  only by local integer ID. Agreed behavior cancels the previous database's
  alarms on account switch/logout and schedules only for the newly open
  account or guest database, without cross-account ID collisions.
- The first local-development MVP is intentionally unauthenticated and limited
  to a trusted local environment.
- The MVP supports multiple accounts, including multiple accounts on one
  client device. Each account has separate local content and sync state; the
  server opens a separate SQLite database file for each account.
- Accounts are created through client UI requests to the server.
  Sign-up also registers the first device. Sign-in from another new device
  returns `DEVICE_NOT_REGISTERED`; a separate UI confirmation precedes its
  registration.
- The MVP hides the existing Delete account action and has no server account-
  deletion endpoint; Sign out remains available.
- Sign out retains the previous account's local database but switches the UI
  to the guest database; it does not show the signed-out account's content.
  This happens immediately offline, closes account synchronization, and has
  no pending server logout operation. Guest mode does not connect to the
  account synchronization channel.
  Signing in to another account switches to that account's local database and
  displays only its data. The first account's database remains inactive.
- Restarting without Sign out restores the last selected account and its local
  database offline; the Desktop authentication stub currently keeps its state
  only in memory and must be replaced.
- The client has a separate offline guest database. Sign-up for a new account
  transfers its decks and cards into that account's local database. After a
  verified transfer, remove the transferred guest data; upload remains manual.
  Sign-in to an existing account never transfers or uploads guest data and
  leaves the guest database unchanged.
- Sync requirements now specify an operation outbox, automatic merge of
  independent changes, structured client-side conflict resolution, and atomic
  deck-plus-cards deletion.
- Sync sends incremental operations. Server revisions order changes; same-entity
  concurrent changes conflict even when they touch different fields.
- Accepted sync operations have durable IDs and server-side idempotent results:
  a lost HTTP response must not apply a committed operation twice on retry.
  SQLite transactions protect accepted batches, but disconnects do not roll
  back batches already committed. Sign out is disabled during active sync and
  re-enabled on completion, failure, or bounded timeout.
- The MVP has no Cancel synchronization action; a failed or timed-out attempt
  ends, and the user starts any retry manually.
- Adding, editing, and deleting decks or cards is disabled during active
  synchronization of that account; reading remains available. Editing resumes
  after success, failure, or timeout.
- Synchronized entities require globally unique client-generated identifiers;
  existing local integer IDs are not valid cross-device identifiers.
- Device records, revisions, change history, and pending local operations are
  separate technical data, not fields mixed into user decks or cards.
- The server retains full change history in the MVP; automatic history cleanup
  is deferred.
- Mnemonic image synchronization is explicitly deferred. Existing Firebase
  Storage image-sync code has not been enabled or verified and is not part of
  the Phase 1 migration.
- Phase 1 replaces Firebase-backed account entry and deck/card sync, but keeps
  Firebase Crashlytics. Firestore word autocomplete is disabled in the MVP:
  no UI invocation or Firestore request for that feature. Its replacement is
  deferred until after core persistence and sync are verified.
- Synchronization is manual in Phase 1. The UI requires a persistent status
  indicator and a details view; it must not auto-sync on app lifecycle or in
  the background. Green, yellow, red, and gray states are defined in the
  requirements; exact placement remains to be designed. Guest mode hides the
  indicator because it has no account synchronization.
- Phase 1 includes registered Android and Desktop devices with stable IDs and
  display names, so the UI can identify the origin of synchronized changes.
- Registered-device details show platform, `/sync-events` connection state,
  last successful synchronization time, and last confirmed server revision.
- Device registration, connection status, and last sync position are scoped to
  each account, even on the same physical Android or Desktop device.
- REST performs data synchronization; a WebSocket event channel keeps the
  synchronization indicator up to date without automatically applying data.
- `/sync-events` sends current synchronization state immediately on connection;
  clients update their own registered-device records when they connect.
- Android and Desktop use developer-local, Git-ignored endpoint configuration
  during the MVP instead of an in-app server-settings screen.
- The MVP starts with an empty server database populated through a manual
  Android Room export and migration script. The script creates the account
  specified by email; after reinstall, the user signs in to it. Direct
  Firestore-to-server migration is deferred.
- The exported Android database is kept as a recovery backup. The source app is
  reset or reinstalled, registers as a fresh device, and downloads the server
  data before normal synchronization starts.
- Desktop also starts from an empty local database and downloads the initialized
  server data; it does not provide a second initial data source.
- Delivery uses test-driven development: target-behavior tests are written and
  reviewed before the corresponding production implementation.
- UI tests are optional and limited to lightweight behavior checks; they do not
  block the core sync test-first suite.

## Error Log

*(Tracked during implementation.)*

## Test-First Checklist

`[x]` means scenario assertions are in test source and the test source has
compiled at least once. Some contract suites still fail at setup for missing
production adapters; they are not yet executable integration checks. `[ ]`
means a test is still to be written. Do not mark Phase 0.5 complete while any
MVP group remains open.

- [x] Local card addition before/after a deck's first review; stale destination
  snapshot; transfer out of a reviewed deck while preserving identity and data.
- [x] Existing transfer tests updated for unreviewed destinations and stable
  card identity.
- [x] Room orphan prevention, parent-child cascade, unaffected sibling deck,
  safe deck updates, stable card move, transaction commit/rollback, and direct
  foreign-key/index checks.
- [x] Migration from the current Room schema, preserving card ownership and
  review data while rejecting orphan rows and leaving the backup untouched.
- [x] Local add/edit/delete of decks and cards with an outbox entry committed
  in the same Room transaction; rejected/failed writes leave neither side.
- [x] Stable cross-device sync IDs, shared interim deck, derived card count,
  and complete card/mnemonic/image-reference preservation on move.
- [x] Server SQLite revision, change log, grouped operations, atomic deck
  deletion, idempotent operation IDs, and partial accepted/conflicting batch.
- [x] Two-client merge and conflict matrix in both arrival orders: independent
  changes; same deck/card; delete versus add/edit/move; review versus stale add,
  removal, or move; and reviewed destination after a pending move.
- [x] Accept-server, keep-local, and manual per-conflict resolution, including
  full deck restoration, card rescue, and review-schedule choice.
- [x] Lost HTTP response, duplicate retry, timeout, incompatible protocol,
  unavailable server, retained outbox, and manual-only retry.
- [x] Account creation/sign-in/device confirmation, guest sign-up transfer,
  guest sign-in retention, offline logout, account isolation and restart.
- [x] One-time import: invalid references abort without partial data, review
  DataStore preservation of the complete review summary, guest/account
  database separation, source-file backup left untouched, and legacy save
  version excluded from the server revision.
- [x] REST protocol, `/sync-events` initial state/reconnect/status-only events,
  device presence and account-scoped notifications; legacy AI `/ws` unchanged.
- [x] Lightweight sync indicator/edit lock/sign-out lock behavior via test-only
  client state contract; no pixel-level UI tests.
