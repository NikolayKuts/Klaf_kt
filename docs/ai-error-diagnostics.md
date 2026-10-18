# Klaf Server AI error diagnostics

AgentDriver `0.10.15-SNAPSHOT` is required to produce the new records. The server
consumer is backward compatible; updating only Klaf without updating the SDK
does not recover details discarded by the old SDK.

Ordinary server logs always include bounded `sdk.request_failure` and
`sdk.provider_failure` metadata. Find the Android request ID in the Klaf failure
record; its `sdkRequestId` matches `sdk.request_failure.requestId`. Provider
thread/turn/RPC IDs in that record link to the `sdk.provider_failure` record.
For an image request, `sdk.image_generation` also links the SDK request and turn.

If the provider code/status does not explain the problem, enable
`KLAF_LOG_PROVIDER_ERROR_DETAILS=true` in the server process environment and
restart the server. This records `sdk.failure_detail` locally. It does not enable
raw assistant response logging or raw process stderr, and does not change the
Android response. The default is false. Values other than true/false are rejected.

Detail records mask common credential formats but may still contain arbitrary
user/provider content. Restrict log access/retention and disable the setting
after reproduction. Never forward the diagnostic stream to Android or include
these details in public errors.

Version `0.10.15-SNAPSHOT` was published to Maven Local on Windows on 2026-10-18.
Klaf's version catalog targets that version for the SDK and speech-runtime plugin.
The Linux host must publish/install the same version before building the server.
Target-server deployment and live reproduction remain pending.
