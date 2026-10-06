# Klaf Server Authentication Implementation Plan

## Platform validation scheduling (2026-10-06)

The operator is continuing on Windows and has deferred Ubuntu-native Klaf
Server/runtime/isolation verification until a future explicit request. Do
not treat WSL Ubuntu SDK test passes as validation of the target Ubuntu
machine, and do not silently advance to Ubuntu setup or deployment. Keep
Ubuntu verification as a release gate before claiming Linux support or
migrating the server there; remind the operator at decisions that rely on
that claim. Windows work may continue without waiting for Ubuntu.

## Resolved sign-in failure diagnosis (2026-10-06)

The restored test Desktop device received HTTP 500 (`SERVER`) during sign-in.
Redacted diagnostics located the failure at `EnrollmentProofVerifier.verify`:
the test server restart omitted `KLAF_PUBLIC_ORIGIN`. A startup-validation
regression now prevents the server from appearing ready without its DPoP
origin. The disposable server was restarted with
`http://127.0.0.1:8090`; sign-in and later live revoke checks succeeded.
Do not print or request credential values.

## Active cutover (2026-10-04)

Before end-to-end Windows verification, add and test an explicit disposable
server storage override. The current default registry is an older schema, so
do not start the new auth server against it during this check. Launch with the
dedicated Codex owner settings, then verify account enrollment, AI feature
parity, cross-device concurrency, revocation, and finally Ubuntu.

Implement the already-approved unified account runtime for every AI feature,
test-first. Wire the credential-free AgentDriver worker and owner token broker
to authenticated account routing; use one persistent worker per account and a
fresh thread for each independent text/image request. Route offline speech
recognition through that account-owned SDK runtime, remove SIWC text and the
one-shot image worker from production wiring, and preserve request-scoped
cancellation and account block/shutdown lifecycle. Verify text/image/audio
feature parity, live concurrent turns, secret isolation, and Windows then
Ubuntu behavior before calling the cutover complete.
The 2026-10-05 Windows SDK audit now passes 267/267 tests. The
transport-loss/resume integration fixture uses a deterministic TCP reset
proxy, rather than reflection, and verifies reuse of the same provider
session. The Windows junction cleanup data-loss bug is fixed and
regression-tested. A checksum-verified temporary JDK 21 was supplied for WSL
Ubuntu; the focused native Linux SDK run passed 11/11 auth, protected-storage,
and TCP-resume tests, followed by a full WSL Ubuntu SDK suite pass of 267/267.
Production Ubuntu integration is still unverified. Resolve the
public-contract status of the in-process `chatgptAuthTokens` refresh callback
before treating synthetic 401 recovery as production auth approval.
The runtime requires `KLAF_CODEX_OWNER_HOME`, `KLAF_CODEX_OWNER_EXE`, and
`KLAF_CODEX_OWNER_ACCOUNT_ID` as explicit server-process environment settings.
No desktop Codex-home fallback is allowed. The owner process starts lazily on
the first AI request, and account workers remain alive until account block or
server shutdown.
The opt-in `ManagedAccountCodexRuntimeLiveSmokeTest` uses
`KLAF_CODEX_RUNTIME_LIVE_SMOKE=true` plus those owner settings to exercise
concurrent text and image requests through the production factory. Ordinary
test runs skip it.

## Decision gate: managed-login token export (2026-10-04)

The user approved the owner-only private-file adapter after the distinction
between the owner `CODEX_HOME` and per-account worker homes was clarified.
Implement it test-first with synthetic `auth.json`, a dedicated explicit owner
home, read-only extraction, account/schema/permission checks and redacted
failures. Codex remains the only credential writer/refresh actor. Do not
inspect the current desktop login file, run a real forced refresh, or switch
production AI traffic during the synthetic phase.
The synthetic reader, refresh protocol client, file-backed owner and dedicated
stdio transport pass focused tests. The installed CLI accepts a separate owner
home, and an opt-in end-to-end refresh passed after the operator prepared its
login. Keep worker homes free of credentials and preserve the current AI path
until the remaining lifecycle, isolation and feature-parity gates pass.

The empty-home CLI smoke is complete on Windows: a persistent native app-server
returned `account: null` from `account/read` under a fresh `CODEX_HOME` and did
not inherit the desktop login. Timeout cancellation, notification flooding and
process-tree/SQLite cleanup discovered by this smoke have regression coverage
or bounded cleanup. This does **not** validate the private `auth.json` format,
real refresh, worker broker handoff, or feature parity. The next live gate needs
a separately prepared owner login, never the active desktop profile.

The operator completed a browser login in the dedicated owner home.
An opt-in Windows live smoke confirmed read-only private-file schema checks,
ChatGPT account metadata, a forced managed refresh that changed the access
token, and stable account binding; no AI thread or production traffic was
involved. The next gate is a credential-free account worker using the broker
with that owner, followed by 401/restart/isolation and feature parity tests.

The first credential-free broker worker gate passed on Windows: an opt-in
`AccountScoped` AgentDriver worker generated text using the dedicated owner
token, and its temporary profile contained no `auth.json`. This was one
executed request, not a production cutover. Next implement a long-lived
per-Klaf-account runtime factory and lifecycle behind the existing account
routing, with TDD for concurrent devices, separate accounts, logout/revocation,
failure/cancellation, and restart. Then verify multiple live workers, 401
renewal, image/audio capabilities, and Ubuntu before replacing SIWC routing.

