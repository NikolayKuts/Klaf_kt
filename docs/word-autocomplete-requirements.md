# Word Autocomplete Requirements

- Replace the disabled account-scoped autocomplete path with suggestions from Klaf Server on Android and Desktop. Keep the existing card-entry UI and `IWordAutocompleteRepository` contract.
- The curated lowercase word list is a versioned UTF-8 text resource of the private server module, not account data or a database table. Load it once per server application process.
- Preserve the former Firestore lookup behavior for a trimmed, case-insensitive prefix starting with one letter: return at most ten matching words in deterministic alphabetical order. Blank or invalid prefixes return an empty list. Do not return an entire dictionary to the client.
- Serve suggestions only to an authenticated, active account/device through the existing proof-protected API boundary. The endpoint must not require AI eligibility or launch an AgentDriver worker.
- Guest clients return no suggestions. A failed lookup does not prevent typing or saving a card. The legacy Firestore repository remains only for legacy storage mode until that mode is removed separately.
- A later curated/frequency-ranked or editable dictionary is deferred; this pass uses the curated 8,004-word list and requires no server database migration.
