# Server database test-coverage audit

## Pre-commit privacy verification (2026-09-28)

The prospective index's 840 paths / 780 text files, including all 17 Markdown
documents and README, were checked for personal emails, user-home paths/device
serials/account storage IDs, provider tokens/private keys, credential URLs,
quoted/property/XML credentials and accidental copies of three real local
credential values. No confirmed sensitive content was found after personal
metadata was replaced with generic placeholders in five documents. Private
original notes remain only under ignored `.local-notes/precommit-privacy/`.
Synthetic example/test emails, password UI labels and Kotlin label-qualified
expressions are not credentials; broad matches were manually triaged. Actual
local properties and Firebase config/service credentials are ignored and not
indexed, with no reachable-history entries for those checked paths. Required
public certificates and schema/test sources are retained. No generated-data
or credential-container files are indexed, and no binary additions/changes
are proposed. The 60 unchanged legacy binary assets and full historical blob
contents were not audited; no history rewrite was performed. Final staged
whitespace validation passes. These are publication/privacy checks, not new
functional test runs; the prior regression results below remain the baseline.

## Full uncommitted-worktree review (2026-09-28)

Scope: staged and unstaged changes relative to HEAD and new untracked files in
`Klaf_kt_remote-storage`, branch `remote-storage-server`. Persistence/schema
integrity, account/guest switching, sync retry/atomicity/conflicts, REST and
WebSocket behavior, mnemonic file lifecycle, client navigation/reminders,
DI/build configuration and the existing test inventory were reviewed. Existing
staged changes were preserved; retired setup-only umbrella tests were not
restored. This is a risk-focused review, not a guarantee of exhaustive coverage.

Five discovered defects were reproduced before their production fixes:

- `RoomSyncDeltaApplierTest`: moving a card out of a deleted source deck must
  preserve its local ID, rather than cascade-delete and reinsert it. Added two
  cases: move/delete ordering and transactional rejection of a card whose
  incoming parent is deleted. The original code failed the ID-preservation
  test. Apply surviving cards before deleting source decks and reject the
  malformed parent reference before applying rows. A supplemental real
  Room/live-REST case in `LiveManualRoomSyncCoordinatorTest` verifies the same
  move/delete batch across server revision 3 -> 4 and a manual client pull.
- `SyncEventFeedTest`: an initial connector's own timeout must reconnect;
  stopping the observer must cancel it without reconnecting. The original code
  treated the connector timeout as caller cancellation and stopped observing.
  Added one case distinguishing child timeout from cancellation of the worker.
- `CachedMnemonicImageAssetRepositoryTest`: an account change during a local
  or downloaded-cache disk read must discard the old account's bytes. Added
  one case covering both sources; the original local branch returned stale
  bytes. Check selection after the read as well as before it.
- `CachedMnemonicImageAssetRepositoryTest`: reject unsafe imported image IDs
  before any write. Added one case checking traversal, absolute/empty IDs and
  excessive length; the original Desktop implementation accepted them. Apply
  the shared safe-ID check to Android and Desktop imports.
- `DeckListSyncRouterTest`: manual Room/REST synchronization must try the
  configured server without an active Android network transport, because USB
  reverse/localhost can work. Added success/transport-error propagation and
  legacy-offline cases; the original router blocked the request. Preserve the
  legacy Firebase connectivity gate and let REST report actual availability.

Eight regression tests added in total. Existing tests were not weakened to
accommodate the fixes. Small refactors: move the DeckListViewModel companion
near the class top and annotate RoomSyncStatusObserver's experimental coroutine
API use. A shared outer `IsolatedStorageTestRule` now guards conflict-screen
and reminder instrumented tests before activity launch/test-body writes; the
ordinary application package is rejected rather than risking real account data.

Verification commands:

```powershell
.\gradlew.bat :data:desktopTest :presentation:desktopTest :klaf-server:test :di:desktopTest :domain:desktopTest :Android:testDebugUnitTest :Android:assembleDebug :Desktop:compileKotlin --console=plain
.\gradlew.bat :domain:desktopTest --rerun-tasks --console=plain
.\gradlew.bat :Android:connectedRemoteStorageTestAndroidTest -PklafAndroidTestBuildType=remoteStorageTest -Pandroid.testInstrumentationRunnerArguments.class=com.kuts.klaf.ConflictResolutionScreenInstrumentedTest --console=plain
```

Regression reports: 122 data, 64 presentation, 191 server, 18 DI, 30 domain
and 6 Android unit tests pass, zero failures/errors (431 executed; one existing
opt-in server smoke skipped). Domain was additionally forced because its task
was initially up-to-date. Android debug assembly and Desktop compilation pass.
Three conflict-screen tests pass on physical RMX2001 / `<test-device-serial>`,
isolated `com.kuts.klaf.remote.storage.test` package: 434 passing tests including
device checks. Reminder acceptance tests were compiled but not rerun.

Safety checks: all 35 files in the immutable `klaf_backup` still match the
saved SHA-256/length manifest. Real Android Google-services config and server
secrets are ignored; the isolated test JSON uses explicitly dummy credentials.
HEAD-wide whitespace validation flags only the pre-existing AGENTS.md final
blank line; review-edited source/test/docs pass the scoped check. No commit,
ordinary-app deployment, ordinary-server restart/account writes or primary-phone
cutover occurred. Production error handling/image import changes compile for
Android, but their full two-device UI acceptance was not rerun. Linux/iOS were
not run; image conflict previews remain explicitly deferred.

## Mnemonic images and actionable errors (2026-09-28 extension)

2026-10-08 update: manual sync now downloads every referenced saved image before
success, skips files already cached, and retries only missing files after a
failed attempt. Card opening no longer initiates an image request. New focused
tests cover bootstrap and delta ordering, missing-file retry, local-only
display, and account separation; the historical test totals below predate
this update. A live Android/Linux check downloaded 15 previously missing
images over two sync attempts; the first timed out without false success,
and the next ended with all 19 cached. Current Windows regression ran 357
Desktop tests with zero failures across data, DI, presentation, and domain;
Android debug installation and Desktop compilation passed.

Final regression: 423 executed tests passed, zero failures (117 data, 62
presentation, 190 server, 18 DI, 30 domain, 6 Android unit tests). One existing
opt-in server smoke was skipped. Android debug assembly and Desktop compilation
also passed. APK installation on the secondary physical phone preserves data;
this does not count as a rendered-image UI acceptance test.

- `CachedMnemonicImageAssetRepositoryTest`: download caching/restart/offline,
  concurrent lookups, account/guest isolation, cancellation, stale account
  results, invalid response retry and preservation of a previous image when
  replacement fails.
- `RoomMnemonicImageUploaderTest`: only referenced saved files in pending card
  changes are uploaded; duplicate IDs in a batch upload once, drafts are
  excluded, legacy missing files are preserved as references, upload failure
  and cancellation propagate, retry preserves the asset ID.
- `ManualRoomSyncAttemptStateTest`: image preparation precedes metadata and
  failure leaves the original outbox operation pending for retry.
- `LiveMnemonicImageDeliveryTest`: production REST and account SQLite stack;
  exact downloaded bytes, registration/reference/account checks, unsafe paths,
  invalid/oversized files, immutable uploads and equal-byte retries. A real
  Room coordinator exercises a lost successful upload reply, keeps its card
  change pending, retries, then publishes metadata and downloads through a
  second device's offline-persistent cache. Upload size enforcement is also
  exercised directly without relying on client-side validation.
- `AccountFailureTransportTest`: real HTTP connection refusal, own request
  timeout versus caller cancellation, invalid JSON and server failure.
- `AccountAuthenticationViewModelTest`: actionable messages for the failure
  kinds, sign-up/registration retries and cancellation without an error state.

Image rendering in ordinary Android/Desktop windows and a user-generated
image's two-device flow still require manual acceptance. Automated image tests
do not invoke paid image generation or modify the real account/backup.

