# Klaf Server Status

## Current SDK dependency (2026-10-06)

AgentDriver account-scoped SDK changes are assigned `0.10.13-SNAPSHOT`, and
Klaf's version catalog targets that distinct coordinate. The earlier
`0.10.12-SNAPSHOT` references below describe historical checkpoints, not the
current dependency. Both server and client SDK sets, including the speech
runtime Gradle plugin, were published to Maven Local; the offline
`:klaf-server:compileKotlin` consuming build passed. The initial compile
attempt failed because only the server SDK set had been published; publishing
the client SDK set resolved it. This version-only check did not rerun tests or
restart the live server. A documentation audit found no personal email,
credential value, private key, or personal path in AgentDriver/Klaf README,
changelog and docs; remaining email/path matches are synthetic examples and
loopback/security-test addresses. No commit was made.

## Current platform scope (2026-10-06): Windows; Ubuntu validation deferred

The operator will use the current Windows setup and explicitly deferred
Ubuntu-native Klaf Server/runtime/isolation validation until requesting it.
WSL Ubuntu SDK tests are not a substitute for a run on the intended Ubuntu
server. Do not claim Ubuntu support or cross-platform production readiness.
Raise this unverified platform gate before deployment/migration to Ubuntu,
publishing Linux support, or another decision that depends on Linux security
isolation or token/storage behavior. No Ubuntu setup or test action is needed
from the operator now.

## Completed implementation (2026-10-06): reversible account Block/Restore AI access

The operator clarified that account Block is temporary: it must cancel
in-flight work and deny all account access, while Restore plus a fresh
password sign-in must recover AI functionality without an offline re-grant or
server restart. The separate AI grant is still needed to authorize only
selected operator-owned Klaf accounts, but Block must not delete it. Explicit
AI-grant revocation remains separate. Existing requirements and tests had
encoded grant deletion on Block; the requirement and implementation plan are
now corrected. Three regression tests cover preserved grant, direct
Block/Restore/sign-in/AI reconnect on the same server, and absent/revoked
grants remaining absent. Production Block now preserves a pre-existing grant
while still revoking every auth session; effective AI authorization also
checks approved/not-blocked account state. Restore does not create a grant,
and revoked old tokens remain invalid. The operator CLI now reports `AI grant
retained` for a blocked account with a persisted grant, rather than `AI
denied`. Storage, CLI and same-server Block/Restore/fresh sign-in/AI WebSocket
tests failed before their respective fixes and passed afterward. The final
full `:klaf-server:test` suite passed: 446 tests, zero failures/errors, two
conditional skips, 83 suites. `git diff --check` passed for the production
and documentation edits. The disposable live server was stopped before
building; Desktop client and test data were not reset. It was restarted at
13:24 on `127.0.0.1:8090` with the final binary, original disposable storage,
dedicated Codex owner settings and loopback public origin. The operator then
reported a live check: Block interrupted the in-flight job; Restore was
confirmed; fresh sign-in succeeded. No offline re-grant or server restart was
reported between Block and Restore. The operator subsequently confirmed that
post-Restore AI generation also worked correctly. This closes the manual
Block/Restore/sign-in/AI recovery gate for the tested client. A separate
Android UI recovery run was not reported; do not claim that platform-specific
check was performed.

## Live observation (2026-10-06): account Block during image generation

The operator blocked the disposable test account while Desktop image request
`mnemonic-image-5c8994ae2b0988fa-2` was in flight. Server log shows start at
12:37:38 and both account AI connections closed at 12:37:48. Desktop failed
the matching request after 10.4 seconds with invalid AI authorization.
Read-only registry checks show the account blocked, zero AI grants and no
active auth sessions; all three devices remain approved/not revoked. This
confirms live account-wide interruption at the client/server boundary. The
user pressed account Restore. Read-only checks confirmed it became active
while its AI grant and active sessions remained absent. The server was stopped,
AI eligibility was re-granted with the offline operator command against the
same disposable registry, and read-only checks confirmed active account + AI
grant + zero active sessions. The server restarted at 12:42 with the original
test storage, dedicated owner and loopback public origin. The user reports
successful Desktop sign-in and a green connection indicator. A post-recovery
AI request remains pending. Importantly, the direct `Block -> Restore ->
Sign In` path without an intervening offline AI re-grant/server restart was
not tested: the re-grant was performed before sign-in. Basic sign-in does not
require that grant, so the restart must not be described as a prerequisite
for sign-in. Current Block deletes the AI grant and Restore does not recreate
it; this is now a confirmed behavior defect against the clarified temporary
Block/Restore requirement. Do not count the full recovery flow as validated
until the implementation and unassisted live path are checked.

## Resolved live issue (2026-10-06): Restore after server JAR rebuild

The operator's Restore on the test Desktop device failed and left it Revoked;
the UI displayed `ServerAccountStorage$restoreApprovedDevice$1` as its raw
failure message. The running Gradle `:klaf-server:run` JVM used a classpath
JAR and had remained alive while later Gradle tests rebuilt the server JAR.
The named coroutine class exists in the current JAR, and earlier automated
restore tests passed. A stale running JVM/JAR mismatch was the leading cause.
The server was restarted at 12:25 with
the latest build, the same disposable storage, and the required owner/public
origin settings. No database state was changed by the assistant. The user
retried Restore and confirmed that it worked. This supports the stale-JAR
diagnosis, although no exception stack was captured. Avoid rebuilding server
code while the live Gradle-run server is active.

## Current focus (2026-10-06): restored Desktop sign-in returns HTTP 500

The new server and Desktop were restarted against the prior disposable test
storage. After operator Restore and Desktop logout, sign-in failed twice with
client classification `SERVER` (HTTP 500). Registry inspection confirms the
Desktop device is approved and not revoked, while previous sessions remain
revoked. The server did not report an exception. Redacted diagnostics now log
only exception class chain and first Klaf server stack location; password,
token, raw body, and exception message are excluded. The redaction test failed
at compile before implementation and now passes. An integration test injects
a SQLite insert failure and verifies HTTP 500 returns generic `SERVER_ERROR`
without its sensitive failure message. Both focused tests passed, and
`git diff --check` found no whitespace errors. The server restarted at 11:47
on the same disposable storage; Desktop remained open for a diagnostic retry.
The user retried at 11:48. Redacted server log located the exception at
`EnrollmentProofVerifier.verify:38`: the restart command omitted
`KLAF_PUBLIC_ORIGIN`, so DPoP proof verification rejected every sign-in.
This was a test-server launch error, not evidence of a wrong password.
A startup-config regression test failed first and now passes after requiring
an explicit HTTP(S) public origin when the server starts. Focused config,
diagnostic, and injected-storage-failure tests all pass. The server restarted
at 11:53 with `KLAF_PUBLIC_ORIGIN=http://127.0.0.1:8090` and the same test
storage; Desktop stayed open. The user then reported successful sign-in.
Desktop logged WebSocket `Ready` (protocol 7), and read-only registry inspection
showed one active session for this Desktop device. The next live gate was
same-device revoke during an in-flight Desktop image request.
That live gate now passed at the client/server boundary: Desktop image request
`mnemonic-image-cb589d25ffb22a65-1` started at 12:05:09. The operator revoked
its device at about 12:05:37; the server closed Desktop connection 4 with
`Device revoked`, and Desktop failed that exact request after 28.9 seconds
with invalid AI authorization. Read-only registry inspection confirmed the
Desktop device and its sessions revoked, while the Android device remained
active. More than two minutes after request start, no matching image-complete
log or Desktop delivery appeared. This does not independently prove the
external Codex worker's internal cancellation; the controlled automated
operation-registry tests cover that path. Android's unrelated background
WebSocket ping timeout was also observed and does not change this gate.
The user then generated a text mnemonic on Android while Desktop remained
revoked. Server log confirms Android request
`mnemonic-association-c09ecaf955ea11be-22` started at 12:08:59 and completed
at 12:09:08; the user confirmed the result appeared. This validates
cross-device isolation for the operator revoke in this live scenario.
Final review moved public-origin validation from configuration construction
to `KlafServer.start()`, preserving offline operator commands while still
rejecting a misconfigured running server before it opens storage/listens.
Focused tests passed after that change. The final full `:klaf-server:test`
run passed with 443 tests, zero failures/errors, two conditional skips across
83 suites; targeted `git diff --check` found no whitespace errors. The live
server is still the immediately preceding binary, differing only in where
startup origin validation occurs. It need not be restarted for this completed
revoke check, but should be restarted before any later claim that the exact
final binary was live-tested. The Desktop test device remains revoked.

## Current fix (2026-10-06): device revoke during active AI request

The live Desktop image-generation request completed after operator device
revoke and reached the revoked client. TDD reproduced the missing immediate
cancel: the new same-server operator revoke test failed before the fix, then
passed after direct cancellation and AI socket closure were added. A sibling
operator account-block test also failed first and now passes. The active
connection registry has a tested best-effort close path that continues after
one socket fails. After the final hardening, full `:klaf-server:test` passed:
440 tests, zero failures/errors, two conditional skips. Focused operator and
connection tests passed as well. `git diff --check` passed. The running
server process still uses the old binary, so a fresh live check after restart
is pending. The owner-home/executable/account settings exist only in that
running server process, not the current shell; preserve it until a safe
restart command with the same settings is prepared. The test Desktop device
remains revoked; no commit is requested.

## Current fix (2026-10-06): AI readiness after account sign-in

User approved a TDD fix for the live Android/Desktop Not ready regression.
Requirements and implementation plan now call for automatic AI reconnect
after successful account selection, including when the prior connection was
failed or disconnected, plus a clear disabled-mnemonic explanation. A review
found a second case:
re-authentication to the already-selected account does not emit a new account
selection; a separate sign-in epoch now triggers old AI session teardown and
reconnect. RED compilation was observed for both the initial manager/UI tests
and the same-account test. Focused tests are GREEN. Full Desktop JVM suites
passed: data 194 tests and presentation 82 tests, zero failures/errors.
`:Android:assembleDebug` and `:Desktop:compileKotlin` passed. `git diff --check`
passed. The updated APK was installed over the test phone without clearing
data, USB port forwarding remains active, and its restart log shows AI WebSocket
Ready with a successful push-token request. A second Android launch after the OS
stopped the app also reached Ready without Retry and remained connected during
the immediate log check. The user has now confirmed that Android and Desktop
sign-out/sign-in reconnect works without manual Retry. A visual check of the
disabled hint still remains. The updated Desktop
build has since been launched with disposable storage.
No commit is requested.

Live background check: connection 12 received an image request at 10:09:49
and completed it at 10:10:53. The server later closed that WebSocket at
10:11:30 because Ktor did not receive a pong (`Ping timeout`); Android
reconnected as connection 13 at 10:11:44. The user reported receiving the
completion notification and saving the image after returning to the app.
This sequence does not indicate an image-generation failure. Android
background network suspension is a plausible cause of the missed pong,
not proven from the server log alone. The updated Desktop app was launched again with the
same disposable client storage root; its window is responding and the
loopback server remains available.
The user reports that an Android-generated mnemonic image was saved, synced
to the server, and appeared correctly on Desktop. This is a positive manual
cross-device image check; no automated assertion or fresh server-log audit
was performed for that observation. Android sign-out/sign-in AI readiness
without manual Retry was subsequently confirmed by the user.
Concurrent two-device image live check passed: the server started Android
`mnemonic-image-c09ecaf955ea11be-10` at 10:38:19.284 and Desktop
`mnemonic-image-915df129129c642e-0` at 10:38:20.151. Both overlapped,
completed independently at 10:39:10.505 and 10:39:14.415, and the user
confirmed images on both devices. Desktop logged its matching request ID and
received image bytes. This proves image/image overlap, not yet text/image
overlap or cross-account isolation.
Two-device logout isolation live check passed: Android image request
`mnemonic-image-c09ecaf955ea11be-12` started at 10:43:24.357. The other
AI connection (17) closed at 10:43:27.883 while one connection remained,
and the Android operation completed at 10:44:41.813; the user confirmed the
image arrived. Desktop logs show a `ClientSessionEndRequest` and deliberate
disconnect on sign-out. This confirms Desktop logout did not cancel the
Android request; it does not test cancellation of a running request owned by
the signing-out device.
Attempted live same-device revoke check: Desktop image request
`mnemonic-image-f04f1e7bd04c2fe3-2` started at 10:48:07.575 and completed
at 10:48:56.102. No server-side revoke was performed by the assistant;
the operation completed before a controlled revoke could be confirmed.
Do not count this as cancellation verification. The Desktop device remains
unmodified by the assistant.
The operator UI lists three approved devices by UUID only. Read-only registry
and current disposable Desktop `account-device.json` inspection mapped
`2cd3a3b7-fbac-4335-9e2a-abd05bba8ad3` to the active test Desktop process,
`a16eb707-1453-4de2-aa87-e1ed9697d1cf` to Android RMX2001, and
`d8711c0b-fae6-4dc4-aff5-09774b7e2343` to another Desktop installation.
Current Desktop console logs contain the completed `f04f...-2` image request.
The operator UI should show device name/platform and a shortened ID to avoid
ambiguous destructive revoke actions; no UI change has been made yet.
The follow-up live revoke exposed a defect: Desktop image request
`mnemonic-image-f04f1e7bd04c2fe3-3` started at 10:52:36.337. The user
revoked the matching Desktop device; a screenshot saved at 10:53:22 showed
it as Revoked, and read-only registry inspection confirmed device and its
auth sessions revoked. Nonetheless the image request completed at 10:54:05.003
and the Desktop client received the image. No AI connection-close/cancel log
appeared immediately after revoke. This is a failed live cancellation gate,
not a successful revoke test. Current `revokeOperatorDevice` updates storage
and sync events but does not directly cancel AI operations/connections; it
relies on the WebSocket authorization monitor. Why that monitor did not stop
this request is not yet established. Keep the test Desktop device revoked
until the user directs restoration; do not claim live security readiness.

## Current live check (2026-10-06)

Preflight confirmed that the dedicated Codex owner login is available, while
the initial shell lacked the three `KLAF_CODEX_OWNER_*` environment variables.
The default server registry exists at schema version 2. To avoid migrating it
as a side effect of the first live check, an explicit absolute disposable
storage-root override was added test-first. The three focused config tests
passed after an expected RED compile failure. A new empty test directory was
created outside Git; Klaf Server started on loopback port 8090 with registry
schema version 10, while the default registry remains version 2. The required
owner environment was set only for the server process. Desktop also started
with its own empty isolated test storage; its initial unauthenticated AI
WebSocket rejection is expected. The operator approved the first test
registration. Read-only registry inspection confirmed one approved, unexpired
registration and zero active accounts/devices; Desktop must still check the
approval and complete enrollment with password re-entry. The subsequent
server UI showed one active account and one active device. Read-only checks
confirmed one active auth session, one AI access grant, a nonblank selected
Desktop account and one protected Desktop session bundle. A later Desktop log
reported `INVALID_CREDENTIALS`; the user confirmed the first password re-entry
was incorrect and the subsequent completion succeeded. The Desktop screen
showed the transferred guest deck and a yellow sync indicator. Manual sync
then completed: both local and known server versions were 1, with zero local
changes waiting to upload. The server history showed the added interim deck.
The first live AI request also succeeded: Desktop automatically requested Word
Insights for a newly created card, the server returned four meanings, and
Desktop logged a completed auto-save. The next check is whether this card and
its insights produce the expected pending-sync state and survive sync/reload.
The user confirmed that the Desktop sync indicator turned yellow after the
Word Insights auto-save. The next manual sync completed at server version 2
with zero pending local changes; history listed both Add card and Edit card.
A read-only check of the isolated test server's account database confirmed
one card with valid stored insights containing four senses. The existing debug
APK was installed over the app on the connected RMX2001 test phone without
clearing data, and MainActivity was launched. USB reverse forwarding for port
8090 was restored after the install; the loopback server is still listening.
Reload from the server on another device remains to be checked.
Android sign-in currently returns `INVALID_CREDENTIALS`. Read-only inspection
of the disposable registry confirmed the attempted email belongs to the
existing approved, unblocked account and recorded three failed password
checks in the unproven-device lane. USB forwarding remains active. This is
not a transport or unknown-account failure; the exact password mismatch
cannot be diagnosed without the user's input, which should not be logged or
shared. Avoid further guessing because the auth endpoint is rate-limited.
The user then completed the operator-assisted password reset and began Android
device enrollment. Read-only registry inspection found one valid, already
approved Android device request, but only one activated device (Desktop).
`Pending approvals: 0` is therefore expected: approval is done, while Android
must still use Check status, re-enter its new password, and press Complete.
The user completed that step. Android now reports synchronized at version 2
with zero local pending changes and lists Desktop plus RMX2001. A read-only
registry check confirms two approved, non-revoked devices, zero unapproved
device requests and one active session. Desktop appears offline, as expected
after password reset revoked its previous session. The card and its insights
were visually verified by the user on Android. This confirms the first
cross-device card/Word Insights round trip. Desktop still needs a fresh
sign-in. Other AI features and lifecycle checks remain open.
Live verification exposed an AI connection lifecycle defect after password
reset/device enrollment: manual sync succeeded, but the mnemonic Generate
button stayed disabled and the drawer showed Not ready until Retry. Sync REST
and AI WebSocket readiness are separate. `ActiveLocalRoomDatabase.selectAccount`
calls `KlafServerSession.endUserSession`, which reconnects only when the prior
connection was Ready. After reset/initial guest auth failure it was not Ready,
and `KlafServerConnectionManager` only initiates its first connection at
startup. The Android foreground reconnecter also triggers only on a
visibility transition. Add a regression test for sign-in from a failed or
disconnected AI connection, then reconnect on successful account selection;
make the disabled mnemonic action explain Not ready instead of silently doing
nothing. Desktop also needs a fresh sign-in after reset; its stale session
cannot be considered restored just because Retry was pressed.
No existing server data or original backup has been changed. Remaining
Android/device lifecycle and AI feature results are pending.

## Current review (2026-10-05): uncommitted Klaf changes

Reviewed the pending server/client authentication and unified AI cutover before
live verification. Two defects were reproduced with regression tests and fixed:

- If the lazy client auth provider could not initialize during logout, the
  selected account and local credentials remained active. Logout now clears the
  protected credential bundle independently and switches to guest mode even
  when remote logout cannot start. Cancellation still propagates after local
  cleanup. The focused desktop account-session tests pass.
- Starting a Codex worker for account A held the runtime pool's global mutex,
  delaying account B revocation. Runtime creation now uses an account-specific
  lock; a generation check prevents a runtime begun before block/restore from
  becoming active afterward. Both regression tests passed after the first was
  observed failing against the old implementation.

The complete post-refactor Windows suites passed: data 190, server 433,
presentation 80 tests, zero failures. `:Android:assembleDebug` also passed.
Live devices, Ubuntu and OS-level isolation are not validated by these tests.
No changes have been committed.

## Current focus (2026-10-04): unified AI production cutover

User requested all AI features on one account-scoped AgentDriver runtime:
replace SIWC text routing and the one-shot mnemonic image worker. Offline
transcription remains offline recognition, but its SDK session should share
the account-owned runtime lifecycle. TDD first; no commit. Current production
wiring routes AI features through the account runtime. Required verification:
authenticated account routing,
feature parity, live text/image overlap, logout/block/shutdown cancellation,
credential-free worker isolation, and Windows/Ubuntu checks.

TDD progress in this turn: `AccountCodexRuntimePool` now dispatches text,
image and offline transcription through one account runtime with bounded
admission; text/image and transcription-sharing tests were written first.
An initial transcription test exposed an illegal cross-coroutine `Flow.emit`
and led to a `channelFlow` delivery fix. The sibling AgentDriver SDK now
supports host-configured per-request instruction profiles on fresh
`AccountScoped` threads; focused SDK app-server tests passed, including an
unknown-profile rejection. The SDK was published only to local Maven.
Klaf has a persistent credential-free account runtime factory, explicit
owner-home configuration, hashed per-account SDK profile namespaces and
production DI routing for text/image/offline transcription. The dedicated
owner and account-worker lifecycle are lazy. At the initial wiring checkpoint,
no live AI request had been made.
The post-wiring `:klaf-server:compileTestKotlin` completed successfully on
Windows (slow: about 20 minutes). Focused post-wiring pool, profile,
factory-config, mnemonic and vocabulary tests passed, including the offline
transcription sharing test. These are fake-runtime/configuration tests, not
live Codex or end-to-end device validation. The full `:klaf-server:test` suite
then passed on Windows: 430 tests, zero failures (about 22 minutes).
The AgentDriver app-server session factory suite compiled and ran 53 tests;
the new text/image profile tests passed. One pre-existing fake-process
cancellation test failed once with `PROCESS_CLOSED` at fixture startup, then
passed when rerun alone. Treat this as a test reliability issue until a full
clean rerun confirms it, not as verified production behavior.

