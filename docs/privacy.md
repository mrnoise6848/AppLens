# AppLens — Privacy

> Status: implemented
> Related: spec §21, `docs/decisions/002-package-visibility-configuration.md`,
> `docs/decisions/003-package-manager-source-of-truth.md`

AppLens is **local-first**: every analysis runs on the device, inside the app process.

## What AppLens does with your data

| Data | Where it goes |
|---|---|
| Installed application inventory | In-memory index in the app process |
| Package names, versions, permissions, SDK levels | Read from `PackageManager`, kept in memory |
| Historical snapshot (for added/removed/updated diffs) | JSON file in the app's own `cacheDir` |
| Application icons | In-memory bounded cache |
| Everything shown on screen | Rendered locally; never transmitted |

## What AppLens never does

* upload installed application lists
* upload package names to a server
* upload APK files
* upload permission information
* require an account
* include analytics
* include ads
* send logs externally

There is **no network permission** in `AndroidManifest.xml`. The app physically cannot open a
socket to anywhere. Verified by manifest inspection: the only declared permission is
`QUERY_ALL_PACKAGES` (see below), and no source file performs network I/O.

## Logging

No `Log.*`, `println` or `System.out` call exists in the production source tree. Nothing is
written to logcat — not the inventory, not permission details, not package names.

## The one permission: QUERY_ALL_PACKAGES

Android 11+ filters which packages `PackageManager` reports. AppLens must show a complete
inventory (including background/system packages), so it declares `QUERY_ALL_PACKAGES`.

* It is a **normal (install-time) permission** — no runtime dialog, no settings toggle.
* It only makes package *metadata* readable; it grants no ability to interact with other apps.
* The resulting inventory never leaves the device (see above).
* Decision and rationale: `docs/decisions/002-package-visibility-configuration.md`.

No other permission is requested: no storage, no location, no usage access, no network.

## Stored files

Only one file is written: `applens-index-snapshot.json` under `context.cacheDir`.

* It contains package name, label, version code, target SDK, permission count, last update time
  and APK size — the minimum needed to compute scan-to-scan diffs.
* It is removed automatically when the app's cache is cleared or the app is uninstalled.
* A missing or corrupt file is treated as "no history"; it never breaks the product.

## Backup / data extraction

The manifest keeps the template's `allowBackup` + `data_extraction_rules` configuration. The
snapshot under `cacheDir` is not backed up by the default rules (cache is excluded), which is the
desired behaviour: a package inventory should not travel between devices.

## Third parties

None. No advertising SDK, no analytics SDK, no crash reporter, no image-loading library with
network fallback. The dependency list is Jetpack Compose, Activity/KTX and test-only artifacts —
see `docs/architecture.md` §1.3.