Current TDD slice: the runtime pool must register each in-flight request before
dispatch, cancel only the target account's requests on account blocking, and
dispose that process exactly once without affecting another account. A normal
device-session logout is handled by the existing operation registry and must
not shut down the shared account process. A failed request leaves the account
runtime reusable; a failed factory must not cache a broken runtime. Server
shutdown closes all runtimes. Production wiring follows these lifecycle tests.
The pool lifecycle suite now passes (10 executed tests). Before a production
factory/cutover, account for the process-wide image capability: current
`AccountScoped` SDK flags turn on image generation for all threads in the
account process, while no verified per-thread deny mechanism is available.
The user confirmed one account worker shared by all devices and features,
and accepts image generation within it for this personal MVP. Do not treat
text-only routing in Klaf as a model-level tool restriction. Continue TDD on
the long-lived worker, account isolation, per-request threads, feature parity
and authorization; do not add feature/device worker processes.

Concurrency decision: independent requests for one account must overlap in
separate ephemeral Codex threads of its one worker. No feature-specific
queue. TDD must first demonstrate overlap, request-scoped cancellation and
event/result demultiplexing with one fake app-server process, then verify
the live Codex behavior. The fake-process gates now pass for text/text,
text/image, cancellation, tool denial, and late thread-start cleanup;
`AccountScoped` uses request-scoped thread IDs and event channels while
other modes retain their existing single-request contract. Klaf's pool now
has bounded account-level admission. Next verify live overlap and wire a
persistent credential-free account runtime; keep production SIWC routing
until the new path and all feature tests pass.

The public app-server account API refreshes managed ChatGPT auth but does not
export access tokens. The private-file adapter is a deliberate compatibility
decision, not a supported token-export API. Confine reads to the trusted
owner, verify schema/account binding and storage permissions, keep all workers
credential-free, and plan for format changes. An official host-owned SIWC
source would be a different login architecture and is not the current target.

## Current execution checkpoint (2026-10-04)

The synthetic SDK-to-Klaf broker bridge is implemented and focused-tested:
public host-side token provider, redacted token wrapper, locally published SDK,
and per-worker Klaf adapter to one serialized owner coordinator. Next: design
and TDD a trusted single-writer managed-login owner/token source; prove
restart, cancellation, 401, account mismatch and no-secret logging. Do not
switch production AI traffic until a supported source and live Windows/WSL2
check succeed. The SDK publication is local only; no commit was made.
Official SIWC-to-app-server documentation offers a host-owned OAuth token
source, but it is a different login route and must not replace the approved
managed Codex login without an explicit architecture decision. The current
experimental broker wire protocol alone is not a complete auth solution.

## Current feasibility spike (approved 2026-10-04)

Test one writable owner of the operator's Codex managed login and read-only
per-account workers. First define synthetic tests for ownership, stale-token
and 401 behavior, concurrent workers, restart/rebind, and cancellation; then
inspect the installed Codex protocol/launch path for a supported way to keep
worker refresh from rotating the shared credential. Do not read/copy real
tokens or force refresh of the real operator login. Record actual evidence and
stop if this boundary cannot be guaranteed. Production remains on SIWC.

Result: stop at this gate. A read-only worker can still invoke Codex's
managed-login refresh against the auth server before a failed local write;
we found no supported worker-side switch that guarantees refresh is disabled.
Synthetic coordinator tests cannot prove that internal behavior safe. Do not
implement or production-wire this credential-sharing scheme. Seek a design
choice before further TDD: one managed-login Codex process with account
isolation enforced above/below its thread boundary, versus the experimental
app-server external-token mode with a trusted token broker. Neither option is
approved by the current requirements yet.

The user accepted investigation of the external-token alternative. Next:
RED tests for a broker-backed account worker with no mounted auth file,
strict account matching, one serialized owner refresh for concurrent 401s,
bounded failures/cancellation and no token exposure in diagnostics. Implement
only the minimal SDK broker transport after RED; then design the single owner
without reading or changing the real credential during synthetic tests.
Production cutover and real-login refresh remain separate decisions.

The first synthetic worker-boundary and Klaf coordinator RED/GREEN cycles are
complete. Next connect the SDK's currently internal broker contract to Klaf
without exporting credential material through public client-facing APIs;
implement a single trusted managed-login owner and credential source, then
test restart, cancellation and real Windows/WSL/Ubuntu behavior. The owner
must never use a worker's account directory for credential storage. Do not
read or force-refresh the operator's real login until the source and safety
checks are complete.

## Unified Codex target (decision 2026-10-03; not implemented)

First vertical slice (in progress, TDD): route Word Insights through an
account-scoped Codex runtime. RED tests must prove that the authenticated
account ID, not a client-supplied ID or device session ID, selects the runtime;
two devices on one account reuse it, two accounts never do; each request gets
fresh context and a failure/cancellation cannot leak a prior result. Then
implement the minimum transport and rerun focused plus WebSocket tests. Keep
the existing SIWC production route intact until the Codex slice is genuinely
verified. Do not claim the other AI features or the security feasibility gates
are complete based on this slice.

The server-side account identity routing and a test-only runtime pool are now
implemented and focused-tested. The next implementation gate is a secure SDK
runtime API: per-account protected provider profile and long-lived app-server
process, fresh ephemeral thread per request, explicit unsubscribe and bounded
thread/process lifetime. Existing AgentDriver Assistant sessions share the
Codex provider profile and bind one thread to one process, so they cannot be
plugged into the pool as-is. The user approved sibling AgentDriver SDK changes
on 2026-10-03. Implement those changes by TDD; production DI must remain on
the proven SIWC route until the SDK contract, credential renewal and sandbox
checks pass.

The user rejected the SIWC-direct-text plus separate Codex-image split. The
target is one operator-managed Codex ChatGPT login and a Codex app-server path
for every AI feature, including Word Insights, mnemonic text/image, Vocabulary
Source text analysis and audio transcription. The trusted Klaf Server JVM
manages exactly one isolated Codex app-server OS process/private runtime per
AI-enabled Klaf account, shared by that account's devices but never by another
account. Each independent request gets a fresh ephemeral thread; keep device
and auth-session ownership for cancellation/result delivery. The current
SIWC text code and per-image-request process remain until their replacements
pass TDD and security checks; do not claim this target already works.