An opt-in live test of the production `ManagedAccountCodexRuntimeFactory`
initially failed before any request: AgentDriver preflight incorrectly required
an `auth.json` inside the credential-free worker profile even with an external
token broker. Added an SDK regression test, made provider preflight accept the
external broker while retaining executable/WSL/sandbox checks, and republished
`:sdk:server` to Maven Local. The production-factory live smoke then passed on
Windows: concurrent text and image requests through one account worker. The
same smoke then passed real Word Insights, mnemonic-association and Vocabulary
Source response parsing/validation through the shared pool. Offline speech
recognition remains fake-runtime tested only; the repository has no suitable
live audio fixture. OS-level cross-account isolation, 401 recovery and Ubuntu
behavior remain unverified. The temporary Windows worker root contained no
`auth.json`; a read-only audit of Ubuntu WSL `AgentDriver/runtime/codex-klaf-*`
after the test also found no worker `auth.json` (the SDK had cleaned its
account runtime directories).
The subsequent combined SDK regression run executed 62 tests: the new broker
preflight/profile tests passed, but four unrelated/legacy cases failed on this
Windows host. Two cancellation fixtures intermittently exited at fake-process
startup (`PROCESS_CLOSED`); two `CodexProfilePreflightTest` cases expect the old
native-login setup and instead receive `ProviderUnavailable` from current WSL
preflight. Do not report the entire AgentDriver suite as green; isolate or
modernize those fixtures before final SDK sign-off.
One later multi-account live run failed before its first AI request with WSL
`SandboxUnavailable`/`ProviderUnavailable`; direct cold-starting Ubuntu WSL
then took longer than the SDK's 30-second WSL preflight command limit and
successfully returned the installed Codex version. Increased only WSL
preflight command timeout to 90 seconds (local commands remain 5 seconds),
with an SDK test expectation. The multi-account live gate must be rerun after
SDK publication; the failure was not evidence of account isolation behavior.
After publishing the timeout fix, the expanded Windows live smoke passed:
account A handled concurrent text/image and three feature contracts, account B
started a separate worker, blocking A rejected new A requests, and B continued
to generate text. This verifies the tested cross-account lifecycle on Windows,
not OS-level isolation from malicious code or Ubuntu parity.
The SDK `CodexExternalAuthProtocolTest` also passed after the timeout change:
an unauthorized refresh request returns only a renewed access token for the
bound owner account, rejects a different account, and fails closed without a
broker. This is protocol-level verification, not a live forced-401 run.
SDK test-suite repair on 2026-10-05: the legacy Codex profile preflight
fixture assumed native Windows login, contrary to the SDK's WSL2 requirement.
It now tests the POSIX login contract independently of host OS; its focused
suite passed on Windows. The 53-test app-server suite then reported two
timing-sensitive failures, and a 56-test rerun passed those two but exposed a
different timeout assertion: the fixture's 100 ms operation deadline could
expire before the fake provider started a turn. Test deadlines were made less
dependent on Windows host load. A subsequent combined run exposed a fake
app-server image-event bug: it used the last global thread ID rather than the
request's thread ID. The image concurrency case passed in isolation after
that test-helper fix. Another combined run still failed a text/text overlap
case, where a child cleanup exception obscured the original failure; the
test now supervises and joins its child. A later combined run captured the
real cause in fake app-server stderr: Windows `AccessDeniedException` during
an atomic state-file replacement while the test reader had the file open.
The helper now retries only that transient file-lock error for a bounded
period. The combined `CodexAppServerSessionFactoryTest` and
`CodexProfilePreflightTest` rerun then passed on Windows: 56 tests, zero
failures. No production runtime behavior changed in this repair. The earlier
full 257-test AgentDriver JVM run with 11 untriaged failures is not claimed
green. Live forced-401 recovery and Ubuntu validation remain open.
SDK audit checkpoint (2026-10-05): a full Windows `:sdk:server:jvmTest`
run executed 267 tests and failed 5 after test-fixture corrections. The
synthetic full-session request/unauthorized-refresh/text-response test and
the three protocol broker tests passed; no real credential was read or
refreshed. The full-suite failures were two Claude response-limit reuse tests
whose second fixture reply itself exceeded the configured limit, two Ktor
transport/shutdown integration tests, and a Windows linked-directory cleanup
test. The Claude fixture now returns a short in-limit reply and both focused
tests pass; the Ktor shutdown test now supervises its expected failing child
and passed its focused rerun. The linked-directory test exposed a real Windows
data-loss bug: stale cleanup followed a junction and deleted an external
sentinel. Guarding every visited path by its canonical root fixed the
regression test. The transport-loss/resume test still fails: closing the
test's HTTP client does not reliably enter `ResumeAvailable`; forcing a close
frame instead causes the server to reject resume. The original fixture was
retained for separate diagnosis. Do not claim the SDK full suite green. Three
other fixture assumptions (provider issue after directory-preflight failure,
exact Windows child environment, and CRLF output) were corrected and passed
their focused tests. Ubuntu WSL is available but has no Java runtime, so
native Gradle verification has not run. Current official OpenAI Docs describe
environment access-token injection and app-server restart on renewal for the
published plan-usage app-server flow; they do not establish our in-process
`chatgptAuthTokens` refresh callback as a stable public contract. Treat the
callback as an unresolved production-compatibility risk despite synthetic
passes. The final Windows SDK full-suite rerun executed 267 tests with one
failure: the unchanged transport-loss/resume fixture above. All other tests,
including the synthetic refresh, linked-directory security regression,
concurrency and corrected shutdown scenario, passed. No real forced-401 test
or Ubuntu-native Gradle run was performed. See the official page at
https://developers.openai.com/siwc/token-sharing-open-source/codex-app-server.
Follow-up (2026-10-05): replaced the failing reflection-based transport-loss
fixture with a loopback TCP proxy that resets the active socket without a
normal WebSocket close frame. The test now proves that `resume()` keeps the
same provider session; its focused run passed. Another full-suite run exposed
an unrelated race in `ServerPolicyIntegrationTest`: receipt of a WebSocket
close frame preceded asynchronous provider cleanup. The test now waits for
that cleanup and passed focused. The subsequent full Windows SDK JVM suite
passed: 267 tests, zero failures. A checksum-verified temporary Temurin JDK 21
is available inside WSL Ubuntu. Gradle downloaded its vendor-pinned Azul JDK
and the focused native Linux `:sdk:server:jvmTest` run passed 11/11 tests:
external auth protocol (3), protected storage (7), and unexpected TCP-loss
resume (1). The follow-up full WSL Ubuntu SDK JVM suite passed 267/267 with
zero failures. This does not validate the eventual production Ubuntu machine,
Linux Secret Service integration, or resolve the auth callback's
public-contract risk.
The SDK snapshot was republished to Maven Local only after the Windows
junction-cleanup fix. Klaf `:klaf-server:test` then rebuilt against that local
artifact and passed 431/431 tests on Windows with zero failures. No remote
publication or Git commit was made.
Follow-up AgentDriver code review found a concurrent external-auth refresh
race when two account requests receive 401 together. A test first reproduced
overlapping broker calls; the SDK now serializes refresh calls within one
app-server connection. The full SDK suite passed 268/268 on Windows and in a
forced WSL Ubuntu rerun. The updated SDK was republished to Maven Local only,
and Klaf `:klaf-server:test` passed 431/431 against it. No Git commit or
remote publication was made; real 401 remains a separate live gate.
Do not call the cutover complete or deploy it until compile/test, account
block/logout/restart behavior, live feature parity and Ubuntu isolation pass.

## Current focus (2026-10-04): production account runtime lifecycle

New concurrency decision: one account worker must permit overlapping
independent requests in different Codex threads, including two requests from
the same device/feature. No per-feature queue and no extra Codex worker per
feature or device. The previous AgentDriver `SessionBusy` and account-wide
request mutex have been replaced for `AccountScoped` mode by per-request
thread context and event routing; legacy modes keep their one-active-request
contract. The Klaf pool now bounds account admission; a live simultaneous-turn
check is still required. Do not claim production concurrency already works.

The Klaf pool now has a focused test in which two requests for the same
feature/account overlap while sharing one fake runtime. The focused Windows
suite passed 10 tests with no failures. This proves the pool's contract only;
the AgentDriver transport now has separate fake-process overlap tests, but
the Klaf production route is not wired to that runtime yet.

AgentDriver TDD checkpoint: a RED fake app-server test first showed that a
second account-scoped turn could not finish while the first waited. The SDK
now opts only `AccountScoped` sessions into multi-request execution, routes
app-server events by request `threadId`, and keeps turn start/interrupt and
unsubscribe request-scoped. Focused text/text and text/image overlap tests,
the request-executor cancellation test, and both related existing JVM suites
passed on Windows. This is fake-process verification only; bounded account
admission verification in production, real Codex overlap, SDK publication, Klaf runtime factory/wiring,
and production feature parity remain open.

Klaf account admission now allows three simultaneous requests per account by
default, with a bounded 30-second wait for an available slot; these are
constructor configuration values, not feature-specific queues. A focused
11-test pool suite passed on Windows, including the two-permit/third-request
timeout case. AgentDriver metrics were adjusted so one SDK session can report
more than one active request; focused domain and lifecycle suites passed.
An end-to-end fake-provider integration test now drives two overlapping text
requests through one Ktor client connection and one SDK session; the second
finishes while the first is held, and only one provider runtime is opened.
That focused Windows test passed.
Further fake-process tests passed for canceling one of two active turns and
for denying a tool-approval request without crossing into the other turn.
Cancellation during `thread/start` now has a RED-to-green regression test:
the in-flight start finishes in the process scope, then the late thread is
unsubscribed without running its turn or stopping other work. The related
AgentDriver focused suites passed together after this change: 66 JVM tests,
0 failures (50 app-server adapter, 10 executor, 5 metrics, 1 Ktor integration).
The full AgentDriver JVM run completed 257 tests with 11 failures in Claude,
process environment, preflight, storage, and integration tests. Those failures
have not yet been isolated against a pre-change baseline; do not call the
full suite green.

Next slice: add a persistent credential-free AgentDriver runtime factory,
publish the tested SDK locally, and validate actual concurrent Codex turns,
multi-account isolation, text/image/audio parity and Ubuntu behavior before
switching production routing. Production SIWC routing remains unchanged.

Lifecycle TDD checkpoint: `AccountCodexRuntimePool` now tracks a child Job per
request, so canceling one device request leaves the shared account runtime
alive. Blocking an account prevents new requests, cancels and joins only that
account's in-flight requests, then closes its runtime; restoring it permits a
fresh runtime. Shutdown cancels all active work and closes all runtime slots.
A failed request keeps its runtime reusable and a failed factory is not cached.
The focused JUnit suite executed 9 tests with 0 failures on Windows; `git diff
--check` reported no whitespace errors (only existing line-ending warnings).
This pool is not yet wired to production DI or operator account blocking.

Capability gate discovered before production factory wiring: AgentDriver's
`AccountScoped` launch disables shell, computer use, browser, apps, plugins and
multi-agent at OS-process startup but enables `image_generation` for the
entire process. Its advertised session capabilities include both text and
image. A text request therefore cannot currently prove that image generation
is unavailable to a prompt-injected model turn. The public app-server
`thread/start`/`turn/start` documentation does not identify a per-request
`image_generation` deny flag; dynamic tools are additive and experimental.
One process per Klaf account with strict per-feature tool allowlists has not
been proved possible with this SDK. The user clarified that the intended
architecture is one Codex worker per Klaf account across all its devices and
features, with a fresh thread per request rather than a persistent thread per
device/feature. Image generation may remain process-wide for this personal
MVP; this does not imply cross-account access or authorization bypass. The
shared login-owner and sandbox/network helpers are separate infrastructure,
not extra account workers. Continue implementation under this explicit
trade-off; production cutover still awaits the other isolation and feature
tests.

Dedicated owner login preparation: created
`%USERPROFILE%\.klaf-server\codex-owner` outside Git, with Windows ACL
inheritance disabled and only the current user, SYSTEM and Administrators
allowed. Confirmed it had no `auth.json` before login. Device-code login was
disabled for this ChatGPT account, so the operator completed browser login
with that directory as both `CODEX_HOME` and `CODEX_SQLITE_HOME` and explicit
file-backed credentials. The existing desktop Codex home remains untouched.
`codex login status` reports ChatGPT, and the owner file is a regular,
non-reparse file of bounded size with only the current user, SYSTEM and
Administrators allowed via the protected parent ACL. A no-output structural
check found the managed-login marker, access token, refresh token and
account ID. An opt-in live owner smoke passed twice on Windows: the dedicated
app-server reported ChatGPT login, `account/read(refreshToken=true)` rotated
the access token, the account ID remained stable, and no AI thread was
started. The test emitted no credential values. Next: account worker broker
handoff and feature parity; do not switch production routing yet.
The live check asserts that forced refresh changes the access token. A
RED/GREEN regression also removed the opaque account ID from owner/token-
provider `toString()` diagnostics. Focused owner and broker tests pass. A
scan of touched AI code and status/plan docs found no personal Windows path,
email address or one-time login code.
An opt-in live broker smoke was forced to execute (not merely Gradle
`UP-TO-DATE`): one `AccountScoped` AgentDriver worker received owner access
through the broker, completed a text request, and left no `auth.json` in its
temporary data root. JUnit reported one executed test in 29 seconds with no
credential values in output; the temporary worker root was removed. This
validates one Windows/WSL worker request, not two-account isolation,
401/restart handling, all feature capabilities, Ubuntu or production routing.

Prior synthetic checkpoint: the user approved a separate owner `CODEX_HOME`
while each Klaf account keeps its own credential-free worker home. Implement
the explicitly unsupported, owner-only `auth.json` compatibility adapter
with synthetic TDD. That checkpoint did not authorize real credential reads,
refreshes or production cutover; subsequent operator login and opt-in live
verification are recorded above. Production cutover remains gated.

Synthetic TDD checkpoint: added a read-only owner `auth.json` reader with
explicit absolute home, bounded file reads, no symlink traversal at the file
boundary, POSIX/Windows ACL checks, strict managed-login/account/schema
validation and generic redacted errors. A file-backed owner now invokes
managed refresh before reading the replacement token; a protocol client sends
`account/read(refreshToken=true)` and checks the returned auth mode. A
dedicated stdio transport initializes one owner app-server process and accepts
only account reads, never AI threads/user prompts. New tests were RED before
implementation and the focused owner, broker and protocol suite passed on
Windows with synthetic credentials. The Windows temp fixture required an
owner-only ACL to pass, as intended. No real login file was opened and the
new owner path is not yet bound to the production AI services.

Owner stdio smoke checkpoint: an opt-in test against the installed native
Windows Codex CLI now launches with a fresh, empty `CODEX_HOME`, completes
`initialize` and `account/read(refreshToken=false)`, and confirms that the
operator's desktop login is not inherited. It runs no AI turn and reads no
real credential. The first live probe exposed a Windows cancellation hang in
blocking stdout reads; `CodexOwnerStdioTransport` now pumps bounded replies
through a cancellable channel, with a regression test. The npm/PowerShell
launcher also exposed child-process and Windows SQLite file-lock cleanup
races; the transport now terminates its process tree, and the opt-in smoke
uses the native CLI plus bounded cleanup retries. Focused transport and smoke
tests pass. The `auth.json` private schema and actual managed-token renewal
still require a dedicated-login verification before any production cutover.
An additional RED/GREEN regression covers 100 unsolicited app-server
notifications before `account/read`: the bounded reader now discards
notifications before queuing responses, so normal owner activity cannot
overflow the response queue.
Five empty-profile smoke directories remain under Windows `%TEMP%` from the
failed cleanup attempts; they contain only isolated Codex test state, not the
operator's desktop login. A direct cleanup command was rejected by the local
execution policy. The final opt-in smoke cleans its own temporary home and
passes. Do not confuse these test directories with a dedicated owner login.

## 2026-10-04: managed-login token-source decision gate

Rechecked current official OpenAI documentation: app-server
`account/read(refreshToken=true)` can refresh a Codex-managed ChatGPT login,
but its response contains account metadata, not the access token. The
experimental `chatgptAuthTokens` mode requires the host to supply and renew
that token. Enterprise Codex access tokens are documented for Business and
Enterprise workspaces, not the personal subscription assumed here. SIWC has
an official host-owned token flow, but the user explicitly rejected replacing
the managed Codex login with SIWC. No real credential was opened, copied or
refreshed during this investigation. Therefore the existing synthetic broker
bridge cannot be live-wired via a documented managed-login token-export API.
Using a trusted, owner-only reader of Codex's private credential store would
be a deliberate unsupported compatibility adapter and requires an explicit
design decision; do not silently add it. Keep the existing AI path active.

## 2026-10-04: public broker bridge (synthetic TDD checkpoint)

AgentDriver now exposes a host-only `CodexExternalTokenProvider` constructor
for Codex Assistants. Its access-token wrapper redacts `toString`, and the
provider is forwarded to the existing experimental app-server external-auth
broker. A RED public-API test failed before the API was added and passed
afterward. The updated server SDK was published to Maven local only, not to
any remote registry. In Klaf Server, a per-worker
`ManagedCodexTokenProvider` now delegates to the shared coordinator while
remembering each worker's rejected token. RED/GREEN tests verified two
workers share one refresh, account mismatch fails closed, cancellation leaves
the previous token retryable, restart reads the owner's current state, and
tokens are not included in diagnostics. Both focused Klaf AI token test
classes pass.

This is a synthetic bridge, not a production AI cutover. No real Codex
credential was read or refreshed, and SIWC remains the active text path. The
next hard gate is a supported trusted owner capable of obtaining and renewing
one managed-login access token without concurrent credential-file writers;
the official app-server `account/read` response alone does not supply it.
Then test the actual broker-backed runtime and feature parity. Do not treat
local Maven publication or these fake-token tests as evidence of live login.
Current official OpenAI guidance for supplying an OAuth token to app-server
describes the separate Sign in with ChatGPT (SIWC) flow, whose host owns its
own credential record and refresh. That is not the user-approved managed
Codex login and must not be silently substituted. The experimental app-server
external-token protocol accepts a token but does not solve how Klaf obtains
and renews one from the managed Codex login. This remains the live blocker.

## Current focus (2026-10-04): single-writer login feasibility

The user approved a focused investigation of one writable Codex managed-login
owner with read-only per-account workers. Start with synthetic tests and inspect
installed Codex behavior; do not force refresh or read/copy the real operator
credential. A real two-worker text smoke test already passed, but renewal and
cross-history isolation are still unverified. Production stays on SIWC.

Feasibility result: the proposed read-only `auth.json` mount is **not a safe
single-writer renewal design**. Official Codex documentation says managed
ChatGPT login automatically refreshes and writes the credential bundle, and
warns against concurrent consumers of one `auth.json`. The upstream Codex
auth-manager implementation obtains a replacement token from the auth server
before persisting it. A read-only worker could therefore rotate the shared
refresh token remotely and then fail to save its replacement, invalidating the
owner's stored token. The installed WSL CLI reports `codex-cli 0.146.0`;
SDK launch code indeed mounts the common file read-only in a private home.
No real refresh, 401 injection, or secret inspection was attempted. Synthetic
ownership tests would only test our coordinator, not prevent Codex's internal
refresh; do not add a misleading GREEN test or wire production to this design.
Official app-server external-token mode can delegate refresh to the host,
but it is experimental and requires the host to own/distribute access tokens.
The user subsequently accepted exploration and implementation of that broker
design. Keep existing SIWC route until verification.

The user accepted the broker design. Current task: TDD on
synthetic credentials for an account worker that receives access tokens over
app-server JSON-RPC and has no `auth.json` mount. The existing SDK has an
experimental broker protocol but rejects it in `AccountScoped` mode; its WSL
launch script also assumes an auth-file mount for every isolated runtime.
No real token should be read or refreshed during this phase.

SDK checkpoint: RED tests exposed the missing no-auth profile mode and mount
ordering bug. AgentDriver now permits the experimental broker for
`AccountScoped`, mounts an empty ephemeral profile without `auth.json` for
Linux/WSL broker workers, and binds the session parent before that nested
profile. Focused SDK isolation, external-auth protocol and session-factory
tests passed with synthetic access tokens. No Klaf production wiring, owner
refresh serialization, real-login broker test or native Linux check yet. The
owner's access-token source is unresolved: official `account/read` does not
return tokens, so a trusted owner would need another approved mechanism to
obtain them. Do not infer approval to inspect the real login file from the
synthetic SDK tests.

Klaf broker coordinator checkpoint: TDD added synthetic tests for two workers
receiving one refreshed token after concurrent 401 responses, rejecting a
changed ChatGPT account, rejecting an unchanged token, and redacting owner
exception text. RED failures were observed before the implementation; focused
`ManagedCodexTokenCoordinatorTest` then passed. The coordinator is an internal
untethered component: AgentDriver's broker interface is still SDK-internal,
there is no credential-source adapter or production DI binding, and no real
secret was read. Next gates are public SDK-to-Klaf broker wiring, one trusted
owner using the single managed login, synthetic restart/cancellation tests,
and controlled Windows/WSL then Ubuntu checks. Keep SIWC active.

## Account-scoped SDK checkpoint (2026-10-04)

AgentDriver now has an opt-in `AccountScoped` Codex mode under development:
validated per-account profile namespaces, distinct WSL profile/runtime paths,
one app-server process per SDK session, and a fresh ephemeral thread per text
or streaming request. Image generation uses the same thread lifecycle wrapper.
Focused fake-provider and policy tests passed, including cleanup after a
failed text request. This is not yet published to
Klaf or wired into production. The current SIWC path remains active. The key
remaining gate is one supported Codex login with safe renewable credentials
across separate profiles, followed by real Windows/WSL2 and Ubuntu isolation
checks. Do not claim production account isolation from these synthetic tests.
At this earlier checkpoint the SDK rejected its experimental external-token
broker in account-scoped mode; the newer SDK checkpoint above supersedes that
restriction. This is still not a production login path.
The larger focused SDK test group (`CodexAppServerSessionFactoryTest`, account
profile paths, image policy and domain namespace validation) passed. At this
point, credential sharing/renewal had not been validated; the subsequent
controlled check below supersedes the immediate-login part of that concern.
No secrets were read or copied, and no commit was made.

Follow-up controlled Windows/WSL2 check: two `AccountScoped` Assistants ran
simultaneously using the existing single managed Codex login; each returned
a real exact-match text response. The worker sandbox mounted the same login
file read-only into a private temporary home. No secret content was read or
copied. This overturns the narrower concern that immediate two-process use
requires a second interactive login. It does **not** resolve long-term
renewal: OpenAI's advanced auth guide says Codex writes refreshed credentials
back to `auth.json` and warns against concurrent jobs sharing one file. We
did not force-refresh or alter the real login. Production remains on SIWC
until a single-writer renewal design and cross-history isolation are tested.

## Architecture decision (2026-10-03; documentation only)

