# Vocabulary analysis comparison

This temporary server experiment compares the existing chunk extraction plus
global reconciliation with whole-text extraction through a read-only data tool.
AgentDriver and the client application are unchanged.

## Preferred workflow: local authenticated desktop test

The operator does not need to copy input/results or configure remote paths.
Start the remote server normally with `KLAF_VOCABULARY_ANALYSIS_MODE=chunked`.
Do not set `KLAF_VOCABULARY_ANALYSIS_INPUT_FILE` or the remote report directory.
The agent runs `RemoteVocabularyComparisonTest` locally, using the preserved
transcript, existing Desktop selected account and standard protected-session/DPoP
request signer. It requests Analyze and saves the complete response locally,
without opening Room, saving items or synchronizing data.

Then change only the remote mode to `whole-text` and restart the server. The agent
collects the second response with the same test and compares the local reports.
The normal server protocol does not echo its configured mode, so local reports
explicitly record `operatorDeclaredMode` and `serverModeVerified=false`; server
Run Configuration/log confirmation is required to identify the two samples.

The local test is opt-in; all paths below are set by the agent on this computer,
not by the operator on the remote server:

```text
KLAF_VOCABULARY_REMOTE_COMPARISON=true
KLAF_VOCABULARY_COMPARISON_MODE=chunked
KLAF_VOCABULARY_COMPARISON_INPUT=<local saved transcript.txt>
KLAF_VOCABULARY_COMPARISON_OUTPUT=<local experiment results directory>
```

Run `:di:desktopTest --tests '*RemoteVocabularyComparisonTest*'`. The test uses a
real model and is skipped during ordinary offline tests. The existing Desktop
login must be valid; credentials are never test arguments or output fields.

## Standalone-word acceptance follow-up

The first revised-policy attempt failed with operator-reported
`serverOverloaded` after293.4s. One unchanged retry failed after902.5s with
operator-confirmed `RequestTimeout`; the configured one-shot total was15min.
Neither returned an analysis, so no coverage acceptance was evaluated.

For the next controlled run, update the server and use these remote Environment
variables (the normal default remains15min if the timeout override is absent):

```text
KLAF_VOCABULARY_ANALYSIS_MODE=whole-text
KLAF_CODEX_TEXT_TIMEOUT_MINUTES=30
```

Fully restart the server process. Its startup log should include
`Codex text generation timeout configured: 30 minute(s)` before requesting the
next sample. The timeout is a server account-runtime setting for **all one-shot
text generation calls**, not just vocabulary. It accepts integer1..60 minutes;
invalid explicit settings fail configuration. Image/audio policies and streaming
settings are unchanged. Remove the timeout variable to restore the15min default.
This extends the wait budget; it does not fix provider overload, prove adequate
output capacity, guarantee completion, or establish vocabulary coverage.

The revised whole-text developer profile requires comprehensive independent
content-word coverage, with explicit function-word/name/noise/opaque-idiom
exceptions. No exact corpus words are embedded in the production instruction.

For the next real run, the agent additionally sets the local test flag
`KLAF_VOCABULARY_COMPARISON_REQUIRE_STANDALONE=true`. This activates six contextual
controls in the preserved transcript: campaign `button`, discerning `tell`,
`constantly`, `leader`, `exercise`, and transport `take`. The test preflights the
source contexts before making a request. Reports retain coverage failures before
assertions, so an unsuccessful evaluation does not discard the model's response.

Controls require independent word occurrences (allowing boundary punctuation and
whitespace) and lexical POS. Larger phrases/example mentions do not substitute
for those occurrences; wrong POS is reported as metadata disagreement. This is a
small coverage guard, not proof of exhaustive recall or correct Russian meanings.
Meaning and the remaining vocabulary require contextual review.

Recorded reports can be evaluated without a provider call using
`KLAF_VOCABULARY_COVERAGE_REPORT=<local report.json>` and
`:di:desktopTest --tests '*StandaloneVocabularyCoverageTest.evaluateRecordedRealResponseForStandaloneWordCoverage' --rerun`.
The old162-item whole-text report was evaluated before changing instructions and
failed all six controls. This intentional baseline RED must not be confused with
an infrastructure regression. Ordinary offline tests leave both opt-ins unset.
The revised policy subsequently passed the real-model acceptance run below.

## Revised-policy real result (2026-10-26)

With the confirmed30min server budget, a fresh response passed all six source-word
controls and manual contextual-meaning review. Request846.7s(14m06.7s),497items,
1021occurrences(1019unique spans). Full Gradle probe passed14m16s. All207 checked
alphabetic single-word content headwords from chunked are represented (206same
lemma plus cuz normalized to cousin). This is not a full sense/recall proof.

Useful idioms were retained with occasional canonical-name changes. New issues
remain: fragmented cooperation is omitted, two exact occurrences are duplicated,
and good from better may be an excess same-sense split. Comprehensive extraction
also exposes probable transcription noise as LOW-confidence entries. Private
`standalone-comparison.md` and `standalone-comparison-metrics.json` contain details.

The successful response is26.3% quicker than the recorded chunked run, with much
broader word coverage than the initial162-item shortlist. Two preceding failed
attempts consumed additional provider wait time; do not treat the successful-run
comparison as a general performance/reliability guarantee. No product default
was changed, source data were not saved/synchronized and no extra model run was
performed after this sample.

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

## Optional standalone alternative: file on the remote server

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

## Recorded real comparison (2026-10-26)

Both real remote runs completed on the same 14,189-character transcript and input
hash. The operator confirmed chunk logs and the whole-text start log respectively;
one incorrectly configured intermediate attempt was stopped and excluded.

| Metric | Chunked + reconciliation | Whole-text |
|---|---:|---:|
| Client request seconds | 1,149.5 | 374.8 |
| Items | 379 | 162 |
| Occurrences / unique spans | 490 / 490 | 272 / 272 |

Whole-text was3.07x faster but selected fewer units, including omissions such as
`What do you mean?`, `get off` and `on the way`. It also found units absent from
chunked and more occurrences for shared forms: `science project`3 vs2 (the input
has3 literal mentions). For142 normalized forms shared by both, it returned250
occurrences vs191. The lower item count cannot be interpreted as just better
deduplication. The operator reported no sanitizer-change log for whole-text.

This single-transcript comparison does not establish ground-truth recall or
general performance. Keep the ordinary chunked default until the intended
whole-text extraction scope/completeness is addressed. Full private metrics and
contextual findings are saved beside the local fixture, outside Git.
