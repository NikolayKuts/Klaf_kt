# Klaf Server Requirements

This file is a temporary working document for the Klaf server implementation.
It should be removed or replaced by permanent documentation after the full implementation is finished.

## Linux Tunnel deployment (2026-10-08)

- Klaf Server and `cloudflared` run on the same Linux host. The server listens
  only on loopback HTTP; the Tunnel forwards to that local listener. Android
  and Desktop use HTTPS for REST and WSS for both WebSocket channels, with
  normal TLS certificate validation.
- The server's ignored local `SecretConstants.Server.PUBLIC_ORIGIN` is set to
  the external HTTPS origin used by clients. DPoP verification uses that
  origin, never the loopback listener or untrusted forwarded headers.
- Do not put the real hostname in tracked source, tests, documentation, commit
  messages, or Git history. Use placeholder domains in committed examples.
- Linux runtime and Tunnel reachability must be verified on the target host;
  Windows tests alone do not establish Linux readiness.
- Move existing Windows account content and images only after connectivity
  verification, as a separate operation. Retain Klaf data and sync history;
  Codex's internal thread history is out of scope. Cross-OS signing-key
  recovery must revoke old sessions and require fresh sign-in.

## Local public origin configuration (2026-10-07)

- Ordinary Android Studio Run must start the server without a separate
  `KLAF_PUBLIC_ORIGIN` environment variable when the configured bind host is
  `127.0.0.1` or `localhost`: derive `http://<bind-host>:<port>` for DPoP.
- The ignored local server `SecretConstants.kt` may explicitly set a
  `PUBLIC_ORIGIN` for the externally visible HTTPS address. Retain an
  explicit process environment/JVM-property override for controlled runs.
- Never infer a client origin from a non-loopback bind host (including
  `0.0.0.0`); require an explicit origin in that case. An explicitly supplied
  invalid origin must fail startup rather than silently fall back.

## Live verification storage safety (2026-10-06)

- Permit an explicit absolute server storage root for disposable live checks;
  otherwise retain the existing default storage location. A test run must not
  implicitly migrate or overwrite an existing server registry or the original
  Android backup. Reject a blank or relative explicit override.

## Unified Codex AI Runtime (2026-10-03)

- Complete the production cutover for all server AI features together: remove
  SIWC direct text routing and the per-image-request Codex process. Use one
  long-lived account-scoped AgentDriver/Codex runtime for text and image
  requests across that account's devices. Speech transcription retains the
  SDK's offline recognizer; route it through the same account-owned runtime
  and lifecycle rather than a separate feature session. Do not imply that
  offline transcription is performed by Codex inference.

- Replace the current split SIWC-direct-text/Codex-image design with one
  operator-managed Codex ChatGPT login and Codex app-server path for all AI
  features: Word Insights, mnemonic text and images, Vocabulary Source text
  analysis, and audio transcription. Do not retain SIWC direct Responses as a
  parallel production text route or introduce a separately billed API key.
- The trusted Klaf Server JVM owns account authorization and routing. For each
  AI-enabled Klaf account, it manages exactly one separate Codex app-server OS
  process and a private account runtime/profile/workspace. Devices signed in to
  that account may submit independent work to that process. Never route another
  Klaf account's requests into it or mount another account's files/history.
- Start a fresh ephemeral Codex thread for each independent AI
  request; do not rely on earlier thread history for feature behavior. Pass
  required context explicitly in the request. Finish/cancel and unsubscribe
  threads after use, bound temporary artifacts, and keep saved Klaf results in
  their existing account-owned storage. Device logout cancels only that
  device's work; account grant revocation/blocking ends that account's work.
- An authenticated device has its own Klaf auth session, but devices and AI
  features of the same Klaf account share that account's one Codex worker.
  Do not create a persistent Codex thread per device or per feature: each
  independent request gets a fresh thread in the account worker. The separate
  operator login-owner process is shared infrastructure, not another account
  worker; sandbox/network helper processes do not represent additional AI
  workers for a device or feature.
- Independent requests from one account, including requests from different
  devices or the same feature on one device, may run concurrently in distinct
  Codex threads within that single account worker. Do not preserve the old
  one-active-request-per-AgentDriver-session limitation as product behavior,
  and do not add feature-specific queues or extra feature/device workers just
  to achieve concurrency. Cancellation and result delivery remain scoped to
  the originating request. Bound account-wide concurrency and waiting so a
  burst cannot exhaust the worker; the exact limits are implementation
  configuration, not a separate queue for each feature. Starting one account's
  worker must not hold up revocation or request handling for another account.
- One process must safely support the different feature capabilities without
  exposing shell/files, credentials, other accounts, or broader tools to an
  untrusted prompt. Separate process directories or threads alone are not a
  security proof. A safe way for isolated account processes to use the one
  operator Codex login, including renewal, is an implementation gate. For the
  personal MVP, image generation may be enabled throughout the account worker
  so that one process handles text and images; this is not a strict per-text-
  request image-tool deny. Keep shell, browser, computer use, apps, plugins
  and multi-agent disabled for the account worker, and do not add feature- or
  device-specific Codex worker processes without a new decision.
- Feasibility finding (2026-10-04): mounting one managed-login `auth.json`
  read-only into multiple account processes does not establish safe renewal.
  Codex can request refresh-token rotation before it tries to persist the new
  credentials. This design must not be used in production without a supported
  way to ensure workers cannot perform managed refresh. The one-process-per-
  account target remains blocked on a new auth architecture decision; the
  existing SIWC implementation remains active meanwhile.
- The user accepted a trusted token-broker design for continued implementation:
  one Codex managed-login owner holds and renews the ChatGPT login; a broker
  component of Klaf Server supplies only the current short-lived access token
  to separate account-scoped Codex app-server processes. The refresh token
  never goes to those processes. Never mount `auth.json` in a worker, print
  tokens, or store them in account directories. Validate account binding,
  concurrent refresh, 401, timeout, restart and cancellation with synthetic
  credentials before any real-login check. This does not yet authorize a
  production cutover; preserve SIWC until the security and compatibility gates
  are resolved. The external-token app-server mode remains experimental.
- The user approved continuing with a dedicated server-side owner
  `CODEX_HOME` in addition to one credential-free `CODEX_HOME` per Klaf
  account. For this personal MVP, the trusted owner may use a deliberately
  version-sensitive, read-only adapter for its private `auth.json` to extract
  the current access token after Codex itself performs managed refresh. It
  must never read the operator's existing desktop login as an implicit
  fallback, copy refresh credentials to workers, or expose tokens in logs.
  Fail closed on unknown file schema, insecure storage, changed account or
  refresh failure. Start with synthetic fixtures; real login and production
  cutover remain separate verification gates.