Work resumed: Word Insights is the first TDD vertical slice of the unified
Codex architecture. Initial inspection found its service and DI binding use
a shared stateless generator and do not receive the authenticated account ID;
the WebSocket principal does contain that ID. Next: RED account-routing and
runtime-sharing tests, minimal implementation, focused validation. This entry
does not claim tests or production Codex cutover have passed yet.

Word Insights checkpoint: new RED tests for account-runtime reuse/separation
and account-aware Word Insights routing were observed failing at compilation;
their minimal implementations then passed. A signed WebSocket integration
test with two devices on one account and one device on another passed. The
server now passes the authenticated principal's account ID into Word Insights;
the existing SIWC generator remains the production adapter. Focused unit tests
passed and the focused WebSocket integration test passed. No live Codex text
request or production Codex cutover was attempted. `git diff --check` passed
for tracked files touched here; line-ending warnings were informational.

New SDK dependency gate: AgentDriver currently opens one Codex app-server
process and one thread per Assistant session, and its protected provider
profile path is shared by all Codex Assistants. Creating an Assistant for each
Klaf account as-is would *not* isolate account history/profile; recycling one
Assistant for independent Word Insights requests would reuse its thread. The
approved design needs an AgentDriver multi-thread/per-account profile API or
an equally secure implementation within Klaf. Do not wire the account pool
to the current SDK, copy operator auth into shared profiles, or claim this
security gate resolved. The user approved modifying the sibling SDK on
2026-10-03. Current focus is TDD for per-account SDK profile/process ownership
and per-request ephemeral threads. This approval does not resolve credential
sharing/renewal, native Windows/Ubuntu sandbox, or all-feature capability
isolation by itself.

The user rejected the locally implemented SIWC-direct-text/Codex-image split:
the target is one operator Codex ChatGPT login, one isolated Codex app-server
OS process/private runtime per AI-enabled Klaf account, and one fresh ephemeral
thread per independent request. All AI features, including Word Insights,
mnemonic text/image, Vocabulary Source analysis and audio transcription, must
use this unified path. The Klaf Server JVM remains the trusted router. One
account's devices may share its process, but request cancellation/results remain
device/auth-session scoped. "Worker" was ambiguous in discussion; the target
process here means an actual child OS process, not an object in the server JVM.
Requirements and plan were updated; production code, tests and deployed state
were not changed in this decision pass. Current SIWC text and per-image-request
worker behavior described below are historical implementation status, not the
approved target. Open design gates: safe per-request tool separation in exactly
one account process, single Codex-login use/renewal across separate profiles,
thread/temp-file lifecycle, and Windows/Ubuntu plus device verification.

## Current focus (2026-10-03)

User clarified that new mnemonic-image generation must survive the auth
migration; a text-only endpoint is not acceptable. The previous dual route is
implemented locally but now superseded: SIWC remains in the text code, while mnemonic
images use a separate ChatGPT-login Codex `ImageOnly` worker. The SDK snapshot
was published to Maven Local and Klaf's mnemonic session now starts one
ephemeral/read-only image worker per request, then stops it. The AI `/ws` route
is now open only for authenticated, explicitly AI-granted accounts; its live
client/server integration and security checks are still in progress. This is
not release-ready. No commit was made.

Current verification checkpoint (2026-10-03): the full Klaf server tests,
desktop data tests, and Android DI compile passed together after the
authenticated AI WebSocket changes. The authenticated image test returned a
real PNG when Ubuntu was running, but a repeat from stopped Ubuntu exposed
SDK preflight's five-second command timeout; actual WSL cold start took about
11 seconds. A focused AgentDriver TDD regression now gives WSL preflight
commands 30 seconds while keeping other commands at five seconds. The focused
SDK test passed and the updated snapshot was republished locally. The
authenticated image retest against that snapshot passed with a real PNG
through the signed WebSocket request. This retest started with Ubuntu already
running; the 30-second WSL cold-start fix has synthetic regression coverage
but has not yet been rerun from a stopped distribution. Review also found
that global WebSocket connection count could suppress one account's push when
another account remained connected, and push deduplication used only the
client-supplied request ID. TDD regressions now scope both to the authenticated
session. The targeted connection/push tests passed. The complete Klaf server,
desktop data and Android DI rerun then passed in 3m 27s. The SDK's full JVM
suite was rechecked separately: 244 tests ran and the same 11 failures
recurred (Claude response-limit tests, process/environment tests, default
Codex/shared preflight expectations, client/server lifecycle tests, and a
Windows symlink cleanup test). None were in the focused ImageOnly/cold-start
tests, but these SDK failures are not being represented as a green full suite;
their individual causes still need triage before release. A small refactor moved
the WSL-specific timeout policy out of the Claude preflight file into a shared
helper; focused cold-start, image-profile isolation and network-bridge tests
passed, and the SDK snapshot was republished. Android debug APK assembly and
Desktop app compile both passed (1m 41s). The final signed WebSocket image
generation test against the republished snapshot also passed with real PNG
bytes (1m 25s). `git diff --check` passed in both repositories; a targeted
scan found no literal private keys or API-style secrets in the touched source
and docs. Remaining release gates: investigate the SDK's 11 full-suite
failures, verify native Ubuntu server/sandbox behavior (this WSL Ubuntu lacks
a Linux Java runtime), and do a physical-device end-to-end smoke check with
the approved account. No commit.

Validation checkpoint: the controlled AgentDriver Windows/WSL2 bubblewrap
test returned a real PNG through the network-bridged worker, and Klaf's
controlled `MnemonicAgentSession` test returned PNG bytes through the locally
published SDK. Full `:klaf-server:test --offline --console=plain
--max-workers=2` passed on Windows (3m 10s). The full AgentDriver JVM suite was
not green: 11 tests failed outside the focused image suites; their baseline
status/root cause is not yet established. The first controlled AgentDriver
image run failed when Ubuntu was stopped; it passed after WSL was active, so
cold-start behavior still needs verification. Ubuntu-native isolation, SIWC
provider launch/refresh, account-bound AI request ownership, and end-to-end
authenticated image generation remain open. Do not commit.
After this checkpoint, an SDK TDD regression exposed acceptance of arbitrary
nonempty decoded image bytes as PNG. The image parser now checks the PNG
signature; focused valid/invalid image tests passed. A separate focused test
guards `ImageOnly` against the experimental external-token broker. The updated
SDK was republished to Maven Local; a complete Klaf rerun against that updated
snapshot passed with `--offline --refresh-dependencies` (5m 53s). The
controlled Klaf mnemonic-image test also passed again against this snapshot
(54s), returning real PNG bytes via Windows/WSL2 bubblewrap. These tests do
not exercise the authenticated `/ws` route, which remains disabled.

AI operation ownership TDD (2026-10-03): registry tests first failed, then
passed after scoping cancellation, client-session end, replay lookup and
metadata to the authenticated session ID. Klaf `/ws` handlers now supply the
principal's session ID to operation start/cancel/lookup, and REST logout calls
`cancelAuthSession` after revoking the session in storage. Focused registry
and live logout tests passed. This does not yet cover device/account revocation
of in-flight jobs, per-message WebSocket revalidation, or transcription upload
state ownership; keep `/ws` disabled.

SIWC text-route direction (2026-10-03): official documentation permits direct
Responses API calls with the OAuth access token, `store=false` and
`stream=true`. This avoids exposing the token to a Codex child and the proven
`/proc` parent-environment leak. A pure request/SSE-event protocol slice went
compile RED then GREEN: stateless one-message request, optional JSON schema,
no tools/history fields, completed-output requirement, generic failure
messages and response-size bounds. This is not yet an HTTP client or feature
integration, and no live SIWC inference has been sent in this slice. A later
TDD slice added a trusted HTTPS transport (token only in Authorization header,
fixed official endpoint, no redirects, bounded SSE read, generic errors) and
an access-token-provider client with cancellation propagation. Focused
protocol/client/mock-transport tests passed. The HTTP transport has not yet
been wired into feature services or exercised with the live SIWC grant.
Live operator probe then succeeded twice with the protected SIWC grant and
the selected `gpt-5.5` model: first free-form text, then strict JSON-schema
output. The CLI printed only a success line; neither token nor model output
was printed. The HTTP response did not expose a Content-Type header to Ktor,
so the transport now accepts missing Content-Type only if the bounded SSE body
parses into completed events; a synthetic regression test covers this. This
proves live text inference entitlement on Windows, not feature integration or
Ubuntu deployment.
All three text features now route through stateless SIWC text gateways:
Word Insights, Vocabulary Source analysis, and mnemonic association. Their
focused TDD routing tests went compile RED then GREEN and verify each feature's
instructions and JSON schema. The old shared Codex text sessions were removed;
mnemonic image generation remains a separate ephemeral ImageOnly worker.
The gateways read the protected SIWC grant only on request and close HTTP
clients on service stop. `/ws` was still disabled at that checkpoint pending
auth-session revalidation, revocation, and transcription ownership tests; do not treat the
successful live operator probe as an authenticated end-to-end feature test.
The opt-in live feature probe then caught HTTP 400 from the Word Insights
strict schema: `uniqueItems` is unsupported by the provider. TDD removed that
keyword from the provider schema and retained duplicate-example validation in
Klaf. After the fix, one controlled test sent synthetic `apple` inputs and
successfully parsed real SIWC responses for Word Insights, Vocabulary Source
analysis, and mnemonic text. No user content or token was printed; this still
does not exercise `/ws` authentication or image generation through `/ws`.
An additional TDD check now validates that an authenticated AI principal loses
its permission after grant or device revocation and after access expiry. The
helper is now wired to each WebSocket frame and a one-second active-connection
monitor. Expired access closes the socket without revoking the refreshable
auth session or killing an already admitted request. Grant/device revocation
cancels running work without permanently tombstoning a session that might be
regranted later (a registry TDD test covers that distinction). Upload chunks
and completion now require an upload started
on the same connection; cancel and client-session-end no longer reach another
session's audio uploads. Push tokens are now keyed by auth session as well as
client session. A TDD authenticated `/ws` handshake test went RED against the
temporary `AI_UNAVAILABLE` gate, then GREEN after the gate was removed. The
production desktop and Android WebSocket client now obtains a fresh DPoP nonce
with a signed GET and signs the upgrade for the selected account. The nonce
preflight MockEngine test and a live server/client WebSocket test with a fake
Word Insights generator passed. Remaining: revocation and cross-owner WebSocket
integration tests, real authenticated image flow, Android compile/device smoke,
full suite, and security review. No commit.

Image compatibility investigation (2026-10-03): the existing WSL Codex
ChatGPT login generated real PNGs with shell/computer tools disabled. A direct
Codex app-server JSON-RPC probe with shell/computer/browser/apps/plugins/
multi-agent disabled, `ephemeral=true` and `sandbox=read-only` returned a
completed `imageGeneration` event with 891,325 decoded bytes and PNG magic.
A stronger bubblewrap probe mounted the operator's `auth.json` read-only over
an otherwise temporary `CODEX_HOME`; that app-server still returned a
completed 784,410-byte PNG. No token contents were printed or copied. This
proves image capability and auth-file-only writable-state separation in a
controlled WSL process. The full AgentDriver network-bridged minimal sandbox
and Klaf-level controlled route passed later as recorded above. An initial
`codex exec` probe also showed
that a textual success claim is not proof of a completed image; production
must validate the actual image item and bytes.

AgentDriver TDD work now adds an opt-in `CodexRuntimeMode.ImageOnly`: new
domain and bridged-mount tests went compile RED then GREEN; image mode disables
unrelated Codex tools, selects ephemeral/read-only app-server turns, advertises
only image generation, uses the temporary profile/read-only auth mount on
bubblewrap paths and rejects non-bubblewrap startup. Focused domain and core
tests passed. This paragraph records the earlier SDK-only checkpoint; see the
current focus above for local publication and controlled live image results.
AI `/ws` remains disabled. No commit was made.

Continue the SIWC provider gate after successful operator login: add a
redacted local credential/readiness check under TDD, then validate a supported
Codex app-server launch and OS-enforced per-context isolation. Official SIWC
preview currently omits native image generation and audio transcription, so
text-provider readiness is not full AI-feature readiness. Keep AI `/ws`
disabled and do not commit. The live access-token refresh and Windows/Ubuntu
sandbox checks have not yet been performed.

2026-10-03 continuation: `--siwc-check` now reads the protected local bundle,
refreshes only when near expiry, prints a redacted readiness line and never
enables AI. Focused `SiwcOperatorCommandTest` went compile RED then GREEN;
the real operator check exited successfully without printing credential values
or sending an inference request. This does **not** prove live refresh, model
entitlement or safe provider launch. A controlled WSL2 bubblewrap probe with
only a synthetic token found that an environment-scrubbed child can read the
parent's token through `/proc/1/environ` in the current `--proc /proc` layout.
The WSL2 launch path therefore cannot receive a real SIWC token yet; a
verified nested PID/tool boundary or trusted token-free provider request
broker is required. The same must be verified on Ubuntu, not inferred from
Windows/WSL2. The full `:klaf-server:test --offline` suite passed on Windows
in 4m 12s after this change. `git diff --check` reported no whitespace errors.

## Live SIWC checkpoint (2026-10-03)

After the operator synchronized the Windows clock, its UTC matched OpenAI's
HTTPS Date header within approximately one second. A repeat
`:klaf-server:run --args="--siwc-login" --offline` completed real browser
authorization, token exchange, signed ID-token and grant validation, and
protected credential persistence. The command exited successfully. The
credential blob and stable host-ID file exist outside the Git repository under
the server's local security directory; no token value was printed or copied
into docs. The token-refresh path remains synthetic-tested only. Provider
worker launch and Windows/Ubuntu OS-level context isolation remain unverified,
so AI `/ws` stays disabled. The Windows Time service was still stopped after
manual clock correction; watch for renewed drift before future reauthorization.

## Current continuation (2026-10-02)

Clock diagnosis (2026-10-03 local time): two independent HTTPS Date headers
were about 58 minutes ahead of Windows UTC, preventing live SIWC token
validation. Await OS time sync before repeating browser authorization.

Live operator check (2026-10-02): `:klaf-server:run --args="--siwc-login"`
opened the browser and received the loopback callback, but code exchange then
failed at `SiwcIdTokenVerifier` with a generic identity rejection. No credential
file was saved. A TDD slice added fixed non-sensitive verifier reason codes;
focused ID-token and OAuth transport tests passed. The repeated live login
failed specifically on `ISSUED_AT`: the verified token's iat is more than five
seconds ahead of the Windows host clock. Independent HTTPS Date headers from
OpenAI and Cloudflare were about 58 minutes ahead of local UTC (2026-10-02).
`w32tm /query /status` says the Windows Time service is not started. Do not
weaken token validation or silently change the OS clock; the operator needs to
correct/sync Windows time, then rerun live sign-in. No credentials were saved.
Never print ID/access/refresh tokens or raw claims. AI `/ws` stays disabled.

The synthetic SIWC core, protected file-store contract, authorization URL
builder, scope checks and Windows DPAPI round-trip are GREEN in focused tests.
This turn added synthetic ID-token signature/issuer/audience/expiry/nonce/kid
tests (compile RED, then GREEN) and a Nimbus verifier. A second RED/GREEN slice
bound the exact loopback redirect URI to the login attempt, authorization URL
and token-exchange contract; the three focused SIWC suites passed together.
The verifier now feeds a concrete public-client HTTP OAuth transport. Synthetic
tests went compile RED then GREEN for exact exchange form, JWKS-verified subject,
invalid nonce rejection and rotating refresh carrying the previously verified
subject. A protected-store RED/GREEN slice retains the verified ID token for
future reauthorization, reads legacy v2 blobs and writes v3. A malformed JSON
regression test first failed because parser diagnostics could reveal token text;
the transport now emits a generic error. Full `:klaf-server:test --offline`
passed on Windows (4m 8s) at that checkpoint.

Further RED/GREEN slices added an ephemeral 127.0.0.1 browser callback listener,
one-time/cancelled login attempts, stable host ID, returning `id_token_hint`,
verified replacement ID token on refresh, and local `--siwc-login` operator CLI.
The CLI has only been exercised with a synthetic provider; **no live OAuth login
was attempted**. A later full server suite completed 348 tests with one failure:
`ServerSyncContractTest.signup creates one account database and registers the
first device` received `401 INVALID_CREDENTIALS` during registration completion.
The same test alone and the entire `ServerSyncContractTest` class passed on
rerun, and a second full `:klaf-server:test --offline` run passed (3m 33s).
Treat the initial failure as an intermittent test issue whose root cause is
not established, not as proof of a fixed regression. No provider-process
launch or OS-level agent isolation has been verified.
AI `/ws` remains unavailable. Preserve all current uncommitted work; no commit
was made.

## SIWC test-only checkpoint (2026-10-01)

Continuation (2026-10-01): user authorized implementation after the test-only
pause. Current focus is the test-defined SIWC credential/refresh and context
partitioning core. Provider OAuth wiring, secure persistent credential storage,
AgentDriver launch integration, and OS sandbox proof remain separate release
gates; the AI endpoint stays disabled until these are verified.
The first focused core run found two bad test fixtures (bootstrap client ID
instead of issued ID); those were fixed without relaxing the production check.
A new two-manager refresh test then failed at runtime because a manager-local
mutex cannot serialize rotation across managers. The store contract now owns
the exclusive lock around load/network refresh/save. The focused 10-test suite
was GREEN. File-backed credential tests were compile RED and then GREEN after
adding an injected-protector store with cross-instance OS file lock, protected
blob, owner-only POSIX permissions and atomic replacement. Four focused store
tests passed on Windows. A synthetic Windows DPAPI round-trip test was RED
then GREEN; existing signing-key recovery tests also passed after reusing the
platform protector with a distinct Linux Secret Service purpose. The Ubuntu
libsecret path has not been run in this turn.
Official SIWC callback guidance exposed a test-fixture-hidden client-ID bug:
initial code exchange must use the issued callback ID, never the bootstrap
`dynamic_agent_client`. New tests were compile RED, then GREEN after fixing
this and rejecting a changed returning client ID or provider subject. Combined
focused core/store/DPAPI/signing-key suite passed. No live OAuth request,
browser interaction, provider session or sandbox proof was performed.

Added `SiwcProviderContractTest` with seven synthetic cases for login callback
state, atomic credential persistence, parallel single-flight refresh, failed
refresh, provider-subject mismatch, context partitioning and token-free tool
environment/diagnostics. Focused `:klaf-server:test --tests
com.kuts.klaf.server.security.SiwcProviderContractTest --offline` reached
`compileTestKotlin` and failed because the proposed `com.kuts.klaf.server.ai`
SIWC/context classes do not exist. This is a **compile RED only**: no test body
ran, and no runtime behavior or OS-level isolation is verified. The tests
currently define a candidate production API; review that boundary before
implementing it. This turn is limited to tests; production provider code,
live credentials,
AgentDriver publication, AI `/ws` activation and commits are out of scope.
Official SIWC documentation requires dynamic client registration, PKCE/state/
nonce and ID-token validation, atomic replacement of rotating credentials,
and restart/resume of the custom-provider app-server when the access token
changes. The documented SIWC preview does not support image generation; this
remains a separate capability/release gate.

This file is a temporary status log for the Klaf server implementation.
It should be removed or replaced by permanent documentation after the full implementation is finished.

## Current Phase

Owner-only AI access grant (2026-10-01): user approved a local server CLI to
list approved Klaf accounts, grant/revoke by immutable account ID, and persist
eligibility separately from ordinary account approval. This registry/CLI gate
was implemented via TDD. The provider auth feasibility gate is unchanged; AI `/ws`
must remain unavailable until supported auth and isolation are verified.
TDD result: Room registry v10 adds an account-ID-keyed provider-owner grant;
new storage, CLI and v9→v10 migration tests are GREEN. Blocking an account
removes its grant in the same transaction, and restoring it does not regrant
AI. The authenticated `/ws` guard now returns `AI_ACCESS_DENIED` without a
grant and retains `AI_UNAVAILABLE` with a grant; its integration test was RED
then GREEN. The CLI is local-only and is not a network route. Full
`:klaf-server:test` passed after the initial implementation (the focused
grant suite passed again after a follow-up CLI path guard and immediate-revoke
assertion). No real server data or provider secrets were touched. Production
AI remains disabled. A manual CLI run against a disposable registry and a
cross-process concurrent-write check remain unverified.

Owner-only provider clarification (2026-10-01): user confirmed the initial
multiple Klaf accounts all represent the user on their own devices, not other
people. Requirements now call for owner-only AI access, separate AI contexts
per account/device/session,
and a replaceable provider association for future per-person sign-in. The
earlier multi-person account-sharing concern does not apply to this scope.
The explicit, default-deny operator link was subsequently approved; account
approval alone cannot prove provider ownership.
Technical/provider gates remain: official Codex protocol source labels
`account/login/start.chatgptAuthTokens` internal-use-only; the documented
Sign in with ChatGPT plan-usage flow is a candidate for personal local apps,
but client registration and the Cloudflare-accessible deployment need checking.
Do not publish/wire the synthetic AgentDriver broker or enable AI `/ws` yet.
No production AI wiring, live login or commit was performed in this update.

Brokered `CODEX_HOME` isolation slice (2026-10-01): the previously RED
same-factory tests now pass. AgentDriver creates a fresh provider home per
brokered context, passes it to native/bridged launch paths, and uses a distinct
WSL session home; the app-server-reported `codexHome` must match that exact
path. Legacy no-broker launch behavior is unchanged. The full focused
`CodexAppServerSessionFactoryTest` and `CodexExternalAuthProtocolTest` run is
GREEN, including the two corrected pre-existing test fixtures. No real tokens
were used. This proves distinct paths in synthetic tests, not that Codex never
writes an access token to a context home or that a model tool cannot read it.
Keep the AI `/ws` gate disabled pending a supported renewable token source,
provider-policy eligibility, real Windows/Ubuntu sandbox checks, token-storage
inspection, SDK integration into Klaf, and end-to-end tests. No publication or
commit has been made.
An additional startup-error redaction test was RED: a broker exception's
message propagated through the startup exception cause chain. The factory
now removes that untrusted cause and returns a generic startup failure; the
full focused factory/protocol suite is GREEN again. This does not prove that
provider-side diagnostics or disk state never contain tokens.

