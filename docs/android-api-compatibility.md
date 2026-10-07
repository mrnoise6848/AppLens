# AppLens — Android API Compatibility

> Status: implemented (Phases 2, 6, 9, 10, 11)
> Rule: build environment untouched — spec §1, `docs/decisions/001-preserve-existing-project-architecture.md`

## Environment

| Item | Value |
|---|---|
| `minSdk` | 29 (Android 10) |
| `targetSdk` / `compileSdk` | 37 |
| JDK / `sourceCompatibility` | 11 |

Nothing in this document changes those numbers. If a feature had required a different SDK or
dependency version, the correct action was to stop and report the blocker (spec §25) — no such
blocker occurred.

## Strategy

1. **Compile against 37, run from 29.** Every API that is not available on all supported levels
   sits behind an explicit `Build.VERSION.SDK_INT` guard, or the domain field is nullable and the
   UI shows "Not available on this Android version" (spec §15, §27).
2. **Never guess.** A field the platform does not report on this device stays `null` /
   "Unknown"; it is never filled with a plausible-looking default.
3. **Per-package isolation.** One package that fails to read is recorded as a read issue and the
   scan continues (spec §24); the detail screen reports partial information instead of failing.

## Guarded call sites

| Location | Guard | Why |
|---|---|---|
| `AppDiscoveryDataSource` | `SDK_INT >= TIRAMISU (33)` for `getInstalledPackages(PackageInfoFlags)` | the flags overload only exists from API 33 |
| `AppMetadataReader` | `SDK_INT >= TIRAMISU (33)` for `getPackageInfo(..., flags)` signing path | same flags-overload migration |
| `AppTechnicalInfoReader` | `SDK_INT >= TIRAMISU (33)` signing-info path; `SDK_INT >= R (30)` for `getInstallSourceInfo` | flags overload (33); install-source API only exists from 30 |
| `AppLibraryEvidenceReader` | `SDK_INT >= TIRAMISU (33)` for declared-components flags | flags overload |
| `AppAnalysisEngine` | `deviceApi = Build.VERSION.SDK_INT` injected | target-SDK lag is relative to the *device* API, not a constant |
| `SdkSection` | reads `Build.VERSION.SDK_INT` | shows device API and whether a target SDK is behind it |
| `Theme.kt` | `SDK_INT >= S (31)` for dynamic color | Material You dynamic color starts at Android 12 |

On API 29–32 the pre-flags method overloads are used, which are still present and behaviourally
equivalent for the fields AppLens reads.

## Package visibility (API 30+)

With `targetSdk 37`, package visibility filtering applies on Android 11+. AppLens declares
`QUERY_ALL_PACKAGES` so the inventory is complete on every supported level — including
non-launcher and system packages.
Details: `docs/decisions/002-package-visibility-configuration.md`.

## Version-name display

`util/AndroidVersions.kt` maps API 21–36 to release names. API 37+ (or any unmapped level) is
displayed as `API nn` — the app never invents a release name for a level that has none.

## Compatibility with platform differences in the UI

* **Target SDK lag** is computed against the running device API (`targetSdk < deviceApi − 2`),
  so the same application can be flagged on a newer device and not on an older one — that is the
  intended meaning of the signal (spec §15).
* **Permissions granted at runtime** are resolved through `PermissionGrantResolver`, which
  combines the `REQUESTED_PERMISSION_GRANTED` flag with `checkPermission` and reports a tri-state
  result (granted / denied / unknown) rather than assuming the declared list equals the granted
  list. "Special app access" permissions are never reported as granted, because the platform
  cannot answer for them.
* **Split APKs / ABI / signing details** are best-effort: reported when the platform exposes
  them, shown as unavailable otherwise (spec §16).

## What is explicitly out of scope

* Android 9 and below (`minSdk 29`).
* Devices that hide packages even with `QUERY_ALL_PACKAGES` (work profiles / Device Owner
  policies). The dashboard's "No applications available to analyze" state covers this honestly
  rather than showing zeros as if the device had no apps.