- This decision supersedes the older per-device/feature persistent AI-thread
  ownership and SIWC/direct-Responses plus separate image-login requirements
  below. The current code still implements that older split; migration, TDD,
  Windows/Ubuntu isolation verification and physical-device validation remain.

## UI Theme Consistency (2026-09-28)

- Uncommitted server log UI and client screens/dialogs/components must support
  both light and dark themes. Keep actual color values in centralized theme
  configuration, not in screen files or log text builders.
- Server log severity, search highlights, inputs and actions must use the active
  palette. Changing theme must rebuild styled log text without losing searches
  or log entries. The server uses a module-local theme configuration rather than
  depending on the client presentation module.

## Branch Integration Decisions (2026-09-28)

- Merge preparation targets `mnemonic-voice-dictation`, bringing in
  `remote-storage-server`. Review conflicts and clarify material behavior before
  performing the merge.
- Existing Vocabulary Source, subtitle and voice-feature data need not be
  preserved or migrated; these feature tables may start empty. This permission
  does not cover decks, cards, account/synchronization data or original backups.
- Explicit logout must cancel unfinished feature work from the user context
  being left, rather than retain it for later delivery. When the server receives
  the logout/cancellation signal, it must stop the affected work and release its
  associated resources/sessions without stopping unrelated work. Late results
  must not be applied or opened in the next account or guest context.
- Logout ends the originating device's session and its unfinished work only.
  Other devices signed into the same account must keep their sessions and work.
  Cleanup must target the session being left, not a later session on that device.
- Logout remains an immediate local action, including when offline: cancel local
  work from the session being left and reject its later results/notification
  navigation. When connected, send server cancellation for that device session.
  Without connectivity, server processing may finish because cancellation cannot
  be delivered; its result must not be adopted after logout. Do not wait for
  server confirmation or create a queued logout for later guest-mode delivery.
  If the local authentication provider cannot initialize, logout must still
  clear the selected account's protected credential bundle and switch to guest
  mode; a failed remote logout cannot leave that account visible locally.
- Vocabulary Sources and their associated persisted feature data are an integral
  part of the user's data, scoped to the selected account/guest database rather
  than a store shared across users. This includes saved text, source items and
  the feature's Ignored Words records.
- Sign-up transfers all such guest feature data along with decks/cards into the
  new account's local database, including when no guest decks/cards exist.
  Preserve source/item/deck/card links and verify the complete copied data before
  removing transferred guest records. Retry/interruption must not lose data or
  duplicate it. Sign-in to an existing account still leaves guest data untouched.
- Permission to reset pre-merge Vocabulary Source/voice data is a one-time
  integration permission, not permission to drop newly created feature data
  during later sign-up/account changes.
- Cross-device synchronization of Vocabulary Sources and associated feature data
  was separated from branch integration. The follow-up was authorized on
  2026-09-29 under the requirements below; it extends the deck/card protocol.
- The merged MVP currently has shared server-wide AgentDriver sessions;
  cancellation distinguishes shared infrastructure from affected work. The
  authentication follow-up below requires account-owned AI session contexts,
  replacing this shared conversation-history behavior.

## Vocabulary Source Synchronization (2026-09-29)

- User authorized the follow-up phase: persist and manually synchronize saved
  Vocabulary Sources and their associated account-owned feature data between
  Android, Desktop and the server. Saving a source must no longer leave the
  synchronization indicator green while its changes are pending upload.
- Extend the existing offline-first Room/REST path, transaction guarantees,
  revision tracking, retry safety and conflict reporting. Do not introduce
  automatic data synchronization or change guest/sign-in/sign-up ownership.
- Preserve sources already saved after integration; the prior permission to
  start feature tables empty does not authorize deleting this new data.
- User agreed to resolve concurrent changes to the same source as a whole:
  source text/metadata and its analyzed word list form one conflict unit. Offer
  accepting the server version or retaining the local version; do not silently
  combine different analyses. Changes to different sources merge automatically.
  The earlier deck/card conflict decisions remain applicable; no silent
  last-write-wins policy is authorized.
- Implement using TDD: write and run tests against the current behavior first,
  confirm meaningful failures, then implement the missing behavior and rerun.
- Keep the existing reviewed-deck restriction when source words create cards.
  Preserve portable source/item/card/deck links, account isolation, guest
  ownership and recoverable sign-up transfer. Do not send local integer IDs as
  references between devices.
- Existing local sources must become pending first-upload data after upgrade,
  without deleting/recreating them. A previously confirmed global revision is
  not proof that an older client has downloaded Vocabulary Source data.
- Keep source-word links consistent when a card/deck is deleted: clear obsolete
  references without deleting the analyzed word or resetting its ADDED status.
  Deleting a source removes its analysis, not independently created cards.
- Conflict snapshots must describe the final committed source state of the
  response, including later accepted operations in the same request. Accepting
  the server version must not restore an obsolete intermediate version.

## Client-access Authentication Interview (2026-09-29)

- Current authorization boundary: the user delegated technical analysis/design,
  then explicitly paused execution. Record the selected decisions in
  requirements, implementation plan and status; after that, ask whether to
  continue and wait for permission so the user can select reasoning. Until that
  permission, do not write tests/production code, change dependencies, run
  builds/tests/runtime probes, publish, deploy, commit or reset live data.
- Concrete engineering choices are recorded in
  [the authentication implementation plan](klaf-server-implementation-plan.md).
  They are delegated design decisions, not newly confirmed product requirements
  or evidence that the current application implements them. That plan supersedes
  earlier interview notes saying a technical choice is still unspecified, where
  it explicitly selects that choice. Runtime/security verification gates remain
  open and must not be represented as verified design guarantees.
- Product authentication scope is Klaf server and its clients. AgentDriver
  remains a reference except for the explicitly approved targeted sandbox
  capability and profile/credential-isolation changes below; do not move Klaf
  account/authentication logic into SDK.
- User requested an interview before implementation, with one question at a
  time and a recommendation accompanying each question. Record agreed decisions
  before writing implementation code.
- User approved a minimum length of 15 characters for user-account passwords,
  with no spaces. Digits and special symbols are permitted but not mandatory;
  do not interpret the absence of composition requirements as banning them.
  Reject a password containing spaces rather than silently removing/trimming
  them. Apply the same policy to new registration and password reset, with
  server-side enforcement and matching client validation. These rules concern
  the sign-up/sign-in account password, not OS unlock or a server master
  passphrase, and do not change OS-protected signing-key storage. Additional
  length/character-handling details remain to be specified; guessing protection
  is below, with concrete thresholds still pending.