Trusted-broker TDD slice (2026-10-01): added a synthetic AgentDriver
JSON-RPC test for `account/chatgptAuthTokens/refresh` with no broker. It was
RED by timeout, then GREEN after the connection began returning an explicit
method-not-found error instead of silently queuing a request that no turn
consumer can answer. Added two more synthetic tests: a matching configured
broker returns only an access token/account ID, while a request for a different
account is rejected before broker renewal. Both were RED against the default
handler, then GREEN after bounded asynchronous handling was added. Focused
`:sdk:server:jvmTest` passed all three tests. This is only a protocol slice:
no canonical token source, Klaf wiring or live provider verification exists yet.
AI `/ws` remains disabled and no credentials were used.
Next initial-login TDD slice: a fake app-server test now requires the SDK to
send `account/login/start` with synthetic external ChatGPT access credentials
after initialize and before `thread/start`. After a compilation-only setup
failure, the test was RED on its login assertion and then GREEN with the
factory's optional broker login step. The brokered response type and returned
account ID are checked; access-token values are redacted from the token
holder's `toString`. The focused initial-login plus refresh tests passed.
This is not a deployable broker: the credential source is test-only.
An expanded 37-test factory run had four failures. Two were existing test
fixtures passing `advertisedCapabilities=emptySet()` while expecting text/image
success; those fixtures were corrected to use the factory's actual advertised
capabilities and both focused tests now pass. The history-home test has since
turned GREEN. A cancellation/startup failure did not repeat in the focused
rerun or the subsequent full focused factory suite. No production capability
gate was relaxed.

Single-provider decision and pre-implementation review (2026-10-01): the user
chose one server-operator Codex/ChatGPT account for all initially approved
Klaf users (including family), with no per-client ChatGPT login. Requirements
and plan now separate shared provider capacity from private Klaf AI-context
history and defer multi-provider OAuth. Read-only review confirmed AgentDriver
still launches every Codex context with the same `CODEX_HOME`, while the
same-factory history-home TDD test remains RED; Klaf AI `/ws` still returns
`AI_UNAVAILABLE`. OpenAI Docs now document additional token-input options, but
no supported renewable source has been validated for the current private
server/deployment. The published app-server auth guidance excludes commercial
or hosted services, so usage eligibility is a release gate, not an assumed
permission. No broker implementation, test run, provider login, secret access,
SDK publication, server deployment or commit occurred in this review.
Implementation preparation is complete; begin with synthetic TDD only after
the user's requested go-ahead, and keep production AI disabled until the
credential, policy and platform gates pass.

Trusted auth-broker scope approved (2026-10-01). Official OpenAI Docs confirm
that Codex app-server has experimental `chatgptAuthTokens` login and a
host-handled refresh request, but that mode expects the host to own the token
lifecycle. Normal `codex login` caches credentials in `auth.json` or keyring;
its documented `account/read` method does not export an access token. Newly
published Sign in with ChatGPT plan-usage docs describe a supported OAuth
token source for an app, but registration/access must be verified before using
it, and it is user-authorized plan usage rather than a proven way to share one
server-owner subscription across independent Klaf users. No production broker
was enabled, no real credentials were read/copied, and AI `/ws` remains
unavailable. Next safe work: synthetic broker protocol tests plus a supported
canonical token-source decision; do not claim full AI isolation yet.

Controlled ChatGPT keyring probe (2026-10-01): user approved an interactive
test in a fresh temporary WSL environment. Started Codex CLI `0.159.3` with
two new `CODEX_HOME` directories, separate temporary XDG home/data/runtime,
an isolated D-Bus/GNOME Keyring, and `cli_auth_credentials_store="keyring"`.
Synthetic `secret-tool` store/lookup succeeded in that keyring. Device-code
login was unavailable because that account has device-code sign-in disabled;
the waiting command was cancelled and its temporary root was removed.
Ordinary browser ChatGPT login then succeeded in another isolated root.
`codex login status` passed for `profile-a` and failed for `profile-b` in
the same OS keyring; neither home contained `auth.json`. Therefore keyring
storage alone does not let separate homes share the one managed ChatGPT login.
The second temporary root was also removed after the test. Existing SDK
profile/login and production keyrings were not used. The next feasibility
step is a trusted auth broker or another supported shared-login mechanism;
do not replace this result with repeated manual per-context sign-ins.

History-isolation decision (2026-10-01): user confirmed that persisted Codex
history must be separated in addition to live threads and working directories,
and explicitly excluded a separately billed OpenAI API key. Official Codex
documentation says `CODEX_HOME` contains auth and session state; moving only
`CODEX_SQLITE_HOME` is insufficient because transcript/history files remain
under `CODEX_HOME`. OS-keyring login is a candidate for distinct homes, but
cross-home credential sharing and AI-tool denial are not documented and need
a controlled synthetic test. A fake access-token CLI probe rejected the
synthetic token before storage, so it proved no keyring behavior. Do not change
production `CODEX_HOME` or enable AI until ChatGPT login and renewal work
within the separate-history boundary. No real provider credentials were read.
The focused same-factory history-home test remains RED after test cleanup was
hardened (one expected assertion failure). A later live auth/renewal smoke test
will require an interactive ChatGPT login into a separate disposable provider
profile; it must not reuse or alter the existing SDK-owned login.

AI isolation TDD restart (2026-10-01): added
`CodexAppServerSessionFactoryTest.sessionsFromOneFactoryDoNotShareProviderHistoryHome`
in the sibling AgentDriver checkout. It opens two Codex sessions through one
factory and requires different, non-canonical `CODEX_HOME` values. The focused
`:sdk:server:jvmTest` run is RED (one test, one assertion failure), confirming
that both currently receive the same provider profile. Existing Codex
preflight checks for a single `auth.json` in that shared profile, and the
SDK does not implement `account/login/start` or token renewal. Simply changing
the home path would strand each process without authentication, so no
production profile change or AI enablement was made. ChatGPT authentication
is confirmed, but the keyring or trusted broker mechanism for separately
isolated profiles is unproven; do not claim this test GREEN or publish the SDK.
Synthetic test data only; no live login touched.

Isolation decision reconfirmed (2026-10-01): retain the already agreed strict
boundary: each account/device/login-session/feature context gets separate AI
history and runtime artifacts, with no readable shared provider profile or
copied independent OAuth refresh credentials. The shared-profile fallback is
not authorized. The remaining problem is implementation feasibility, not a
pending user preference: the current AgentDriver Codex launch uses one
`CODEX_HOME` for all sessions. Next TDD slice must first prove per-context
runtime/history ownership and a trusted authentication/renewal path with
synthetic credentials; keep AI `/ws` unavailable until the combined boundary
passes end-to-end checks. No live login or user data was changed.

WSL feasibility follow-up (2026-10-01): with user approval, installed Codex
CLI `0.159.3` and `0.161.0-alpha.9` into separate user-local test prefixes;
the system `/usr/local/bin/codex` remains `0.146.0` and unchanged. Both new
versions' generated experimental app-server schemas still lack restricted
`readOnlyAccess`/`readableRoots`, despite the current official app-server docs
describing them. A live synthetic request to stable `0.159.3` explicitly
rejected `workspaceWrite.readOnlyAccess` and directed callers to permission
profiles. A custom profile on the installed `0.146.0` with root deny,
minimal runtime reads and workspace access allowed `pwd` but denied direct
and symlinked reads of a file outside the workspace. This is a viable
direction, not a completed boundary: a synthetic proxy credential inherited
via `HTTP_PROXY` was readable by a sandboxed command. Setting
`shell_environment_policy.inherit="none"` hid it from that child command,
and a nested `bwrap --unshare-all` plus Codex sandbox allowed a workspace
command while denying a foreign file. In that nested synthetic probe, the
proxy credential was absent from the child environment and not readable from
`/proc/1/environ`. This validates a narrow local command path, not a full
bridged-provider/network/auth canary. Provider auth/renewal and account-owned
history remain unresolved: the SDK currently points every session at one
shared `CODEX_HOME`, and copying independent refresh credentials is explicitly
rejected by the plan. The AI credential/profile isolation gate remains unmet;
`/ws` is not enabled. No real provider profile or credentials were used.

Because passwordless `sudo` is unavailable, extracted Ubuntu libsecret,
Secret Service, and OpenJDK packages into user-local test prefixes without
installing system packages. An isolated `dbus-run-session` plus ephemeral
GNOME Keyring and synthetic-only keyring data successfully stored and fetched
a synthetic value with `secret-tool`. The first Klaf JNA integration test was
RED: JNA searched for unversioned `libsecret-1.so`, which is absent from the
Ubuntu runtime package. Loading the versioned `libsecret-1.so.0` and
`libglib-2.0.so.0` fixed it; the same focused JUnit test now passes on Linux
with a fresh synthetic keyring and re-instantiated protector (1/1). The test
is gated off on ordinary Windows runs. No production keyring or backup was
touched. The subsequent full Windows `:klaf-server:test` run passed: 302
tests, zero failures, two intentional skips (including the Linux-only test).
Native Ubuntu deployment and failure-mode testing still remain.

Lost signing-key recovery TDD slice (2026-10-01): `ServerSigningKeyRecoveryTest`
was RED before the recovery API existed and is now GREEN. A missing protected
key still blocks ordinary startup. The only operator entry point is an explicit
offline `--recover-lost-signing-key` server launch; it refuses a present key or
an installation without existing registry metadata. A replacement private
bundle is durably staged first, then one Room transaction changes its public
fingerprint/epoch, revokes all sessions and deletes outstanding refresh
receipts and password-reset tokens. Accounts and content databases are not
deleted. After commit, the staged key becomes active; an interrupted
commit-before-move is reconciled on ordinary restart only when its fingerprint
matches the committed registry metadata. Focused tests verify explicit
recovery, revoked old access, account continuity and interrupted recovery.
This was tested with temporary synthetic databases only; no live key, account
or user backup was touched. Full `:klaf-server:test` regression passed:
300 tests, zero failures, one intentional skip. `git diff --check` passed;
Git emitted only existing CRLF conversion warnings. An additional focused
test confirms that a corrupt staged replacement leaves the epoch unchanged
and listeners blocked (4/4 recovery tests pass); the preceding full suite
was 300/300, before this test-only addition. Manual operator UX and
cross-process/power-loss crash validation remain to be done.

The first read-only WSL probe found Ubuntu and a session D-Bus address, but no
system `secret-tool` or libsecret shared library. Subsequent user-approved
user-local test installations and synthetic validation are recorded above;
no system package or real keyring was changed. Physical-device acceptance
remains pending.

AI SDK isolation follow-up (2026-10-01): the sibling `AgentDriver` checkout
has an uncommitted, narrowly scoped network-capability fix. A new
`NetworkBridgeCapabilityDropTest` was RED then GREEN. The bridged launcher
still uses `CAP_NET_ADMIN`/`CAP_NET_RAW` to install firewall rules, but now
adds setup-only `CAP_SETPCAP` and executes the provider through `setpriv`
with an empty capability bounding/inheritable/ambient set and
`no-new-privs`. Preflight requires `/usr/bin/setpriv`. An isolated WSL
`bwrap --unshare-all` probe showed `CapEff=0`, `CapBnd=0`, `NoNewPrivs=1`
in the final process; an `iptables` policy change after the drop failed with
permission denied. The new SDK focused test passed. This is not yet a full
bridged-provider canary test, is not published to Maven Local, and does not
solve shared provider-profile credentials or per-account AI contexts.
Klaf `/ws` remains intentionally `AI_UNAVAILABLE`. No SDK commit was made.
The current SDK still sends the named `:workspace` permission profile on
`thread/start` and `turn/start`; source inspection shows no explicit restricted
read roots. Official app-server documentation describes
`sandboxPolicy.workspaceWrite.readOnlyAccess` as restricted roots, but the
installed provider/version and interaction with named permission profiles
need a synthetic live contract test before changing this protocol. Do not
infer that the profile-mounted `auth.json` is tool-inaccessible merely from
the workspace path. The provider-profile and account-scoped context gaps
remain release blockers.
Installed WSL `codex-cli 0.146.0` was checked with its own generated
experimental app-server JSON schema: `TurnStartParams.sandboxPolicy` accepts
`workspaceWrite` with `writableRoots` but does **not** define `readOnlyAccess`
or restricted readable roots. Its `permissions` field cannot be combined with
`sandboxPolicy`. Subsequent live synthetic checks above identified custom
permission profiles as the supported restricted-read path; do not send the
unsupported shape or claim complete credential isolation.

AI operation ownership TDD slice: a new registry test was RED for missing
authentication-session ownership and is now GREEN. Operations may carry an
`ownerSessionId`; another session cannot attach to the same request ID, and
`cancelAuthSession` removes/cancels only that session's jobs while preserving
unaffected jobs. A second RED/GREEN assertion now verifies that a revoked
session cannot immediately replay/start the same request; bounded in-memory
revocation tombstones prevent it. The full registry test class passed. This
is a foundation, not yet wired to protected `/ws` or
logout/revoke/reset routes; `/ws` remains disabled and no AI calls were made.
The subsequent full `:klaf-server:test` regression passed: 297 tests, zero
failures, one intentional skip (3m43s). The AgentDriver targeted network/
proxy/Windows-sandbox set passed 9/9. Both repos passed `git diff --check`.

Password reset TDD started (2026-10-01): two new storage tests were RED for
missing reset-token operations and are now GREEN. Room registry schema 9 adds
one digest-only, expiring reset token per account; replacement invalidates the
prior token. A successful atomic consume changes the account password hash,
deletes the token and revokes all account sessions. Expired and replayed tokens
cannot change the hash. `9.json` was generated. At this first checkpoint it was only the storage core:
operator issuance UI, public proof-gated reset route, client form, negative
rate-limit/restart/session tests and AI cancellation are not yet implemented.
Do not use this partial reset feature or claim full auth completion.

Password reset server flow follow-up: a live HTTP test was RED at compile for
missing operator issuance, then GREEN after adding local-only operator token
generation and `POST /api/v1/auth/password/reset`. The endpoint requires a
valid 32-byte reset token, fresh DPoP proof and password policy, checks token
validity before Argon2, performs atomic consume and closes revoked event
subscriptions. The test confirmed old access and old password fail, new
password succeeds, and token replay fails. At that checkpoint, the operator UI
and client reset form were still absent; the next slice supersedes this.

Password reset UI/client follow-up: local operator Manage panel now confirms
token issuance and shows a selectable 30-minute secret until explicitly hidden;
the raw token is not logged or written to Room. Sign-in screen has a reset
dialog (token/new password/confirmation), with password-policy validation and
a dedicated invalid/expired-token message. `SecureAccountRestClient` sends
the DPoP-bound request; successful `SecureAccountFlow` reset drops stale local
credentials and requires regular sign-in, without switching offline profile.
The client transport and ViewModel focused tests were RED for missing methods,
then GREEN. Operator Compose UI compiled but has not been visually exercised.
A live restart/concurrent-consume test passed: exactly one reset succeeds.
The full post-reset regression passed on Windows: 296 server tests (0 failures,
1 intentional skip), 186 Desktop data tests and 80 Desktop presentation tests
(0 failures), 4m20s. `:Android:compileDebugSources` also passed in 1m8s.
`git diff --check` found no whitespace errors. Android physical-device UX,
operator-window visual inspection and Ubuntu secure storage remain unverified.

Source/global password-work admission slice (2026-10-01): added an in-memory,
mutex-atomic 60-second pre-Argon2 limiter with configurable server defaults
(120 attempts per unproven source, 1,000 unproven globally, 1,200 total,
60 per proven device key/source). An unproven flood cannot consume the final
200 global slots reserved for proven keys. It is now called after DPoP proof
verification and before hashing/verifying passwords on registration, approved
registration completion, sign-in and approved device completion. Exceeded
budgets return 429 `AUTH_RATE_LIMITED` with bounded `Retry-After`; the client
maps this to its existing throttled/countdown UI. The live sign-in TDD case
was RED (401 instead of 429) then GREEN; limiter unit tests 2/2 GREEN.
Registration-route and client mapping tests were added afterward; both
focused tests subsequently passed.
The full 2026-10-01 `:klaf-server:test :data:desktopTest` regression passed:
292 server tests (0 failures, 1 intentional skip) and 185 Desktop data tests
(0 failures), 3m27s. This is per-process admission, not
distributed DDoS defense; the true transport peer behind a same-host tunnel
may be shared. No live external deployment or hardware test was done.

Authentication TDD and implementation authorized after design review. Proceed
through the complete flow without asking for routine phase approvals; keep this
status current. Current checkpoint: focused auth, signed sync/Room/source,
protocol, history, event and image tests pass. The latest full server run
(2026-10-01) passed: 288 tests, 0 failures and 1 intentional skip (3m15s).
Legacy passwordless and public device-management expectations were migrated
or removed after equivalent secure-flow coverage was added; production
authorization remains closed to those old routes.

All nine `LiveKlafServerProtocolTest` cases have been
rewritten around the current password/approval/key-bound protocol and the
closed legacy/AI entry points; the focused class passed 9/9 and the full suite
later passed.

The seven-case `LiveServerAccountSessionTest` class was removed because it
targeted the disabled passwordless client and all seven cases failed only at
its first old endpoint call. Its relevant product scenarios are now exercised
against the production secure Desktop factory: 4/4 focused tests passed,
including guest transfer/recovery, reviewed deck+card upload, distinct local
accounts and guest profile, new-device approval, and lazy device identity.
The deleted tracked test file is recoverable from Git; no production legacy
code was removed in this slice. Full-suite recheck passed.

`LiveDeviceManagementTest` was rewritten to test the current security rule:
public legacy PATCH/DELETE device routes return 401, while an operator revoke
ends only the targeted device's access and leaves its name, synchronized deck
and another account intact. Its focused test passed. The earlier public
rename/delete flow is no longer an authorized API. All three obsolete failing
test classes have now been migrated or replaced; the full suite passed.
The operator Compose panel compiled but has not been manually exercised. SDK
real-launch isolation, Ubuntu secure storage and platform hardware verification
remain explicit gates. No commit permission was given; original backups and
live accounts must remain unchanged.

Current TDD slice: operator account/device block, revoke and restore now have
focused live tests and server-window controls (window not visually exercised).
The combined authentication/event/Desktop regression set passed 31/31 on
Windows. Persistent sign-in throttling, expiry/backoff, unknown-device pause,
and both approval-completion throttle routes are focused-test GREEN. The
source/global admission and obsolete passwordless-test migration slices are
now implemented above. Next add event-publish-failure injection and address
reset/recovery, session-scoped AI cancellation and platform isolation/secure-
storage gates.

Password throttle first slice (2026-10-01): the live RED test observed 200
on the sixth request after five wrong passwords. Room registry schema 8 now
stores account/source/device-key attempt lanes; sign-in returns 429 with
`AUTH_THROTTLED` and bounded `Retry-After` during cooldown. The focused live
test passed, including persistence across a fresh server instance and an
unaffected approved second device. A separate RED/GREEN secure-client test
passes the retry delay into `AccountOperationException`, and a view-model test
passes it into a user-visible localized error. These focused tests passed.
That first slice did not yet implement source/global admission limits or
throttle coverage on enrollment completion; later slices above supersede this
historical limitation. Behind a same-machine Cloudflare Tunnel,
the transport source host is shared, so the proven-device key is essential
for isolating legitimate devices.

Completion-route throttle slice (2026-10-01): separate live tests first failed
because repeated wrong passwords did not limit approved registration or new
device completion. Both routes now use persisted account/source/approved-key
lanes before Argon2; five wrong passwords cause the sixth request to return
429 `AUTH_THROTTLED`, and successful verification clears that lane. Both
focused tests passed. A combined/full regression run remains due.

Clock-skew review: a focused RED test showed a backward wall-clock jump could
leave a persisted cooldown active much longer than intended. Storage now
ignores that lane's old cooldown if the current time precedes its window start;
the next failed attempt starts a new window. The focused test passed. This
trades away the old short cooldown after a privileged system-clock rollback
to avoid an unbounded denial of service.

Throttle follow-up (2026-10-01): time-injected Room tests passed for first
30-second cooldown, expiry, doubled 60-second cooldown, and successful-login
counter reset. A second test was RED because 20 unproven failures across four
source hosts did not pause a new source; an account-wide unproven lane now
pauses unrecognized devices for 60 seconds while leaving a proven device-key
lane eligible. Both focused tests passed. No real clock waiting was used.

Next legacy-contract migration: replace obsolete success expectations in the
old account REST client tests with live assertions that every passwordless
entry point stays closed, including retries. The supported password/approval
flow remains covered by `AuthenticationBoundaryTest` and secure-client tests.

Legacy account REST contract migration: all four `LiveAccountRestClientTest`
cases now assert that the old passwordless client receives
`401 AUTHENTICATION_REQUIRED` for sign-up, sign-in, device registration and
repeated sign-up ID, with no account/device side effect. The complete focused
class passed. Supported secure enrollment/sign-in behavior remains tested in
`AuthenticationBoundaryTest` and `SecureAccountRestClientTest`; this change
does not claim those old methods are a usable sign-in API.

Next integration slice: exercise the production Desktop secure-session factory
against the real loopback server: guest data remains visible while account
approval is pending, then moves to the approved account and uploads through a
device-bound signed sync client. This is a Windows/Desktop check, not Ubuntu
or Android secure-storage verification.