First prove with tests that one account process can enforce different
per-request capabilities without exposing shell/files/other accounts, and that
the single Codex login can be used and renewed safely across separate account
profiles. Preserve the existing feature outputs and account authorization,
unsubscribe finished threads, bound memory/temp artifacts, and stop idle or
revoked account processes without affecting other accounts. If the one-process
constraint conflicts with safe tool isolation, raise that design blocker to
the user rather than silently spawning feature processes or restoring SIWC.
Retire the direct Responses/SIWC AI route only after feature parity and
Windows/Ubuntu/physical-device verification; no API-key fallback or commit.

## Current implementation checkpoint (2026-10-03)

This checkpoint records code before the Unified Codex target above. The
historical gates below describe the order of work, not the new target.
Authenticated `/ws` is now enabled for an active account/device session
with an explicit AI grant; per-frame/periodic revalidation, revocation,
owner-scoped operations/uploads/push, and signed desktop/Android WebSocket
upgrade are implemented and covered by focused tests. SIWC direct Responses
handles the three text features; mnemonic images remain on the separately
sandboxed, ChatGPT-login Codex `ImageOnly` worker. A controlled signed
WebSocket test returned a real PNG, and the full Klaf server/desktop data
tests plus Android DI compile passed at the prior checkpoint. The latest
multi-account push fixes are undergoing a full rerun. Ubuntu-native isolation
and physical-device end-to-end verification remain release gates. See the
status diary for the precise validation record. Do not commit yet.

## Historical SIWC checkpoint (2026-10-02; superseded as target)

The user first narrowed work to synthetic tests for Sign in with ChatGPT
(SIWC), then authorized implementation. The login/refresh core, protected
credential store, signed ID-token verification, exact redirect-URI binding and
public-client HTTP token transport now pass synthetic tests; the full server
test suite passed on Windows at an earlier checkpoint. The loopback callback/
operator login CLI and retained-ID-token reauthorization path are now implemented
and synthetic-tested; live operator login succeeded on 2026-10-03 after clock
correction. Next: resolve the
intermittent full-suite `INVALID_CREDENTIALS` failure (second full run passed), validate live provider
token renewal with redacted local checks, then implement actual provider runtime
integration and verified Windows/Ubuntu process isolation. Keep AI `/ws`
disabled until the provider and isolation gates pass. Do not commit without
separate permission.

The documented SIWC preview currently excludes native image generation and
audio transcription. Treat these as explicit feature capability gaps rather
than allowing a text-only provider check to declare the entire AI integration
complete. A trusted local readiness check may verify stored grant/refresh
without exposing credentials or enabling the AI route.

User confirmed on 2026-10-03 that mnemonic image generation is a hard
compatibility requirement. Investigate whether the original Codex `image_gen`
path can be kept behind verified tool/profile isolation while adopting new
account authorization; test the actual image event/bytes on the selected
authentication route. Do not silently substitute SIWC text capability for
image capability or use a separately billed API key.

Selected image compatibility direction (2026-10-03): use two explicit provider
routes behind the same authenticated Klaf account boundary. SIWC serves only
the capabilities its documented preview supports; mnemonic images continue
through the existing ChatGPT-authenticated Codex app-server image tool. The
image worker is single-purpose: disable shell/computer/browser/apps/plugins/
multi-agent tools, use a read-only ephemeral thread, and run Codex under an
OS sandbox with a temporary `CODEX_HOME` plus only a read-only bind of its
operator-managed `auth.json`. The profile's image/history/state writes stay
in that temporary mount and do not persist across workers. The server accepts
only a completed `imageGeneration` item with validated PNG bytes, never a
textual claim that generation succeeded. The image route must fail closed if
its dedicated login, sandbox, or capability probe is unavailable. Local SDK
and Klaf wiring now exists and a real PNG passed controlled Windows/WSL2
bubblewrap tests. The SDK rejects non-PNG signatures and image-only use of
the experimental external-token broker. This is not yet a deployable Klaf AI
integration: keep AI `/ws` disabled pending SIWC text-provider wiring,
per-account/device/session request ownership and revocation, Ubuntu-native
verification and authenticated end-to-end tests.

Text-route security decision (2026-10-03): prefer the documented direct SIWC
Responses API from trusted Klaf server code, with `store=false` and
`stream=true`, over handing an access token to a Codex child process. The
current bubblewrap `/proc` layout exposed a synthetic parent environment
token to a child process, so merely removing the token from tool environment
is insufficient. The text requests are self-contained and do not require
Codex local tools or persistent provider history. This route still needs TDD,
protected refresh/cancellation and feature integration. A Windows operator
probe has now completed both plain-text and strict JSON-schema inference with
the protected SIWC grant and selected model. Keep image generation on the
separate operator Codex login path;
do not send SIWC tokens to that worker.

## State and authorization

Design recorded on 2026-09-29. Product decisions are in
[requirements](klaf-server-requirements.md); work history and verification are in
[status](klaf-server-status.md). This plan covers the authentication/security
follow-up, not a rewrite of completed storage/synchronization features.

The user delegated technical choices and approved the full TDD/implementation
flow after the design pause. Proceed through tests, implementation and checks
without routine approval pauses. Stop only for a material new product decision,
an external prerequisite or a safety blocker. Do not commit without separate
permission; do not reset live accounts or modify original backups.