- User approved server-enforced temporary throttling after repeated incorrect
  password attempts. Return an actionable message indicating when sign-in may
  be attempted again. Do not permanently block the account or require operator
  intervention solely because this threshold was reached; the waiting period
  expires automatically. This is distinct from deliberate operator blocking
  and must not revoke already active login sessions, delete data or interrupt
  offline study. The delegated implementation plan now selects initial
  attempt limits, delay/backoff, accounting keys and persistence; they are
  configurable engineering defaults, not an immutable product policy. Tests
  must include protection against an attacker causing unnecessary denial of
  service to legitimate users.
- User reaffirmed the TDD workflow for this authentication phase and its
  approved SDK isolation fixes: finish gathering requirements first, then write
  and run tests covering the agreed behavior and corner cases before writing
  production implementation. Confirm meaningful failures caused by missing
  behavior, not merely compilation/setup errors; implement to satisfy the
  requirements/tests rather than weakening assertions to fit the code. Include
  token expiry/renewal/rotation/reuse, revocation and restart persistence,
  account/device isolation and the agreed interruption/concurrency cases in
  the test coverage. Keep requirements/status current throughout. Do not commit
  changes until the user gives separate explicit permission; interview approval
  or permission to implement/test is not permission to commit.
- Unexpected sign-in server failures must be diagnosable from the local server
  log without recording submitted passwords, tokens, raw request bodies, or
  exception messages that may contain those values. Return only a generic
  error code to the client; preserve cancellation semantics.
- The server must reject startup when its DPoP public HTTP(S) origin is absent
  or malformed, rather than appearing ready and failing account sign-in later.
  Loopback HTTP is valid for the local test setup; deployed public access uses
  its externally visible HTTPS origin.
- User selected OS secure storage for the server signing secret instead of a
  separate application master passphrase. Protect the persisted signing secret
  at rest; distinguish this from transport TLS, password hashing and client
  token/device-key protection. Ubuntu Linux is the primary server deployment OS;
  retain Windows support. User will start the server manually from an
  interactive Ubuntu desktop session, not via a headless/SSH-only startup flow
  in this stage. Automatic service startup is not a current requirement.
  Concrete backends, actual keyring availability and unlock integration remain
  open; desktop startup alone does not prove an installed/unlocked secure store.
  Do not assume hardware backing or availability of a desktop keyring for a
  service. This choice does not imply encryption of the entire account database
  or verified isolation of secrets from AI processes.
- Initial implementation validation and manual testing will take place on
  Windows. After that testing, Ubuntu Linux is the primary deployment target.
  Keep both supported; Windows results alone do not verify the Linux storage
  backend or sandbox integration. No deployment or data transfer is authorized
  by recording this rollout order.
- User approved fail-closed server startup when OS secure storage is locked or
  unavailable: do not accept client connections until the signing secret is
  safely accessible. Offer OS unlock where supported; if access still fails,
  show an actionable error and Retry. Never fall back to plaintext persistence
  or automatically replace an existing signing key to bypass a storage failure.
  First-time key provisioning is distinct from recovery of an existing key;
  its concrete protocol remains to be specified.
- User approved server signing-key-loss recovery with one explicit operator
  confirmation, followed by automatic recovery work. For a restored server
  database whose signing key is lost, generate and persist a new key in OS
  secure storage and invalidate all pre-recovery authentication sessions and
  their credentials, including refresh/recovery access. Applications require
  ordinary password sign-in on subsequent connection; do not require account
  re-registration. Preserve accounts, password hashes/salts, approval/block
  state and user data. Apply the existing session-revocation rules for active
  connections/unfinished AI jobs if present. Do not accept client access until
  new-key storage and old-session invalidation have safely completed; detailed
  interruption/restart handling remains to be specified. Do not run this reset
  automatically for a temporary locked/unavailable store; use unlock/error/Retry
  instead. No live key reset or session revocation is authorized by this note.
- Android/Desktop client connections to Klaf must use encrypted transport in
  this authentication phase: HTTPS for REST (including account, sync and image
  requests) and WSS for AI and sync-event WebSockets. This supersedes the
  earlier trusted-development plain HTTP/WS client transport decision.
  Public client TLS terminates at Cloudflare in the selected tunnel deployment.
  No plaintext client-network fallback or disabled certificate validation is
  approved; the private same-host origin connection is specified below.
- Target deployment is the user's Ubuntu laptop exposed through Cloudflare Tunnel,
  including access from outside the local network. Client authentication and
  account/device authorization belong to Klaf, not a Cloudflare Access service
  token supplied by the client. The user reports having removed that earlier
  Access-token gate in the demo setup; its live configuration has not been
  inspected. Tunnel connector credentials are separate infrastructure secrets,
  not client-account tokens. No live tunnel configuration changes are authorized
  by this interview note.
- Run cloudflared and Klaf on the same laptop. Public client traffic uses
  HTTPS/WSS, Cloudflare-to-cloudflared traffic uses the encrypted tunnel, and
  cloudflared-to-Klaf uses HTTP/WS on loopback only. Bind the origin listener
  to loopback, not LAN/public interfaces; no origin TLS certificate is required
  for this selected path. This is not permission for plaintext connections from
  other network devices. Cloudflare is a trusted TLS-terminating intermediary,
  not application-level end-to-end encryption hiding content from Cloudflare.
- A client-access token grants access to one specific account only, not every
  account on the server. Account-owned REST/WebSocket requests and data access
  must be restricted to the authenticated account; a caller-supplied email must
  not authorize another account. Reject account/credential mismatches.
- Each device accessing an account receives its own token. Android and Desktop
  must not share one common account token. Tokens are device-key-bound as
  specified below; operator access revocation is defined below.
- User confirmed automatic device-key binding: each application installation
  generates its own protected asymmetric key pair without manual key entry.
  The private key stays on the client; the server receives the public key and
  binds issued access/refresh credentials to that authorized key. Protected
  access and renewal/recovery must require proof of possession, not merely a
  caller-supplied device/session ID. A copied token alone, without the bound
  private key, must not grant access. This supplements password sign-in,
  operator approval, TLS and revocation; it does not replace any of them.
  It does not guarantee safety if the device/private key is also compromised.
  DPoP was proposed as the standard reference; exact protocol integration,
  replay checks, key storage/lifecycle and safe recovery details still need
  specification before implementation. Do not invent custom cryptography.
- User prefers automatic credential provisioning rather than manually pasting
  a token. Password-based registration/sign-in with automatic per-device token
  issuance and the enrollment/completion flow below are confirmed. Manual token
  entry was not accepted as a requirement.
- Preserve authenticated sign-in across application restarts without repeatedly
  asking for the password. Persist device credentials securely, not the user's
  password or a token compiled into the application. Android encrypts stored
  token material with a key held in Android Keystore; Desktop uses OS-protected
  credential storage. Exact Desktop backends remain to be specified; startup
  failure behavior is below. Stored credentials do not override server revocation.