Live secure Desktop account integration (2026-09-30): new Windows-only
`LiveSecureDesktopAccountSessionTest` uses the production Desktop session
factory, DPAPI-protected key/session stores, real operator approval and a
DPoP-signed sync client against the loopback server. The focused test passed:
guest deck stays visible while approval is pending, moves to the selected
account after password re-entry, and uploads in the first manual sync.
On non-Windows this test is intentionally skipped, not proof of Ubuntu support.
The same live scenario was extended and rerun successfully: offline sign-out
returns to an empty guest profile after its original deck was transferred;
new guest content remains there across sign-in to the existing account and
is not silently transferred.
Second live Desktop case passed after correcting an invalid test assertion:
it deliberately interrupts guest transfer after the account copy, then
recreates the production secure session from the same protected local files.
Password re-entry/completion resumes the exact deck/card transfer with two
initial operations, and sign-out shows an empty guest database. The first RED
run tried to read an account outbox while guest was selected; that was a test
setup error, not a product failure. No production code changed for this case.
Third live Desktop case passed: a separate installation with its own protected
device key receives pending approval when signing into an existing account,
keeps its guest deck visible until approval, then selects the existing account
without transferring the guest deck. Sign-out restores that guest deck.

Next security TDD slice: add a local operator-device revocation test. Revoking
one approved device must atomically mark its authorization and login sessions
invalid while another approved device on the same account remains usable;
the old public device-management REST route stays closed. Operator UI and
active event/AI cancellation need separate follow-up tests.

Operator revocation storage slice: the new live test first failed to compile
because `revokeApprovedDevice` was absent; after adding a Room DAO update and
one registry transaction that revokes the device and its sessions, the focused
test passed. A second approved device on the same account remains able to
bootstrap; the revoked device's existing access token immediately receives
401. This is a storage/security primitive only, not a finished operator UI or
active socket/job cancellation flow. Device re-enrollment after revocation
also needs a deliberate policy/test before exposing this action to operators.

Operator revocation event slice (2026-09-30): added a live two-device test and
`KlafServer.revokeOperatorDevice` to close affected sync-event subscriptions
after the registry transaction while leaving the other device authorized.
The first run exposed a buffered pre-revocation `State` frame, so the test now
asserts eventual socket closure rather than incorrectly requiring the receive
queue to be empty at the exact revocation instant. The focused test passed.
There is still no operator UI for this action, no revoked-device restoration
flow, and no cancellation of an already running AI job.

Operator account-block slice (2026-09-30): TDD live test first failed because
block/restore operations did not exist. Room registry now atomically blocks an
approved account and revokes all its authentication sessions; restoring the
account does not restore those sessions. The focused test passed: both devices
get 401 during and after block, and a fresh password sign-in authorizes only
the signing-in device. A second RED/GREEN live test verified the server's
operator block closes the blocked account's sync-event socket while another
account's socket and REST access remain usable. Operator panel controls and
AI-job cancellation are not implemented by this slice.

Operator account panel slice: a RED/GREEN live test established an approved
account inventory that exposes only email and blocked state, never password
hashes or key material. The server Compose window now lists accounts with
confirmation for Block/Restore. Blocking calls the tested server operation;
restore requires a new client sign-in. `:klaf-server:compileKotlin` passed.
The panel has not been manually exercised, and device revoke/reset remain
absent from its controls.

Operator device restore/panel slice: focused RED/GREEN tests now prove that
restoring a revoked approved device keeps its old token invalid until the same
device key performs a fresh password sign-in, and that a minimal operator
device inventory reflects revoke/restore without returning stored public keys.
The server window's collapsed Manage section now exposes confirmed account
Block/Restore and device Revoke/Restore actions. `:klaf-server:compileKotlin`
passed; the visual panel has not been manually exercised. Restoring the device
deliberately trusts its prior key; if that key is suspected compromised, use
a new installation/device identity instead. Running AI jobs still need
session-scoped cancellation on these actions.

Combined regression checkpoint: `:klaf-server:test` filtered to
`AuthenticationBoundaryTest`, `AuthenticatedSyncEventConnectorTest` and
`LiveSecureDesktopAccountSessionTest` passed: 23 + 5 + 3 tests, zero failures
or skips on Windows (1m49s). This is not a full-suite or Ubuntu/Android pass.

Approved-device completion replay regression (2026-09-30): extended the live
authentication-boundary test to repeat an approved completion with the same
request ID and correct device proof/password, simulating loss of the first
response. The focused test passed: a second token pair is issued, the first
session is revoked, and the recovered session remains active. This is safe
for a client that never stored the first response; it is not an exact-response
receipt and does not cover a failure in event publication after activation.

Authenticated image integration checkpoint: `LiveMnemonicImageDeliveryTest`
now creates a real approved account, seeds its temporary Room content database,
uses production DPoP-signed sync/image clients, and approves separate test
devices before cross-device download. All six tests passed, covering immutable
upload, lost upload response, local image caching, safe identifiers, image
size/format checks, and account/device isolation. Raw HTTP negative assertions
also pass with valid auth so they test image validation rather than 401.

Latest client checkpoint (2026-09-30): `SecureAccountFlow` now persists approved
tokens before reporting sign-in or approval completion, never stores a pending
login, and removes local credentials on offline logout. Its targeted desktop
tests passed (4 cases, 20s). A cancellation test was RED (it started an
unnecessary revocation attempt after a cancelled store write), then GREEN
after cancellation was propagated immediately. `DesktopProtectedSessionStore`
now zeroes its raw serialized bytes even if temporary-file creation fails.
The flow is **not wired** to the production `ServerAccountSession`, UI, sync or
AI requests; do not use this checkpoint as evidence of end-to-end login.

Latest server hardening: `AccountPasswordHasherTest` failed for missing unknown-
account verification, then passed after adding dummy Argon2id work. The sign-in
route now takes the same hashing path for unknown/unapproved/blocked accounts
as for approved ones. `AccountPasswordHasherTest` plus all focused
`AuthenticationBoundaryTest` cases passed (42s). This does not implement
persistent throttling, bounded waiting, source admission or full enumeration
resistance; do not equate dummy verification with rate limiting.

Latest local-account integration: new `SecureServerAccountSessionTest` was RED
on absent pending result/session types, then both Room-backed desktop tests
passed (19s). `SecureServerAccountSession` submits signup without moving guest
data or selecting a profile, persists only pending metadata (no password),
requires password re-entry for completion, transfers guest data only on account
completion, and keeps sign-in guest data separate. Offline logout removes
credentials and reselects guest. **Production DI/UI still use the previous
session**, so this is not yet an end-to-end enabled client. The older
`ServerAccountSession` password methods remain fail-closed.

Follow-up account UI/DI checkpoint (2026-09-30): the ViewModel test for
approval completion first failed to compile for absent status/completion
actions. The UI now treats sign-up/new-device enrollment as pending, clears
password fields, offers manual status check and explicit completion after
approval with password re-entry; it does not report success on submission.
The authentication screen shows a non-modal pending message and actions.
The production Android/Desktop `IAccountSession` DI bindings now construct
the secure account session lazily, loading OS-protected keys only at first
account request. Targeted Room-backed data and ViewModel tests passed (49s);
`:di:compileKotlinDesktop :di:compileDebugKotlinAndroid` passed (1m45s).
This is **not** full auth integration: the drawer/account section does not yet
show pending status, all sync/image/AI connections still lack DPoP access
authorization, and Linux desktop credential protection fails closed pending
libsecret implementation. Do not use live accounts or deploy.

Follow-up drawer checkpoint: the account drawer now observes the pending
enrollment flow and displays its email/awaiting-approval status without
blocking guest study. The status survives screen recreation through the
existing pending-attempt store. Targeted Room and ViewModel tests plus desktop
DI compilation passed (1m05s); `git diff --check` reported no whitespace
errors (only Git CRLF conversion notices). The drawer message directs the
user back to the sign-in/sign-up screen for manual status check/completion;
end-to-end authenticated content requests remain unimplemented.

Protected content-request checkpoint (2026-09-30): `SecureRequestAuthorizerTest`
was RED for missing signer/error type, then GREEN after adding strict same-
origin, account-scoped token retrieval and fresh access-bound DPoP proofs.
The first JUnit run had a test-method return-type setup error; adding explicit
`Unit` fixed that test harness issue. Sync bootstrap MockEngine test was RED
for absent authenticated constructor, then GREEN after applying Authorization
and DPoP to bootstrap, batch sync, history and revision confirmation, with one
fresh-proof nonce retry. The same TDD sequence added protected image upload/
download and secure auth failure messages. Focused data and presentation
tests passed (49s); full `:data:desktopTest` and both platform DI compiles
passed at the preceding checkpoint (1m21s) before the latest image changes.
At that checkpoint protected requests did not renew expired access tokens;
the later refresh checkpoint below supersedes that limitation. AI WebSocket
authentication remains unfinished. No production end-to-end verification.

Refresh recovery checkpoint (2026-09-30): added a RED client test for a lost
refresh response, then implemented expiry-aware access-token renewal. Before
the network call, Android/Desktop persist a refresh operation ID inside the
OS-protected session bundle. A later request retries the same operation ID and
old refresh token, allowing the server's rotation receipt to recover the new
pair. Separate per-profile OS file locks plus process mutexes serialize
read/journal/refresh/write across client processes. Server rejection with
`SIGN_IN_REQUIRED` clears the unusable local credentials; connection failures
leave the journal for retry. Tests for lost response, six concurrent requests,
rejection, signed sync and signed image requests passed; Android/Desktop DI
compilation passed (54s). This is **not** full security completion: no live
cross-process or physical-device refresh test yet; AI WebSocket and other
network paths remain unfinished, and full legacy server fixtures are still
RED. No commits, backup edits, or deployment.

Refresh follow-up: a replacement-write failure test passed: the old encrypted
bundle retains its journal and recovers the server receipt on the next call.
Full `:data:desktopTest` and `:presentation:desktopTest` passed. Full
`:klaf-server:test` remains RED: 273 tests, 155 failures, 1 skipped (1m08s).
The failures are concentrated in the old passwordless live/contract sync
fixtures: 40 `ServerSyncContractTest`, 35 `LiveManualRoomSyncCoordinatorTest`,
22 `LiveServerSyncProtocolTest`, 21 `LiveVocabularySourceSyncTest`, and other
older live server tests. These tests must be migrated to the approved device
auth flow; do not weaken the new server boundary to make them green.

Sync-event auth checkpoint: a desktop MockEngine test first failed to compile
for missing signer/preflight support, then passed after adding an account-bound
DPoP preflight that obtains the server nonce. The WebSocket upgrade now sends
fresh Authorization and nonce-bearing DPoP headers; Android/Desktop DI passes
the same protected request signer. The preflight + upgrade are bounded by a
5-second connect timeout, and unexpected challenge responses fail closed.
Focused test and both DI compiles passed. A real authenticated WebSocket
upgrade/reconnect test is still required; do not claim this path end-to-end
verified merely from the preflight MockEngine test.

Protected-store regression: reopened desktop store reads the persisted refresh
journal; two separate store instances cannot enter the same account's refresh
critical section simultaneously. Focused desktop store test passed. This does
not replace a true independent-process test or Android hardware test.

Device-key loss hardening: a new desktop test was RED when an OS-protected
account bundle existed but its DPoP installation key was absent: the client
silently made a new identity. Both Desktop and Android key stores now refuse
to regenerate the key while any local protected account session remains.
Desktop test passed; the corresponding Android instrumented test compiled but
has **not run on physical hardware**. Desktop key serialization also zeroes
its raw bytes if temporary-file creation fails. `:di:compileDebugKotlinAndroid`
passed. This protects local continuity, not recovery from OS key loss; the
operator/user must deliberately clear old local credentials before enrolling
as a new device.

Argon2 admission checkpoint: a new bounded-gate test was RED for missing
capacity controls, then passed. The server now permits two active Argon2id
workers with a ten-request total admission cap, of which at most eight may be
unproven callers. A valid approved-device DPoP key can use the reserved
capacity; an unproven/unknown device cannot. At saturation authentication
routes return `429 AUTH_BUSY` with `Retry-After: 2`, mapped to a client message
to retry in a few seconds. Focused gate/hasher/auth-boundary tests, client
error test, presentation tests, and both platform DI compiles passed. This is
**not** the requested persistent per-account/IP password-failure throttle;
that remains unimplemented. No load benchmark or live admission test yet.

Live sync-event checkpoint: new `AuthenticatedSyncEventConnectorTest` passed
on the real loopback CIO server. It creates a pending account with a P-256
device proof, approves it through the test registry, completes login, then
uses the production client sync-event connector for the nonce preflight and
WebSocket upgrade. The approved device receives its account state. This closes
the previous MockEngine-only handshake gap on Windows, but Android hardware
and reconnection/revocation behavior still require tests.

Logout revocation checkpoint: a live authenticated event-socket test was RED
(still open two seconds after successful logout), then GREEN after binding
hub subscriptions to the verified auth-session ID. Logout revokes that DB
session first, then closes only its event subscriptions; buffered event messages
are no longer sent after revocation. A hub test verified a different auth
session/device remains subscribed. These focused tests passed. Other revocation
paths (device replacement, refresh-reuse detection, operator block) still need
the same active-connection cancellation and dedicated tests; the AI socket
remains unavailable pending isolation.

Revocation follow-up: the hub can now recheck open subscriptions against the
persisted auth session/account/device state and close only those invalidated.
This recheck runs after approved re-sign-in, approved device completion, and
refresh-reuse revocation. A new live loopback test verifies that signing in
again closes the prior session's open sync-event socket; a unit test verifies
the newly valid session/device remains subscribed. Focused hub, live socket,
auth-boundary, and bounded-password-gate tests passed. Operator block and AI
work cancellation still need dedicated integration work.

Legacy sync-test migration checkpoint: full `ServerSyncContractTest` was RED
40/40 under the new auth boundary. A test-only account client now performs
registration, explicit approval, login and per-request DPoP with nonce retry;
it does not re-enable passwordless production routes. The fixture's event
WebSocket now uses the same verified device proof. Assertions that required
passwordless signup or the currently disabled AI socket were updated to the
new security behavior. Two stored-device metadata tests use direct test
storage calls because the old public device-management route is intentionally
disabled; this is **not** evidence of a finished authenticated operator
device-management UI. After these changes all 40 sync contract tests passed
(56s). The other live/Room server test classes have not yet been migrated.

Live Room/source sync migration checkpoint (2026-09-30): the
`LiveManualRoomSyncCoordinatorTest` and `LiveVocabularySourceSyncTest`
fixtures now create approved accounts/devices and sign sync requests with
their own DPoP device keys. The event reconnect test uses an authenticated
WebSocket connector; the legacy-protocol deletion test sends its phone request
with the phone signer rather than the desktop signer. Both full classes passed:
56 tests, no failures. This verifies their Room/source behaviors through the
new auth boundary, but the remaining legacy live server suites are still RED
and should be migrated without reopening unauthenticated production routes.

Live sync-protocol migration checkpoint: `LiveKlafServer` owns a lazy test-only
approved-account client whose registry storage is closed with the fixture.
`LiveServerSyncProtocolTest` now registers/approves both devices through the
new auth flow and signs the protocol requests with the selected device key.
The complete class passed on Windows. Tests that intentionally exercise a
protocol error still send an authenticated request so the protocol behavior,
not a missing credential, determines the outcome.

Full server regression checkpoint after these migrations: `:klaf-server:test`
ran 279 tests; 37 failed and 1 was skipped (build RED). Failures are concentrated
in older live account/session, image, event/history and REST-client suites that
still use passwordless account creation or unsigned protected requests. This is
an outstanding migration/test-design task, not a reason to restore the old
passwordless routes. The focused 40 contract, 35 manual Room sync, 21 source
sync, and 22 sync-protocol tests passed separately.

REST/history client checkpoint: two more live classes now use approved test
accounts and signed production sync clients. The negative transport case
asserts `401 AUTHENTICATION_REQUIRED` for an unsigned request; a device ID
different from the signed principal now asserts `ACCOUNT_ACCESS_DENIED` rather
than the old unauthenticated `DEVICE_NOT_REGISTERED`. Both complete classes
passed. The full 279-test count above predates this change and has not yet
been rerun.

Sync-event regression checkpoint: migrated `LiveSyncEventClientTest` and
`LiveSyncEventsTest` to approved-account/DPoP handshakes and signed REST
commits. The first run exposed a real missing production broadcast: completing
approval for a new device did not update other open sync-event connections.
`AuthenticationRoutes` now publishes the current sync state after successful
device activation. Both complete event classes passed on rerun (five tests).
An unregistered/unsigned device is rejected at the WebSocket handshake with
401, not accepted then closed with the old 1008 policy code.

Device-position isolation checkpoint: `LiveDeviceSyncPositionTest` now uses
separate approved account signers for Alice and Bob even though both test
accounts use the same textual device ID. Signed sync/confirm and both event
connections passed; repeated confirmation retains its timestamp, and revision
regression/future confirmation still fail as before. The legacy redundant
passwordless `registerDevice` assertion was removed from this scenario.

Review note: successful approved-device activation and its event-state
publication currently occur inside one broad `try` in `AuthenticationRoutes`.
If a later event-state lookup/publish throws `IllegalStateException` after the
session commits, the route can misreport `DEVICE_NOT_APPROVED`; response-loss
recovery for this exact edge needs a dedicated RED test and a narrower
transaction/error boundary. Do not treat the event GREEN slice as proof of
atomic enrollment completion across response failure.

Follow-up on that review: the `DEVICE_NOT_APPROVED`/invalid-request catches
are now scoped to device registration and token activation; later event
publication cannot be mislabeled as an approval rejection. The complete
`AuthenticationBoundaryTest` and `LiveSyncEventsTest` focused run passed.
This does **not** make completion replay safe if state publication fails after
activation; the response-loss test and durable recovery design remain open.

Initial RED check: `:klaf-server:test --tests '*AuthenticationBoundaryTest'`
compiled and ran on Windows. Three tests failed on existing behavior:
passwordless sign-in returned a non-auth error rather than rejecting the missing
password, sync bootstrap looked up the account without auth, and AI `/ws` sent
Ready without auth. A fourth sync-event test initially used a nonexistent
account and was vacuous; it has been corrected to seed a registered account
before checking the unauthenticated socket. The corrected fourth test has not
yet been rerun. This is no implementation or security gate completion.

Expanded and reran the focused boundary suite: seven tests compiled and all
seven failed for expected missing behavior, including the corrected registered
sync-event socket, image route, old passwordless sign-up and new registration
password validation. These are valid RED failures, not setup/compile errors.
Next work is to introduce the server auth registry/protocol, make every boundary
fail closed, then convert existing fixtures to authenticated flows and extend
the persistence/concurrency tests before client integration.

First GREEN slice: the seven `AuthenticationBoundaryTest` cases now pass. The
server has a fail-closed interceptor that rejects unauthenticated legacy REST
and both WebSocket entry points before they touch account storage. A new auth
registration route rejects a short password. This is **not a usable login**:
valid registration currently returns `NOT_IMPLEMENTED`, and no authenticated
principal can yet pass the interceptor. Do not run/deploy this working tree as
a functional server until the remaining auth flow is implemented and the full
regression suite passes. Existing passwordless client tests will temporarily
fail by design; do not call the feature complete based on the focused suite.
Ktor 3.5.1 API required application-pipeline `context` rather than the older
route interceptor shape; the compile issue was fixed and the focused suite
passed afterward.

Second RED/GREEN slice: added a registration scenario with generated P-256
DPoP proof. It failed against the placeholder registration route, then passed
after implementing a nonce challenge, proof signature/key/URI/time/replay
checks, account password validation and Argon2id hash/salt persistence in a
new Room registry table (`2 -> 3` auto-migration). The focused eight-test
suite passes. Added candidate JVM security dependencies and explicit public
origin configuration; only the loopback test fixture sets an HTTP origin.
Production needs its configured HTTPS origin before registration will proceed.
This slice still does **not** approve an account, verify a stored password,
issue tokens, protect accepted principals, or provide secure OS key storage.
The current DPoP replay/nonce state is in memory and needs the broader
persistence/restart tests and implementation. Do not treat focused GREEN as
feature completion.

Third RED/GREEN slice: a new test failed on missing approval completion, then
passed after password re-entry verifies the stored Argon2id hash, creates the
account/device and persisted session, and issues an ES256 access/opaque refresh
pair. Another test failed because a valid session was still blocked; it now
passes with token+DPoP proof, account/device ownership checks and duplicate
proof rejection. `AuthenticationBoundaryTest` and `ServerAccountStorageTest`
passed together after the Room registry `3 -> 4` additions. Existing content
routes now check the authenticated principal against request ownership.

Critical incomplete state: the signing key is currently **in memory only**
inside `AccessTokenSigner`; this is a TDD scaffold, not approved OS-protected
storage, and sessions would not survive a new server process. Refresh rotation,
device approval beyond first signup, operator UI, client token/key persistence,
AI isolation, rate limiting and full regression remain pending. AI `/ws` is
deliberately `AI_UNAVAILABLE` even for authenticated callers until isolation is
implemented. Do not deploy this intermediate worktree.

Fourth RED/GREEN slice: extended the bound-token test through a fresh server
object. It failed when the signing key was regenerated; after adding a Room
signing-key fingerprint/epoch record (`4 -> 5` migration) and a Windows
current-user DPAPI-protected key file, the focused test passes across that
restart. A missing existing key now blocks startup rather than silently
regenerating it. This supersedes the earlier in-memory-only key note **on
Windows**. Ubuntu libsecret storage, operator-confirmed key-loss recovery,
keyring failure UI/Retry, atomic crash reconciliation and live OS verification
remain pending. A fresh-process test object is not a manual production deployment.

