# AppLens

**Which apps on your phone are worth a closer look?**

A permission list tells you what an app requests. It takes more work to connect that list with its target Android version, update history, installer and package details—especially across a phone full of apps.

AppLens brings that information into one local inventory. It highlights reasons to review an app, explains every contribution to its review score, and lets you compare two apps before opening Android settings to act.

## Open an app. Follow the reasons.

Start with the dashboard, narrow the inventory by name or filter, then open **Why review?** Permissions are grouped by purpose, with grant state shown separately from requested capability. Technical details include signing information, ABIs, split APKs and manifest-based library evidence.

The review rules are concrete:

| Observed fact | Score contribution | Why it appears |
|---|---:|---|
| Requests background location | +20 | A capability worth checking against the app's purpose |
| Built as debuggable | +15 | A build configuration worth investigating |
| Requests microphone access | +14 | Sensitive access that deserves context |
| Targets more than two API levels behind the device | +10 | An older platform target to review |

The score is the sum of applicable factors, capped at 100. Location rules use the strongest requested form instead of counting all forms. A separate dashboard rule flags an app when it has a high-priority signal or at least two signals.

**This is a review priority, not a security verdict.** A high score explains where to look; it does not establish harmful behavior. [All rules and thresholds](docs/scoring.md) are documented alongside the [implementation](app/src/main/java/com/noise/applens/domain/review/WhyReviewRules.kt).

## An inventory that follows the device

`PackageManager` supplies a fresh inventory, including system packages. A cached snapshot records added, removed and updated apps since the previous scan. Individual package-read failures are collected without discarding the rest of the scan, and details handle an app being uninstalled during inspection.

Search uses an index built once per scan. Icons load on demand into a 150-entry cache. Pure scoring, search and comparison logic sit apart from Android readers and Compose screens, keeping the reasoning easy to inspect. [Architecture](docs/architecture.md) · [Design decisions](docs/decisions/)

## Run AppLens

Android 10 / API 29 or newer. Open the project in Android Studio with the configured SDK/toolchain, or:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Scan, inspect an app's factor breakdown, compare it with another app, then revisit the dashboard after an install or update. Check the reported permission state against Android's own settings.

## Data and platform boundaries

Analysis runs on the device. The manifest declares `QUERY_ALL_PACKAGES` and no `INTERNET` permission; the previous-scan baseline lives in the app's cache directory. [Privacy details](docs/privacy.md)

Signals describe metadata and requested capabilities, not observed runtime behavior. Special access and library detection have platform/evidence limits. Package size covers APKs rather than application data. Only one previous scan is retained; navigation survives rotation but not process death. Sideloading is the documented distribution route; Play distribution would require package-visibility review.

Device acceptance checks and measured scan-performance results are still needed. No project license is specified.
