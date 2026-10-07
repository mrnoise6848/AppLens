# AppLens — Architecture

> Status: **all phases implemented** (1–14 + documentation). This document records the existing
> project first, then the extension as built.
> Build environment: unchanged from §1.1 — see `docs/decisions/001-preserve-existing-project-architecture.md`.

---

## 1. Existing project snapshot (verified by inspection)

### 1.1 Build environment — DO NOT CHANGE

| Item | Value | Source |
|---|---|---|
| Gradle Wrapper | `9.8.0` | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | `9.4.1` | `gradle/libs.versions.toml` (`agp`) |
| Kotlin | `2.2.10` | `gradle/libs.versions.toml` (`kotlin`, used by `kotlin-compose` plugin) |
| Java / JDK | `11` (`sourceCompatibility` / `targetCompatibility`) | `app/build.gradle.kts` |
| `compileSdk` | `37` (`release(37)`) | `app/build.gradle.kts` |
| `targetSdk` | `37` | `app/build.gradle.kts` |
| `minSdk` | `29` (Android 10) | `app/build.gradle.kts` |
| `applicationId` / namespace | `com.noise.applens` | `app/build.gradle.kts` |
| Version | `1` / `1.0` | `app/build.gradle.kts` |
| Configuration cache | enabled | `gradle.properties` |
| Repositories | `google()`, `mavenCentral()` (FAIL_ON_PROJECT_REPOS) | `settings.gradle.kts` |

### 1.2 Modules

Single module only: `:app` (`settings.gradle.kts` → `include(":app")`).

No feature modules, no dynamic delivery, no benchmark module.

### 1.3 Dependencies (current)

Version catalog: `gradle/libs.versions.toml`.

| Dependency | Version | Scope |
|---|---|---|
| `androidx.compose:compose-bom` | `2026.09.00` | platform |
| `androidx.activity:activity-compose` | `1.13.0` | implementation |
| `androidx.compose.material3:material3` | (BOM) | implementation |
| `androidx.compose.ui:ui`, `ui-graphics`, `ui-tooling-preview` | (BOM) | implementation |
| `androidx.core:core-ktx` | `1.19.1` | implementation |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.11.0` | implementation |
| `junit:junit` | `4.13.2` | test |
| `androidx.test.ext:junit` `1.3.0`, `espresso-core` `3.7.0`, `ui-test-junit4`, `ui-test-manifest`, `ui-tooling` | (BOM / catalog) | test / debug |

**Not present:** navigation library, ViewModel Compose artifact, Room / SQLite, DataStore, Hilt / Koin,
Coil / Glide, coroutines artifact (available only **transitively** through `lifecycle-runtime-ktx`),
serialization plugin, Kotlin Android plugin (AGP 9 applies Kotlin itself).

### 1.4 Source tree (complete)

```text
app/src/main/java/com/noise/applens/
├── MainActivity.kt          # single Activity, `setContent { AppLensTheme { Scaffold { Greeting } } }`
└── ui/theme/
    ├── Color.kt             # default template palette (Purple / Pink)
    ├── Theme.kt             # Material3, dynamic color on Android 12+
    └── Type.kt              # default template Typography (bodyLarge only)

app/src/main/
├── AndroidManifest.xml      # 1 activity (launcher), no <queries>, no permissions
├── keepRules/rules.keep     # empty R8 rules
└── res/                     # default template resources (themes.xml, colors.xml, strings.xml, webp icons)

app/src/test/.../ExampleUnitTest.kt                 # template test (not executed)
app/src/androidTest/.../ExampleInstrumentedTest.kt  # template test (not executed)
```

### 1.5 Architecture (current)

The project is a **fresh Compose template**:

* single Activity, no `Fragment`, no XML layouts
* **no** ViewModel, **no** UI state holder, **no** navigation graph
* **no** repository / data layer, **no** database, **no** DI framework
* **no** existing domain models or utilities to reuse
* theme = Material 3 with dynamic color (Android 12+) and a fallback template palette

### 1.6 Existing tests

Two template tests exist (`ExampleUnitTest`, `ExampleInstrumentedTest`). They must **not** be executed
until every implementation phase is finished (spec §31).

---

## 2. Implications for AppLens

Because there is no existing architecture to preserve beyond "single-module Jetpack Compose app",
the AppLens extension keeps exactly that shape:

* **one module** (`:app`) — no new Gradle modules
* **Compose-only UI** — no Fragments, no XML
* **no new frameworks** — no Hilt, no Room, no navigation artifact unless it becomes unavoidable
* **no version changes** — every version in §1.1 stays byte-identical

### 2.1 Target logical layering

```text
UI (Compose screens)                 com.noise.applens.ui.*
        ↓  (reads immutable UiState, emits events)
State holders                        com.noise.applens.ui.*.  + AppLensAppState
        ↓