## Earlier baseline regression

Final review/regression on 2026-09-28: forced server/shared-client suites
passed 392 tests (one optional manual imported-Desktop smoke skipped).
Android's six active unit tests and three new physical-device screen tests
also passed: 401 executed tests total, zero failures. Desktop compilation,
ordinary Room-mode Android debug assembly, and isolated Android/test APK
assembly passed. Review and transfer test fixtures were repaired to exercise
the agreed atomic review-save and ID-preserving/unreviewed-target contracts;
no historical commented-out tests were counted. Subsequent default-launch
rollout is recorded below; primary-phone cutover remains unperformed.

Ordinary-default rollout later on 2026-09-28: default/override/legacy-file guard
tests first failed under the old default, then passed after selecting Room
by default in shared runtime policy and Android build configuration. A new
Desktop test resolves the production DI graph with ordinary (not isolated-mode)
configuration in a temporary directory, performs guest/account Room writes,
and verifies account-only outbox recording and the atomic review-save binding.
Full no-mode-flag regression passed 401 executed tests: server 180 (one optional
manual smoke skipped), domain 30, data 108, presentation 59, DI 18, Android
unit 6. Ordinary Android assembly and Desktop compilation passed. On secondary
physical RMX2001, the no-flag APK retained account data/version 11 on offline
startup, explicitly synchronized to green 11/11 against the retained disposable
server, and retained decks/synchronized status after cold restart. Server
revision, card count, and history count stayed 11/1/14; SQLite `quick_check`
passed. This is not a new Desktop GUI test or a legacy-data migration.

The 31 cases in the retired `ClientSyncContractTest` shared a deliberately
unimplemented `ClientSyncFixture`; all failed during setup and provided no
executable evidence. They were removed after the individual audit below.
The test harness does not replace Room or Ktor with a test-only synchronization
algorithm. `data` cannot depend on `klaf-server` without a dependency cycle;
live cross-layer checks therefore belong in `klaf-server/src/test`.

