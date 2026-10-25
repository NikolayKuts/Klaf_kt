# Vocabulary analysis comparison

This temporary server experiment compares the existing chunk extraction plus
global reconciliation with whole-text extraction through a read-only data tool.
AgentDriver and the client application are unchanged.

## Preserved input

The Android source was read with SQLite `-readonly` through ADB. No application
data was modified. Its local, untracked copy is outside the repository:

`C:/Users/kutsn/.codex/experiments/klaf-vocabulary-comparison-2026-10-25/`

- `transcript.txt`: complete cleaned transcript, 14,189 characters.
- `source-snapshot.json`: source metadata and all 382 saved items / 463 occurrences.
- `baseline-audit.json`: repeated-form groups for contextual review.
- `manifest.json`: input counts and SHA-256.

Input SHA-256: `8b096fa1b9b4f41c1cd221ef950b4f6ad9c543db018ede4b5b1849b56f8d8460`.
The phone is no longer needed. Do not add this private fixture to Git.

## Run Configuration on the remote server

Copy `transcript.txt` to the server using the available file transfer method.
Use the existing Klaf Server Run Configuration (`com.kuts.klaf.server.MainKt`),
with its existing configured Codex owner. Set these Environment variables using
absolute paths on that server (replace the example paths):

```text
KLAF_VOCABULARY_ANALYSIS_MODE=chunked
KLAF_VOCABULARY_ANALYSIS_INPUT_FILE=/absolute/path/transcript.txt
KLAF_VOCABULARY_ANALYSIS_REPORT_DIR=/absolute/path/vocabulary-comparison-reports
```

Run once. The process reads the file, performs analysis, writes a uniquely named
JSON report and exits. It does not start the network server/UI or open the Klaf
account database. The existing configured model/account runtime is used with a
separate experiment namespace. Shell access is not enabled.

Change **only** `KLAF_VOCABULARY_ANALYSIS_MODE` to `whole-text` and run again.
No phone, client Analyze button, Save or synchronization is required.

Reports contain mode, UTC creation time, elapsed milliseconds, transcript hash,
the transcript itself and the complete final item/occurrence result. Failures
write a failed report with safe diagnostics when possible, then fail the run.
Repeated runs never intentionally overwrite previous reports. An export failure
is logged and does not turn a successful normal Analyze into an error; if no
report appears, check the configured directory and server logs.

Reports contain private source content. Keep the configured directory outside
the Git checkout and public/static web directories. Transfer both JSON reports
back to this computer for comparison; ADB is not required.

## Interpret the results

- Verify both reports have `status=success` and the same `textSha256` as above.
- Compare elapsed time, item count and occurrence count, then inspect content.
  A successful request or a smaller list alone does not establish better quality.
- Check equivalent phrases with different lengths/translations, distinct senses,
  noun/verb distinctions, omitted useful units and occurrences near the text end.
- Validate source spans/timestamps. Whole-text requires an exact `surface_text`
  per occurrence in its internal model response and checks/repairs offsets against
  that surface, retaining inflected/expanded spans. The field is not added to the
  client contract. Semantic equivalence still requires model/manual review.
- The existing saved baseline is useful but is not a controlled new chunked run:
  use the two fresh reports for the primary comparison.
- Whole-text still has model context/output limits and the SDK tool-size bound.
  Its schema allows up to 10,000 items rather than the per-chunk limit of 120;
  this is a safety ceiling, not an extraction target or guarantee of completeness.

## Restore normal startup

Remove `KLAF_VOCABULARY_ANALYSIS_INPUT_FILE` to start the ordinary server again.
Remove the mode flag (or set `chunked`) to retain the existing production flow.
Remove `KLAF_VOCABULARY_ANALYSIS_REPORT_DIR` to disable diagnostic exports.
Leaving only the mode/report flags applies them to ordinary Analyze requests.
Invalid mode values fail startup instead of silently selecting a different mode.

The implementation can be verified offline with fake generators, but real-model
comparison requires the two remote runs. No real comparison is claimed yet.
