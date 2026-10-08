# Manual Android Backup Bootstrap

## Existing password-authenticated account (2026-10-07, completed)

The one-time offline importer into an approved account was removed after the
successful migration. It is not a supported repeatable Gradle task. Do not
point the older new-account `importAndroidBackup` task at an existing account.
Restore from the preserved server-storage backup if this migration must be
recovered.

The live import followed a disposable-snapshot trial:
57 decks, 1093 cards and 19 saved images at server revision 2. The server DB
passed SQLite integrity and foreign-key checks; the PNGs matched the source
hashes. Android manual sync advanced its confirmed revision from 1 to 2 and
the imported decks appeared. Opening one saved image on the device remains
the final visual check.

## Historical new-account bootstrap

This is a developer-assisted, one-time import. Do not run it on the real backup
until the export and target server root have been checked. Keep the original
files outside the repository as recovery copies.

Machine-specific paths and account/device IDs below are placeholders, not
literal command arguments. Use your own paths and account when running tools.
The development machine's original handoff notes are kept only in the ignored
`.local-notes/precommit-privacy/` directory; never stage or publish that folder.

## Inputs

### Ordinary-server import completed on 2026-09-28

The user-approved imported account (personal email omitted) now contains 57 decks and
1093 cards at server revision 0 in `C:\Users\<user>\.klaf-server`.
The protected source `C:\Users\<user>\Desktop\klaf_backup` was only read and
copied. All 35 original files match their pre-import SHA-256 values. Working
copies, the hash manifest and the checkpointed Room export are kept at
`C:\Users\<user>\Desktop\klaf_import_account_20260928`.

Registry/account integrity checks passed. Card contents, deck schedules,
counts and parent references match the source, allowing the intentional
legacy interim-deck ID remapping. The source has no review-info DataStore,
so explicit no-review mode was used. Image references remain in card data.
The later 2026-09-28 image-display extension copied all 19 saved PNG files
from the verified working backup into the account's server image directory:
`C:\Users\<user>\.klaf-server\accounts\<account-database-id>.db.images`.
The original 35 files and each copied image were hash-verified. No database
rows or revision were changed by image deployment.

### Image delivery and manual uploads

Saved files live beside an account DB in `<database-file-name>.images`, named
`<imageAssetId>.png` (also supported: jpg/jpeg/webp). Do not put drafts in this
directory. The download endpoint is
`GET /api/v1/images?email=...&deviceId=...&assetId=...`. It checks account/device
registration and a matching current card reference; safe ASCII identifiers
and a 16 MiB file-size limit are enforced. File links and non-image signatures
are not served. `PUT /api/v1/images` uses the same account/device/asset query
parameters and a binary image body. A registered device may upload before the
card metadata exists; downloads still require a current card reference. Asset
IDs are immutable: equal-byte retries return success, differing-byte reuse is
rejected. The server stages new files before publishing them. This is not
authentication: the trusted-network MVP's existing
account/device identifiers remain the access model.

Android/Desktop manual synchronization downloads all saved images referenced
by the resulting account cards before reporting success. Downloads are cached under
`mnemonic-remote-cache/<SHA-256-of-account-email>/mnemonic-images/saved` in
the client app's storage. Cached images remain available offline. A missing
file or unavailable server makes synchronization fail; repeat the manual sync
once connectivity returns to retry only missing files. Card/review screens do
not download files themselves. During manual sync, saved
images referenced by pending AddCard/EditCard operations are uploaded before
the metadata request. Failure leaves the outbox intact for retry; immutable
file IDs make a lost successful upload response safe to retry. Legacy missing
files are skipped without deleting their references. Unused drafts are not
uploaded. Server files are not removed when cards are deleted, and orphaned
uploads from failed/rejected metadata are retained; garbage collection is
deferred. Do not activate legacy Firebase image sync as a substitute.

The ordinary server was started on `127.0.0.1:8090`. This account has no
registered devices yet. On Desktop, use **Sign in** with this email, confirm
device registration if prompted, then manually synchronize. **Do not Sign up
or repeat the import.** Keep the primary phone unchanged until the imported
content is checked. Server availability here is local to this computer;
primary-phone LAN access is a separate deployment step.