Below, **selected** means a delegated engineering decision; **verification gate**
means it has not been demonstrated by the current implementation. Neither means
the feature is implemented. No additional product interview is needed to begin
the planned technical checks once continuation is authorized.

## Existing integration points

- `ServerAccountRegistryDatabase` stores account-to-content-database mappings;
  `ServerAccountStorage` owns the registry and per-account Room databases.
  Extend these boundaries rather than introducing another account registry.
- `AccountRoutes` currently accepts email/device identifiers without passwords
  or tokens. Old sign-up/sign-in/device-registration routes must not remain a
  passwordless bypass after the new authentication protocol is enabled.
- `KlafServer` installs account/sync/image routes, AI `/ws` and `/sync-events`.
  Each account-owned entry point needs authentication and authorization.
- `KlafServerOperationRegistry` currently deduplicates by caller request ID;
  feature sessions are server-wide. Both need verified owner namespaces.
- `KlafServerSession` currently resends pending AI requests after reconnect.
  Resume must distinguish a transient disconnect from a server restart.
- Client account session/REST client/authentication view model currently expose
  the passwordless MVP flow. Reuse screens/themes where possible; add password,
  pending-approval and secure-store/error states without redesigning unrelated UI.
- Existing offline account databases, manual data sync, conflict policies and
  recoverable guest-data transfer remain the base behavior.

## Selected libraries and primitives

Candidate pins verified in public Maven metadata; build/transitive/native
compatibility is not verified. Do not change them automatically to dynamic versions.

| Purpose | Selection | Boundary |
| --- | --- | --- |
| Password hashing | Bouncy Castle `bcprov-jdk18on:1.86`, Argon2id | JVM server; pure Java rather than an Argon2 JNI dependency |
| Access JWT | Existing Ktor `3.5.1` authentication/JWT modules, Auth0 `java-jwt:4.6.1`, ES256 | Stable API; compose with device-proof validation |
| DPoP/JWK | Nimbus `nimbus-jose-jwt:10.10` | JVM verifier/encoder; Android uses platform key signing |
| OS integration | JNA and `jna-platform:5.19.1` | Windows DPAPI and a narrow Ubuntu libsecret binding |
| Randomness/encryption | JDK/Android SecureRandom, AES-256-GCM | Platform primitives, unique random nonces |
| Persistence/tests | Existing Room Multiplatform, bundled SQLite, Ktor and test infrastructure | No JPA, no unrelated framework migration |

Keep common DTOs/errors in the existing contract boundary and platform crypto
behind interfaces. Do not put JVM-only libraries into common/native source sets.
DPoP is not implemented by installing the JWT plugin: its scheme/header handling,
proof verification and persistent session check require a composed provider.
Inspect resolved dependency compatibility and advisories before adding pins in
the later implementation phase. Version availability is not a security audit.

### Password processing and guessing protection

- Selected account policy: 15–128 Unicode code points, no whitespace, including
  tabs/newlines/non-breaking spaces; reject malformed text/NUL and oversized
  input. Match client/server validation. Do not trim, normalize, truncate or
  require digits/symbols; different exact Unicode sequences are different passwords.
- Argon2id version 19, initially 64 MiB, three passes, parallelism one, independent
  16-byte random salt and 32-byte output. Persist algorithm/version/parameters/salt
  and hash; use vetted primitives and constant-time comparison. Benchmark before
  tuning; never silently reduce work parameters to make a test pass.
- Server hashing has two worker slots and a bounded queue of eight; reserve
  capacity for a proven approved device. Unknown-account password attempts use
  a fixed dummy hash with equivalent work and the same invalid-credentials error.
- Initial persistent throttling: five failures per normalized account/IP lane
  in ten minutes triggers a 30-second cooldown, doubling consecutive cooldowns
  up to 15 minutes. Twenty failures per account in ten minutes also pauses
  unrecognized-device attempts for 60 seconds. Proven approved-device attempts
  use their own account/key/IP lane, not the unrecognized-device cooldown.
- Apply source/global admission limits before expensive hashing; return bounded
  `Retry-After` with a specific error, never revoke sessions from rate limiting.
  Treat these thresholds as configurable engineering defaults. Test restart,
  clock changes, successful-login reset and an attacker trying to throttle a
  legitimate device. No promise of complete availability under distributed attack.

## Storage and key lifecycle

### Platform protection

- Windows server/Desktop: current-user DPAPI through JNA, no machine-wide flag,
  no additional Klaf passphrase. Persist only DPAPI-protected credential bundles
  in application-private files. Do not depend on the deprecated DPAPI prompt flow.
- Ubuntu server/Desktop: installed libsecret/Secret Service, persistent collection
  in the interactive desktop session. Store a randomly generated application
  wrapping key there; AES-GCM protects local bundles with purpose/installation/
  profile/version in authenticated metadata. Use non-varargs libsecret APIs and
  cancellable calls off the UI thread. No plaintext CLI/argv/environment fallback.
- Android: installation P-256 private key in Android Keystore, plus a separate
  Keystore AES key for credential bundles. Hardware backing is not assumed.
- Shared protection interface distinguishes missing, locked/unavailable, denied
  and corrupt state. Missing on first launch differs from lost material for an
  existing installation. Atomic replacement, restrictive ACL/modes, symlink
  rejection and crash-safe journals are required. Keep server/client stores
  separately scoped; OS protection does not replace AI-tool isolation.
- Server private bundle contains the JWT signing key and an independent recovery
  receipt encryption key. Room stores public key ID/fingerprint, installation ID
  and authentication epoch, never the plaintext private bundle/wrapping key.
- Desktop credentials/device private key remain OS-protected, not DataStore or
  ordinary Room fields. Token bundles are keyed by server origin/account/session.
  A single installation key can bind distinct account sessions; it is not an
  account-wide token. Serialize credential updates across app processes.