Fifth RED/GREEN slice: added a refresh-rotation test. Missing route was RED;
`AuthenticationBoundaryTest` is GREEN after adding persisted refresh history,
atomic Room generation replacement (`5 -> 6` migration), 120-second encrypted
same-operation receipt, and reuse revocation. The recovery assertion now
restarts a fresh server object between first renewal and retry and receives
the identical token pair. The protected server bundle now contains a separate
AES-GCM receipt key. Missing protected key test also passes. The Linux
Secret Service/JNA backend compiles on Windows but was **not run**: this WSL
Ubuntu currently has a session bus but no visible `libsecret-1` library or
`secret-tool`; primary Ubuntu desktop behavior remains unverified.

Sixth RED/GREEN slice (2026-09-29): a new test first failed because password
sign-in for an approved device and explicit enrollment for a second device did
not exist. Added `auth_device_requests` Room table (`6 -> 7` auto-migration),
password + DPoP sign-in, 202 `AWAITING_APPROVAL` for a new device, and an
approval-completion endpoint requiring a fresh device proof and password
re-entry. The focused `AuthenticationBoundaryTest` suite is GREEN after this
change. Approval in the test is a direct registry update: operator UI and
administrative authorization are **not** implemented, so this is still not a
deployable user flow. Next: negative/expiry/race tests for device requests,
then server-side revocation/logout/operator approval controls and client flow.

Seventh RED/GREEN slice: a new logout test failed because no authenticated
logout route existed. Added DPoP-protected `/api/v1/auth/logout`, immediate
Room session revocation and rejection of the old access/refresh credentials;
the complete focused `AuthenticationBoundaryTest` suite passes. This only
revokes an HTTP session: cancellation/closure of any active WebSocket or AI
job and the client's offline-local logout flow remain pending. No deployment
or commit is authorized yet.

Full-suite checkpoint after seventh slice: `:klaf-server:test` ran 266 tests,
155 failed, one skipped. Most failures occur because old fixtures/clients call
the passwordless account and sync APIs now correctly denied by the new auth
boundary; the AI WebSocket is deliberately unavailable until isolation exists.
This is a known migration/red state, **not** a passing regression suite. Keep
the boundary fail-closed. Migrate production clients and shared test setup to
the new protocol, then diagnose the remaining failures individually.

Eighth RED/GREEN edge case: expiring an unapproved second-device request and
retrying sign-in used to return the same unusable request ID indefinitely.
Extended the live auth test to expire it; it failed, then passed after storage
removes the expired request for that account/device before creating a fresh
pending request. Focused auth suite is GREEN. Registration-request expiry and
concurrent enrollment still need explicit coverage.

Ninth RED/GREEN safety check: a valid token/proof could still call the old
`PATCH /api/v1/accounts/devices` route, changing registered-device metadata
without changing auth device/session state. The live test exposed this; the
legacy device-management path is now closed even to authenticated callers.
The focused auth suite passes. Replacement authorized device management is
still pending; old client tests that expect the legacy route remain RED.

Tenth RED/GREEN operator edge case: an expired account request could previously
be approved and also blocked a new request for the same email. The live test
failed, then passed after time-bound approval/list queries and removal of an
expired pending request before replacement. The same expiry rule now applies
to device approval/list. Focused auth suite GREEN; operator UI still absent.

Operator approval work in progress: added a rejection test (first RED attempt
was interrupted by a slow Gradle daemon before useful compiler output), Room
rejection methods for pending account/device requests, a server-side local
approval facade, and a theme-aware Compose Approve/Reject/Confirm panel with
device key thumbprints. Account and device rejection plus wrong-password/key
cases now have tests. Main and test sources compiled. The first focused test
execution was INCONCLUSIVE: the `--no-daemon --max-workers=1` build took 40
minutes and 9/16 tests failed at the shared HTTP helper's 3-second startup/
request timeout, not at their auth assertions. Raised the test-only timeout to
30 seconds. Regular-daemon reruns passed: the single account-rejection test,
then all 16 `AuthenticationBoundaryTest` scenarios (`BUILD SUCCESSFUL`, 6m50s,
2026-09-29). Operator UI compiles but was not manually exercised. Storage
regression `:klaf-server:test --tests '*ServerAccountStorageTest'` also passed
(`BUILD SUCCESSFUL`, 3m42s). `git diff --check` is clean apart from Windows
line-ending notices. Full client integration remains pending.

Client TDD has started: `AccountAuthenticationViewModelTest` now expects a
password on sign-in, rejects blank/short/whitespace/mismatched sign-up
passwords, and expects the password field to be enabled. RED confirmed by
`:presentation:desktopTest --tests '*AccountAuthenticationViewModelTest'`:
6 tests ran, 2 failed exactly on the old passwordless behavior (8m34s). Test
source compiled; this is not a build-setup failure.
Added a shared `AccountPasswordPolicyTest`; its first `:domain:desktopTest`
failed at the expected unresolved `AccountPasswordPolicy` reference (test-first
compile RED, 3m53s). Added the common policy implementation with exact input,
15–128 Unicode code points, whitespace/NUL/unpaired-surrogate rejection and a
UTF-8 size cap. The targeted `:domain:desktopTest` result XML confirms one
test, zero failures/errors (`2026-09-29T19:03:34Z`); the Gradle session itself
was lost during context rollover, so the XML is the available pass evidence.
UI and secure transport still do not use it yet.
Next adapt the account session interface/ViewModel and client credentials,
then run the targeted test again. Do not confuse this with the server auth
suite, which passed before these presentation-only test edits.

Client UI GREEN slice (2026-09-30): `AccountAuthenticationViewModel` enables
password fields, validates the shared exact-input policy and sign-up
confirmation, and sends the password through new `IAccountSession` methods.
`AccountAuthenticationViewModelTest` asserts that the password reaches the
session. Targeted `:presentation:desktopTest --tests
'*AccountAuthenticationViewModelTest'` passed (6 tests, 2m32s). Production
`ServerAccountSession` deliberately rejects these new methods until device
proof, secure credential storage and token transport are implemented. This is
fail-closed intermediate code, not working registration/login; the old
passwordless server endpoints also remain blocked. Approval UI semantics
still need redesign: new signup/device request is pending, not immediate
success. Existing test fakes still model the old immediate registration path.

Enrollment-status TDD slice (2026-09-30): added a test proving that a pending
request can be polled only with its original P-256 device key, and that
operator approval changes the reported state without creating a login. The
test ran RED against the missing endpoint (one test, one assertion failure,
1m38s), then passed after adding proof-gated
`POST /api/v1/auth/enrollments/status` (one test, 48s). Added follow-up
assertions for device pending/expired/rejected/approved states. The expanded
17-test `AuthenticationBoundaryTest` suite passed (31s). No password is sent
in polling; missing or
wrong-key request IDs return the same 404. This endpoint alone does not wire
client polling, completion or secure key storage.

Added `AccountPasswordPolicyParityTest` because divergent Kotlin/JVM Unicode
rules would make client-side password validation misleading. Targeted server
test passed (2 tests, 16s), comparing every BMP code unit plus supplementary
and malformed-surrogate/boundary examples against the server policy.

Client DPoP format TDD slice (2026-09-30): `DpopProofFactoryTest` first failed
to compile for missing proof/key/primitives interfaces. Added the KMP common
builder that emits RFC 9449-style ES256 JWS header/claims, strips URI query,
includes nonce/`ath` when applicable and requires a fresh JTI and 64-byte
JOSE signature from a platform device key. First GREEN attempt exposed a
test-decoder error: Kotlin Base64 decoder requires padding while JWS removes
it. Corrected the test helper and reran `:data:desktopTest --tests
'*DpopProofFactoryTest'` successfully (2 tests, 18s). This verifies
serialization only: no Android Keystore/Desktop OS key, secure token store,
HTTP retry integration or end-to-end proof validation exists yet.

Desktop device-key TDD slice (2026-09-30): tests first failed to compile for
missing store/protector, then verified a persisted P-256 key produces a
verifiable raw ES256 signature and refuses an existing bundle that cannot be
unlocked. Added Windows current-user DPAPI protection, atomic key-file write,
and explicit fail-closed behavior on other desktop OSes. A forced concurrent
first-run test revealed two store instances could produce different keys;
added a process mutex plus cross-process file lock and reran all three
`DesktopDpopDeviceKeyStoreTest` cases GREEN (19s). The Linux Secret Service
protector is **not yet implemented** for desktop clients; Linux currently
fails closed. This key store is not wired into DI/auth HTTP, and its Windows
DPAPI path has only compiled; tests inject a fake protector, so live Windows
DPAPI and Ubuntu Secret Service require separate verification.
Windows current-user DPAPI was later exercised with a temporary real
protector/key bundle: it reopened the same public key and signed after
restart; all four `DesktopDpopDeviceKeyStoreTest` cases passed (11s).
Ubuntu Secret Service remains unverified/unimplemented for this client.

Android signing prerequisite TDD slice (2026-09-30): added a test for
canonical ASN.1 DER-to-64-byte JOSE ES256 conversion, including 64 real
P-256 signatures and malformed/noncanonical inputs. It failed to compile
before `EcdsaDerSignature` existed; the common parser now passes the two
targeted desktop tests (27s). Android Keystore will use this converter because
its standard ECDSA signature API returns DER. No Android device test has run
yet for this new auth code.

Android hardware slice (2026-09-30): added `AndroidDpopDeviceKeyStore` using
the app-scoped Android Keystore P-256 key and DER-to-JOSE converter, plus an
instrumented test with a unique temporary alias that is deleted in `finally`.
The test source initially failed to compile for the missing class (and a
missing kotlin-test dependency); after implementation, Android main and test
sources compiled. `:data:connectedDebugAndroidTest` on physical RMX2001 then
failed **before any test ran**: UTP/ddmlib timed out installing the 17 MB
test APK (`ShellCommandUnresponsiveException`, 0 tests, 6m37s). Do not mark
Android Keystore verified. Asked the user whether the phone shows a USB
install confirmation. No app data or original backup was intentionally
modified; the test alias was never created because the test did not start.

Secure account HTTP TDD slice (2026-09-30): added a MockEngine test that first
failed to compile for missing `SecureAccountRestClient`. The new KMP client
submits signup to `/api/v1/auth/registrations`, retries one server nonce
challenge using a fresh DPoP proof, requires pending approval response, and
refuses non-HTTPS origins except same-host loopback. Targeted
`:data:desktopTest --tests '*SecureAccountRestClientTest'` passed (one test,
26s). This client method is **not wired into the account session/UI yet**;
it does not store credentials or cover completion/sign-in/status/refresh.
Response-loss signup recovery was not covered in that client slice.

Platform proof primitives (2026-09-30): the desktop test first failed to
compile for absent primitives, then passed (one test, 16s) after adding
CSPRNG-generated 24-byte JTI, SHA-256 and wall-clock seconds. Android has the
same Java platform implementation and compiles, but its hardware test remains
blocked by test APK installation. These primitives are not yet DI-wired.

Expanded HTTP contract TDD (2026-09-30): a new MockEngine test first failed
to compile for sign-in/status/completion types. `SecureAccountRestClient` now
distinguishes approved-device login (tokens) from new-device pending approval,
polls proof-gated enrollment status, and completes account or device approval
with password re-entry. Both targeted transport tests passed (26s). The
responses are still in-memory return values; no protected token persistence,
refresh/logout, DI binding or UI pending-flow logic exists. `git diff --check`
has no whitespace errors (only CRLF notices).

Signup response-loss TDD slice (2026-09-30): test first failed because a
repeat registration returned 409 after the server had saved the pending
request. The server now returns the same pending request ID with 202 only
when email, device ID/name/platform, original key thumbprint and password
all match the existing unexpired enrollment. A different key or password
still gets 409. Targeted `AuthenticationBoundaryTest` passed (one test, 22s).
This does not yet cover client-side persistent pending enrollment or
rate-limiting against repeated password guesses.

Desktop session-store TDD (2026-09-30): new test first failed to compile for
missing protected session store. Added profile-scoped token bundles under
hashed origin/email filenames, protected with the desktop OS protector,
atomic replacement and process/file locks. Reads validate origin/account
and reject corrupted bundles instead of pretending there is no session.
The first GREEN attempt exposed a test-expression visibility issue (test
method returned an internal exception type); fixed the test, then both
`DesktopProtectedSessionStoreTest` cases passed (12s). Test protector is fake;
production DPAPI path and Android AES credential store were not yet verified
at that checkpoint. A subsequent temporary-bundle Windows DPAPI test passed;
all three `DesktopProtectedSessionStoreTest` cases GREEN (10s). Android AES
credential storage remains unimplemented.

Android credential-store TDD slice (2026-09-30): added instrumented tests
for profile isolation, nonexportable Android Keystore AES key, reopening and
corruption rejection. The test first failed to compile for absent
`AndroidProtectedSessionStore`; the Keystore AES-256-GCM implementation now
compiles with `:data:compileDebugAndroidTestKotlinAndroid` (16s). It stores
per-profile encrypted bundles in app-private files, binds origin/email as
GCM AAD, and refuses a missing key if any bundle already exists. Runtime
tests remain **unrun** because the physical device rejected/timed out the
test APK installation. Do not treat compile success as crypto verification.

Origin-validation security review (2026-09-30): a regression test first
failed because prefix checks accepted `http://localhost:80@evil.example` as
if it were loopback. Added a shared canonical-origin validator for new auth
REST and Android/Desktop protected session files: HTTPS host (optional port)
or actual localhost/127.0.0.1 HTTP only; userinfo/path/query are rejected.
Targeted REST and desktop session-store tests passed together (25s). The
older passwordless clients still have broader URL acceptance but their
server routes remain fail-closed; they need removal during migration.

Regression checkpoint: full focused `AuthenticationBoundaryTest` (18 cases)
and `AccountPasswordPolicyParityTest` (2 cases) passed together (28s). The
full server suite is still expected RED until old passwordless fixtures and
clients are migrated; this focused pass does not clear deployment gates.

Refresh/logout HTTP TDD (2026-09-30): MockEngine test first failed for absent
client methods. `SecureAccountRestClient` now sends refresh with a caller-
provided stable operation ID, retries nonce challenge with the same JSON
body and a new proof, and sends logout with `Authorization: DPoP` plus the
access-token `ath` proof claim. All `SecureAccountRestClientTest` cases passed
(23s). This is transport only: a crash-safe client renewal journal, single-
flight coordination, persistence of replacement tokens and resource request
integration still need implementation and tests.

Broad client-data checkpoint: complete `:data:desktopTest` passed (23s),
including pre-existing data tests plus the new DPoP, DER, protected-store and
auth-transport suites. This does not exercise Android hardware or the full
app/server integration.

Refresh limits, throttling, concurrent rotation, stale receipt and revoked
WebSocket delivery still need more tests. The client does not yet journal
renewals or use the new protocol. Operator approval UI is unverified;
password reset, key-loss recovery and
safe AI contexts are not implemented. Do not use the partial server with live
clients or declare the security phase complete.

Authentication technical design recorded (2026-09-29); the earlier
reasoning-selection pause is released. See
[implementation plan](klaf-server-implementation-plan.md) for selected libraries,
Room model, enrollment/proof/refresh/reset protocol, platform storage, revocation
ordering and the TDD matrix. Product flows are recorded; technical choices were
delegated by the user. The user subsequently authorized the entire TDD and
implementation flow without routine approvals. Continue on your own unless a
material unexpected issue requires input. No commits without separate permission.

This design pass used read-only source inspection and primary/public documentation
and Maven metadata. It changed documentation only: no tests, production code,
Gradle dependencies, builds, runtime probes, publication, deployment, account
resets or original backups. Earlier audit/probe results below are historical,
not checks repeated during this documentation-only pass.

Selected: Bouncy Castle Argon2id, ES256 access JWT via stable Ktor/Auth0 integration,
Nimbus DPoP processing, current-user Windows DPAPI and Ubuntu libsecret/Secret
Service through JNA, Android Keystore, registry-based transactional auth state
and bounded encrypted refresh receipts. Versions/parameters and recovery bounds
are engineering selections, not build/security verification. No whole-database
encryption or hardware-backed-storage guarantee is implied.

Outstanding verification gates: dependency compatibility/advisories, real OS
storage/unlock behavior, actual provider credential-broker integration and
restricted-read policy support, Windows/WSL2 and Ubuntu final sandbox launch,
public-origin proof handling behind TLS termination and the full regression
matrix. AI must stay unavailable if safe isolation cannot be demonstrated; do
not silently broaden SDK scope or use a shared-profile fallback. Live tunnel
configuration, secrets and backup files were not inspected or changed.

### Interview record (historical; selected details superseded by the plan)

