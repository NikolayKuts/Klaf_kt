# Server Database Requirements

This document captures requirements for adding persistent database storage
to the Klaf Server, replacing the current Firestore dependency.

## Agreed Requirements

### Goal

Replace Firebase/Firestore as the storage and synchronization path for decks
and cards with a self-hosted database managed by Klaf Server. Primary
motivations:

- Remove Google infrastructure from the core account and deck/card sync path,
  without requiring removal of every Firebase integration in this MVP.
- Synchronize and persist data between the user's own devices and the server.
- Enable data backup as a side benefit of server-side storage.

Local Firebase configuration files, including Android `google-services.json`
and the server service-account JSON, must remain outside Git commits. The
service-account file is not part of the new account/deck/card storage path;
Firebase Crashlytics remains in the Android app during this MVP.

Pre-commit hygiene: keep local Kotlin/Gradle caches and JVM crash logs out of
Git, while retaining required Room schema history, implementation tests and
documentation. Remove retired staged-only test placeholders and stage the
reviewed current versions. Audit the final index for credentials and accidental
database/build artifacts; do not print credential values during the audit.
Publishable README/documentation must use generic examples rather than personal
emails, user-home paths, real device serials or account-specific storage IDs.
Actual local configuration, secret files and private handoff notes stay ignored.

### User Model

The MVP supports multiple accounts, including multiple accounts used on the
same Android or Desktop device. Each account's decks, cards, sync revisions,
pending operations, and device registrations must stay separate. A client
identifies the account in its server requests; without authentication, this
is only a development convenience and provides no account security. The
server keeps a separate SQLite database file for each account. A user ID is
not needed inside deck and card tables because each file contains only one
account's data.

The MVP creates its account through the client application's UI. Account
creation requires a working server connection: the client sends a request, the
server creates the user record and registers that first device in the same
operation, and the client receives the result. The user does not enter a
password, and the server does not authenticate subsequent connections. The
first device does not receive a `DEVICE_NOT_REGISTERED` response or a separate
registration prompt during sign-up.

When a client signs in to an existing account from an unregistered device, the
server returns a structured `DEVICE_NOT_REGISTERED` result without registering
the device. The client shows a separate prompt offering to register this
device. Only after the user confirms does the client send a device-registration
request; the server creates the device record and returns the result. The
client must not register a new device silently as part of sign-in.
After confirmed registration succeeds, the client opens that account's local
database; it does not ask the user to repeat Sign in. Guest data remains in
the guest database and is not moved into an existing account.
For the initial REST contract, `POST /api/v1/accounts/sign-in` checks the email
and device ID without changing server data; `POST /api/v1/accounts/devices`
registers the device only after that confirmation. Repeating a successful
registration request for the same account, device ID, name, and platform is
safe and does not create a second device record. Reusing a device ID with
different details is rejected rather than silently overwriting its record;
device renaming is a separate action.

Reuse the current client sign-up and sign-in screens where practical. For the
passwordless MVP, disable the password field and, on sign-up, the password
confirmation field. Update form validation and request contracts so empty
password fields do not block submission and no placeholder password is sent or
stored. Keep the existing email field as the account identifier and validate
its format. The MVP does not verify ownership of the email address.

Hide the existing "Delete account" action in the client UI for this MVP. Do
not add a server account-deletion endpoint yet; deletion semantics need a
separate decision. Keep the existing sign-out action.

Signing out does not delete the account's local decks or cards. Instead, the
client switches immediately to the separate guest database and shows only its
content. This is a local action that works without network access or server
confirmation: close the previous account's `/sync-events` connection, stop
account-scoped requests, and do not queue a pending logout. The guest mode
does not open account synchronization connections. The server may continue to
show the old device as connected only until its existing connection closes or
times out; no guest connection is used to relay a logout later.
While a manual synchronization is in progress, disable Sign out. Re-enable
it when synchronization succeeds, fails, or reaches a bounded timeout; the
user must not remain stuck in a non-dismissible syncing state.
Synchronization is unavailable while signed out. If the user signs in to
another account, the client switches to that account's own local
database, downloads its server data as needed, and displays only that
account's content. The previous account's local database remains on the
device, inactive and available when the user signs in to it again. The client
must not merge data or sync metadata between accounts.

The client persists the currently selected account locally. Closing and
reopening the app without an explicit Sign out restores that account's local
database even when the server is offline; reopening does not start automatic
data synchronization. Only an explicit Sign out clears the selected account
and switches to the guest database.
Pending sync operations must be bound to the selected account database by the
client, rather than accepting an unrelated account identifier at the write
call site. Guest mode must not create account outbox entries.

Android review reminders belong only to the currently open local database.
On sign-out or account switch, cancel alarms for the previous account before
scheduling reminders for the guest or newly selected account. On returning
to an account, rebuild its local reminders from its deck data. Reminder IDs,
periodic checks, and notification navigation must not confuse decks with the
same local integer ID in different accounts.