- After successful sign-in or device enrollment, reconnect the account's AI
  channel automatically with the new credentials, even if its previous
  connection was failed or disconnected. A new sign-in to the already-selected
  account must also end its prior AI session and reconnect under the new
  authentication session. Logout and password reset must not
  silently retain an authorized AI channel from the old session. Data sync
  readiness does not imply AI readiness. When mnemonic generation is disabled
  because AI is not ready, show a clear status and recovery action instead of
  a button that appears to do nothing. Do not auto-retry a revoked session.
- User approved Desktop-client startup behavior when OS-protected credentials
  or the bound device private key cannot be accessed: retain the current local
  account/profile and allow its existing offline data/study, show an actionable
  secure-storage error and do not connect to the server until access is restored.
  Do not automatically log out, switch to the guest database, delete local data
  or recreate the device key because the store is temporarily unavailable.
  Never fall back to storing credentials/private keys in an unprotected file.
  After storage access is restored, normal token/key/session validity checks
  still apply; local profile continuity is not authorization to access the
  server. Distinguish store failure from an ordinary first launch with no
  saved account credentials. No new AI Retry UI is introduced by this rule.
- No compatibility or initial-password enrollment feature is required for
  existing passwordless MVP accounts. The user plans to remove test users and
  register new accounts, validate on the disposable test device/server, then
  deploy to the intended server device and transfer the preserved data. This
  does not authorize deleting current users/data now or modifying original
  backup files. Data import is distinct from migrating old account credentials.
  Password recovery for newly registered accounts is specified separately below;
  it does not introduce legacy-account password enrollment.
- User approved operator-assisted password recovery for this MVP, without an
  automatic email delivery service. The server operator verifies the account
  owner's identity, generates an account-scoped, single-use, expiring reset
  token in the server UI and delivers it privately through a trusted channel.
  The client accepts that token and a new password; persist only the password
  hash/salt, never the plaintext password. On a successful reset, consume the
  token, update the password and revoke all existing authentication sessions
  of that account consistently. Apply the agreed AI-job cancellation rules;
  retain account data and require ordinary password sign-in afterward, not
  automatic login from the reset flow. Merely issuing a reset token is not a
  password change or session revocation. A reset token authorizes password
  recovery only, not data/AI access, account unblocking or device approval.
  Token lifetime, limits, protected storage and detailed reset protocol still
  need specification before tests/implementation. This interview approval does
  not authorize sending actual reset tokens or changing any live password.
- AI requests remain owned by account, device, authentication session and
  functional area for authorization, cancellation and result delivery. Codex
  process/profile ownership is per account and thread ownership is per request
  as specified in Unified Codex AI Runtime above. Logout of one device must
  not terminate another device's work. This partitions AI work, not the
  account-owned persisted data synchronized between devices.
- User approved AI conversation-context lifetime tied to the current login
  session: after logout or access/session revocation, a successful new sign-in
  creates fresh request threads without inheriting the previous login's requests
  or conversation history, even for the same account/device. Preserve saved
  cards, images, Vocabulary Sources and other user data; leave unaffected
  devices' AI work intact. A new login is distinct from routine token
  renewal within the same active authentication session. Physical cleanup of
  provider-history files and idle handling still need specification;
  a fresh conversation identity alone is not filesystem isolation.
- User approved fresh AI conversation context after a Klaf server restart:
  do not resume the previous server run's AI threads or inherit their temporary
  request/conversation history. Preserve persisted authentication sessions and
  remembered client sign-in, subject to normal expiry, approval and revocation
  checks; restarting alone must not force password entry or recreate accounts.
  Preserve saved cards, images, Vocabulary Sources and other user data. Old
  provider artifacts must not become accessible to fresh AI contexts. Physical
  cleanup remains to be specified; interrupted AI-request handling is below.
- User declined a new AI retry mechanism and new Retry UI for this stage.
  Apply this consistently to Word Insights, mnemonic text/image generation,
  vocabulary analysis and audio transcription. If an AI request is lost due
  to a server restart, finish its loading state with an error; do not silently
  start a replacement generation. The user can invoke existing request/retry
  controls, or leave and reopen the feature screen to trigger its existing
  initial-load flow where available. Do not add a Retry action to the Word
  Insights initial-load error panel as previously proposed. Preserve saved
  results and existing drafts according to their current feature behavior.
  This does not disable connection reconnection or recovery of the same
  surviving request after a transient disconnect, and does not change database
  synchronization retry safety or the agreed token-renewal recovery rules.
  Distinguishing a surviving request from one lost on restart still requires
  protocol design and tests; a lost request must not remain loading forever.
- User approved adding a targeted AgentDriver SDK fix to the implementation
  plan: trusted sandbox setup may configure network restrictions, but the AI
  provider and its child processes must not retain/reacquire the privileges
  to change those restrictions after launch. Cover the actual bridged launch
  path with regression tests while preserving permitted provider connectivity.
  This is a scoped SDK exception, not authorization for unrelated SDK changes.
  Continue the requirements interview before starting this implementation.
- User additionally approved the minimum SDK changes needed for Klaf to isolate
  provider history/artifacts between AI sessions and restrict AI-tool access to
  provider credentials and Klaf server secrets/authentication storage. Preserve
  the provider runtime's necessary authentication/connectivity without granting
  the same secret access to agent-controlled tools. Separate provider login
  credentials from Klaf account passwords and the server signing key; do not
  move Klaf account/authentication logic into SDK. Start with synthetic-secret
  and cross-session file-access regression tests on the actual launch path,
  then implement the isolation changes under TDD. Separate directories/threads
  alone are not proof of denied access. Concrete enforcement, artifact cleanup
  and provider-authentication integration remain to be specified. Unrelated
  SDK changes are not approved; finish the interview before implementation.
- SDK must apply the required AI isolation automatically when launching the
  provider, combining provider tool-access policy with OS-enforced sandbox
  restrictions. Do not require a manual isolation step before each Klaf server
  startup. Initial component installation and provider sign-in remain separate
  setup prerequisites. Preserve provider authentication while denying
  agent-controlled tools access to credentials and other sessions' histories;
  a working-directory change or the current workspace preset alone is not
  sufficient. The concrete mechanism and compatibility with the deployed
  provider version still require implementation design and real-launch tests
  on Windows/WSL2 and Ubuntu; this is not verified current behavior.
- User approved independent server availability when safe AI sandbox startup
  fails: keep account sign-in and data synchronization available if their
  authentication/data dependencies are healthy, but disable affected AI
  features. Return an actionable AI-unavailable error; show the cause and Retry
  in the server UI. Never launch AI without its required sandbox as a fallback.
  This does not override the signing-secret storage failure rule: inaccessible
  signing storage still blocks client connections. Cover unavailable/failed
  AI sandbox startup separately from authentication/storage failures in tests.