| Client contract cases | Executable coverage already present | Remaining check |
|---|---|---|
| 1 guest offline; 3–8 sign-up, sign-in, account switch/restart | `ActiveLocalRoomDatabaseTest`, `LiveServerAccountSessionTest`, `AccountAuthenticationViewModelTest`, `RoomSyncStatusObserverTest` | A live Room+REST test interrupts guest transfer after the account copy, verifies that guest data and the persisted sign-up attempt survive, and retries without duplicate rows before manual upload. Other live tests cover sign-in without guest transfer and Alice → guest → Bob → guest → Alice isolation. The ordinary Room-mode Android app was also checked on physical RMX2001: sign-out showed guest data, sign-up transferred a guest deck/card only to a second account, manual sync cleared three pending operations, switching back to the imported account restored its 57 decks/1093 cards without the second account's deck, and an offline restart retained the selected second account. Read-only copies of guest and both account Room files passed SQLite `quick_check`. An isolated Desktop rendered check verified offline sign-out to guest, distinct guest/B test decks, B persistence across restart, and imported A restoration (57 decks/1093 cards); all three Desktop Room files passed `quick_check`. Desktop B selection used a disposable fixture while the app was closed, not its sign-in UI. A separate fresh Desktop client then passed user-assisted passwordless sign-in/new-device registration, first manual download of the imported 57 decks/1093 cards, synchronized 1/1 status with no pending operations, and offline restoration after restart. The registration prompt was completed by the user, not independently captured. |
| 2 passwordless forms | `AccountAuthenticationViewModelTest`, `LiveKlafServerProtocolTest`; `AuthenticationScreen` disables both password fields and `Drawer` hides account deletion when `canDeleteAccount` is false | Passwordless submission is automated; visual field state and menu visibility are code-reviewed, not UI-test automated. |
| 9–13 local atomic outbox, review rollback, grouped deck delete | `AccountScopedTransactionOutboxTest`, `RoomSyncOutboxTest`, `RoomDeckReviewInfoTest`, `RoomCardDeckIntegrityContractTest` | Covered by executable account-scoped Room tests. |
| 14–16 card move, image reference, reviewed-deck restriction | `RoomSyncIdentityTest`, `RoomCardDeckIntegrityContractTest`, `ServerSyncContractTest`, `BootstrapImportContractTest`, `LiveManualRoomSyncCoordinatorTest`, `LiveServerAccountSessionTest` | Live Room-to-REST image-reference round trip passed; file bytes remain outside the data-sync protocol. A separate live first-upload test now proves that a reviewed guest deck and its existing card upload together without triggering the later-addition restriction. |
| 17–22 manual sync, events, network/lost response, busy state | `LiveManualRoomSyncCoordinatorTest`, `RoomSyncStatusObserverTest`, `ManualRoomSyncAttemptStateTest`, `ServerSyncContractTest`, `DeckListSyncRouterTest`, `AccountSignOutAvailabilityTest` | A real Ktor event client reconnects after two server restarts: it refreshes Room status but neither downloads a remote deck nor uploads a pending local deck until each explicit REST sync. A failed upload stays pending through status reconnection, with no implicit retry; timeout releases the edit pause. Rendered Desktop offline creation/reconnect/manual upload passed on disposable copies: one pending operation survived reconnection, uploaded once on click, and a second sync was idempotent. Rendered reconnect/manual pull also passed. A 2026-09-28 follow-up on a 60-deck Desktop list showed the newly pulled row at the end of the already open list after scrolling, with client/server versions 5/5 and no restart. This explains the earlier apparent absence as likely off-screen position; the real Room/REST observed-deck test also passes. |
| 23–26 edit/delete conflicts, server/local/manual choices | `LiveManualRoomSyncCoordinatorTest`, `RoomSyncConflictResolverTest`, `RoomSyncConflictSnapshotTest`, `ConflictResolutionUiModelTest` | Local Room names replace technical IDs in conflict descriptions; saved server data supplies names if local rows were deleted. A real Room test verifies account isolation across a switch. On 2026-09-28 three production-screen Compose instrumentation tests passed on physical RMX2001 in the isolated package: all eight exact action labels, bulk callback dispatch, existing/new move destinations, blank-name prevention, busy-state disabling, and complete per-item choices with their operation IDs. These use synthetic models; earlier Room/REST and real structural-conflict runs provide separate integration evidence. Five additional model regressions enforce consistent bulk/per-item keep-local eligibility for moved, deleted, unrelated, or mismatched entities. |
| 27 stale move to reviewed destination | `ServerSyncContractTest`, `RoomSyncConflictResolverTest`, `LiveManualRoomSyncCoordinatorTest`, `ConflictResolutionUiModelTest`, `AccountScopedTransactionOutboxTest` | Real Room+REST checks cover an existing or new destination, stable card identity/data, review duration, a later remote review, local rejection without mutating the conflict, and lost-response idempotent retry. Both destination-picker choices also passed on a physical Android device against a disposable server. |
| 28–29 card removal versus remote review, retain schedule or due now | `ServerSyncContractTest`, `LiveManualRoomSyncCoordinatorTest`, `ConflictResolutionUiModelTest` | Both choices pass real Room+REST and UI-model tests; due-now retains the completed review summary and groups multiple removals into one schedule adjustment. A lost-response retry is idempotent. Two additional live tests cover pulling a later remote deck/card addition or its addition-then-deletion after resolution. Both choices also passed on physical RMX2001 against a disposable server on 2026-09-27: conflict text identified the card and remote review, the card stayed deleted, the completed review remained, and each schedule choice produced the expected server state and synchronized UI. |
| 30–31 interim deck and reminders across accounts | `ActiveLocalRoomDatabaseTest`, `RoomReminderSelectionTest`, `ServerSyncContractTest`, `ScopedDeckReminderInstrumentedTest` | Five isolated-package instrumentation tests passed on physical RMX2001 (5/5). They cover guest → Alice → Bob → Alice alarm switching with identical local deck IDs, separate alarm PendingIntents and legacy-alarm cleanup, actual notification posting/dismissal with distinct account-scoped navigation PendingIntents, tapping the notification in Android's shade to open the rendered review screen, and OS-scheduled alarm delivery after five seconds to the selected account/deck. Test alarms, notifications, PendingIntents, and the temporary deck/card are cleaned up. Ordinary-app cutover remains unverified. |