Domain                               com.noise.applens.domain.*
   InstalledApp, AppPermission, AppRiskSignal, AppReviewSummary,
   AppTechnicalInfo, LibraryInfo, AppComparison
   PermissionAnalyzer, ReviewScoreCalculator, LibraryAnalyzer
        ↓
Data                                 com.noise.applens.data.*
   AppDiscoveryDataSource (PackageManager), AppIndexStore (cache/snapshot)
        ↓
Android APIs                          PackageManager / ApplicationInfo / PackageInfo
```

`PackageManager` stays the **source of truth**; the local index is only a cache, snapshot and
derived-analysis layer (spec §8).

### 2.2 Planned package structure

```text
com.noise.applens
├── MainActivity.kt                      (existing, extended to host the app root)
├── AppLensApp.kt                        app root composable + navigation host
├── navigation/
│   ├── Screens.kt                       sealed screen definitions + args
│   └── AppNavigator.kt                  minimal back-stack state holder
├── state/
│   └── AppLensAppState.kt               top-level holder: scan state, index, selection
├── domain/
│   ├── model/                           InstalledApp, AppPermission, AppRiskSignal,
│   │                                    AppReviewSummary, AppTechnicalInfo, LibraryInfo
│   ├── permission/PermissionAnalyzer.kt categories + sensitivity mapping
│   ├── score/ReviewScoreCalculator.kt   deterministic, explainable scoring
│   ├── library/LibraryAnalyzer.kt       evidence-based SDK detection
│   └── review/WhyReviewRules.kt         deterministic "why review" rules
├── data/
│   ├── AppDiscoveryDataSource.kt        PackageManager queries
│   ├── AppMetadataReader.kt             per-package field extraction, API-safe
│   ├── AppIconCache.kt                  bounded icon cache
│   └── AppIndexStore.kt                 in-memory index + JSON snapshot cache
├── ui/
│   ├── theme/                           (existing)
│   ├── components/                      shared rows/cards/labels
│   ├── dashboard/                       Phase 4
│   ├── apps/                            Phase 5 + 13 (list, search, filter, sort)
│   ├── detail/                          Phase 6, 9, 10, 14
│   ├── compare/                         Phase 12
│   └── components/                      shared icons, headers, badges
└── util/                                formatting, API-level guards, settings intents
```

Files are added, existing files are edited minimally. `ui/theme/*`, `MainActivity.kt` and the Gradle
files are the only pre-existing files expected to change (MainActivity: replace `Greeting` with the
app root; theme: only if the palette must be adjusted for legibility).

### 2.3 State, threading and DI

* **State**: immutable `StateFlow`-free approach is *not* required; a plain observable state holder
  (`mutableStateOf` inside `AppLensAppState`) created once in `MainActivity` and remembered across
  recomposition is sufficient and avoids adding a ViewModel artifact.
* **Threading**: `lifecycleScope` + `Dispatchers.IO` from `lifecycle-runtime-ktx` (already a
  dependency). PackageManager work never runs on the main thread.
* **DI**: manual construction in `AppLensAppState` (composition root). No Hilt/Koin — one object graph
  is not worth a codegen dependency.

### 2.4 Navigation

No `navigation-compose` artifact exists. Planned: a minimal typed back stack
(`AppNavigator` holding `List<Screen>` + `push/pop`) driven by Compose state. Screens:

```text
Dashboard → AppList → AppDetail(packageName) → Compare(a, b)
```

Back handling via `BackHandler` (already available from `activity-compose`).
**Fallback**: if this proves fragile, `androidx.navigation:navigation-compose` may be added — the
reason must be recorded in `docs/decisions/` first (spec §19: inspect existing capability first).

### 2.5 Local index (Phase 3) — no database dependency

There is **no** database in the project, so "reuse the existing database technology" cannot apply.
Adding Room would mean: a new dependency + KSP/Kotlin codegen + schema files, for data whose only
purpose is caching and historical comparison (spec §8 explicitly says the cache is *not* the source
of truth).

Decision: `AppIndexStore` keeps the live index **in memory** and persists historical snapshots as
`org.json` documents under `context.cacheDir` (framework-provided JSON, zero new dependencies).
Package names and derived signals only — no uploaded data, no sensitive payload
(spec §21, `docs/privacy.md`).

---

## 3. Data flow

```text
AppDiscoveryDataSource.discover()
        │  (Dispatchers.IO, per-package try/catch — one failure must not kill the scan)
        ▼
AppMetadataReader.read(packageName)  ──►  InstalledApp (immutable domain model)
        │
        ├──► AppIndexStore.index()           (cache, snapshots, derived lists)
        │
        ▼
PermissionAnalyzer ──► AppPermission[] + categories
ReviewScoreCalculator ──► ReviewScore (with factor breakdown)
WhyReviewRules ──► List<AppRiskSignal> (deterministic, evidence-based)
LibraryAnalyzer ──► List<LibraryInfo> (Detected / Likely / Unknown)
        │
        ▼
AppLensAppState ──► UiState (dashboard counts, list rows, detail sections)
        │
        ▼
Compose screens
```

### Domain concepts (spec §22)

| Concept | Purpose |
|---|---|
| `InstalledApp` | full local record of one installed package |
| `AppPermission` | requested/granted permission + category + platform notes |
| `AppRiskSignal` | one deterministic reason to review |
| `AppReviewSummary` | score + factors + signals for one app |
| `AppTechnicalInfo` | SDK/debuggable/system/ABI/signing data |
| `LibraryInfo` | detected SDK with confidence level |
| `AppComparison` | side-by-side diff of two apps |

### Responsibilities (spec §22)

`AppDiscoveryDataSource`, `AppMetadataReader`, `PermissionAnalyzer`, `ReviewScoreCalculator`,
`LibraryAnalyzer`, `AppRepository` (the repository role is split between `AppDiscoveryDataSource`
and `AppIndexStore`; a separate `AppRepository` wrapper will only be added if a real indirection
need appears).

---

## 4. Platform / API constraints

* `minSdk 29` → Android 10. Anything below API 29 must not be supported or assumed.
* `targetSdk 37`, `compileSdk 37` → modern `PackageManager` flags are compile-available, but runtime
  behaviour differs by API level; every field access must be guarded where it is not universally
  available (`firstInstallTime`/`lastUpdateTime` are safe at 29+, ` ApplicationInfo.minSdkVersion`
  needs API 24+, `getPackageInfo` signing info needs API 28+, split APK / ABI details vary).
* **Package visibility (Android 11+, API 30+)**: with `minSdk 29` and `targetSdk 37`, an app cannot
  enumerate arbitrary packages without declaring package visibility. The product requires a **full
  inventory** of installed applications (not only launcher apps), so `<queries>` intent matching for
  `MAIN/LAUNCHER` is insufficient. Decision recorded in
  `docs/decisions/002-package-visibility-configuration.md`.
* No permission besides package visibility is required. The app must never request
  `QUERY_ALL_PACKAGES`-adjacent capabilities it does not need (spec §7).

---

## 5. Non-negotiable guardrails (spec §1)

1. No project recreation, no architecture migration, no new modules.
2. No change to: Gradle wrapper, Gradle, AGP, Kotlin, JDK, Compose/BOM versions, `applicationId`,
   `minSdk`/`targetSdk`/`compileSdk`.
3. No dependency upgrades/downgrades for their own sake.
4. No broad refactors unrelated to the current feature.
5. No test execution until every implementation phase is complete (spec §31).
6. If a required feature cannot be built with the current configuration → **stop and report the
   blocker** instead of silently changing foundational versions (spec §25).

---

## 6. Phase → code mapping

| Phase | Deliverable | Primary code |
|---|---|---|
| 1 | this document | `docs/architecture.md` |
| 2 | installed app discovery | `data/AppDiscoveryDataSource.kt`, `data/AppMetadataReader.kt`, manifest |
| 3 | local index / cache | `data/AppIndexStore.kt`, `state/AppLensAppState.kt` |
| 4 | dashboard | `ui/dashboard/*` |
| 5 | app list + filter | `ui/apps/*` |
| 6 | app details | `ui/detail/*` |
| 7 | permission intelligence | `domain/permission/*` |
| 8 | why review + score | `domain/review/*`, `domain/score/*` |
| 9 | SDK / platform info | `domain/model/AppTechnicalInfo`, detail UI section |
| 10 | APK / technical metadata | `data/AppMetadataReader`, expandable detail section |
| 11 | library inspection | `domain/library/*` |
| 12 | comparison | `ui/compare/*`, `domain/model/AppComparison` |
| 13 | search | `domain/analysis/SearchIndex.kt`, `domain/analysis/AppListQuery.kt`, `ui/apps` |
| 14 | settings integration | `util/SettingsIntents.kt` + detail UI action |
| — | documentation & README | `docs/*`, `README.md` |
| — | final verification | build + tests + lint + manual pass (spec §32) |

---

## 7. Known risks / open questions

| # | Risk | Mitigation |
|---|---|---|
| R1 | No ViewModel/navigation artifacts — state loss on config change | hold state in `remember`-scoped holder; verify rotation in final manual pass; add artifacts only with a recorded decision |
| R2 | Package visibility limits on API 30+ | explicit `<queries>`/permission decision (Phase 2), documented in `docs/decisions/` |
| R3 | Icon loading with 300+ apps | bounded icon cache + lazy loading in `LazyColumn`, never load all icons up front (spec §23) |
| R4 | Per-package failures must not abort scanning | try/catch per package + partial-information state (spec §24) |
| R5 | Fields unavailable on some API levels | nullable domain fields + "Not available on this Android version" UI (spec §15) |
| R6 | Configuration cache / AGP 9 behaviour | never touch Gradle files; build after each phase without running tests |