- A consistent, checkpointed Android Room `.db` export. Stop the Android app
  before exporting. A nonempty adjacent `-wal` file is rejected because the
  database file alone may omit recent writes. If the export includes a nonempty
  `-wal`, retain the original `.db`, `-wal`, and `-shm` together; create a
  checkpointed SQLite backup from a disposable copy of that set, and give only
  the checkpointed copy to the importer. Never import the `.db` alone in this
  case or checkpoint the recovery originals in place.
- The separate `DeckRepetitionInfos` DataStore file from the same installation,
  **if that source installation has one**. Without it, latest-review summaries
  are absent after import; Room review counts, schedules, and deck/card data
  remain. Select the explicit no-review mode only after confirming this is
  acceptable for the source backup. Never substitute a file from another
  device.
- A new account email and the intended server storage root. Stop the server
  before import, and use an email not already registered there.

## Run

From the repository root in PowerShell, use absolute paths:

```powershell
.\gradlew :klaf-server:importAndroidBackup `
  '-PbootstrapServerRoot=C:\path\to\server-storage' `
  '-PbootstrapEmail=user@example.test' `
  '-PbootstrapRoomBackup=C:\path\to\android-backup.db' `
  '-PbootstrapReviewBackup=C:\path\to\deck_repetition_info_file_name'
```

If the source installation has no review DataStore file, replace the last
property with `'-PbootstrapNoReviewBackup=true'`. The task requires exactly
one of these two options; it does not silently skip a missing review file.

The tool migrates a temporary copy of Room, validates any supplied review
data, cards, references, and IDs, then creates the account database. It never
edits either input file. Invalid input does not publish an account. Imported
content starts at server revision 0 with no registered device. After verifying
the result, reinstall/reset Android, sign in with that email, explicitly
register the fresh device, and perform the first manual download. Do not reset
the source app or discard either backup before that verification.

The reader's version-7 and version-8 Android Room paths, including the legacy
interim deck with ID `-1` and explicit no-review mode, are covered by
synthetic end-to-end import tests; later schema migrations are configured but
not all export versions were exercised. The supplied Room/WAL backup has also
been imported in a disposable server root in no-review mode: 57 decks and
1093 cards, with no orphan cards. The source files were preserved. This does
verify the first manual download into the isolated Android test package:
the imported decks appeared, including the interim deck's 92 cards, and a
repeat sync stayed green with no pending upload. It does not verify an
ordinary-app cutover. An opt-in Desktop integration smoke test also registered
a fresh device and verified the first download into an isolated Room database:
57 decks, 1093 cards, matching review schedules and card content, no pending
changes, and a stable repeat sync. The actual Desktop sign-in UI was not
automated. The isolated Desktop window was opened afterward and displayed
the imported decks, a card preview, and synchronized status at revision 0.
The original backup files remain untouched.

## Guarded client cutover

Ordinary Android and Desktop builds now default to account-scoped Room/REST;
no storage-mode property or Gradle flag is needed. After verifying the server
import, prepare a clean installation before installing/launching these builds.
The app refuses the Room/REST path when that
installation still contains `klaf_kt.db`; this switch does not migrate or
delete the old file. The isolated Android package and disposable Desktop
directory remain available without changing this property. Do not enable the
ordinary build against an existing installation before its recovery copy and
clean-install sequence are confirmed.

For developer recovery, `klaf.client.storage.mode=legacy` in the Git-ignored
root `local.properties` explicitly selects the legacy path (rebuild Android;
restart Desktop). Android's `-PklafClientStorageMode=legacy` is a one-off build
override and does not edit that file. An explicit `room` setting is still
accepted but is no longer required. Isolated test identities stay scoped even
with a legacy override. None of these choices migrates or deletes old data.

### Prepared ordinary Desktop (2026-09-28)

The previous `C:\Users\<user>\.klaf_kt` directory was preserved in
`C:\Users\<user>\Desktop\klaf_desktop_legacy_backup_20260928_before_room\legacy-original`.
All four original file hashes match; SQLite integrity was verified only on
`verification-copy`, not on the preserved files. The backup README documents
recovery, including preservation of the database's WAL.

The ordinary Desktop was launched without test flags using its fresh `.klaf_kt`
directory. It created guest schema 14 with one interim deck and no cards,
pending uploads or checkpoint; SQLite `quick_check` passed. This preparation
did not sign in, import the old Desktop data, or start a server. Desktop is
ready for the later account connection; the primary phone and its full backup
were not changed.