### Startup, recovery and failure

- First launch provisions the protected bundle before accepting clients. Use a
  durable provisioning journal and public metadata to reconcile a crash between
  secure-file persistence and Room metadata, without replacing an existing key.
- Server store failure blocks client listeners; UI reports OS unlock/Retry where
  supported. AI sandbox failure instead disables only AI when auth/data are healthy.
- Desktop store failure retains the selected offline profile/data and disables
  networking. No automatic guest switch, logout, key replacement or deletion.
- Lost-key recovery requires one explicit operator confirmation. Journal the
  target key ID/epoch, persist one replacement bundle, then transactionally bump
  the epoch, revoke all old sessions and invalidate refresh/recovery material.
  Keep listeners blocked until reconciliation completes. Resume interrupted
  recovery rather than regenerate keys/reopen access halfway through it.
- Current offline operator entry point is the explicit
  `--recover-lost-signing-key` launch argument. A protected staged bundle is
  the recovery journal. Recovery is refused while the ordinary key exists;
  ordinary startup can complete only a registry-committed staged replacement.
  Manual operator UX and cross-process crash testing remain validation gates.
- Retain passwords, approvals and account content. Keyring lock/corruption is not
  permission for an automatic reset. Routine planned key rotation/headless
  startup are not new requirements in this stage.

## Room authentication model and revocation ordering

Extend the existing registry through an additive, tested migration. Keep legacy
content intact; passwordless accounts cannot authenticate under the new protocol.
No automatic legacy enrollment or current test-account deletion.

| Record | Required state/invariants |
| --- | --- |
| Account | Immutable opaque account ID, normalized unique email, password hash parameters, approval/block state, content DB mapping |
| Device authorization | Account/device identity, public JWK/thumbprint, approval/revocation state; UUID/name/platform alone grants nothing |
| Enrollment | Random request ID, new-account vs existing-account provenance, requesting key/device, status, expiry; bounded seven-day pending lifetime |
| Authentication session | Random signed `sid`, account/device/key ownership, epoch, validity/revocation and refresh inactivity deadline |
| Refresh generation | Digest of high-entropy credential, generation and consumed/active state; used-token evidence retained through the family validity period |
| Recovery receipt | Session/generation/request/body-digest/key ownership, expiry, encrypted already-issued result; no ordinary response cache |
| Password reset | Account, random-token digest, expiry/consumption; one active reset token per account |
| Proof/admission state | Replay IDs/nonces and bounded throttle counters with indexed expiry/cleanup |
| Server metadata | Installation/key fingerprint, epoch and provisioning/recovery journal state |

Password reset, refresh rotation and account/device/session revocation use registry
transactions and uniqueness/compare-and-set constraints. No orphan replacement
credential or partially reset account may become active.

Registry and per-account content are different databases. Use a fixed-order
owner authorization barrier shared by revocation and content commits: recheck
access under the barrier and hold it through the content transaction. Account/
device revocation covers all affected sessions. Do not hold a registry SQL
transaction while waiting for a content transaction. A write committed before
the revocation barrier is completed work; a later write must fail. Recheck owner
validity before publishing AI results or issuing an image/file access response.

## Public protocol and ownership

### Enrollment, login and reset

- Proposed contract namespace `/api/v1/auth`: registration submission, sign-in,
  enrollment status/completion, refresh, logout and password reset. Names are
  internal design choices, not a compatibility obligation for passwordless routes.
- Registration supplies email/password/device metadata and key proof; save only
  the hash at submission, require account and first-device approval. Existing
  account sign-in verifies the password before creating an unapproved-device
  request. Neither pending request nor approval issues data/AI credentials.
- Pending status requires the random enrollment ID and fresh proof from its
  requesting key; expose only that request's status. Poll only while the account
  UI is active, with bounded backoff. Never retain the password while waiting.
- Explicit completion requires password re-entry, current approvals and the
  original device proof. Issue a new session. A password login replaces prior
  active login sessions for that account/installation, not other devices.
- Enrollment provenance controls guest transfer: successful new sign-up uses
  the existing recoverable transfer checkpoint, including waiting-period edits;
  existing-account sign-in never transfers guest data. Approval observation
  does not switch profiles or synchronize automatically.
- Operator approval/block/device revoke/reset-token issuance are trusted local
  server UI actions, not publicly exposed administration routes. Show account/
  device names in the server's existing light/dark theme system.
- Reset tokens: 32 random bytes, URL-safe encoding, 30-minute expiry, digest-only
  persistence. Issuing a replacement invalidates an earlier reset token but not
  login sessions. Successful reset atomically changes the hash, consumes reset
  tokens and revokes all account sessions/jobs; return no login credentials.
  Current implementation uses local operator issuance and
  `POST /api/v1/auth/password/reset` with fresh DPoP proof. Room stores one
  digest per account; the client requires a normal sign-in after reset. AI-job
  cancellation remains a separate incomplete gate while AI `/ws` is disabled.

### JWT and device proof

- ES256 only; validate signature/key ID, issuer, audience, immutable account
  subject, `sid`, device, issued/expiry times, epoch and key thumbprint `cnf.jkt`.
  Access lifetime remains 15 minutes. No password/email/refresh secret in JWTs.
- Use RFC 9449 proofs with P-256/ES256, `typ`, public JWK, `htm`, canonical `htu`,
  `iat`, unique `jti`, nonce and `ath` for resource access. Reject private/remote
  JWK material and unsupported algorithms. Bind both token types to the key.
- Initial proof window: age at most 60 seconds, future skew at most 30 seconds;
  persisted replay entries live at least 120 seconds. Nonces are short-lived
  (two minutes), purpose/key/epoch-bound; every retry creates a fresh proof ID.