- Registration/access must require explicit approval by the server operator,
  both for each new account and for each new device accessing that account.
  An approved account alone does not authorize an unapproved device; device
  access requires both approvals. Do not activate applicants automatically.
  The applicant sets a password when submitting the registration request.
  The server stores its hash and salt at submission, not after approval, and
  never stores the plaintext password. Pending credentials must not grant
  access before account and device approval. The password hashing algorithm
  and parameters remain to be specified.
- User approved password re-entry when completing approval of a new account
  or a new device: submit the enrollment request, wait for operator approval,
  then explicitly complete access with the account password entered again.
  The client must not retain the password while waiting, either on disk or as
  a pending-flow secret. Validate the password and current account/device
  approval before creating the authenticated login session and issuing bound
  device credentials. Approval/status observation alone must not issue access
  or switch databases. Preserve the distinction between completion of a new
  sign-up (recoverable guest-data transfer) and existing-account sign-in
  (no guest-data transfer). This does not require password re-entry after every
  app/server restart or routine token renewal of an already valid login.
- If an applicant repeats sign-up from the same device with the same password
  after operator approval but before completion, recover the existing request
  and report its actual `APPROVED` status. The client must show that approval
  and offer explicit password re-entry/completion without requesting another
  operator approval, creating another account/device, or issuing credentials
  from the repeated sign-up response alone.
- Approve/reject registration requests in the server application's window.
  Show a request list with the account and device name and actions to approve
  or reject. A separate web administration panel is not required for the first
  stage.
- The operator can block an entire account or revoke access for an individual
  device from the server window. Account blocking denies server access for all
  its devices; individual-device revocation affects that device only. Close
  affected active connections and reject subsequent requests. Do not delete
  server or local data as a consequence of these actions. Previously issued
  tokens for affected access must remain invalid even after the operator
  restores access. Require a new password sign-in and issue a new device token
  only when account/device access is approved. Unfinished AI jobs are cancelled
  as specified below.
- User confirmed cancellation on authentication-session revocation: closing a
  connection alone is insufficient. Cancel unfinished AI jobs owned by the
  revoked session, including mnemonic text/image generation and subtitle
  analysis; release their resources and reject late results/events. Target the
  session being revoked, not a later sign-in or shared infrastructure used by
  unaffected sessions/devices. Device blocking cancels affected sessions for
  that device; whole-account blocking covers all its affected device sessions.
  Preserve already saved data and the existing transaction guarantees; do not
  delete data or undo completed operations as part of AI cancellation.
  The trusted operator revoke action must trigger cancellation of affected
  in-flight jobs as part of that action; periodic WebSocket authorization
  polling alone is not sufficient. A result completed after the revocation
  takes effect must not be delivered to the revoked device.
- User confirmed JWT access-token validation with an additional server-side
  authentication-session check, not signature/expiry-only acceptance. The
  session identifier is carried inside the signed access token and identifies
  a server-owned login session for one account/device. A separately supplied
  session ID must not override the authenticated token's session identity.
  For every protected request, verify the token and require its session and
  account/device access to remain valid; a revoked or unknown session denies
  access even before the JWT expires. Store authentication-session ownership
  and validity/revocation in a server database table, retaining these records
  across server restarts; this is Klaf persistence, not automatic JWT-plugin
  storage. Close affected active WebSocket connections. Restoring access must not
  reactivate a revoked session; a successful new password sign-in creates a
  new session identity. Revocation does not undo already completed operations;
  unfinished AI jobs follow the cancellation rule above.
- User confirmed an access/refresh pair with automatic client renewal: JWT
  access tokens expire 15 minutes after issuance. A refresh credential is used
  only to obtain fresh access, not to authorize business requests directly.
  Refresh access expires after 30 days from initial issuance or the last
  successful server-side renewal. Regular successful renewal extends this
  inactivity deadline; it is not a mandatory password login every 30 days.
  After that deadline, require password sign-in again. Local data and offline
  study remain available without renewal/network connectivity. Renewal must
  require a valid refresh credential and an active, approved account/device
  and authentication session; it must never revive revoked access.
- User confirmed refresh-token rotation: every successful renewal issues a
  new access token and a new refresh credential, replacing the previous refresh
  credential. Activating the replacement and invalidating its predecessor must
  be atomic; the client securely stores the replacement for subsequent renewal.
  A replaced refresh credential must not authorize another independent renewal.
  Refresh credential format and the concrete response-loss recovery protocol
  remain to be agreed; no blanket grace period for independent renewals with
  replaced tokens is approved.
- User confirmed the response to reuse of a replaced refresh credential for
  another independent renewal: revoke the affected authentication session,
  deny access through all its tokens and require password sign-in again.
  Retain local/server data and leave other devices/sessions unaffected. This
  is a possible-compromise response, not proof of who used the token. Legitimate
  retries must follow the separate recovery requirements below; do not interpret
  every duplicate request as detected malicious reuse.
- User approved bounded automatic recovery when a successful renewal's response
  is lost: recover the result of that same renewal, rather than independently
  issuing another token pair or advancing the rotation/inactivity deadline
  again. Recovery must not bypass current account/device approval or session
  revocation, including when returning a previously completed result. If safe
  recovery is unavailable or fails within its bounds, require password sign-in.
  The recovery time/attempt limits, authentication/proof for retry, protected
  result persistence and exact protocol still need specification before tests
  and implementation. Request/session IDs alone are not authorization to
  retrieve token material; this is not permission for unrestricted old-token
  reuse or ordinary HTTP caching of credential responses.
- For new registration, submitting a request shows a one-time notification;
  retain a non-blocking "Awaiting approval" status in the account section, not
  a modal dialog that prevents local use. The user remains in guest mode and
  may create/edit decks/cards and study with data saved locally. Server data
  synchronization and AI features remain unavailable before account/device
  approval and successful authentication.
- Operator approval must not automatically switch the active local database
  or interrupt editing/study. Show an approved status and an explicit action
  to complete registration. On successful completion for a new account,
  transfer guest data including changes made while awaiting approval, using
  the existing recoverable transfer rules. Connecting to an existing account
  still leaves guest data untouched. Approval alone must not move/delete it.
- Open security concern raised by the user: malicious client text could attempt
  to persuade the server-side AI agent to read/exfiltrate other users' password
  hashes, salts or credentials. Discuss technical isolation of the AI process
  from authentication storage and secrets, including filesystem, tools and API
  access. Prompt instructions alone are not an isolation guarantee. User
  requested auditing existing isolation in both SDK and Klaf before deciding
  what to add; findings and verification limits are recorded in the status.
  The targeted network-capability and minimum profile/credential-isolation
  fixes above are approved SDK scope; unrelated SDK changes remain outside scope.