Before any account is selected, the client uses a separate local guest
database. A user can create decks and cards there while offline. When the user
signs up for a new account, the client transfers the guest decks and cards
into that new account's local database. Their sync identifiers and pending
operations must be valid for the new account; the data is uploaded only when
the user starts synchronization. Since the account has just been created, its
server database is empty and no cross-account merge is required. The transfer
must be recoverable: copy and verify the account data before deleting the
transferred decks and their cards from the guest database. Handling guest
data on sign-in to an existing account is different: do not transfer it,
do not prompt to transfer it, and do not upload it to that account. The guest
database remains separate and unchanged. A failed or interrupted sign-up
transfer must not lose data or create duplicate copies when retried.
Persist a unique sign-up request ID on the client before contacting the
server. If the response is lost or the local transfer is interrupted, retry
with the same ID and return the same newly created account only when the
original email and first-device details match. A fresh sign-up attempt for an
existing email must still fail; it must not silently sign in or merge guest
data into that existing account.
The first manual upload must also preserve cards already present in a guest
deck with review history. It must not send a reviewed deck first and then let
ordinary `AddCard` validation reject those existing cards as new additions.

For the first local-development MVP, the server runs only in a trusted local
environment. Requests do not require authentication, a password, or a client
key. This exception must not be used for a remotely exposed or production
server.

### Database Technology

Room (Kotlin Multiplatform) on the server, reusing the applicable entity and DAO
definitions from the client. The MVP must isolate server data by account; the
server opens a separate SQLite database file for each account. Account
identity and the mapping to database files are maintained separately from
the account's deck and card tables.

### Architecture Model

Offline-first. Room remains the working database on the client. All operations
happen locally first. The server is the authoritative source when both are
available. The app must be fully functional without server connectivity.

### Sync & Conflict Resolution

The application remains offline-first: each client applies user changes to its
local Room database first and remains fully usable without the server.
The active app dependency bindings must use that same selected account or guest
Room database for visible decks/cards, account sessions, reminders, outbox, and
manual synchronization. Do not enable the Room/REST UI while ordinary local
deck/card edits still bypass the account outbox; guest edits create no account
outbox operations. The Firebase synchronization worker must not run in the
new account path.

Before making the new path the default, verify the account-scoped bindings in
an explicitly enabled developer test configuration. Its Android application
identity and Desktop local-data directory must be separate from the installed
legacy app so tests cannot overwrite or display existing user data. The normal
configuration remained on the legacy source until the disposable-data checks
passed. Those checks now permit the ordinary Android/Desktop default to use
the account-scoped Room/REST path without a storage-mode opt-in.

For the clean-install cutover, an explicit developer-local storage-mode switch
may select the account-scoped Room/REST path in an ordinary Android or Desktop
build. It defaults to Room/REST; an explicit `legacy` override remains available
for developer recovery. Both default and explicit Room/REST selection must refuse account-scoped startup
when that installation's legacy Room database file already exists; never show
an empty account/guest database over an existing local database. The isolated
test package/directory remains separately enabled regardless of this switch.
The switch does not migrate, delete, or overwrite the legacy database.

Phase 1 synchronization is started manually by the user. The application must
show a persistent synchronization-status indicator so the user can distinguish
at least: synchronized data, pending changes, synchronization in progress,
a conflict requiring a decision, a synchronization error, and an unavailable
server connection. The MVP does not perform automatic synchronization on app
open, app close, or in the background.

The MVP has no user-facing "Cancel synchronization" action. A network failure
or bounded timeout ends the current attempt; the user can start the next
attempt manually. Already committed server operations remain committed and
are recognized by their durable operation IDs on retry.

While synchronization of an account is active, the client must prevent user
actions that add, edit, or delete that account's decks or cards. Existing
content remains readable. Editing becomes available again when the attempt
finishes, fails, or times out; a conflict-resolution decision is made after
the active attempt has ended.

The synchronization indicator is hidden in guest mode, where no account is
selected and no server synchronization is available. It appears for a signed-in
account, including when the server is unreachable.
Before an account's first successful manual synchronization, a connected client
must show that a download is still needed, even if both the local default and
the imported server baseline have revision 0. Matching revision numbers alone
must not show "Synchronized" without a durable local checkpoint. An
unreachable server still shows the unavailable state.

The indicator uses these state meanings:

- green: the local data matches the last known server revision and there are no
  pending local changes;
- yellow: local changes are pending upload and/or the client knows that newer
  remote changes are available;
- red: a synchronization error occurred or a conflict needs a user decision;
- gray: the server or synchronization event channel is unavailable.

The exact placement and whether the unavailable state is a gray dot or a
crossed-out database icon remains a UI design decision.

Tapping the indicator must open synchronization details. At minimum, the details
show the local data state, the last known server revision, pending local changes,
and an action to start synchronization.

Phase 1 includes a list of registered clients. Each Android or Desktop client
must have a stable device identifier, stored locally and sent with
synchronization requests. The server must retain a device record and display
name so synchronization details and conflict information can identify changes
from another device. Device records are informational in this unauthenticated
MVP and must not be treated as authorization.

The application assigns a readable default display name when registering a
device, such as an Android or Desktop name. The user can rename the device.
For the MVP REST contract, `PATCH /api/v1/accounts/devices` renames an existing
device without changing its ID, platform, or synchronization position;
`DELETE /api/v1/accounts/devices` removes its registration. Both requests
identify the account and device explicitly, and a changed device list is
published on `/sync-events`.