- Authorization scheme is `DPoP`, not an accepted bearer-token downgrade.
  Claim/session validation and account/device approval run on every protected
  request. Verify proof before a reused refresh credential can trigger revocation.
- Configure a trusted public HTTPS origin for proof URI checks behind Cloudflare;
  do not derive identity from arbitrary forwarded headers. Query/body ownership
  still needs explicit checks: DPoP is not a signature over the whole request body.
- TLS validation remains enabled. The sole plaintext exception is same-host
  cloudflared-to-loopback origin; automated local fixtures do not authorize a
  plaintext client-network production mode. Never log credential/proof headers.

### Refresh rotation and response-loss recovery

1. Client persists a protected pending-renewal journal before sending: old
   refresh credential, random operation ID and canonical request parameters.
   One renewal at a time per session, including competing REST/WS/app processes.
2. Server verifies fresh device proof and the old 32-byte opaque refresh secret
   using its digest. Within one Room transaction, consume the current generation,
   activate a new pair and its 30-day inactivity deadline, and persist the
   AES-GCM-encrypted receipt with owner/generation/request/body bindings.
3. On response loss, repeat the same operation with the same old secret and a
   fresh proof. Selected limits: 120 seconds after commit and three automatic
   client recovery attempts; server responses never rerotate or extend deadlines.
4. Recheck access/epoch, exact request binding and whether the receipt's generation
   is still current before returning that exact pair. A stale receipt must not
   overwrite newer client credentials. A matching but expired/stale recovery
   returns sign-in-required, not a false compromise accusation.
5. A consumed secret with a different renewal operation, presented with valid
   bound-device proof, revokes that family/session. An invalid/unbound proof must
   not let a token-only attacker revoke another device's valid login.
6. Client atomically persists the returned pair and clears the journal. A storage
   failure preserves recovery state and offline data; do not retry a new rotation
   with an uncertain old token. Restore/restart tests cover both sides of commit.

Use `Cache-Control: no-store` for credential responses. Encrypt receipt payloads
with independent random GCM nonces and owner/purpose/epoch/generation authenticated
metadata. Request IDs alone never retrieve credentials. Prune receipts after
their bounded lifetime without discarding still-required reuse evidence.

## REST, WebSockets, images and AI lifecycle

- Route principals, not request emails, select account storage. Cover sync,
  device-management and image upload/download as well as authentication routes.
  Image network loaders must attach per-request credentials/proof; never use
  tokens in image URLs. Cache private images per local profile, enforce safe paths
  and reject cross-account filenames/references.
- Authenticate both WebSocket handshakes with headers, not query tokens. Keep
  server-owned account/device/session on the connection. Check session validity
  and access expiry before business messages, binary uploads and result delivery.
- Before access expiry, renew via REST and reconnect authenticated WebSockets;
  do not invent a second token-renewal protocol inside socket messages. Surviving
  work stays attached to the same authentication session through this reconnect.
  Access expiry alone does not cancel a valid session's job; revocation does.
- Namespace operation IDs, events/push registrations, uploads and deduplicated
  results by verified session; reject repeated IDs with different type/body.
  Caller request prefixes are correlation data, not an authorization mechanism.
- AI contexts are keyed by account/device/authentication session/feature/server
  run. Mnemonic text/image may share their own functional context, never another
  owner's. Serialize turns where the provider requires it; no global feature history.
- New server-run ID plus a resume/status contract distinguishes surviving jobs
  from lost/evicted requests. Reconnect resumes existing work, never implicitly
  executes a missing old request. Restart-lost requests end loading with an error;
  preserve the user's decision against new AI Retry mechanisms/buttons.
- Logout/revocation cancels target jobs and denies late results/artifact promotion.
  Copy completed selected artifacts to verified account storage before temporary
  context cleanup. Never undo saved results or terminate unaffected contexts.
- Offline logout remains immediate/local with best-effort online revocation and
  no queued guest-mode logout. No promise that an unreachable server can cancel
  a job immediately. Clear local token/renewal material, not unrelated profiles.

## SDK isolation selection and feasibility gate

Selected boundary: separate context-owned provider runtime/profile/history and
workspace; credentials managed only by trusted runtime/broker code; explicit
restricted agent-tool reads combined with outer OS enforcement. Directly mounting
the current shared provider profile into every agent workspace is rejected.
Keep ChatGPT subscription authentication; API-key billing is not an option.

Initial provider ownership is one operator-connected Codex/ChatGPT account
used only by that same human through explicitly linked Klaf accounts/devices.
Klaf account approval is not proof of provider ownership. Agreed MVP gate:
a local server-operator CLI lists approved accounts (email plus immutable ID),
grants/revokes AI eligibility by ID, and stores a default-deny provider-owner
link in the server registry. No remote grant API or UI for this slice. Test
unknown/pending/blocked accounts, persistence, idempotency and revocation
before implementation. The eventual AI request boundary must enforce this
grant as well as normal account/device/session authorization.
Implemented 2026-10-01: Room registry v10, local CLI list/grant/revoke,
and authenticated `/ws` grant check. The original account-block grant removal
is superseded. Implemented 2026-10-06: temporary Block preserves a
pre-existing grant while effective AI authorization remains denied; Restore
plus fresh sign-in resumes AI authorization without offline re-grant or
server restart. The operator CLI distinguishes a retained grant on a blocked
account from effective eligibility. Storage and live-server flow regression
tests were written before the fix and pass afterward. The route
still returns `AI_UNAVAILABLE` after a valid grant. Focused storage/CLI/v9→v10
migration and endpoint tests passed; the full server suite passed before a
follow-up focused rerun. Live provider integration remains a separate gate.
Keep provider identity/credentials separate from Klaf context ownership; each
account/device/auth-session/feature retains independent runtime and history.
Represent the provider association behind a replaceable interface so later
per-person sign-in does not require changing Klaf data ownership.