Additional ordinary-app Android acceptance on secondary RMX2001 (2026-09-27):
one locally created deck and its single pending operation survived process
restart, uploaded on explicit synchronization, and did not duplicate or
advance the revision on a repeat sync. A remote device then added a different
deck; WebSocket status showed client/server revisions 2/3 while the open list
remained unchanged. Manual synchronization reached 3/3 and immediately
rendered that deck in the already open list. Server SQLite `quick_check`
passed and each test deck appeared exactly once. This is rendered evidence
for cases 8 and 17–19. A subsequent ordinary-app run added exactly one card
through the rendered UI, manually uploaded it, completed a one-card review,
and manually synchronized the review on physical RMX2001. A focused position
test caught the one-card crash, and a JSON round-trip test caught the initial
review upload's omitted-null HTTP 400. After those fixes, the phone showed
versions 5/5 and no pending operations; the server held one card and one
review. A later Desktop follow-up found the appended deck at the bottom of the
already open long list; no refresh failure was reproduced.
On 2026-09-28 the same ordinary Android installation edited that reviewed
deck's existing card through the rendered editor. Two pending edits (automatic
valid Word Insights, then explicit text change) uploaded together as server
revision 6. The server retained exactly one edited card, and repeat sync did
not advance the revision. The rendered card-management screen then deleted
the card while retaining the deck's one review; local status showed one
pending change. Manual sync reached 7/7 with zero pending, server card count
zero and exactly one accepted deletion. Repeat sync remained at revision 7.
The server SQLite file passed `quick_check`. This adds physical-device
evidence for cases 10 and 16; it does not claim automated UI coverage.
The same ordinary Android app also passed a rendered offline-retry check on
2026-09-28: an account deck created with the test server stopped survived an
app restart, an explicit failed offline sync retained its one pending change,
and reconnect alone did not upload it. A second explicit attempt synchronized
at revision 8/8 with zero pending changes. The server had exactly one deck and
one accepted `AddDeck`; repeat sync left revision 8 unchanged and SQLite
`quick_check` passed. This adds ordinary-app evidence for cases 8, 17, and 19.
The same disposable empty deck was then renamed and deleted through the
ordinary Android UI, each time with one pending local change and an unchanged
server until manual sync. The rename reused its sync ID at revision 9; the
delete removed the server row at revision 10 and left exactly one accepted
`DeleteDeck`. Repeat sync did not duplicate it or advance the revision.
Rendered status was synchronized 10/10 with zero pending changes and server
SQLite `quick_check` passed. This adds ordinary-app evidence for cases 10 and
17, without claiming automated UI coverage.
An ordinary Android rendered card-transfer check on 2026-09-28 moved one
existing card between two unreviewed account decks. Local source/target
counts changed to 0/1 with one pending operation; the server remained at
revision 10 until manual sync. Sync reached 11/11 with zero pending and one
accepted `MoveCard`; server card identity and word fields were unchanged,
and the target held exactly one row. Repeat sync stayed at revision 11 with
no duplicate card or move. SQLite `quick_check` passed. This adds physical
ordinary-app evidence for cases 10, 14, and 17.
The next ordinary Android check exposed a picker mismatch for case 16: a
once-reviewed deck was listed as a move destination, although the domain
transaction rejected the attempted move without changing the card or outbox.
Two new `CardMoveTargetSelectionTest` cases cover the allowed-target filter
and an empty eligible set. After the filter fix, the ordinary Room-mode APK's
rendered picker excluded the reviewed deck; the card remained in its source,
local revision was 11, and pending uploads were zero. Full presentation
Desktop tests and the Android debug build passed.
Two real `AccountScopedTransactionOutboxTest` cases now additionally invoke
the production creation/transfer domain use cases against account-scoped
Room. They reject card creation after one review and a move using a stale
unreviewed destination snapshot, proving unchanged decks/cards, local save
version, and empty outbox. Focused and full data Desktop tests passed.
An ordinary Android addition-form check retained zero cards, local revision
11, and zero pending uploads after attempted saves into the reviewed deck.
The form is still accessible; its generic error mapping was code-inspected,
but the transient rendered error was not captured.
The follow-up introduced a typed transaction rejection and explicit addition
feedback. `CardAdditionFailureMessageTest` covers the specific reviewed-deck
message and the unrelated-error fallback; the domain mutation test asserts
the rejection type. Full domain/data/presentation Desktop suites and the
ordinary Room-mode Android build passed. On physical RMX2001 the new message
was captured after a rejected save; both entered fields stayed in the form,
card count remained zero, local revision 11 and pending uploads zero.