For every registered device, synchronization details show its display name,
platform, current `/sync-events` connection state, last successful
synchronization time, and the last server revision that device confirmed.
The server records this confirmation only after the client has applied a
conflict-free synchronization result to its local Room database. The client
reports the confirmed revision through REST; a repeated report is idempotent,
and an older report must not move the device's position backwards. A server
response or accepted write alone does not prove the client applied the result.
Registration and synchronization state are separate for each account, even
when two accounts are used from the same physical device. One account's device
list must not expose the other account's registrations or sync position.

The user can remove an obsolete registered device, for example after an app
reinstall. Removing a device deletes only its technical device record; it must
not delete decks, cards, or server change history.

Synchronization uses a server revision and client-side change operations, not
replacement of one complete database snapshot with another. A client records
each unsynchronized local operation in an outbox in the same Room transaction
as the local data change. Each operation has a durable, unique ID and identifies
the server revision on which the local change was based.
While an explicit manual synchronization request is in flight, account deck
and card writes are paused. The pause begins after any already-started local
transaction completes and ends on success, failure, or cancellation. Guest
edits remain independent. A failed write attempt must not leave a partial
local change or outbox entry.

The server revision is the source of truth for synchronization ordering. Each
synchronized deck and card records the server revision in which it was last
changed. Timestamps may be stored and displayed to the user, but must not decide
which change wins because device clocks are not reliable for conflict handling.

The legacy local `StorageSaveVersion` is used only by the manual migration of
the old Android database and does not participate in the new synchronization
protocol. After migration, clients use the shared server revision.

The server applies an accepted batch of operations, advances its revision, and
records the resulting changes atomically in one SQLite transaction. When the
server has advanced since the client's base revision, it compares the client's
pending operations with the server changes made since that revision:

- independent changes are merged automatically;
- overlapping or semantically dependent changes are returned to the client as
  a structured conflict and are not applied automatically.

In the MVP, any concurrent changes to the same deck or the same card conflict,
even when they modify different fields. Automatic merging applies to changes of
different independent entities.

New cards may be added, and existing cards may be moved into a deck, only while
that destination deck has had no reviews (`reviewCount == 0`). Once its first
review has occurred (`reviewCount > 0`), both incoming actions are prohibited.
If a card-addition form is already open, a rejected save must explain that
cards cannot be added after the deck's first review, rather than show only a
generic storage error. Keep entered card data available and do not queue a
sync operation for that rejected save.
Existing cards in a reviewed deck may still be edited or deleted, and moving a
card out of it is allowed. The restriction applies to incoming-card actions,
not to restoring or downloading an existing reviewed deck and its cards during
initial import or synchronization. This replaces the earlier
proposal to change a reviewed deck's next review time and interval when a card
arrives. The superseded design is preserved only in
`server-database-deferred-card-addition.md` and is not part of this MVP.

When a card is moved, the existing local transfer rule recalculates the last
review-pass durations of both source and destination decks. The MoveCard sync
operation must carry those final values in the same atomic change so that the
server and every client obtain the same deck state. Card-count changes still
come from the actual cards, not from a separately edited count.

The card-moving destination picker lists only decks with `reviewCount == 0`.
Before committing a local card creation or move, the domain operation checks
the destination's current stored `reviewCount` inside the same transaction as
the write; a stale deck snapshot passed from UI cannot bypass the restriction.
The server enforces the same rule when accepting synchronization operations.
The test-first suite replaces existing transfer tests that expect a move into
an already reviewed target with allowed-target and rejected-target cases.

After the user starts synchronization, different cards added to the same
still-unreviewed deck on different devices can merge automatically. Recalculate
the deck's stored card count from its resulting cards; a count changed only by
card additions must not create a false deck conflict. A stale addition against
a deck already reviewed on another device must not bypass the restriction;
return a conflict, retain the pending card, and keep the other device's
completed review. The conflict screen offers to place the card in a newly
created deck or another existing unreviewed deck. Apply the same rule to a
stale move into a destination that has since been reviewed: preserve the
card's identity and data until the user selects an allowed destination; do
not silently delete it or insert it into the reviewed deck.
For this resolution, offer an existing still-unreviewed deck or a new deck.
Apply the server's intervening changes before rebasing the move, keep the
card's global ID and all card fields, and create a new operation ID against
the current revision. Revalidate the chosen destination inside the account
Room transaction; a stale or reviewed selection must leave the saved conflict
and pending move intact. Creating a destination and moving the card must be
one local transaction, with AddDeck ordered before MoveCard for REST. A lost
response after server commit must be retryable without a duplicate deck or
card. Preserve the review-duration redistribution that a normal move would
have made for the newly selected destination.

If one device completes a review of a deck while another device independently
removes or moves cards in that deck before seeing the review, return a conflict
instead of silently combining the changed deck membership with the review
result. The conflict UI must explain that the completed review covered the
prior deck contents and identify the membership change. The removed-card and
source-deck move resolution actions are defined below.

For an independently removed card, do not merge the deletion with the completed
review automatically. Explain that the review result and schedule were
calculated before that card was removed. Let the user keep the removal and
either retain the completed review schedule or make the remaining deck due
for review now. Preserve the completed review record in both cases.
Resolve either choice only after applying the server's completed review to
the local account database. Rebase the card removal with a new operation ID
against the current server revision and send it through the ordinary REST
sync path. The due-now choice additionally records a distinct, replay-safe
schedule adjustment. It replaces the deck's current next-review date with
the server's current time, while preserving review count, pass dates,
interval/duration fields, and the latest completed-review summary (including
the date originally scheduled by that review). A repeated request must not
append another review or apply the removal twice. An interrupted request
leaves the rebased operations pending for manual retry.

