---
name: android-device-debugging
description: Run, inspect, tap through, and debug the Klaf Android app on a real device or emulator using the connected `mobile` MCP server. Use whenever a change has to be checked in the running app rather than only compiled — verifying a screen, reproducing a bug, reading logcat, granting a runtime permission, taking a screenshot, or driving a UI flow. Triggers on "check it on the device", "run the app", "why does it look like that", "reproduce", "screenshot", "logcat", "test on emulator", or any claim about behaviour that compilation alone cannot prove.
---

# Debugging the Klaf Android app on a device

## The rule

**Device interaction goes through the `mobile` MCP server. Never through `adb` in Bash or
PowerShell, and never through built-in shell mechanisms.**

This is not a style preference — the MCP server is the agreed single path for touching a running
app. If it is unavailable or a call fails, say so and ask what to do. Do not silently fall back
to `adb`.

What is *not* device work and still belongs in the shell:
- Gradle builds (`./gradlew :Android:assembleDebug`)
- Unit tests (`./gradlew :presentation:desktopTest`, `:domain:allTests`)

## Project facts

| Thing | Value |
|---|---|
| Application id / package | `com.kuts.klaf` |
| Main activity | `com.kuts.klaf.MainActivity` |
| Debug APK | `apps/Android/build/outputs/apk/debug/*.apk` |
| Build task | `./gradlew :Android:assembleDebug` |
| Install task | `./gradlew :Android:installDebug` |

## Before anything else

```
device(action: "list")
```

If no Android device appears, **stop and tell the user**. Do not guess, do not report behaviour
you did not observe, and do not substitute a compile for a device check. A browser-only target
means there is no phone or emulator attached.

If several devices are listed, pin one with `device(action: "set", deviceId: "...")`, or pass
`deviceId` on every call.

## Standard loop

1. **Build and install** — shell is fine here:
   `./gradlew :Android:installDebug`
2. **Launch** — `app(action: "launch", package: "com.kuts.klaf")`
   To get a clean start: `app(action: "restart", package: "com.kuts.klaf")`
3. **Look** — `ui(action: "tree", format: "semantic")` first. It is text, roughly ten times
   cheaper than a screenshot, and it gives exact element text and resource ids.
4. **Act** — `input(action: "tap", text: "...")` or `resourceId`, or
   `ui(action: "find_tap", description: "...")` when the label is fuzzy.
   Input calls return a UI diff by default (`hints`), so no follow-up read is needed.
5. **Check** — `ui(action: "assert_visible", text: "...")` /
   `ui(action: "wait", text: "...", timeout: 5000)`.
6. **Screenshot only when the question is visual** — colour, spacing, an icon, a progress ring:
   `screen(action: "capture", preset: "low")`. Do not screenshot after every tap.

For a known multi-step path, collapse it into one round trip with
`flow(action: "batch", commands: [...])`.

## Runtime permissions

Klaf asks for `POST_NOTIFICATIONS` and `RECORD_AUDIO` (voice dictation of mnemonic comments).

```
system(action: "permission_grant",  package: "com.kuts.klaf", permission: "android.permission.RECORD_AUDIO")
system(action: "permission_revoke", package: "com.kuts.klaf", permission: "android.permission.RECORD_AUDIO")
system(action: "permission_reset",  package: "com.kuts.klaf")
```

Test **both** paths when a permission gate is involved: granted, and denied. A denied path that
was never exercised is a path that was never tested.

## Logs

```
system(action: "clear_logs")
... reproduce ...
system(action: "logs", package: "com.kuts.klaf", lines: 200)
```

The app logs through LoKdroid, so app messages show up in the normal log stream.

## Reporting honestly

State what you actually did. "Compiles and unit tests pass" is not "works on the device".
Compilation cannot catch a wrong `RecognizerIntent` extra, a system beep, a colour that reads
wrong in dark theme, or a button that is behind the keyboard. If a claim needs the device and the
device was not available, say the claim is unverified.