## Individual disposition of the 31 retired scaffold cases

Names below identify executable test classes and distinctive test methods;
the grouped table above records the additional device/manual evidence. Case 2
has automated behavior tests plus inspected Compose conditions, but no UI
automation; that distinction is intentional, not a claim of pixel-level proof.

| # | Scenario | Replacement evidence |
|---|---|---|
| 1 | Offline guest, no sync status/event | `AccountScopedTransactionOutboxTest`: guest transaction; `RoomSyncStatusObserverTest`: guest/account status isolation. |
| 2 | Passwordless form, no account deletion | `AccountAuthenticationViewModelTest`: blank-password sign-up/sign-in; `AuthenticationScreen` `isPasswordless` field conditions and `Drawer` `canDeleteAccount` condition inspected. |
| 3 | Sign-up adopts guest data, upload stays manual | `LiveServerAccountSessionTest`: signup moves guest data; `ActiveLocalRoomDatabaseTest`: guest transfer queues initial upload. |
| 4 | Interrupted sign-up retries without duplicate | `LiveServerAccountSessionTest`: interrupted guest transfer and lost signup response; `ActiveLocalRoomDatabaseTest`: exact account-copy retry. |
| 5 | Sign-in leaves guest data alone | `LiveServerAccountSessionTest`: signing in to another account keeps guest separate. |
| 6 | Offline sign-out selects guest | `LiveServerAccountSessionTest`: signup and offline signout; `ActiveLocalRoomDatabaseTest`: repository flow switches selection. |
| 7 | Account databases isolate colliding integer IDs | `ActiveLocalRoomDatabaseTest`: cards/vocabulary/metadata follow selected database; `LiveServerAccountSessionTest`: other account remains separate. |
| 8 | Offline restart, no automatic sync | `ActiveLocalRoomDatabaseTest`: selection survives restart; `ManualRoomSyncAttemptStateTest`: device identity resolved only at manual start. Rendered Desktop/Android restart checks above. |
| 9 | Local data and outbox commit atomically | `AccountScopedTransactionOutboxTest`: account transaction queues creation; failed account transaction rolls both back. |
| 10 | Deck/card edit/delete each queue change | `AccountScopedTransactionOutboxTest`: rename, card edit, move, deletion. |
| 11 | Failed review rolls back schedule and outbox | `RoomDeckReviewInfoTest`: account review and outbox commit or roll back together. |
| 12 | Failed local write leaves no partial data/change | `AccountScopedTransactionOutboxTest`: failed account transaction rolls back data and outbox. |
| 13 | Deck deletion groups its cards | `AccountScopedTransactionOutboxTest`: deck deletion with child cards; `RoomCardDeckIntegrityContractTest`: cascading integrity. |
| 14 | Card move preserves identity and mnemonic/image reference | `RoomSyncIdentityTest`: ordinary edits and moves preserve IDs; `LiveManualRoomSyncCoordinatorTest`: manual rescue preserves moved card. Image reference is in `mnemonicJson`, not a separate file upload. |
| 15 | Image reference, not bytes, in data sync | `LiveManualRoomSyncCoordinatorTest`: manual Room upload preserves card image reference through REST. File transfer remains outside MVP. |
| 16 | Reviewed deck blocks new cards, allows existing edit/delete | `ReviewedDeckCardMutationContractTest`: first-review restriction and existing-card edit/delete. |
| 17 | Only manual sync uploads pending operations | `LiveManualRoomSyncCoordinatorTest`: manual pull/upload; `ManualRoomSyncAttemptStateTest`: device resolved on manual start; `RoomSyncStatusObserverTest`: pending status. |
| 18 | Remote event marks pending without download | `LiveManualRoomSyncCoordinatorTest`: remote revision event changes status without downloading until manual sync; real event channel reconnects after server restart without automatic Room download. |
| 19 | Failure retains IDs; reconnect does not retry | `LiveManualRoomSyncCoordinatorTest`: failed upload remains pending after status reconnect until explicit retry; real event channel reconnects without automatically uploading a queued deck. |
| 20 | Lost response reuses operation ID | `LiveManualRoomSyncCoordinatorTest`: lost response leaves outbox pending and retry reuses accepted operation. |
| 21 | Sync blocks edits/sign-out; failure/timeout unlock | `ManualRoomSyncAttemptStateTest`: edit pause, cancellation, timeout and failed state; `AccountSignOutAvailabilityTest`: sign-out availability. |
| 22 | Event outage gray, REST still works | `LiveManualRoomSyncCoordinatorTest`: disconnected event channel stays gray while explicit REST sync downloads changes. |
| 23 | Server conflict preserves pending local action | `LiveManualRoomSyncCoordinatorTest`: server conflict leaves local edit/checkpoint untouched; `ConflictResolutionUiModelTest`: concurrent card edit offers choices. |
| 24 | Accept server, then sync remaining changes | `LiveManualRoomSyncCoordinatorTest`: accept server discards only conflicting card edit; partial acceptance remains retryable. |
| 25 | Keep local rebases and uploads | `LiveManualRoomSyncCoordinatorTest`: keep local deck/card edit rebases and uploads; mixed manual choices. |
| 26 | Rescue moved card after source deck deletion | `LiveManualRoomSyncCoordinatorTest`: manual rescue preserves moved card; `ConflictResolutionUiModelTest`: deleted source offers rescue. |
| 27 | Reviewed destination requires another deck | `LiveManualRoomSyncCoordinatorTest`: reviewed destination retarget, later review rejection, lost-response retry; `ConflictResolutionUiModelTest`: appropriate choice. |
| 28 | Card removal versus review retains schedule | `LiveManualRoomSyncCoordinatorTest`: keeping card removal retains completed review schedule and summary. |
| 29 | Card removal versus review makes deck due now | `LiveManualRoomSyncCoordinatorTest`: due-now choice keeps completed review; lost-response retry. |
| 30 | Separate interim deck per account | `ActiveLocalRoomDatabaseTest`: interim check uses selected account; account switch/restart selection. |
| 31 | Reminder isolation across accounts | `RoomReminderSelectionTest`: old reminders canceled before colliding IDs; `ScopedDeckReminderInstrumentedTest`: five physical-device checks. |

The three `RoomMigrationContractTest` placeholders model an in-place
device-global DataStore-to-account migration. That is not the agreed cutover:
the exported Android source is imported into a revision-0 server account, then
clean installations download it. Their valid invariants are already checked
by executable `CardDeckForeignKeyMigrationTest` (v8 rows, foreign key, orphan
abort), `RoomSyncIdentityMigrationTest` (old local save version stays distinct
from server revision), and `BootstrapImportContractTest` (recovery backup
unchanged, review summary when available, orphan abort, revision 0). The
obsolete scaffold may be removed; those real tests must remain.