If a card was moved from deck A to deck B on one device while another device
completed a review of A, show the conflict rather than discarding the move or
silently applying A's review schedule. The user may keep the move and choose
whether A retains the completed review schedule or becomes due for review
again. Preserve A's completed review record. The move into B is allowed only
if B is still unreviewed; a stale move into a reviewed B must not bypass the
incoming-card restriction and requires conflict resolution.

If one synchronization request contains both independent and conflicting local
operations, the server applies the independent operations and returns conflicts
only for the affected operations. Accepted operations are committed atomically;
conflicting operations remain pending locally until the user resolves them.
The client keeps a conflict response for the selected account in its local Room
database until it is resolved or superseded by a later response. Closing and
reopening the app must not lose the conflict details or pending operation IDs.
Saving an unresolved response alone does not advance the confirmed revision or
discard pending operations. When the client can safely separate independent
accepted changes from the conflicted aggregate, it may apply those changes,
acknowledge only their operation IDs, and advance the checkpoint in one local
transaction while retaining the original conflict response and conflicting
operations for resolution. A later complete resolution clears the saved
response in the same transaction as its local data and checkpoint update.
For the MVP, once part of a conflicted response has been applied and its
checkpoint advanced, prevent new deck/card edits in that account until the
saved conflict is resolved. Reject the write before changing any local row or
creating another outbox operation. This temporary pause does not affect guest
data or other accounts, and editing resumes after resolution.

The server records accepted operation IDs and their outcomes in the same
transaction as the data changes. If the client loses the response after the
server commits, it cannot infer that the transaction rolled back. It keeps the
operations pending until a later manual sync; when the same IDs are submitted
again, the server returns their previous result without applying them twice.
One server transaction protects an accepted request/batch, not an entire
multi-request synchronization session. A lost network connection or client
cancellation cannot undo a transaction that has already committed.

Opening an existing card editor is not itself a user edit. An unavailable,
empty, or invalid automatic Word Insights result must not rewrite that card or
create a pending synchronization operation. A valid new result may be saved
and synchronized as card data.

One user action that changes several records is represented as one grouped
synchronization operation. For example, deleting a deck and its cards is one
operation in the outbox, server change history, and conflict-resolution UI.
Moving a card between decks is also one grouped change: update the existing
card's deck reference while preserving its local and global identifiers, text,
insights, mnemonic, and image asset reference. Update both affected decks'
derived card counts and review aggregates consistently. Do not model a move
as deleting one card and creating another. The current
`TransferCardsToDeckUseCase` and its test that expects a new ID must change
when this behavior is implemented.

The conflict response must contain enough information about both the local
pending operations and the intervening server changes for the client to explain
the conflict. The client shows one conflict-resolution screen; it must list the
specific affected changes, including added, changed, and deleted decks and
cards. For a card-text conflict, show both the native and foreign text from
each side so edits to either field are visible before the user chooses. The user can:

- accept the server changes and discard the affected local pending changes;
- keep local changes and submit them again against the current server revision;
- resolve the affected changes manually in the conflict-resolution screen.

In manual resolution, the user chooses a result for each conflict separately.
A bulk keep-local action must satisfy the same entity-identity and deletion
checks as the corresponding per-item choice for every affected conflict.
An edited card that moved to another deck on the server cannot be kept as an
ordinary text edit; unrelated or deleted server entities must not enable this
action. Deleted-deck restoration remains a separate structural choice.
A deck deletion and its affected cards are presented and resolved as one group.
When the user accepts the server for every conflict in the current response,
apply its complete delta, retain independently accepted server changes, remove
the corresponding old outbox operations, clear the saved conflict, and advance
the checkpoint in one local transaction. If the selected account, checkpoint,
or pending operations changed after the response, do not apply that stale
resolution over the newer local state.

There is no separate "complex conflict" mode in the MVP. Deck deletion and its
affected cards must be represented clearly in this same screen.

After the user chooses a conflict resolution, the client immediately repeats
synchronization for the remaining pending operations and updates the visible
synchronization state with the result.

If synchronization fails because the server or network is unavailable, pending
local operations remain on the client. The MVP does not retry automatically;
the user starts the next attempt manually.

Synchronization details show the user a short, understandable description of
the latest error. Detailed diagnostics are written to application and server
logs without logging card content.

Every synchronization request declares its protocol version. The server must
continue supporting older protocol versions when their operations can be
interpreted safely. If a request is genuinely incompatible, the server rejects
it without changing data and returns a structured update-required error.
An optional nullable review-summary field may be omitted by the client JSON
serializer; the server must interpret its absence as `null`. The isolated
test server must use the same JSON null/default handling as the ordinary
server so an actual Android review upload behaves the same in both modes.

The server supports the current synchronization protocol version and one
previous version. Older versions require an application update.