- Selected issuance/provisioning, device-key, secure-storage and refresh-recovery
  details are in the implementation plan below. Previously unconfirmed product
  proposals do not become requirements merely because they appeared during the
  interview. Live tunnel setup and actual platform/provider compatibility remain
  unverified; recording the design does not authorize live configuration changes.

## Authentication Technical Design (2026-09-29)

- Use Argon2id through Bouncy Castle for password hashes, signed ES256 access
  JWTs through the stable Ktor authentication/JWT integration, and Nimbus JOSE
  for DPoP proof/JWK processing. Do not implement signature encodings, password
  hashing primitives or encryption primitives manually. Initial parameters and
  candidate dependency versions are recorded in the implementation plan; no
  Gradle dependency changes have been made by this design phase.
- Extend the existing server Room registry for account/device approval,
  authentication sessions, refresh rotation, recovery receipts and reset tokens.
  Keep the existing per-account content databases; do not introduce JPA or a
  second competing account identity store. Authentication-related changes must
  be transactional; cross-database content writes need an explicit revocation
  ordering barrier, not an unsupported claim of one transaction across databases.
- Bind account/device access to a verified principal and a P-256 installation
  key. Use the RFC 9449 DPoP proof format and validation rules as the reference,
  with an explicit Klaf API profile; this does not make Klaf a complete OAuth or
  OpenID Connect provider. A device UUID, request ID or email is never a proof.
- Protect Windows server/Desktop secrets with current-user DPAPI. On Ubuntu,
  use libsecret/Secret Service to hold the application wrapping key and AES-GCM
  to protect local credential bundles. Android uses Keystore-backed protection.
  These are OS-protected secrets at rest, not full database encryption, guaranteed
  hardware backing or isolation from arbitrary programs running as the same OS user.
- Refresh credentials are high-entropy opaque secrets; store their digests in
  Room. Keep only bounded, encrypted receipts for recovering an already committed
  renewal. Recover the same pair only with the original refresh secret and a new
  valid device proof; recheck access and the current rotation generation. Never
  return superseded credentials or turn a recovery request into a new renewal.
- Protect all account-owned REST routes, sync, image upload/download, the AI
  WebSocket and sync-event WebSocket. Retire passwordless entry points rather
  than leaving a compatibility bypass. Public HTTPS/WSS origin configuration
  and device proof validation must also work behind the selected TLS-terminating
  Cloudflare tunnel; do not trust arbitrary forwarded host headers.
- Partition AI request ownership by account/device/authentication session/feature
  and server run, with one Codex OS process/profile per account. Request
  deduplication, result delivery and cancellation must use that verified
  ownership, not the existing caller-supplied request prefix.
  Reconnection may recover surviving work; restart-lost work ends with an error
  rather than silently becoming a new generation.
- SDK isolation is a release gate, not a presumed property of a workspace preset.
  Require restricted tool reads, non-shared runtime profiles/history, private
  trusted setup files, removal of firewall-changing privileges before provider
  launch and denial of escalation/out-of-sandbox execution. Provider login must
  remain outside agent-readable storage. The credential-broker integration and
  installed Codex policy compatibility require a focused feasibility check;
  no shared-profile or unsafe-launch fallback is allowed.
- The server AI must use the single operator-managed Codex ChatGPT login. An
  OpenAI API key is explicitly excluded because it adds separately billed
  usage. Separate account runtime/history storage; do not treat separate
  working directories or thread IDs as proof of filesystem isolation.
- Isolated account processes must use that one Codex login through a verified
  supported credential/renewal path. Never copy a long-lived refresh credential
  into agent-readable homes or tools. A trusted broker is an option only if it
  preserves this single-login requirement; it is not a second SIWC login or
  authorization for the current direct Responses route.
- Earlier feasibility concern (2026-10-04, partly superseded below): an
  account-specific `CODEX_HOME` does not automatically inherit managed login.
  A second interactive login was considered, but the controlled single-login
  check below shows it is not needed for immediate two-process text requests.
  The renewal and security gates remain open. Do not silently copy `auth.json`.
- Controlled feasibility result (2026-10-04): two simultaneous sandboxed
  account-scoped Codex processes both completed real text turns using the
  server's one existing managed login. The current bubblewrap launch gives
  each process a private temporary home and read-only access to the one auth
  file. Thus a separate interactive sign-in per Klaf account is **not** needed
  for immediate requests. This does not yet satisfy the production requirement:
  managed refresh updates `auth.json`, and OpenAI's advanced auth guidance
  warns against concurrent jobs using one credential file. Do not force
  refresh against the real login or let workers independently rotate it.
  Design and test one trusted writer for renewal plus safe worker restart,
  and verify cross-process history/file isolation before switching production.
- Approved feasibility scope (2026-10-04): investigate one trusted writable
  managed-login owner and read-only account workers. Start with synthetic
  credential/renewal and failure-path tests; never force renewal of the real
  operator login merely to test this design. Account workers must not mutate
  the canonical auth file or silently retry a generation after uncertain
  completion. If Codex can still rotate a refresh token from a read-only
  worker, fail this design rather than claiming the filesystem mount solves it.
- Owner-only provider scope (2026-10-01): the operator is the sole human AI
  user in the initial phase. Multiple Klaf accounts/devices may represent that
  same person; account names alone do not establish ownership. AI access must
  be restricted to operator-owned Klaf accounts through an explicit, local
  server-operator CLI grant keyed by immutable Klaf account ID. The CLI lists
  approved accounts with email for human recognition and ID for selection;
  email is never the grant key. Grants persist in the server registry,
  are default-deny, and can be revoked. Account approval alone does not grant
  AI access. Temporary account Block must cancel active AI work and deny all
  access, but preserve any existing AI grant. Restore must make that grant
  effective again after a fresh password sign-in; revoked old sessions and
  tokens must stay invalid. An explicit operator AI-grant revocation remains
  permanent across Block/Restore until the operator explicitly grants access
  again. Neither Restore nor sign-in may create a grant for an account that
  did not already have one. No remote grant endpoint or new UI is required
  for this MVP.
  The initial grant targets the server-operator provider slot, not an email or
  an OAuth token. Before provider integration, bind that slot to a verified
  provider identity and invalidate/reconfirm grants if its human owner changes.
  Klaf clients receive no provider tokens. Each Klaf account has its own
  Codex process/profile; each independent request has a fresh thread even
  when provider ownership is the same. Provider outage must not disable local
  data or sync.
- Model provider ownership separately from Klaf context ownership so a later
  per-person provider sign-in or additional provider account can be added
  without migrating or merging existing Klaf data and AI histories. No access
  for family members or other people through the operator's personal ChatGPT
  account is authorized by the owner-only scope.