Feasibility update (2026-10-01): the user clarified that all initial AI-using
Klaf accounts are their own; this removes the multi-person account-sharing
scenario. It does not authorize the internal-only `chatgptAuthTokens` method.
Keep the synthetic AgentDriver broker uncalled. Evaluate the documented Sign
in with ChatGPT plan-usage flow for a personal locally run app and whether
the intended Cloudflare-accessible deployment is eligible; it may require
client registration and owner interaction. Do not publish/wire the prototype
or enable AI `/ws` before supported authentication and isolation pass. No
separately billed API-key solution is presumed acceptable.

- Canonical provider login belongs to trusted credential management. Do not copy
  OAuth refresh credentials into concurrently independently refreshing profiles,
  expose secrets through argv/environment, or assume `account/read` exports tokens.
- The external `chatgptAuthTokens` host-managed path was evaluated with
  synthetic fixtures, but its protocol definition now explicitly says
  internal-use-only. It is not a production integration candidate.
- The user approved implementing a trusted broker in AgentDriver (2026-10-01).
  A worker may receive only an access token, never the canonical refresh token.
  Add synthetic contract tests for initial login, refresh, context separation,
  failed renewal, and secret non-disclosure before production integration.
  The official app-server external-token mode supplies the worker protocol but
  does not export credentials from a normal Codex login: `account/read` is not
  a token source. Do not parse private `auth.json`/keyring formats as a silent
  fallback. A supported renewable token source remains a feasibility gate.
- Updated official OpenAI Docs document `CODEX_ACCESS_TOKEN`, custom-provider
  command-backed auth and Sign in with ChatGPT as potential supported inputs;
  none is yet proved safe with this SDK's nested sandbox, installed Codex build
  and one-server-account design. The normal managed `codex login` does not
  document a token-export API. Assess Sign in with ChatGPT eligibility for this
  private/local server and family/remote access before a real provider login or
  deployment: app-server managed auth is documented for local/open-source apps,
  not commercial or hosted services. Do not treat a Cloudflare tunnel as proof
  of eligibility or silently parse `auth.json`/keyring internals.
- Explicit first-phase test matrix: approved versus unapproved Klaf principal;
  two Klaf accounts sharing provider capacity but not memory/history/files;
  two devices/sessions of one account remaining distinct; logout/revoke/failed
  refresh cancelling only the affected owner where applicable; one expired
  provider access token renewed once by trusted code; no token in client
  response, agent tool environment, logs, argv, or another context home;
  shared rate-limit/provider outage yielding AI-only unavailability. Use
  synthetic credentials before any live sign-in check.
- Controlled WSL test with a new browser ChatGPT login and isolated GNOME
  Keyring disproved cross-home reuse: `profile-a` authenticated, while
  `profile-b` in the same keyring did not. Neither wrote `auth.json`. Do not
  depend on keyring storage alone or ask for a manual login per AI context.
  Continue evaluating a trusted broker that owns one renewable ChatGPT login
  and supplies only short-lived credentials to isolated context runtimes.
- Read-only-access restrictions must deny provider credentials/private profile,
  Klaf database/keys, foreign context directories and indirect symlink/process/
  environment access while retaining minimum platform/runtime reads. Check both
  thread and turn policy; deny escalation, unapproved MCP/plugins and commands
  executing outside the thread sandbox. Never expose `thread/shellCommand` to
  untrusted feature input as a way around the policy.
- Runtime feasibility update: installed and current test Codex CLI builds reject
  `sandboxPolicy.workspaceWrite.readOnlyAccess`; use a named custom permission
  profile with root deny, minimal runtime reads and scoped workspace access.
  Synthetic WSL tests deny direct/symlinked foreign files. Provider proxy
  credentials currently appear in sandboxed child environment by default;
  `shell_environment_policy.inherit="none"` hides them in a synthetic command,
  but parent `/proc` access and nested sandbox behavior must be tested before
  adoption. A custom profile alone does not satisfy per-context history or
  trusted provider-auth renewal requirements.
- Controlled WSL2 probe on 2026-10-03 confirmed the remaining `/proc` exposure:
  a child with `ACCESS_TOKEN` removed from its own environment could read a
  synthetic parent token from `/proc/1/environ` under the current bubblewrap
  `--proc /proc` layout. Do not put the real SIWC access token in that provider
  process environment until tool-child PID/environment separation is proven or
  a trusted request broker keeps the token entirely outside agent-visible
  processes. An empty `toolEnvironment` map is not a security boundary.
- Move trusted setup scripts/bridge control data out of agent-writable workspace.
  After configuring network rules, drop NET_ADMIN/NET_RAW from all capability
  sets, disable privilege reacquisition and test provider/child launch paths.
  Keep only approved provider networking; do not inherit host secret environment.
- Per-context work/history paths must not mount another context or canonical
  provider profile. Logout/revocation/server restart makes old histories
  inaccessible. Use a trusted owned-root manifest for deferred cleanup; do not
  touch preserved user images/data/provider-login setup. Idle resource eviction
  must not cross owners or replay interrupted generation.
- **Verification gate:** demonstrate provider authentication, automatic renewal,
  restricted reads and final capability/network behavior together on Windows/WSL2
  and Ubuntu. If the provider cannot meet this boundary, keep AI unavailable with
  a specific cause/Retry in server UI; account/sync may still work. No unsafe
  shared-profile/workspace-preset fallback. Broker scope is now approved, but
  the canonical renewable-token source and provider integration remain
  unproven. Synthetic protocol tests do not authorize enabling production AI.

## TDD sequence (authorized)