`Deck` is an aggregate with its `Card` children. Deleting a deck also deletes
all cards in that deck. Conflict detection must therefore treat a deck deletion
as conflicting with changes to any of its cards, including an add or move of a
card into that deck. If the user ultimately keeps a deck deletion, the server
must delete the deck and all of its cards atomically so no orphan cards remain.

Each card belongs to exactly one deck at a time. Moving it updates that one
deck reference; the MVP does not support the same card belonging to multiple
decks simultaneously and does not need a deck-card junction table.
The new Room schema declares a foreign key from the existing
`RoomCard.deckId` column to `RoomDeck.id` and a non-unique index on
`RoomCard.deckId`. Neither adds a new user-data column or a junction table.
Validate existing data before enabling the constraint in the Room migration.
If a legacy card references a missing deck, stop without modifying the old
database and report the card and deck IDs so the saved data can be repaired
before retrying; do not delete that card automatically. After migration, card
insert/update cannot reference a missing deck.

Also detect a conflict when one device moves a card *out of* a deck based on
an older server revision while another device deletes that source deck and
its then-present cards. Do not silently delete or resurrect the moved card.
The conflict screen must name the affected card, source and destination decks,
and the devices' concrete actions. Offer at least these outcomes: retain the
source deck's deletion while restoring the moved card in its destination
deck if that destination is still unreviewed (otherwise select another
unreviewed deck), or accept the server deletion of the card. The selected outcome must be
applied without leaving orphan cards or duplicate card identities.

The automatically created interim deck is one shared deck per account, and
its real cards participate in normal manual synchronization. Guest mode has
its own separate interim deck. Each client may continue using local integer
ID `-1` for its special navigation behavior, but that number is not a
cross-device identity: account clients must resolve the same stable sync ID
for the account's interim deck and must not create duplicates on first sync.
The account-scoped interim sync ID is derived from the normalized, immutable
MVP account email; the guest interim keeps its separate local sync ID. On
sign-up, transfer remaps the guest interim deck to the account ID while
preserving its cards and queues a normal first upload. On another device,
creating the same account interim deck and uploading it later must not create
a duplicate or block independently added cards. The server need not insert an
empty interim deck at account creation: the first accepted manual upload
creates the one shared row.
The one-time Android backup import must also recognize the legacy interim
deck's local ID `-1`, assign its canonical account sync ID, and preserve its
cards and review data. Server-local integer IDs may differ from the backup;
other negative deck IDs remain invalid.
The local existence check and creation must use the same selected database
and transaction, including when an account switch is requested concurrently.

If a deck was deleted on one device while another device changed a card in it,
and the user chooses to keep the latter device's local changes, the server
restores that device's complete deck with its cards as one operation. A card
must never be restored without its parent deck.

Every synchronized entity must have a globally unique, immutable synchronization
identifier generated on the client. The sync identifier is used by Android,
Desktop, and the server to refer to the same entity. Existing local integer IDs
may remain for local storage and UI use during the migration, but must not be
used as cross-device identifiers. A new local deck or card receives its sync
identifier when first persisted; subsequent local edits and moves preserve it.
The last-changed server revision remains unchanged by an unsynchronized local
edit and changes only when the corresponding server result is accepted.

Synchronization metadata is stored separately from user decks and cards. The
server database includes technical records for registered devices, the current
server revision, and the change history. Each client database includes technical
records for its device identity, last confirmed server revision, and pending
outbox operations.
The client persists the confirmed revision in the selected account's Room
database, never in the legacy `StorageSaveVersion`. Applying a server response
must update that revision and remove accepted outbox operations in the same
local transaction; a failed local apply leaves both unchanged for retry.
An explicit manual sync sends the selected account's durable pending operations
and its confirmed revision through REST. Before applying a conflict-free
response, recheck the selected account and pending operation IDs so a sign-out,
account switch, or new local edit during the request cannot overwrite another
database or silently acknowledge the wrong snapshot. Transport failures leave
the outbox unchanged; retry uses the same operation IDs.
After a successful manual pull, the already open deck list must display the
downloaded decks without requiring an app restart or navigation away and back.
Applying a conflict-free server delta must preserve existing local integer IDs
by matching immutable sync IDs, remove deleted cards before their decks, and
insert parent decks before cards. Reject a delta that refers to a missing
parent deck or has inconsistent deck card counts; do not advance the confirmed
revision or acknowledge operations when the local apply fails.
An ordinary local edit records its outbox operation against that same confirmed
revision; a stale caller cannot write a change with a different base revision.

The MVP retains the complete server change history and does not automatically
delete old change records. History cleanup or compaction is deferred until it
is needed in real use.

Synchronization details show a concise recent history, initially the latest 20
entries. Each entry identifies the device, action, affected deck or card, time,
and server revision. This history is shown in normal use as well as supporting
conflict investigation. The client fetches the recent history through REST when
the user opens synchronization details; `/sync-events` only signals that newer
server state is available.
The history read is scoped to the selected account and requires a registered
device, like the existing sync/bootstrap endpoints. It returns identifiers and
metadata rather than card contents; opening details does not start a sync.

### Data Scope

Phase 1 (this implementation): `Deck` and `Card` data plus the new server
revision and sync metadata. The legacy `StorageSaveVersion` is read only during
the manual Android database migration.

