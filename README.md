# AppLens

Understand what your installed apps can access.

## The Problem

Most users have dozens or hundreds of installed applications,
but Android application information is scattered across system settings.

Permissions live behind one screen, target SDK behind another, APK size and update history
behind a third — and none of it adds up to an answer to a simple question:
*why should I review this app?*

## The Solution

AppLens analyzes installed applications **locally, on the device**, and explains:

- **permissions** — grouped by category (location, microphone, contacts…), with granted/denied/unknown state
- **technical metadata** — version, APK size, installer, signing, ABIs, split APKs
- **review signals** — deterministic reasons to take a closer look, each one traceable to a fact
- **SDK information** — target SDK vs. the device's Android version, with release names
- **application differences** — side-by-side comparison of any two installed apps
- **libraries** — manifest-evidence-based SDK detection with confidence levels

No account, no network, no analytics. The app has no INTERNET permission at all.

## Screenshots

Screenshots are captured during the manual validation pass (spec §32). To try the app yourself:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Features

| Feature | Where |
|---|---|
| Full installed-app discovery (`PackageManager`, every package incl. system) | scan at startup, pull-to-retry |
| Dashboard of real counts — no hardcoded statistics | Home |
| App list with search, 8 filters and 6 sort orders | `Search applications` / metric rows |
| Ranked fast search by app name and package name | local `SearchIndex`, built once per scan |
| App details: overview, permissions, SDK, technical metadata, libraries | detail screen |
| Explainable **Review Score** (`0–100`, every point shown as a factor) | detail → Why Review |
| "Why review?" signals with evidence and priority | detail → Why Review |
| Comparison of two apps, difference-focused | detail → Compare with another app |
| Scan-to-scan diff (added / removed / updated) | dashboard → Since your last scan |
| Hand-off to Android's official app settings | detail → Open Android App Settings |
| Removed-package recovery (uninstalled while inspecting) | detail screen states it honestly |

### Review score in one paragraph

The score is **review effort, not a security rating**. It is `min(100, Σ weights)` of
deterministic signals over observable metadata (background location, debuggable, old target SDK,
large APK, …). Identical metadata always yields the identical score, and the UI lists every
`+N` factor — nothing contributes silently. Full rules: [`docs/scoring.md`](docs/scoring.md).

## Architecture

Single-module Jetpack Compose app (`:app`), one Activity, no Fragments, no XML layouts:

```text
ui/          Compose screens (dashboard, list, detail, compare) — read immutable state
state/       AppLensViewModel (composition root), UiState holders, AndroidViewModel
domain/      models + pure analysis (permissions, scoring, search index, comparison)
data/        PackageManager readers, icon cache, JSON snapshot store
navigation/  typed back stack (Screen + AppNavigator), no navigation library
util/        formatting, API-level helpers, settings intents
```

* **`PackageManager` is the source of truth.** The local index/snapshot is a cache and a
  scan-diff baseline only — it is rebuilt from a fresh scan every time.
* **No new dependencies.** Compose BOM, Activity/KTX, Lifecycle — everything else (JSON,
  icon cache, navigation, DI wiring) is hand-written on what the template already shipped.
* **State survives rotation** via `AndroidViewModel`; package work runs on background
  dispatchers.

Details: [`docs/architecture.md`](docs/architecture.md) and the ADRs in
[`docs/decisions/`](docs/decisions/).

## Privacy

AppLens is **local-first**:

- no Internet permission — the app *cannot* send data anywhere
- no analytics, no ads, no account, no third-party SDKs
- nothing is logged to logcat (no `Log.*` calls in the source)
- the only stored file is a scan-diff snapshot in the app's own `cacheDir`
- the only declared permission is `QUERY_ALL_PACKAGES`, needed to list *all* installed packages

Full statement: [`docs/privacy.md`](docs/privacy.md).

## Performance

Designed to stay responsive with 300+ installed applications:

- one scan per launch; a completed inventory is never silently rescanned
- package reading happens off the main thread, with per-package error isolation
- search runs over a prebuilt normalized index — keystrokes only re-score it
- icons are loaded lazily for visible rows into a bounded `LruCache` (150 icons)
- list rows are computed in `remember`, and results change only when query/filter/sort change

## Limitations

- **No security verdicts.** AppLens never says an app is safe or dangerous — it lists reasons
  to review.
- **No behavior analysis.** Signals come from manifest/platform metadata, not from observing
  what an app does at runtime.
- **APK size only** (not app data): app-data size would require usage-access permission.
- **Special access permissions** show as "special access", not granted/denied — the platform
  cannot answer that question.
- **`minSdk 29`** (Android 10 and older are unsupported).
- Back stack and in-progress search are not restored after process death (state survives
  rotation, not a full OS process kill).
- Long-term scan history is out of scope: only the previous scan is kept for diffing.
- Google Play distribution would require justifying `QUERY_ALL_PACKAGES`; the intended
  distribution for this build is sideloading.

## Testing

Per spec §31, tests are not executed during implementation phases. After all phases are
complete, final verification runs: `./gradlew assembleDebug`, unit tests, static analysis, and
the manual validation checklist from spec §32 (discovery, list, search, sorting, permissions,
score, details, SDK, settings, comparison, edge cases, performance).

## Roadmap

- scan history beyond the previous snapshot (trend of review scores over time)
- export a review report as local text/PDF
- widget / quick tile for "what changed since last scan"
- per-app permission deep-links for special access categories

## License

Not specified in this repository.