All entries pending. Start with contract/policy tests and a focused isolation
feasibility check; do not write the entire production security layer first.
Use injected clocks/randomness, fake storage only for logic tests and synthetic
credentials; never read real secrets/backups to build a test fixture.

1. Write password/approval/owner/session/proof/refresh/recovery/reset policy tests.
   Observe meaningful failures from missing behavior. Define narrow interfaces
   when necessary; compilation/setup failures alone are not a completed RED step.
2. Add real Room tests for migration, approval/revoke/reset transactions, replay
   uniqueness, concurrent rotation and crash/restart recovery. Preserve existing
   content and synchronization regression assertions.
3. Add Ktor integration tests for the full entry-point inventory: wrong/missing
   credentials/proof, cross-account access, unapproved devices, expired/revoked
   sessions, both sockets, uploads/images and absence of legacy bypasses.
4. Implement the tested server policies/storage/routes, then client credential
   single-flight/journals/account states. Add client state tests with fakes and
   integration tests for actual secure persistence where available.
5. Add actual SDK synthetic-secret and cross-context regression tests before its
   targeted fixes. Prove final bridged launch/capability restrictions and provider
   connectivity on supported OS paths; report skipped/unavailable checks plainly.
6. Run focused then full existing regression suites, inspect staged/untracked
   paths and secret/log leakage. Manual Android/Desktop acceptance follows;
   Ubuntu-native verification remains required even if Windows tests pass.

Live reconnect regression (2026-10-06): RED tests were added for sign-in
following failed/disconnected AI connection, account-switch teardown,
same-account reauthentication and the disabled mnemonic status. The AI
connection manager now observes account selection and sign-in epoch, reconnects
only after new credentials are active, and shows an actionable not-ready hint.
Focused and full data/presentation tests, Android build and Desktop compile
passed. Android and Desktop manual logout/sign-in without Retry were later
confirmed by the user. Visual verification of the disabled-state hint remains.

Live device-revoke regression (2026-10-06): a Desktop image job started before
operator revoke but completed and was delivered after the device and auth
sessions were marked revoked. Add a deterministic failing test for same-server
operator revoke during an in-flight AI job, implement targeted cancellation
and active AI socket closure without touching other devices. Check the sibling
operator account-block path for the same gap, then run focused
and broader server tests. Repeat the live check on the disposable account;
do not restore revoked credentials or count the existing live gate as passed.
The operator device-revoke and account-block RED/GREEN tests now pass. A
separate socket-close failure regression is also GREEN. The full server suite
passed after the final guard (440 tests; zero failures/errors, two skips).
The disposable server was restarted with the new binary and the required
public origin. A live Desktop image request was revoked in flight: the server
closed the correct device connection, Desktop failed the request, and no late
image delivery was observed. The Android device remained active. Further
cross-device and account-block live checks are separate follow-up gates.
The user then generated a text mnemonic on Android after Desktop revoke;
server log and user confirmation show it completed. Final server suite passed
(443 tests, zero failures/errors, two skips). The test Desktop stays revoked
until intentionally restored for later use.
The operator later restored the test Desktop, then live-tested account Block
during an in-flight Desktop image request. Both account AI sockets closed and
the Desktop request failed; account restore, AI re-grant and client re-sign-in
are still required before further live tests.

Minimum failure matrix: password boundaries/Unicode/oversize; duplicate enrollment
and lost responses; pending expiry/rejection; guest edits while awaiting approval;
account/device/session identity mismatch; forged/replayed/wrong-URI proofs; nonce/
clock skew and restart; access/refresh deadlines; parallel renewal/reuse vs legitimate
recovery; response loss before/after commit; storage failures before/after saving
tokens; stale receipts; reset expiry/reuse/concurrency; signing-key recovery crashes;
revocation racing content commit/upload/delivery/new login; cross-account image
cache; logout offline/online; both socket expiry/reconnects; server restart without
silent AI regeneration; sandbox failure without disabling healthy account/sync;
provider credential/foreign-history reads, symlinks, process access, escalation
and network rule changes. Existing saved data must survive all failure paths.

Lightweight view-model/state tests cover pending/error/offline/theme behavior;
do not introduce a heavyweight end-to-end UI suite by default. Track genuine
failures, environment limitations and exact test results in status, not estimates
that imply untested security is ready. Do not commit without separate permission.

## Primary design references

- [OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
  provides the Argon2id baseline; selected stronger starting costs still need a benchmark.
- [Bouncy Castle Argon2 generator](https://downloads.bouncycastle.org/java/docs/bcprov-jdk18on-javadoc/org/bouncycastle/crypto/generators/Argon2BytesGenerator.html),
  [Ktor JWT](https://ktor.io/docs/server-jwt.html),
  [Auth0 Java JWT](https://github.com/auth0/java-jwt) and
  [Nimbus JOSE](https://connect2id.com/products/nimbus-jose-jwt/download) document the selected library boundaries.
- [RFC 9449](https://www.rfc-editor.org/rfc/rfc9449.html) is the device-proof reference;
  Klaf's enrollment/recovery/API semantics remain explicitly application-specific.
- [Windows DPAPI](https://learn.microsoft.com/en-us/windows/win32/api/dpapi/nf-dpapi-cryptprotectdata)
  describes user-scoped protected data, not AI-tool access control.
- [libsecret](https://gnome.pages.gitlab.gnome.org/libsecret/) documents the selected
  Ubuntu backend; keyring presence/unlock behavior must be tested on the target desktop.
- [Codex App Server](https://learn.chatgpt.com/docs/app-server) documents restricted
  reads, out-of-sandbox shell commands and experimental host-managed ChatGPT
  authentication. It does not prove deployed SDK/provider compatibility.