- Use only a documented, eligible Codex login mechanism. The
  `chatgptAuthTokens` app-server login variant is marked internal-use-only in
  the public protocol source; do not substitute that synthetic prototype for
  verified production authentication. The existing SIWC registration/refresh
  implementation is historical code to retire from the AI path, not a second
  required login. A future expansion to other people requires separate
  authorization and provider eligibility decisions.
- Image generation is not an optional downgrade of the AI integration. Preserve
  the existing mnemonic-image generation behavior while changing provider
  authentication; do not declare the migration complete with text-only AI or
  replace generated images with placeholders. Any alternate provider path must
  retain ChatGPT-subscription use, account/session isolation, and credential
  protection without a separately billed API key.
- Image jobs still require a fresh ephemeral thread, disabled unrelated agent
  tools and a validated binary image result under the unified account process.
  This must not become a Klaf client login or bypass Klaf account/device/session
  authorization. Missing Codex login or failed isolation disables AI requests
  explicitly; it must not silently return text or a placeholder as an image.
- Implement next through requirements-driven unit tests, real Room persistence
  and Ktor route/WebSocket integration tests, then actual Windows/Ubuntu secure
  storage and sandbox checks with synthetic secrets. Passing fake storage tests
  does not verify OS access control. Keep SDK changes within the already approved
  capability/profile/credential-isolation scope.

## Goal

The Goal/Current Concept/Decisions sections below record the original server
MVP and may describe its then-current feature-wide sessions. For AI runtime
ownership and provider routing, the Unified Codex AI Runtime decision above
supersedes those historical details; Klaf's client/server transport and saved
data requirements remain unchanged.

Create a separate Gradle module for a Klaf server application inside the existing `Klaf_kt` Gradle project.

The server will be a wrapper around the AgentDriver SDK. It will use Ktor for HTTP/server transport and Koin for dependency injection, following the style already used in the Klaf Kotlin Multiplatform project.

## Current Concept

The server should create and manage AgentDriver/Codex sessions with feature-specific developer instructions. The authentication follow-up requires separate conversation contexts for each account/device/functional area; grouping requests by functional area alone is not sufficient isolation.

The server should be similar in spirit to the AgentDriver demo app: a wrapper process around the AgentDriver SDK, but with Klaf-specific behavior and feature-specific session instructions.

Initial planned sessions and current MVP status:

- Word Insights session: implemented.
- Mnemonic text/image session: implemented as one shared session in the current MVP; account isolation is required by the authentication follow-up.
- Vocabulary source / subtitle analysis session: planned later.
- Standalone image generation session: planned later if needed outside mnemonic image generation.

Review note: keeping mnemonic text and mnemonic image in one session creates a possible instruction conflict because text-generation rules include JSON/tool prohibitions while image generation needs the image tool path. Splitting mnemonic text and mnemonic image into separate feature sessions is the preferred follow-up if image generation behaves inconsistently.

## AgentDriver SDK Changes

AgentDriver SDK needs support for passing session-level developer instructions into Codex app-server `thread/start`, instead of wrapping the same instruction into every `turn/start` prompt.

This has been implemented in the local AgentDriver working tree. Klaf Server targets the distinct `0.10.13-SNAPSHOT` coordinate for the current account-scoped SDK changes; the previous `0.10.12-SNAPSHOT` is historical. Publish the new SDK to Maven Local before a consuming build.

The current SDK shape is:

- Codex config exposes nullable `baseInstructions`.
- Codex config exposes nullable `developerInstructions`.
- Both fields are sent in Codex `thread/start` when configured.
- `turn/start` receives only the actual request prompt and optional response schema.
- The old shared `systemInstruction` wrapper was removed from the request prompt path.

Klaf Server uses `baseInstructions` for a short shared Klaf application context and `developerInstructions` for feature-specific behavior.

## Transport

AI feature commands use the Klaf WebSocket protocol. The merged account/storage
functionality uses REST for account and manual synchronization operations plus
the separate `/sync-events` WebSocket for state updates. These transports coexist
in one server; the AI-only WebSocket decision does not prohibit storage REST.

Long-running operations, such as subtitle analysis, should therefore be handled through the WebSocket protocol rather than long blocking HTTP requests.

The Klaf server should expose its own Klaf-specific WebSocket protocol. The Android app should send business-level commands such as vocabulary analysis or word insights, not raw AgentDriver prompts. AgentDriver remains an internal dependency of the server.

The Klaf app should keep one persistent WebSocket connection to the Klaf server for the whole app. Multiple feature requests should be multiplexed over this connection using request IDs.

For the MVP, the Klaf server can own feature-specific AgentDriver `Assistant` instances and start their AgentDriver servers on internal loopback ports. The Klaf server then talks to those internal sessions through the AgentDriver client SDK. This keeps the public Android-to-Klaf protocol separate from AgentDriver, while still using the SDK's current public server/client API.

WebSocket messages should use JSON with Kotlinx Serialization.

The current trusted-development MVP uses plain WebSocket at `/ws`. The authentication follow-up requires WSS for client network connections. The selected Cloudflare deployment terminates public TLS at Cloudflare and uses HTTP/WS only between cloudflared and the same-host loopback origin; operational setup remains pending.

Protocol DTOs should be separate from pure domain entities. A shared `:klaf-server-contract` module should define serializable WebSocket request/response/event DTOs used by both the Android app and `:klaf-server`.

The protocol should preserve existing feature business inputs where possible. The main migration change is that the Android app should stop sending full AgentDriver prompts; instead, it should send a command name plus the same business inputs as before. The Klaf server owns prompt construction, session instructions, response schema selection, parsing, and validation.

The first command should be `wordInsights.generate`, with the same business input as today: `word`.
Mnemonic MVP commands should be:

- `mnemonic.association.generate`, with `word`, optional `comment`, and already-used sound anchors.
- `mnemonic.image.generate`, with selected mnemonic data and optional image comment.

For MVP, preserve the current non-streaming behavior for text generation features. If a feature currently receives one complete text/result response, the Klaf server protocol should also return one complete response message rather than streaming partial text.

Feature request failures should be returned as structured protocol error messages tied to the original request ID. Invalid assistant JSON, invalid contract data, request validation failures, and KlafServer failures should not close the WebSocket connection unless the connection itself is broken or unauthorized.

Logging should use short summaries by default. Full raw assistant responses should only be logged in an explicit debug mode because future requests may contain user text or subtitle content.

Connection readiness is defined by the public Klaf Server WebSocket connection, not by internal AgentDriver sessions. The client is ready only after it receives `server.ready` from the server.

## Secrets

If the implementation uses a secrets file or local sensitive configuration, it must be added to `.gitignore` and must not be committed to the repository.

