# AppLens

**Find which installed Android apps deserve a closer look—and see the evidence behind every reason.**

An installed app's permission list, target Android version, installer and package size are useful facts, but inspecting them one app at a time makes it difficult to prioritize a review. AppLens brings those facts into a local inventory and turns observable metadata into explicit reasons to investigate.

The result is a review workflow: scan → filter apps that need attention → inspect contributing factors → compare apps → open Android's settings to act. It does not make malware or trust judgments.

## From metadata to a review decision

The dashboard summarizes installed packages, including system apps. Search, filters and sorting narrow the inventory; detail screens group permissions and expose technical metadata, signing information and manifest-based library evidence. A previous-scan snapshot shows added, removed and updated packages.

The most important output is **“Why review?”** Each signal has a reason and weight. For example, a declared background-location permission contributes 20 points; a debuggable build contributes 15. Location rules select the strongest applicable form rather than counting all three forms.

```text
PackageManager metadata + requested permissions + device API + current time
    → deterministic review rules
    → min(100, sum of factor weights)
    → visible factor breakdown and review reasons
```

The separate dashboard gate flags any high-priority signal or at least two signals. The score concerns requested capabilities, regardless of whether runtime permission is granted. Time-dependent signals can change as an app's last update ages. Full weights and thresholds: [scoring rules](docs/scoring.md).

## Engineering choices that keep the evidence inspectable

- **Fresh platform data:** `PackageManager` supplies the inventory. The JSON snapshot is a comparison baseline, not an alternative authority for installed packages.
- **Partial scan recovery:** discovery runs on an IO dispatcher, checks cancellation between packages and records individual read failures without discarding the rest of the inventory. Details handle packages removed during inspection.
- **Bounded UI work:** a normalized search index is built per scan; icons load lazily into a 150-entry cache. These are implementation choices, not measured performance claims.
- **Evidence strength stays visible:** library detection uses manifest evidence and confidence levels. Special-access permissions are not presented as ordinary granted/denied runtime permissions.

A single Compose module separates screens, ViewModel state, pure analysis and Android readers. This boundary makes the score and comparison logic inspectable without burying it in UI code. See [architecture](docs/architecture.md) and [design decisions](docs/decisions/).

## Privacy and limits

The manifest requests `QUERY_ALL_PACKAGES` and declares no `INTERNET` permission. Analysis runs locally; the scan baseline is stored in the app's cache directory. See [privacy details](docs/privacy.md).

- Metadata describes declared capabilities, not observed runtime behavior or proof of misuse.
- Package size covers APKs, not application data. Library evidence cannot establish every bundled SDK.
- Android 10 / API 29 or newer is required. Broad package visibility would need justification for Google Play distribution; sideloading is the documented workflow.
- ViewModel state survives rotation, but navigation and searches are not restored after process death. Only the previous scan is retained.
- No product screenshots or measured scan-performance results are included yet.

## Run and inspect

Open the project in Android Studio with the SDK and toolchain versions declared in the Gradle files, or build and install:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

On a device, verify discovery, permission states, factor breakdowns, comparisons and scan differences against Android's own app settings. This README review verified source behavior; it does not establish device performance or a completed manual acceptance pass.

Source entry points: [discovery](app/src/main/java/com/noise/applens/data/AppDiscoveryDataSource.kt), [review rules](app/src/main/java/com/noise/applens/domain/review/WhyReviewRules.kt), [score calculation](app/src/main/java/com/noise/applens/domain/score/ReviewScore.kt).

No license is specified in this repository.