Client-access authentication interview started (2026-09-29). User requested
questions and agreed requirements before implementation. Ask one question at
a time with a practical recommendation; record confirmed decisions in the
requirements. Product authentication scope is Klaf; AgentDriver remains a
reference except for the approved targeted SDK capability and profile/credential
isolation changes below.
Conversation preference confirmed: use English technical terms such as
requirements, status, session, device key and token rotation in Russian
explanations; explain unfamiliar terms plainly and keep one question at a time.
User reaffirmed the order: complete requirements, write/run tests and observe
meaningful failures, then implement to meet the tests, including corner-case
coverage for authentication and the approved SDK isolation fixes. Do not adapt expected
behavior to the implementation. No commits until separate explicit permission.
Tokens are confirmed to grant access to one account only; caller-supplied email
cannot authorize access to another account. Each device receives a separate
token rather than sharing an account-wide token.
User confirmed password-based registration/sign-in with automatic per-device
token issuance, rather than manual token entry. The complete login flow remains
open.
User requires separate AI session contexts per account/device/functional area,
including different devices of the same account. Do not share conversation
history or stop an unaffected device's AI session on logout/device blocking.
This is not implemented yet; account data synchronization remains unchanged.
User confirmed explicit server-operator approval for each new account and each
new device accessing it. An approved account cannot authorize an unapproved
device. Approval/rejection will use a request list in the server application's
window, showing account and device name; no separate web panel in this stage.
Waiting behavior confirmed: one-time submission notification, non-blocking
account status and continued local guest work; no server sync/AI access before
approval and authentication. Approval does not automatically switch databases
or interrupt work. An explicit completion action transfers current guest data
to a new account; connecting to an existing account does not transfer it.
Password timing confirmed: set it when submitting the registration request;
store only its hash and salt immediately, with access still denied pending
approval. Hashing algorithm/parameters are not decided yet.
Post-approval completion confirmed: after approval of a new account/device,
the user enters the account password again to complete access. Do not retain
the submitted password while waiting. Recheck password and current approvals
before issuing a login session/device-bound credentials; approval alone grants
no access. New sign-up completion still transfers guest data; existing-account
sign-in leaves guest data untouched. Remembered valid login/normal renewal
does not require another password solely due to app/server restart.
User declined initial-password enrollment for existing passwordless accounts.
Rollout will start with new registrations after the user resets test accounts;
validate on the disposable setup before deployment and preserved-data transfer.
No users/databases/backups were deleted or changed in this interview turn.
Operator revocation confirmed: the server window can block an entire account
or revoke one device. Close affected active connections and deny subsequent
requests; retain server/local data and unaffected devices' access.
Restoration confirmed: require a new password sign-in and issue a new device
token after operator restoration; previously invalidated tokens must never
become valid again. Account/device approval remains required.
JWT/session validation confirmed: signed access tokens identify a server-owned
account/device login session. Every protected request must also check that
session's current validity and account/device access, allowing revocation to
deny subsequent requests before JWT expiry. Close affected WebSockets;
restoration/new password sign-in creates a new session rather than reviving
the revoked one. This is an agreed design, not an implemented authentication
mechanism.
Server authentication-session table confirmed: Klaf persists session ownership
and validity/revocation in its server database, retaining the state across
restarts. The JWT plugin does not automatically create/manage these records.
Encrypted client transport confirmed for this phase: HTTPS REST and WSS for
both client WebSocket paths, replacing the previous trusted-development plain
transport. No transport configuration changes have been implemented yet.
Deployment target confirmed: primarily a Linux laptop behind Cloudflare Tunnel,
with Windows support retained, reachable
outside the LAN. Klaf performs client authentication/authorization without
requiring the former Cloudflare Access client-token gate. The user described
the demo configuration; no live Cloudflare settings or secrets were inspected.
Same-host origin choice confirmed: cloudflared and Klaf on one laptop, public
HTTPS/WSS terminating at Cloudflare, encrypted tunnel to cloudflared, and
HTTP/WS only to the loopback-bound Klaf origin. No origin certificate for this
path; direct client network connections must not use this plaintext exception.
Demo code inspection found Desktop uses WSS on 443 with SDK BearerToken only;
Android's separate cloudflare branch still requires Access Client ID/Secret.
Authentication selection and the secure transport flag are independent. These
are source-code findings, not verification of the user's deployed tunnel.
Targeted SDK sandbox capability fix approved for the plan: configure network
restrictions in trusted setup, then remove the provider/children's ability to
change them; verify the final bridged launch and allowed connectivity. No SDK
implementation has started; unrelated SDK changes are not authorized.
Remembered sign-in confirmed: persist device credentials securely across app
restarts, never persist the password or compile credentials into the client.
Android uses token encryption with a Keystore-held key; Desktop uses protected
OS credential storage. Concrete Desktop backends remain open. Startup failure
behavior confirmed: if stored credentials/device key are inaccessible, retain
the active local profile and offline use, show an actionable error and keep
server access disabled until secure-store access is restored. No automatic
logout/guest switch, data deletion, device-key recreation or plaintext fallback.
Restored storage access does not bypass ordinary credential/session checks;
distinguish a store failure from a first launch with no saved credentials.
Token lifetimes and automatic renewal confirmed: JWT access lasts 15 minutes;
the refresh credential allows automatic renewal without password entry and
expires after 30 days from issuance/last successful server-side renewal.
Regular renewal extends that deadline; expiry requires password sign-in, not
deletion of local data or loss of offline study. Renewal still checks approved
account/device access and the active authentication session.
Refresh rotation confirmed: each successful renewal replaces the refresh
credential and invalidates its predecessor atomically; the client securely
stores the replacement. Response-loss recovery is separate from independent
renewal; no blanket old-token grace period is approved.
Refresh reuse response confirmed: an attempt at a new independent renewal with
a replaced credential revokes that login session and all its tokens, requiring
password sign-in. Keep data and unaffected devices/sessions intact. Do not
classify an allowed recovery retry as a new independent renewal.
Bounded automatic response-loss recovery confirmed: recover the same completed
renewal's result without another token issuance/rotation/deadline extension and
without bypassing approval/revocation. Require password sign-in if safe recovery
fails or is unavailable. Time/attempt limits, proof, protected result persistence
and exact protocol remain unspecified; an ID alone must not grant recovery.
Device-key binding confirmed: generate a protected asymmetric key pair per
installation automatically, keep the private key on the client, register the
public key and bind access/refresh credentials to it. Require proof for access
and renewal/recovery; a token or device ID alone is insufficient. DPoP is the
proposed standard reference, not a completed protocol/implementation. Exact
integration, replay checks, key storage/lifecycle and recovery details remain
open; protection does not cover compromise of both device/key and token.
AI-job cancellation on session revoke confirmed: cancel unfinished mnemonic
text/image generation and subtitle-analysis jobs belonging to affected sessions,
release resources and reject late results/events. Scope cleanup to the revoked
session, not a later sign-in or unaffected devices/shared infrastructure. Device
blocking affects that device's sessions; account blocking affects all its device
sessions. Preserve saved data and existing transaction guarantees.
Password recovery confirmed for this MVP: operator verifies the account owner,
creates an account-scoped single-use expiring reset token in the server UI and
delivers it privately; no automated email service. Client supplies token/new
password; successful reset updates hash/salt, consumes the token and revokes
all that account's authentication sessions, with scoped AI cancellation. Keep
data, require ordinary sign-in and preserve account/device approval/block state.
Issuing the token alone does not change the password or revoke sessions. Reset
lifetime/limits/storage/protocol remain to be specified; no actual password
change or token delivery was performed. Legacy enrollment remains out of scope.
Server signing-secret protection confirmed: use OS secure storage rather than
a separate application master passphrase. Ubuntu Linux is the primary server OS;
Windows remains supported. Initial implementation checks and manual testing
will run on Windows, followed by the primary Ubuntu deployment after testing.
Windows checks are not proof of Linux storage/sandbox behavior; Linux-specific
verification remains necessary. No deployment/data transfer was performed.
Manual server startup from an interactive Ubuntu desktop session is confirmed,
not a headless/SSH-only flow for this stage. Automatic service startup is not
currently required. Concrete providers, actual keyring/hardware availability
and unlock integration remain open; desktop startup is not proof of a working
secure-store backend.
Do not equate protected storage at rest
with full database encryption or verified AI-process isolation. No storage
backend has been implemented or configured.
Storage failure behavior confirmed: block client connections while OS secure
storage is locked/unavailable, offer OS unlock where supported and show an
actionable error with Retry if access fails. No plaintext persistence fallback
or automatic replacement of an existing signing key. Key-loss recovery is now
approved: after one explicit operator confirmation, automatically generate and
store a new signing key and invalidate all old authentication sessions and
their credentials, including refresh/recovery access. Preserve accounts,
password hashes/salts, approval/block state and data; require ordinary sign-in
on subsequent client connection, not re-registration. Apply scoped revocation
cleanup and keep client access blocked until recovery completes safely.
Temporary keyring failures still follow unlock/error/Retry, not automatic reset.
First-time provisioning, interruption/restart handling and planned key rotation
details remain open. No recovery implementation or live reset was performed.
Account password policy confirmed: minimum 15 characters, no spaces, no
mandatory digit/special-symbol composition rules. Digits/symbols are allowed,
not prohibited. Reject spaces rather than silently changing the password;
use consistent client/server validation for registration and reset. This is
an account-password rule, not an OS unlock/signing-key requirement. OS secure
storage remains selected without a separate Klaf server master passphrase.
Further character/length limits still need design. Password-attempt throttling
confirmed: repeated incorrect passwords cause a temporary wait, with actionable
time-to-retry feedback. No permanent account block or operator intervention
solely for reaching the attempt threshold; active login sessions and offline
data remain unaffected. Concrete limits/backoff/accounting/persistence and
denial-of-service corner cases remain for design/tests. Documentation only.
AI sandbox failure behavior confirmed: keep sign-in/data synchronization
available when their own authentication/data dependencies are healthy, disable
affected AI features with an actionable unavailable error and show the cause
and Retry in the server UI. No AI launch without the required sandbox as a
fallback. Signing-storage failure still blocks client connections. This is
agreed behavior, not implemented or runtime-verified yet.
AI context lifetime confirmed: after logout/revocation, a new password sign-in
starts fresh AI contexts without the previous login's conversation history,
including the same account/device. Preserve saved cards/images/sources and
unaffected devices' AI sessions. Routine token renewal is not a new login.
Provider-history file cleanup, idle handling and actual filesystem
isolation remain open; a fresh thread alone does not isolate a shared profile.
No context-lifecycle implementation or new runtime tests have started.
SDK profile/credential-isolation scope approved in addition to the network
capability fix: make only the SDK changes needed for Klaf to isolate histories
and restrict AI-tool access to provider credentials/server secrets/auth storage.
Keep provider authentication working without exposing its credentials to
agent-controlled tools; Klaf account/auth logic remains in Klaf. Use TDD with
synthetic-secret/cross-session file-access tests on the real launch path.
Concrete enforcement/cleanup/authentication integration is still open; separate
threads/directories are not sufficient proof and no implementation has started.
Source recheck confirmed the profile is passed as CODEX_HOME and mounted
read/write by SandboxNetworkBridge.kt; WSL builds a provider-wide profile path,
not an account/device/login-scoped one. Codex uses the profile for configuration,
cached provider login and local history/artifacts. These credentials are not
Klaf user passwords or its server signing key. Actual credential values were
not read, model/tool exfiltration was not attempted and no runtime was changed.
Automatic isolation setup confirmed: SDK must apply provider tool-access policy
and OS sandbox restrictions during AI launch, without a manual step before every
Klaf server start. Initial dependency installation/provider sign-in are separate
prerequisites. Current thread/start and turn/start parameters use the :workspace
preset; explicit credential-read restrictions were not found in those parameters.
This source check is not proof of effective tool isolation; the concrete mechanism
and deployed-provider compatibility still need design and real-launch tests.
Server restart context decision confirmed: begin fresh AI contexts without the
previous run's conversation history. Preserve persisted authentication sessions
and remembered sign-in subject to normal validity checks; keep saved cards,
images and Vocabulary Sources. Restart alone does not require password sign-in.
Old provider artifacts must not be accessible to fresh AI contexts; physical
cleanup remains open. Documentation only; no restart behavior was implemented
or runtime-tested.
Interrupted AI-request UX decision confirmed: no new retry mechanism/buttons
for Word Insights, mnemonic text/images, vocabulary analysis or transcription.
For a request lost on restart, end loading with an error and let the user use
existing controls or leave/reopen the screen to trigger its existing load flow.
Do not add the proposed initial Word Insights Retry button. Preserve saved
results/drafts under existing feature rules; reconnection and recovery of a
surviving request are distinct from silently starting a replacement generation.
Database-sync and token-renewal retry rules remain unchanged. Existing client
KlafServerSession resends pending requests after reconnect; implementation must
distinguish a surviving request from one lost on server restart before replay.
This is an agreed target, not a changed or tested runtime behavior.
Ubuntu interactive desktop startup is confirmed. The actual keyring backend
and availability remain unverified; Secret Service is a candidate, not an
approved or configured provider. Headless service support is not a current
requirement. Main product flows are recorded; interview remains open while
requesting confirmation to select the remaining technical details and prepare
a concrete security design/implementation plan for user review before TDD.
That transition is proposed, not permission to implement or commit. Hashing
parameters, secure-store backends, key-bound request/replay protocol and bounded
refresh-response recovery still require concrete specification and validation.
Credential format and concrete recovery protocol details also remain open;
these product decisions are not yet a complete implementation-ready security
protocol.
These interview decisions changed documentation only. No implementation or
runtime tests were performed; `git diff --check` passed.
User raised credential exfiltration through malicious AI input; AI access to
authentication storage/secrets is now an open interview topic. User requested
an audit of existing SDK and Klaf isolation before choosing additional controls.
The resolved SDK artifact was checked and isolated WSL probes were performed;
findings below prevent treating isolation as fully verified. Effective isolation
of a running Klaf/Codex session has not been tested end to end.
Remaining credential issuance details, device-key protocol/lifecycle, OS storage
backend details, refresh format/recovery protocol details and operational
tunnel/TLS setup remain to be decided. OS-protected server signing-secret storage
is selected; its concrete backend and lifecycle still need specification.
Unconfirmed suggestions are not
approved requirements.
No authentication implementation or new tests have started.
The firewall-management privilege fix and the minimum profile/credential
isolation changes required for Klaf are approved scope exceptions for
AgentDriver. Unrelated SDK changes still need separate approval; finish the
interview before starting implementation.

Vocabulary Source synchronization implementation and automated verification
complete (2026-09-29). Device deployment/acceptance remains separate.

### AI Isolation Audit (2026-09-29)

Scope: inspect the current SDK and Klaf integration, not implement authentication
or modify SDK behavior. No live AI requests, real credential reads, production
database reads/writes or running server restarts were performed. Only existing
tests, dependency inspection and disposable namespace probes were run.

Artifact identity:

- Klaf resolves `io.github.nikolaykuts:server-jvm:0.10.12-SNAPSHOT`; offline
  `:klaf-server:dependencyInsight` confirms the runtime dependency. The local
  SDK also declares `sdkVersion=0.10.12-SNAPSHOT`.
- Nine security/session/environment source files from the published sources
  JAR match the sibling checkout. The published server binary JAR and the SDK
  build's same-version JAR have identical SHA-256 hashes. This prevents assuming
  that a different SDK checkout/version supplies Klaf's reviewed protection.

Existing technical boundaries:

- `AssistantSandboxPreflight.kt` selects WSL2/bubblewrap for Codex on Windows
  and bubblewrap on Linux. Startup rejects missing/unverified sandbox support;
  Klaf calls `preflight()` and the SDK repeats startup checks. There is no
  Klaf-specific independent OS sandbox around its JVM.
- `CodexAppServerSessionFactory.kt` starts the provider inside the sandbox over
  stdio, allocates a session workspace and uses `:workspace`/`never`. It declines
  provider approval requests and unexpected provider-initiated calls.
- `SandboxNetworkBridge.kt` mounts system dependencies read-only, the provider
  profile read/write and the individual session workspace read/write. Windows
  drives and Klaf storage are not mounted by this default WSL path.
- `AssistantEgressProxy.kt` permits reviewed provider HTTPS destinations only,
  rejects local/private/literal-IP targets and uses a generated proxy credential.
  Native Linux clears the inherited environment; WSL launch does not use
  `--clearenv`, relying on WSL forwarding rules. Caller-controlled `WSLENV` and
  effective inherited values still need a deployment check without logging
  secret values.
- SDK internal listeners use loopback in this integration. Klaf clients pass
  `authentication=null`; the SDK default is `NoClientAuthenticator`. Loopback
  restricts network exposure, but does not authenticate local processes.

Findings (not fixed):

1. High: the bridged sandbox grants `CAP_NET_ADMIN` and `CAP_NET_RAW` to UID 0
   and does not drop them before executing the provider. An isolated WSL probe
   with these exact capability flags changed its own OUTPUT policy from DROP
   to ACCEPT; effective/bounding/ambient capability masks were `0x3000`.
   This confirms the outer namespace firewall is modifiable by code holding
   those capabilities, not an immutable boundary. It is not an end-to-end
   demonstration that a Codex model command can bypass every additional guard.
   Drop privileges after bridge setup and test the actual final command path.
2. High: Klaf's feature AgentSession/Service objects are singleton instances.
   A persistent internal client reuses a provider runtime and the same Codex
   `threadId` for successive turns; there is no account/device ownership or
   context reset on this path. Local database partitioning and logout operation
   cancellation do not partition the model's conversation history. Distinct
   users may share prior prompt/result context without reading a database.
3. High: the provider profile is mounted read/write in every provider sandbox.
   It contains `auth.json` and provider history/generated artifacts, and is
   shared across feature sessions. There is no explicit credential-file read
   denial in Klaf's SDK configuration; OS permissions on a directory already
   mounted for the provider do not separate its credentials from agent tools.
   Actual credential access via Codex tools was not attempted. A separate
   workspace/thread alone does not isolate a shared visible profile.
4. High for untrusted deployment: current MVP Klaf REST routes accept supplied
   email/device identifiers, and the public AI WebSocket has no authenticated
   account principal. A device registration check is not proof of identity;
   the device registration endpoint itself takes an email without credentials.
   Room account files isolate ordinary application queries, not hostile clients.
   SDK internal token authentication, even if enabled later, would not secure
   these independent public routes. Protect REST, images and both WebSocket
   paths with application-level account authorization.
5. Validation gap: WSL preflight runs a simpler, unbridged sandbox success probe,
   not a hostile test of the final bridged provider launch. Existing POSIX abuse
   tests return early on Windows and do not verify this network bridge. Server
   raw-response logging can also retain user content when enabled; log ownership
   and redaction remain deployment concerns.

Checks performed:

- A WSL namespace probe confirmed the host project directory exists outside
  the sandbox but is not visible inside; the Windows drive mount is absent.
  This supports default filesystem mount isolation, not a comprehensive
  sandbox escape audit or proof about every deployed process.
- The firewall modification probe affected only its freshly unshared network
  namespace, used no network bridge and sent no traffic. It did not change host
  firewall rules or read credential files.
- Existing SDK focused tests: 65 total, 59 passed, 6 failed, zero reported
  skips/errors. `SandboxPreflightTest` (7), `RestrictedEgressProxyTest` (5), and
  `WindowsElevatedSandboxCommandTest` (3) pass, subject to platform-return caveat.
- Failure triage: two dispatcher fixtures advertise an empty capability set
  but expect text/image generation, which the dispatcher correctly rejects;
  two broken-profile preflight fixtures expect five issues although provider
  preflight is skipped when directory preparation fails; the environment test
  expects exact equality although Windows cmd adds standard variables. These
  failures do not establish secret inheritance or sandbox bypass. One linked
  child cleanup test fails its first directory-preservation assertion on
  Windows; external-target preservation was not reached by that assertion and
  needs separate investigation. Do not call the SDK suite green.
- Reproduction from AgentDriver root:
  `gradlew.bat :sdk:server:jvmTest --tests '*SandboxPreflightTest' --tests '*RestrictedEgressProxyTest' --tests '*CleanProcessEnvironmentTest' --tests '*ProtectedStorageServiceTest' --tests '*CodexAppServerSessionFactoryTest' --tests '*SharedSecurityPreflightTest' --tests '*WindowsElevatedSandboxCommandTest' --offline --console=plain --max-workers=2`.
- Klaf regression: all 24 selected existing tests passed (operation registry 7,
  account storage 10, live account-session flows 7), zero failures/errors/skips.
  These cover current application data partitioning and cancellation, not
  isolation of a real shared AI thread or newly implemented authentication.
  Command from Klaf root:
  `gradlew.bat :klaf-server:test --tests '*KlafServerOperationRegistryTest' --tests '*ServerAccountStorageTest' --tests '*LiveServerAccountSessionTest' --offline --console=plain --max-workers=2`.

Reference checked: official Codex app-server documentation describes restricted
read roots separately from workspace write restrictions:
https://learn.chatgpt.com/docs/app-server#sandbox-read-access-readonlyaccess.
Do not infer secret confidentiality merely from the selected working directory.

Audit follow-up scope: the targeted network-capability fix and minimum SDK
profile/credential-isolation changes required for Klaf were subsequently
approved for the plan. Concrete enforcement remains open and unimplemented;
unrelated SDK changes are not approved.
First resolve the immutable network-boundary and provider-credential exposure
questions in the SDK, then
design account-scoped Klaf authorization and genuinely separated AI context and
profile/workspace visibility. Use synthetic multi-user and secret-canary tests
on the final launch path. Choosing a login method remains an interview decision.

### Pre-commit Review (2026-09-29)

- User reports the updated feature appears to work and authorized review,
  targeted refactoring/regression tests and a local commit. Rechecking all
  uncommitted files, including the user's two launch-command comments.
- Review found a stale source conflict snapshot when a later operation in the
  same request was accepted. A failing live REST regression reproduced it;
  the server now captures final committed source snapshots for conflicts.
  A complementary regression verifies later deletion returns absence instead
  of resurrecting the source. All 21 source REST scenarios pass.
- Refactored source/item persistence mapping to named constructor arguments,
  making field correspondence and portable/local identity links explicit.
  User launch-command comments in both entry points are preserved.
- Final six-suite review regression: 555 total, 554 passed, zero failures/errors,
  one existing optional imported-baseline smoke skipped. Totals: data 150,
  domain 46, contract 13, presentation 76, DI 18, server 252 (one skipped).
  Android debug APK assembly and Desktop compilation use the combined command
  recorded below. iOS and exhaustive device/AI acceptance remain unverified;
  the user's general working smoke report is not exhaustive device coverage.
- Prepared local snapshot under the user's explicit authorization. Staged
  whitespace checks pass; 45 changed/new text files have no matches for the
  checked private-key/provider-token, personal email/home-path or LAN-address
  patterns. The same sensitive patterns have no matches in indexed text.
  Actual local properties, client/server SecretConstants and Firebase secrets
  remain ignored/untracked; the only indexed Google-services JSON is the
  pre-existing dummy isolated-test fixture. Required schema exports/history
  remain; no database/cache/build artifact or private notes were staged.
  This is a scoped pattern/index audit, not a complete historical/binary audit.
  No push, live account/server restart, device deployment or protected backup
  modification was performed.

### Completed Vocabulary Source Follow-up

- Sources, complete analyzed word lists and additive Ignored Words now use the
  existing manual Room/REST synchronization path. Local changes and durable
  outbox operations are committed together; the pending indicator becomes
  yellow when connected, with existing gray/error/conflict priorities retained.
- Client schema 16 adds stable source/item identities and feature revision
  tracking through a non-destructive 15 -> 16 migration. Server schema 7 adds
  account-scoped feature tables through its 6 -> 7 auto-migration. Existing
  client sources become first-upload data, without deleting or re-importing.
- Protocol 3 covers source upsert/delete and ignored rules; the updated server
  continues accepting deck/card protocol versions 1 and 2. Older clients that
  already confirmed the global revision receive an explicit initial feature
  snapshot, tracked by a durable vocabularySyncInitialized checkpoint flag.
- Whole-source conflicts include text/metadata and analyzed words. Bulk and
  per-source choices accept the server or keep/re-upload the local aggregate.
  Different sources merge automatically, including independent remote sources
  when every local operation conflicts. Conflicts remain durable across retry.
  A keep-local operation cannot restore missing card/deck links silently.
- Portable sync IDs map links to each device's own integer IDs. Card/deck
  deletion clears obsolete source links transactionally without removing the
  source word or its ADDED status, including deletions by old deck/card clients.
  Source deletion removes its word list, not independently created cards.
- Preserved guest sign-up transfer/retry, sign-in isolation, account separation
  and sync edit gates. Lost-response retries retain exact operation identities;
  invalid source batches roll back server data, history and revision together.
  Ignored rules merge as a normalized union using canonical server values.
- TDD baseline: 13 new Room tests ran with 9 expected failures and 4 existing
  guarantees passing; the initial seven live REST/two-client tests failed for
  missing feature synchronization. Migration and nullable-omission tests also
  failed meaningfully before implementation. Additional regressions exposed
  lost local identity on restore, missing independent partial downloads,
  divergent ignored-rule values, upgrade baseline omissions and dangling links;
  each was run red before its corresponding fix.
- Final six-suite regression: 553 tests total, 552 passed, zero failures/errors,
  one existing optional imported-baseline smoke skipped. Module totals: data
  150, domain 46, contract 13, presentation 76, DI 18, server 250 (one skipped).
  The 19 real REST/two-client source scenarios include independently keeping
  one local analysis and accepting another server analysis. Android debug APK
  assembly and Desktop compilation passed in the same combined Gradle run:
  `:data:desktopTest :domain:desktopTest :klaf-server-contract:jvmTest
  :presentation:desktopTest :di:desktopTest :klaf-server:test
  :Android:assembleDebug :Desktop:compileKotlin --continue --max-workers=2`.
  Existing Gradle/KMP/deprecation warnings remain; none failed this build.
- Changed/new text privacy scan covered 43 files with no private-key/token,
  personal email/home path or LAN-address matches. Real local properties,
  runtime secrets and Firebase configuration remain ignored/untracked.
  Protected backups, live server/account, physical devices and sibling worktrees
  were not modified. No commit, push or deployment was performed.

### Run the Updated Feature

- Restart the server from this updated checkout and rebuild/update both clients
  before testing protocol 3. Existing account/source data is preserved by the
  migrations; no database reset or backup re-import is needed.
- Save/edit a Vocabulary Source: with a connected status channel it becomes
  pending/yellow. Explicitly synchronize, then synchronize the second device.
  Concurrent analyses of one source open the existing conflict screen with
  local/server text/word previews and whole-source choices.
- Real Android/Desktop acceptance and iOS compilation were not performed by
  this follow-up; automated REST tests used disposable loopback servers/databases
  and no paid AI requests. The existing opt-in imported-baseline smoke stays
  skipped. Deferred image conflict previews and killed-process recovery remain
  outside this feature.

### Historical Merge Validation (Before This Follow-up)

Local branch integration is complete (2026-09-29): `remote-storage-server`
(`2e00cee`) is merged into `mnemonic-voice-dictation`, following the requirements
checkpoint `101d11c`. No push or deployment is included.