Detailed deck review information currently stored in the device-global
`DeckRepetitionInfo` DataStore must move into the account's Room data and
synchronize between that account's devices in Phase 1. Do not keep
per-deck review information in DataStore. Store the latest review summary in
the `Deck`/`RoomDeck` row: reuse existing review fields where their meaning
matches, add only missing fields, and do not duplicate values in a one-to-one
table. Review completion must update the deck and its pending sync operation
as one local Room transaction. A separate review-session table is reserved for
a future requirement to retain every historical review, not this latest
summary. The review-summary date is the date captured when the review ended;
later edits to the deck's current schedule must not rewrite that snapshot.
The review screen must keep Start and difficulty-choice buttons fully reachable
above Android system navigation, including when its statistics row is shown;
shorter viewports may scroll rather than clip these actions.
Reviewing a deck with exactly one card must accept each difficulty choice
without crashing and must preserve that card while advancing or finishing the
review according to the existing scheduling rules.
The review synchronization operation must carry the deck's exact updated
review-pass and scheduled-date lists. After the first pass of a two-pass
iteration, the server must not invent a completed iteration date.
The manual bootstrap must include any existing DataStore review information
before the Android app is reset; exporting only the current Room database
would omit it. When the source installation has no such file, an explicitly
selected no-review-backup mode imports the Room deck/card data without
fabricating latest-review summaries. Existing Room review counts, iteration
dates, and schedules remain; the separate latest-review summary is absent.
Do not silently enter this mode when a specified DataStore path is missing.
When a DataStore export exists, read it without changing the backup, and
reject orphan or duplicate review entries or a review count that disagrees
with its deck before changing the target account database.

Phase 1 also replaces Firebase-backed sign-up/sign-in with the agreed
passwordless server account flow. Firebase Crashlytics remains in use and is
not part of this migration. Firestore-backed word autocomplete is secondary:
disable the autocomplete feature in Phase 1 so the UI does not invoke it or
present it as working. Existing implementation code may remain for a later
stage, but it must not make Firestore autocomplete requests while disabled.
Reconsider its server-side replacement only after deck/card persistence and
synchronization have been verified. This staged scope does not promise
complete removal of Firebase dependencies.

The new server starts with an empty database. Initial server data is loaded by
a manual developer-assisted bootstrap procedure, not by a client UI flow. The
user exports the existing Android local Room database, and a migration script
reads that export, handles applicable schema differences, creates the account
identified by an email specified for the import, and populates that account's
server database. After reinstalling, the user signs in with that same email,
registers the fresh device, and starts the first manual download. Normal
client synchronization begins only after that
bootstrap procedure is complete. Direct migration from Firestore to the server
is not part of the MVP.
The import publishes the account only after its staged deck/card database is
complete; an invalid backup must not leave a visible partial account. Imported
data is the revision-0 baseline, with no registered client device until the
fresh Android or Desktop installation explicitly registers through sign-in.
Because this baseline has no accepted-change history, an empty newly signed-in
client must download the complete server snapshot on its first manual sync,
including when both local and server revisions are 0. It applies the snapshot
and checkpoint in one Room transaction, then confirms the revision to the
server. Subsequent manual syncs use the incremental change protocol. A client
with pending local edits or an unresolved conflict must not replace them with
the snapshot.

Before changing the server database, the bootstrap validates that every card
references an existing deck. If orphan cards or other structural inconsistencies
are found, abort the import, report the affected records, and leave both the
source backup and target server data unchanged. Repair the migration input or
script deliberately and rerun; never silently drop cards or guess a parent deck.
Read and migrate a disposable copy of the Android Room export, never the
recovery backup itself. Require a checkpointed export without an outstanding
WAL file and reject card JSON that cannot be represented without loss instead
of quietly replacing it with empty data. Run the one-time import while the
server is stopped.

The user-provided `C:\Users\<user>\Desktop\klaf_backup` is permanently
read-only for assistant operations: do not open its databases with SQLite,
checkpoint, migrate, edit, move, or delete its files. Copy inputs into a separate
working directory before any database operation; compare original file hashes
before and after preparation/import. Preserve all original DB/WAL/SHM files.

The exported Android database is retained outside the app as a recovery backup.
After bootstrap, the source Android app is reset or reinstalled rather than
attempting to reconcile its old local database with the new server database.
The fresh app registers as a new device for the migrated account and downloads
the initialized server data on the user's first manual synchronization.

For the later password-authenticated server, the operator may instead import
this legacy backup into an already registered account. That one-time path must
preserve the account's authentication, device registrations and sync continuity;
it must reject a target with populated content rather than merge ambiguously,
and must not run the old new-account bootstrap against the live registry. Use a
verified disposable copy of the Room/WAL/SHM export,
leave the original backup unchanged, and require an explicit no-review choice
when the matching review DataStore is absent. Import saved mnemonic image
files separately from card metadata, preserving asset IDs and checking file
contents. Validate the complete result and its availability to the connected
Android device before discarding any working copy.
After the one-time import is verified, remove its temporary executable tooling
from the server module; retain the migration record and recovery backups.

Desktop follows the same first-connection process: it starts with an empty
local database, registers as a new device, and downloads the initialized server
data when the user starts its first synchronization. The manually imported
Android database is the only initial data source.