For MVP, the Klaf server should use a local `SecretConstants.kt` file, similar to the existing `data` module pattern. The real file must be ignored by Git, and a committed example/template file should show the expected structure without real secrets.

For first local/LAN MVP testing, Klaf app to Klaf server authentication can be temporarily disabled. Security/authentication must be implemented before committing or using the server outside this trusted local development setup.

## Open Questions

- Production security model before using the server outside trusted local development.
- Whether mnemonic text and mnemonic image should remain in one shared AgentDriver/Codex session or be split into separate sessions to avoid instruction conflicts.
- Whether the client WebSocket session should include explicit request timeouts for very long operations or keep relying on caller-level cancellation/loading state.

## Known Risks From Review

- `KlafServerSession.disconnect()` and the connection-failure cleanup path can deadlock because they cancel/join the reader job while holding the same lifecycle mutex that the reader job needs in `finally`.
- Pending client requests can wait forever if the WebSocket incoming loop ends normally without throwing, because pending responses are failed only in the exception path.
- The shared mnemonic session currently combines text-only JSON/tool restrictions and image-generation rules in one `developerInstructions` block.
- AgentDriver documentation has been updated for the new `baseInstructions` /
  `developerInstructions` API in the main architecture notes and Ralph planning docs.

## Decisions

- The Klaf server application will be implemented as a separate Gradle module inside the existing `Klaf_kt` project.
- The Gradle module name will be `:klaf-server`.
- The normal target environment is a remote computer/server machine. Development flow may be: commit locally, push, pull on server machine, run the Klaf server there.
- For the current implementation/debugging phase, the Klaf server will be run on this same development computer and should be reachable from a physical Android device over the local LAN/hotspot network.
- Later, the target environment can be a remote computer/server machine. Development flow may be: commit locally, push, pull on server machine, run the Klaf server there.
- Local debugging should use a configurable bind host. For physical Android device testing over local Wi-Fi/LAN/hotspot, bind to the computer's LAN IP. For same-computer testing, `127.0.0.1` can be used.
- The MVP transport direction is WebSocket-style communication, not REST.
- The Klaf server will expose a Klaf-specific WebSocket protocol, not the raw AgentDriver protocol.
- The Klaf server may use internal loopback AgentDriver server/client sessions per feature because the AgentDriver SDK public API currently exposes `Assistant.startServer(...)`, not direct in-process text generation.
- The Klaf app will use one persistent WebSocket connection to the Klaf server for the whole app.
- WebSocket requests/results should use request IDs so multiple operations can share the same connection.
- Feature-specific AgentDriver/Codex sessions should be created lazily on first use for MVP, not eagerly on server startup.
- If a feature session fails or the Codex thread dies, the Klaf server should recreate that session and retry the same request once. If the retry fails, the server should return an error to the app.
- The first end-to-end feature should be Word Insights.
- Word Insights should reuse the existing prompt/response contract from the current Klaf client code for the first server implementation.
- The Android app should move to the Klaf server path without fallback to the old direct AgentDriver connection. Do not add fallback flags or parallel old/new routing.
- The Klaf server should use a local ignored `SecretConstants.kt` file for MVP secrets/configuration, plus a committed example/template file.
- Klaf app to Klaf server authentication can be temporarily disabled for first local/LAN MVP testing.
- Security/authentication must be implemented before committing or using the server outside the trusted local development setup.
- AgentDriver SDK should be modified first to support session-level Codex instructions before building `:klaf-server`.
- A small shared Klaf app context may be passed through Codex `baseInstructions` if useful, while feature-specific rules should live in `developerInstructions`.
- AgentDriver SDK should expose explicit behavior configuration fields for `baseInstructions` and `developerInstructions`, matching Codex app-server concepts.
- AgentDriver SDK should remove the existing `systemInstruction` behavior field and stop wrapping every prompt with `<system_instruction>`.
- Codex provider should pass `baseInstructions` and `developerInstructions` in `thread/start`, while `turn/start` should receive only the actual request prompt plus any per-turn `outputSchema`.
- Codex `baseInstructions` and `developerInstructions` should be optional nullable fields. If not explicitly set, AgentDriver should omit the corresponding fields from Codex app-server `thread/start`.
- AgentDriver should keep the current construction style, for example `Assistant(AssistantType.Codex) { ... }`, but expose provider-specific builder options inside the typed configuration lambda.
- AgentDriver SDK tests should verify that Codex `thread/start` includes `baseInstructions` and `developerInstructions` when configured and omits them when null.
- After AgentDriver SDK changes, publish the SDK to local Maven and make `:klaf-server` consume it as a normal dependency.
- `:klaf-server` should be a JVM-only Gradle module because it runs as a server process on a computer/server.
- `:klaf-server` should include a Compose Multiplatform desktop UI window. It should not be only a headless console process.
- The Klaf server should start automatically when the desktop app launches.
- The initial UI should be minimal: essentially an app/server icon and text indicating that this is the Klaf server. It should not include advanced controls or visible logs for MVP.
- Logs should be written to the console/output, not displayed in the Compose UI.
- Console logs should show useful server/session state and request activity, similar to the AgentDriver demo app.
- Console logs should use the same logging library/style already used by the Klaf app, not raw `println`.
- `:klaf-server-contract` should remain Kotlin Multiplatform for shared client/server DTOs.
- WebSocket messages should use JSON with Kotlinx Serialization.
- Create a shared `:klaf-server-contract` module for serializable WebSocket DTOs.
- `:klaf-server-contract` should be a Kotlin Multiplatform module, with shared DTOs in `commonMain`.
- Protocol DTOs should be separate from pure domain entities, with mapping between DTOs and domain models where needed.
- The Android app should not send full AgentDriver prompts through the Klaf server protocol.
- Feature commands should keep the same business inputs where possible.
- The first WebSocket command will be `wordInsights.generate`.
- Mnemonic text and mnemonic image generation will also use Klaf server WebSocket commands.
- MVP text-generation features should use one request and one complete final response, not streaming partial text.
- Feature failures should return structured per-request error messages over WebSocket.
- Logs should use short summaries by default; full raw assistant responses should only be logged in explicit debug mode.
- Logs should be console/output logs only; do not add an in-app log panel for MVP.
- Server logs should use the existing Klaf app logging library/style, not raw `println`.
- Do not create Git commits during implementation. The user wants to inspect diffs manually.
- Server bind host and port should be configurable through the local ignored server `SecretConstants.kt`.
- The MVP WebSocket path will be `/ws`.
- The original trusted-development MVP used plain `ws://`; the authentication follow-up supersedes this with HTTPS/WSS client transport.
- Android/Desktop client feature code should not keep fallback or old direct AgentDriver server paths after migration.
- AgentDriver SDK should be used inside `:klaf-server`; the app should talk to the Klaf-specific server protocol.