- Combined textual conflicts, retaining both storage and voice functionality.
  Client Room schema is version 15. Explicit integration migrations handle both
  historical branch layouts without destructive fallback; a preserved voice-v12
  schema fixture supports regression coverage alongside the storage schemas.
- Added account-scoped Ignored Words, complete guest source/item/rule transfer
  and source-only retry support. The two new ownership tests were first run red,
  demonstrating missing transfer behavior before its implementation.
- Added device-session end signaling, server operation/upload cancellation,
  namespace rotation and late push/notification rejection on account transitions.
  Ordinary disconnections retain existing transcription behavior.
- Final combined Gradle run succeeded: 515 tests total, 514 passed, zero failures
  or errors, and one optional imported-baseline smoke test skipped. Module totals:
  data 136, domain 46, contract 11, presentation 73, DI 18, server 231 (one skipped).
  Android `compileDebugKotlin` and Desktop `compileKotlin` both succeeded.
  The run included all six test suites with `--continue --max-workers=2`.
- Verification follow-up: fixed an Android push map-receiver compile error,
  normalized the voice branch's missing source URL SQL default while preserving
  its rows, and added online session reconnection coverage. Account changes
  reconnect only a previously ready connection using the new session namespace;
  offline logout still makes no connection attempt. Both branch-layout migration
  tests and guest ownership/transfer regressions passed in the final run.
- Earlier verification found that the existing clean-close replay test
  exceeded its aggregate five-second window across cold client initialization
  plus reconnect. A focused run reproduced this: the request was sent and the
  connection closed, but the outer deadline cancelled it before replay. The test
  now connects initially before measuring replay, without changing production
  timeouts or relaxing the replay deadline. It passed in the final combined run.
- Follow-up review prevents ended requests from surfacing generic failures in
  the next account: request failures and transcription events are converted to
  cancellation when their originating namespace has changed. The online logout
  regression asserts cancellation specifically; logout during the initial
  handshake is also covered. All six client lifecycle regressions passed.
- Android application compilation exposed a module boundary issue in notification
  routing: the activity referenced the data-layer session interface directly.
  A domain-only `IClientSessionScope` now exposes the rotating namespace; common
  DI binds the existing session to it. The default-binding regression asserts
  both interfaces resolve to the same instance. DI tests and final Android
  compilation verified this fix. A JUnit return-type error in the new server
  logout test was also fixed; the final server suite passed.

- Compared clean branch heads `mnemonic-voice-dictation` (`a0b44a3`) and
  `remote-storage-server` (`2e00cee`), with common base `e6f6297`. A `merge-tree`
  simulation found 19 conflicted paths without changing branches or the index.
  Main overlaps: divergent Room schema versions, dependency wiring, server
  lifecycle, Android notification navigation and card-insights behavior.
- Vocabulary Source/voice data may start empty, but deck/card/sync data and
  immutable backups must remain protected.
- User chose cancellation on explicit logout rather than retaining unfinished
  feature results across account changes. Logout ends only the originating
  device's session/work; other devices of the account are unaffected. Offline
  logout is immediate and cancels local work; undeliverable cancellation may
  leave server work running, but its results are rejected locally. No queued
  logout is introduced. Integration now uses a rotating originating device
  session namespace for cancellation/results, with automated lifecycle coverage.
- User confirmed Vocabulary Sources and their associated persisted feature data
  are part of account-owned content. Sign-up must copy/verify/transfer them with
  decks/cards, preserving links and supporting source-only guest databases;
  sign-in still leaves the guest database untouched. Transfer implementation has
  been extended; recovery/isolation tests passed in the combined verification.
- User agreed to cross-device source-data synchronization as a separate phase
  immediately after the merge, with its own conflict rules/tests. The current
  storage protocol at that checkpoint contained deck/card operations only;
  the completed follow-up above extends it to source data.
- No original backup, real account database, sibling project or live device was
  modified. Runtime/device testing remains separate from disposable test fixtures.

### Remaining Verification and Next Phase

- No merged physical-device acceptance, notification-tap/visual smoke check,
  real AI inference, or deployment was performed. Historical device results
  below belong to the pre-integration branches.
- The optional `ManualImportedDesktopSmokeTest` was not enabled against a real
  imported baseline. Automated migration/REST tests used disposable fixtures.
- iOS remains unverified with the known unresolved LoKdroid native publication
  issue; Android/Desktop success does not resolve it.
- Cross-device Vocabulary Source synchronization was subsequently implemented
  in the follow-up above. Source-to-card additions continue using the deck/card
  outbox and respecting review/sync edit gates.

### Prior Branch History (Before Integration)

The entries below describe validation and decisions on the mnemonic branch
before the current merge; their test totals are not combined-branch results.

MVP implementation is in local testing/review.

- Commit preparation (2026-09-28): user authorized a local snapshot of all
  current branch changes, including audited implementation/tests, both UI themes
  and privacy cleanup. Last full validation: 116 passing tests and Android/Desktop
  compilation; the final server theme follow-up also passes. The diagnosed
  LoKdroid iOS publication issue below is deliberately included as a known open
  issue, not fixed or hidden. No push, merge or sibling-project changes requested.

- IDE import dependency check (2026-09-28): `help` and all five KMP
  `resolveIdeDependencies` tasks complete, but their JSON reports contain 12
  unresolved iOS dependencies in data/di/presentation. LoKdroid `0.2.0-alpha`
  requests `LoKdroid:core-iosarm64:unspecified` and
  `LoKdroid:core-iossimulatorarm64:unspecified`. Android/Desktop build success
  does not validate these iOS artifacts. Latest IDE sync is recorded as
  successful despite these dependency errors; the earlier AgentDriver plugin
  lookup failure is not reproduced now. No library/configuration fix or
  publication was performed; correcting the sibling library publication needs
  separate authorization.

- Theme follow-up (2026-09-28): server/client colors centralized and both
  palettes covered by tests. Manual visual smoke checks remain; no live server
  or device was launched for this change.

## Uncommitted-worktree Audit (2026-09-28)

- All staged/unstaged/new work was reviewed relative to HEAD on
  `mnemonic-voice-dictation`. Existing staging/user edits were preserved; no
  commit, merge, deployment or paid-provider request was performed.
- Fixed binary/upload validation, start ordering, temporary-file lifetime,
  cancellation/queue isolation, pending registry completion and transcription TTL.
- Fixed connection cleanup lock inversion and covered cancellation before Ready,
  close before Ready and pending-request replay through a temporary loopback server.
- Push registration routes by originating client session; a desktop request can
  never fall back to the last registered phone. Delivery deduplication records
  confirmed success only and has bounded retention. Diagnostic errors avoid
  credential paths, push payloads and exception details sent to clients.
- Log capture is bounded per pending line (16 KiB) and history (5,000 entries);
  console output remains intact. Installation is idempotent; full-history
  autoscroll follows content, not the now-constant entry count.
- Actual SDK/plugin version is `0.10.12-SNAPSHOT`, published locally from the
  sibling checkout under existing user authorization without SDK source edits.
- Server 34 tests and contract 10 tests pass. The final cross-module run passes
  all 109 tests plus `:di:compileKotlinDesktop` and `:Android:compileDebugKotlin`.
- Remaining audio gaps and manual verification are in `audio-transcription-status.md`:
  selected-file duration display, disconnected failure push, real phone/provider/Linux
  checks. Killed-process reconciliation is explicitly deferred by the user for
  the current commit. These are not completed by a compile/test-only audit.

## Completed

- UI theme consistency (2026-09-28): server window/log styles now consume
  centralized light/dark `ServerColors`; initial selection follows the system,
  with an in-window toggle. Inputs, caret, selection, search borders/highlights,
  logo and actions follow the active palette. Styled log caching includes the
  palette, while entries and search text survive a toggle. No dependency on
  the client presentation module was added.
- Seven new palette/style regressions cover light/dark selection, every log
  part, unchanged text/search ranges, readable contrast, source badge colors,
  disabled tokens and direct palette initialization. The full cross-module run
  passed 116 tests plus Android and Desktop DI compilation. See the source
  feature status for client colors and previews. No commit was performed.
- Final commit privacy/hygiene check (2026-09-28): removed tracking for 15
  local/generated files without deleting them, added a neutral presentation
  config template, sanitized documentation/test examples, and checked both
  current text and the index. Server 34 tests pass after fixture sanitization.
  Details and the old-history/staging caveats are in `audio-transcription-status.md`.
- Created initial requirements document.
- Created initial status document.
- Captured the first high-level concept:
  - separate Gradle module;
  - Ktor server;
  - Koin DI;
  - AgentDriver SDK dependency from local Maven;
  - multiple feature-specific AgentDriver/Codex sessions;
  - SDK changes likely needed for `developerInstructions` support.
- Implemented AgentDriver SDK code changes for Codex thread-level instructions:
  - removed old shared `systemInstruction` behavior config;
  - added Codex-only nullable `baseInstructions` and `developerInstructions`;
  - pass those fields through Codex app-server `thread/start`;
  - keep `turn/start` prompts plain, without a repeated system-instruction wrapper;
  - updated SDK unit/fake-server tests at code level.
- Ran AgentDriver reference scans for old `systemInstruction` usage.
- Ran `git diff --check` in AgentDriver; only line-ending warnings were reported.
- Added initial Klaf server Gradle modules:
  - `:klaf-server-contract` for shared Kotlinx Serialization DTOs;
  - `:klaf-server` for the JVM/Compose Desktop server app.
- Added the MVP Klaf WebSocket contract:
  - `server.ready`;
  - `wordInsights.generate`;
  - `wordInsights.generated`;
  - structured `request.error`.
- Added the initial Klaf server app:
  - minimal Compose Desktop window;
  - automatic Ktor WebSocket server startup at `/ws`;
  - Koin module;
  - LoKdroid console logging;
  - local ignored `SecretConstants.kt` plus committed example file.
- Added the first Word Insights server path:
  - starts a feature-specific internal AgentDriver/Codex session lazily;
  - sends shared Klaf context through `baseInstructions`;
  - sends Word Insights rules through `developerInstructions`;
  - sends only a word-specific prompt and response schema per request;
  - recreates the feature session and retries once after request/session failure.
- Verified `:klaf-server-contract:compileKotlinMetadata` successfully.
- Fixed the Klaf server Ktor engine type after the first compile attempt.
- Bumped AgentDriver SDK version to `0.10.10-SNAPSHOT` in the sibling `AgentDriver` checkout.
- Updated Klaf version catalog to consume AgentDriver `0.10.10-SNAPSHOT`.
- Published AgentDriver `0.10.10-SNAPSHOT` to Maven local with `publishSdkToMavenLocal`.
- Verified `:klaf-server:compileKotlin --refresh-dependencies --no-configuration-cache` successfully.
- Added Klaf client-side WebSocket session in `data` for the new Klaf server protocol.
- Added Android/Desktop Ktor WebSocket client factories.
- Switched Android/Desktop `IWordMeaningInsightsRepository` DI binding from direct KlafServer to `KlafServerWordMeaningInsightsRepository`.
- Added local client config fields under ignored `data` `SecretConstants.KlafServer`.
- Verified:
  - `:data:compileKotlinMetadata`;
  - `:data:compileKotlinDesktop :di:compileKotlinDesktop`;
  - `:data:compileReleaseKotlinAndroid :di:compileReleaseKotlinAndroid`.
- Added a temporary Room downgrade migration from schema version `8` to `7` after stashing
  the Vocabulary Source work. This drops only the Vocabulary Source tables created by the
  stashed feature and preserves the existing deck/card tables.
- Extended the Klaf WebSocket contract for mnemonic generation:
  - `mnemonic.association.generate`;
  - `mnemonic.association.generated`;
  - `mnemonic.image.generate`;
  - `mnemonic.image.generated`.
- Added the Klaf server mnemonic path:
  - one feature-specific internal AgentDriver/Codex session for mnemonic text and image;
  - mnemonic text/image rules sent through session-level `developerInstructions`;
  - per-turn prompts contain only request-specific word/comment/selection data;
  - mnemonic text response parsing and validation preserved from the old client implementation;
  - mnemonic image bytes returned to the app as Base64 over WebSocket.
- Added client-side Klaf server repositories:
  - `KlafServerMnemonicAssociationRepository`;
  - `KlafServerMnemonicImageRepository`;
  - `KlafServerConnectionManager`.
- Switched Android/Desktop mnemonic text and image DI bindings from direct KlafServer to Klaf server repositories.
- Removed old client-side direct KlafServer networking/repository/session implementation from `data`.
- Removed the `agentdriver-client` dependency from the `data` module. The AgentDriver SDK is now used by `:klaf-server`, not by the Android/Desktop client feature path.
- Verified:
  - `:klaf-server-contract:compileKotlinMetadata`;
  - `:klaf-server:compileKotlin`;
  - `:data:compileKotlinMetadata`;
  - `:data:compileReleaseKotlinAndroid`;
  - `:di:compileReleaseKotlinAndroid`;
  - `:data:compileKotlinDesktop`;
  - `:di:compileKotlinDesktop`.
- Added internal AgentDriver session diagnostics for Klaf Server:
  - server state subscription;
  - server metrics subscription;
  - provider diagnostics subscription;
  - client connection state subscription;
  - connected session details: provider, model, reasoning effort, capabilities, capability details, reconnect grace period;
  - explicit logs for preflight, internal server start, and client connect.
- Verified `:klaf-server:compileKotlin`.
- Switched Klaf Server internal Codex sessions to explicit `gpt-5.5` model with `low` reasoning effort.
- Verified `:klaf-server:compileKotlin`.
- Fixed Klaf client connection status after server migration:
  - `KlafServerConnectionManager` no longer reports `Ready` by default;
  - connection state now comes from the shared `KlafServerSession`;
  - app startup triggers an initial Klaf Server connection attempt;
  - `Ready` is emitted only after the WebSocket receives `server.ready`;
  - failed connection attempts emit `Error`, so the drawer can show Retry instead of false readiness.
- Verified:
  - `:data:compileKotlinMetadata`;
  - `:data:compileReleaseKotlinAndroid`;
  - `:di:compileReleaseKotlinAndroid`;
  - `:data:compileKotlinDesktop`;
  - `:di:compileKotlinDesktop`.
- Completed a first code-review pass over the Klaf Server migration and documented the main follow-up risks.
- Updated project documentation and changelog for the current Klaf Server migration state.
- Renamed public client/domain/UI/data connection concepts from AgentDriver naming to Klaf Server naming where the app now talks to the Klaf Server wrapper instead of directly to AgentDriver.
- Removed the stale direct AgentDriver client secret/config block from the local ignored `data` `SecretConstants.kt`, leaving only the Klaf Server host/port config for the client path.
- Corrected docs/changelog wording so AgentDriver remains the name of the internal SDK/assistant bridge, while Klaf Server remains the public app/server protocol.
- Updated the Klaf AgentDriver SDK dependency target to `0.10.11-SNAPSHOT` for the WSL2 data-root readiness fix in AgentDriver preflight. This requires publishing the matching AgentDriver snapshot to Maven Local before rebuilding `:klaf-server`.
- Bumped AgentDriver to `0.10.12-SNAPSHOT` for the progressive speech-to-text
  client API and configuration-cache-safe local config tasks. Published the
  server/client SDK sets to Maven Local, updated the Klaf version catalog, and
  verified `:klaf-server:compileKotlin --refresh-dependencies --no-configuration-cache`.
- Verified after rename:
  - `:domain:compileKotlinMetadata`;
  - `:data:compileKotlinMetadata`;
  - `:presentation:compileKotlinMetadata`;
  - `:di:compileKotlinMetadata`;
  - `:data:compileReleaseKotlinAndroid`;
  - `:di:compileReleaseKotlinAndroid`;
  - `:presentation:compileReleaseKotlinAndroid`;
  - `:data:compileKotlinDesktop`;
  - `:di:compileKotlinDesktop`;
  - `:presentation:compileKotlinDesktop`.

## Decisions

- The previous Klaf Vocabulary Source work is saved in Git stash:
  `WIP vocabulary source feature before Clav server app`.
- The Klaf server application will be implemented as a separate Gradle module inside the existing `Klaf_kt` project.
- The Gradle module name will be `:klaf-server`.
- The server is intended to run on a remote computer/server machine, with local localhost debugging also supported.
- The MVP transport direction is WebSocket-style communication, similar to the current AgentDriver client/server usage.
- Any local secrets/config files must be ignored by Git.
- The Klaf server will expose a Klaf-specific WebSocket protocol and use AgentDriver internally.
- The Klaf app will use one persistent WebSocket connection to the Klaf server for the whole app, with request IDs for multiplexing operations.
- Feature-specific AgentDriver/Codex sessions will be created lazily on first use for MVP.
- Failed feature sessions should be recreated automatically, with one retry for the failed request.
- Word Insights was the first end-to-end feature.
- Word Insights will reuse the existing client-side prompt/response contract at first.
- No fallback to the old direct AgentDriver path should be added.
- The Klaf server will use a local ignored `SecretConstants.kt` file for MVP secrets/configuration, plus a committed example/template file.
- Klaf app to Klaf server authentication can be temporarily disabled for first local/LAN MVP testing.
- Security/authentication must be implemented before committing or using the server outside the trusted local development setup.
- AgentDriver SDK should be modified first to support session-level Codex instructions before building `:klaf-server`.
- A small shared Klaf app context may use Codex `baseInstructions` if useful; feature-specific rules should use `developerInstructions`.
- AgentDriver SDK behavior config should expose explicit `baseInstructions` and `developerInstructions` fields.
- AgentDriver SDK should remove `systemInstruction` and stop wrapping every prompt with `<system_instruction>`.
- Codex provider should send `baseInstructions` and `developerInstructions` through `thread/start`.
- Codex instruction fields should be optional nullable config; unset values should be omitted from `thread/start`.
- Keep the current `Assistant(AssistantType.Codex) { ... }` construction style while exposing provider-specific builder options.
- Add AgentDriver SDK tests for Codex `thread/start` instruction fields.
- Publish AgentDriver SDK to local Maven after SDK changes; `:klaf-server` should depend on that artifact.
- `:klaf-server` will be JVM-only.
- `:klaf-server` will include a Compose Multiplatform desktop UI window.
- Server startup will be automatic on desktop app launch.
- Initial UI will be minimal: icon/text only, without an in-app log panel.
- Logs should go to console/output and show server/session state plus request activity.
- Server logs should use the existing Klaf app logging library/style, not raw `println`.
- `:klaf-server-contract` will be Kotlin Multiplatform.
- WebSocket messages will use JSON with Kotlinx Serialization.
- A shared `:klaf-server-contract` module will hold serializable protocol DTOs.
- `:klaf-server-contract` will be a Kotlin Multiplatform module.
- Protocol DTOs will stay separate from pure domain entities.
- The Klaf protocol will send command names plus business inputs, not full AgentDriver prompts.
- The first WebSocket command will be `wordInsights.generate`.
- MVP text-generation features will preserve the current one-request/one-final-response behavior.
- Feature failures will be returned as structured per-request WebSocket errors.
- Logs should use short summaries by default; raw assistant responses only in explicit debug mode.
- Internal AgentDriver session logs should include observable state changes and connected session
  details, not only "started" messages.
- All current Klaf Server internal Codex sessions should use one shared default model/effort config:
  `gpt-5.5` and `low`.
- Do not add an in-app log panel for MVP.
- The current implementation/debugging phase will run the Klaf server on this same development computer and should support physical Android device testing over local LAN/hotspot.
- Do not create Git commits during implementation.
- Server bind host and port will be configurable through the local ignored server `SecretConstants.kt`.
- The MVP WebSocket path will be `/ws`.
- MVP local/LAN testing will use plain non-TLS `ws://`.
- AgentDriver's external plugin and JVM/root metadata were published locally
  under the user's explicit authorization; no SDK source was modified by this audit.
- `:klaf-server` currently compiles against locally published `0.10.12-SNAPSHOT`.
- Android/Desktop Word Insights and mnemonic text/image now use the Klaf server path.
- Direct AgentDriver client/server interaction has been removed from the Android/Desktop app feature path.
- The domain/UI connection concept has been renamed from AgentDriver naming to Klaf Server naming:
  `KlafServerConnectionState`, `IKlafServerConnectionManager`,
  `ObserveKlafServerConnectionStateUseCase`, and `RetryKlafServerConnectionUseCase`.
- Drawer readiness must reflect the Klaf Server WebSocket state, not internal AgentDriver readiness.
  The client is considered ready only after the server sends `server.ready`.
- The Room `8 -> 7` migration exists only because the local development device may already have
  opened the stashed Vocabulary Source schema. When Vocabulary Source is restored, schema version
  `8` and the normal `7 -> 8` migration should come back with that feature.
- The current implementation keeps mnemonic text and mnemonic image in one internal Codex session,
  but this should be revisited because text rules and image rules can conflict.
- No Git commits should be created by Codex during this implementation phase.

## Review Findings Rechecked

- Connection cleanup lock inversion and clean-close pending-request hangs are
  fixed and covered by loopback lifecycle regression tests.
- Mnemonic text/image still share an internal session, now with both text and
  image contracts in its developer instructions. Alternating real provider turns
  were not tested here; the old text-only-instruction finding is not proof of a
  current defect. This pre-existing policy was not changed by the audit.
- AgentDriver docs were updated for the new `baseInstructions` / `developerInstructions` API
  in the main architecture notes and Ralph planning docs.

## Next

- Address the audio implementation gaps listed in `audio-transcription-status.md`
  without including the user-deferred killed-process recovery in the current scope.
- Run manual end-to-end checks:
  - set Klaf server bind host to the computer LAN/hotspot IP;
  - set client `SecretConstants.KlafServer.HOST` to the same IP;
  - start `:klaf-server`;
  - open the app and request Word Insights from the screen;
  - generate mnemonic association text;
  - generate mnemonic image.
- Keep AgentDriver naming only in `:klaf-server` internals and documentation when referring to the actual SDK/assistant bridge.