The first-sync setup assumes the newly installed client has no user-created
local data. The user will prepare and verify the clean installations manually;
the MVP does not provide a special merge flow for data created before the first
synchronization. If unrelated local edits already exist when a revision-0
server baseline must be downloaded, the client must stop with an explicit
error and retain those edits; it must not report success while omitting the
baseline. A retry after the server accepted ordinary operations at a later
revision must still use the incremental protocol.

Automatic server database backups are out of scope for the MVP. The manually
exported Android database and the server database file are retained as manual
recovery copies. Automated backup scheduling is deferred.

Branch integration decision (2026-09-28): Vocabulary Sources, items and Ignored
Words are account-owned local data. Sign-up transfers the complete guest feature
data together with decks/cards, preserving links and supporting retries and
source-only guest databases. Sign-in leaves guest data untouched. Integration
tests cover ownership, transfer and source-to-card compatibility. Cross-device
source synchronization was authorized as the next phase on 2026-09-29; its
whole-source conflict/TDD requirements are in `klaf-server-requirements.md`.
The user permits a one-time reset of old source/voice data only, not of deck/card
or sync data and not of newly created future sources.

The older Phase 1 scope below explains the storage branch's original boundaries;
the integration and follow-up decisions above supersede its deferral of source
ownership, synchronization and tests.
Later phase: `VocabularySource` and `VocabularySourceItem`. Their screen and
Room tables already exist in this branch and remain accessible; Phase 1 does
not hide or disable them. Phase 1 does not import, synchronize, or test source
records or source-item records, nor guarantee their cross-device availability.
Using that experimental screen can still create ordinary Card rows, but the
source-to-card workflow is not covered by Phase 1 compatibility testing.
The user will manage a separate backup of any source data they want to retain
and clean up existing source data before that later phase. A new baseline
schema/import with merged backup values is a possible later approach, not a
decided migration design. Their links to deck/card integer IDs are likewise
deferred; Phase 1 must not mistake these tables for part of its deck/card
bootstrap.

2026-09-28 scope extension: display existing mnemonic images on Android and
Desktop. Copy saved image files from a working backup into account-specific
server file storage, keeping the original backup immutable. Retrieve missing
images on demand through REST and cache them per account for offline display.
Reuse existing card-management and review image UI. Missing files or network
failure must not prevent use of the card; cancellation must remain cancellable.
The read endpoint requires a registered device and a matching card reference,
rejects unsafe file identifiers, and never exposes another account's files.
2026-09-28 further scope extension: manually synchronizing a card with a new
saved image uploads that file through REST before publishing its card change.
Image asset IDs are immutable: retries with the same bytes are idempotent;
different bytes under an existing ID are rejected, never overwritten. If an
upload fails, keep the local card/outbox changes for retry and do not publish
that sync batch. Only saved images referenced by pending card changes are
uploaded, not unused drafts. Downloads reuse the existing account-scoped cache.
Legacy missing image files remain valid references and do not prevent data
sync; absence of bytes is not converted into deletion. Uploaded files from
failed or rejected card changes are retained for safe retries; remote deletion
and garbage collection remain deferred. Existing Firebase image-sync code
must not be activated for the new Room/REST path.

Sign-in, sign-up and device registration must show actionable error messages
for an unreachable server, timeout, missing account, existing account, device
registration error and invalid/server responses. Expected cancellation is not
an error; request timeout must not leave the UI stuck loading. Preserve the
explicit device-registration confirmation flow and retryable sign-up state.

Phase 1 still preserves each card's mnemonic image asset identifier. The
identifier is the stable file name/reference required by the later image-sync
phase; image delivery is separate from the deck/card revision transaction.

If a card references an image file that is not available on the device, the
card remains usable and is displayed without that image. This is not a
synchronization error.

### Transport

REST API for database CRUD and synchronization operations. The existing
WebSocket protocol (`/ws`) remains for AI operations only (WordInsights,
Mnemonic, VocabularySource analysis). The separate `/sync-events` WebSocket
channel provides real-time synchronization events. Both transports coexist in
the same Ktor server.

For this MVP, REST must expose synchronization operations. Database CRUD is an
internal implementation concern of the client and server rather than a separate
public API required by the UI flow.

Android and Desktop receive the Klaf Server endpoint from the existing
Git-ignored client `data/src/commonMain/.../SecretConstants.kt` file. Both
clients use its `KlafServer.HOST`, `PORT`, and `IS_SECURE` values; neither
Android `BuildConfig` nor a Desktop runtime read of `local.properties` is the
endpoint source. There is no in-app endpoint settings screen. The same shared
source can be used if iOS later gains Klaf Server integration.

The real public hostname must not enter tracked source, documentation, tests,
or Git history. A committed example may show only placeholder values. The
client application binary still necessarily contains its configured endpoint;
server-side authentication, not hostname secrecy, controls access.

The root `local.properties` may continue to hold unrelated developer-local
settings and must not be committed to Git. The remaining client setting includes:

- `klaf.client.storage.mode`, optionally `room` (default) or `legacy`, used
  for the guarded clean-install client cutover.

For the Linux deployment, both clients use external HTTPS/WSS through
Cloudflare Tunnel; `cloudflared` reaches the server over loopback HTTP on the
same host. Client REST, WebSocket authorization, sync events, and stored
credentials must use one consistent external origin. A default HTTPS port
must not create a different textual DPoP origin. Plain HTTP/WS remains
available only for explicit local development configurations.

The synchronization WebSocket is an event channel, not a second write API:

- REST request/response performs synchronization, applies changes, and returns
  conflicts or data required by the client;
- the WebSocket notifies connected clients when the server revision changes,
  when new remote changes are available, and when registered-device state
  changes;
- after a remote-change event, a client updates its status indicator but does
  not automatically apply remote data or overwrite local data; the user still
  starts synchronization manually;
- if the event channel is disconnected, REST synchronization remains fully
  functional after reconnection or an explicit user action.

After a server restart or network recovery, the client reconnects to
`/sync-events` automatically and refreshes the displayed server state. Reopening
the event channel does not start data synchronization; the user still starts
that manually.

The UI may show a client as connected only while its synchronization WebSocket
is connected. This is not a general device-online guarantee, especially for a
mobile app in the background.

When a client connects to `/sync-events`, the server immediately sends the
current synchronization state, including the current server revision and the
registered-device information required by the UI. The client updates its own
device record on connection so other connected clients can receive current
device-state information.

### Security

Authentication and authorization are explicitly deferred beyond the first
local-development MVP. Multiple accounts in this trusted-LAN MVP are separated
logically, not protected from other people on the network: a client can claim
another account's email. Before the database REST API is reachable outside the
trusted local environment, the server must authenticate requests and derive the
selected account from the authenticated identity rather than trusting a
client-supplied identifier.

### Test-First Delivery

Review regression requirements (2026-09-28): applying a delta that moves a card
out of a deleted deck must preserve its existing local ID; a delta cannot
retain a card in a deleted parent. A WebSocket connection's own timeout must
allow reconnection, while cancellation by the caller must still stop it.
Image byte reads must discard results if the selected account changes during
the read, and local saved-image imports must reject unsafe asset identifiers
before writing files. These strengthen existing identity/isolation rules;
image-specific conflict previews remain deferred.
Manual Room/REST sync must attempt the configured server even if Android has
no active network transport: localhost and USB reverse can still be reachable.
REST failures determine availability and preserve pending changes; the legacy
Firebase worker retains its existing connectivity check.
Android conflict-screen and scoped-reminder instrumented tests must refuse
to run against the ordinary application package, before launching the test
activity or modifying reminders/data; use the isolated test package only.

Implementation follows test-driven development. After requirements and protocol
decisions are complete, write the tests for the target behavior before writing
the corresponding production implementation. Do not adapt tests merely to make
already-written implementation code pass.

The initial test suite must cover the sync behavior agreed in this document,
including local add, update, and delete operations; deck deletion together with
its cards and full-deck restoration after a deletion conflict; stable
card identity and complete card data during a move between decks;
synchronization identifiers; outbox and server-revision
behavior; automatic merge of independent changes; conflicts and all three user
resolution choices; REST failure behavior; and synchronization WebSocket state
events. Implementation begins only after these scenarios are specified in
tests.

Treat cross-device and interrupted-sync corner cases as high-risk tests, not
optional happy-path coverage. Before implementation, specify tests for both
arrival orders where relevant: independent card changes; move out versus deck
deletion; deck deletion and full restoration; first review versus a stale card
addition, and review completion versus card removal/move; lost HTTP response
after server commit and duplicate
request IDs; partial batch application with conflicts; account switching and
guest-data transfer; first import and invalid references; shared interim deck;
and notification isolation between accounts. Assert that no scenario loses a
card, duplicates an operation, creates an orphan, mixes accounts, or silently
changes review scheduling. Keep these tests in the test-first suite before
production implementation.

UI tests are optional in the MVP. Add them only for lightweight, stable behavior
such as mapping a synchronization state to the correct indicator and invoking a
conflict-resolution action. Do not delay implementation for pixel-level or
fragile visual tests.

Use unit tests with fakes for pure synchronization and conflict-decision logic.
Use integration tests with a temporary real SQLite database for Room/DAO,
repository, transaction, revision, change-log, and cascade-deletion behavior.
REST and `/sync-events` protocol tests must exercise the Ktor server with its
real server-side integration stack. These test layers complement each other.
The complete client flow is verified in the server test module, where a real
temporary Ktor server and account-scoped Room client can run together. Avoid
an in-memory substitute for either side merely to satisfy an umbrella
contract. A test contract may be retired only after its individual scenarios
are mapped to executable, focused tests and any uncovered scenarios are added.
The old Room-v8 migration remains tested as a schema migration, but the agreed
bootstrap does not perform an in-place transfer of old device-global
DataStore review summaries into a newly selected account database: the source
backup is imported to the server, then clean client installations download it.

## Open Questions

*(Tracked here until resolved, then moved to Agreed Requirements.)*

- Deferred by the user on 2026-09-28: image-specific conflict details in the
  conflict resolver, including an indication that the selected image differs
  and side-by-side local/server image previews. Do not implement this now.
  The current whole-card local/server choice remains unchanged; independent
  per-field or image-only resolution is not an agreed requirement.
- Define automatic deletion/retention and garbage collection of mnemonic image
  files in a later phase; upload and download are now included.
- Define automated server backup scheduling and retention in a later phase.

## Decisions

*(Final decisions recorded here.)*
