# 003 — PackageManager is the source of truth; the local index is only a cache

* Status: **Accepted**
* Date: 2026-10-07
* Phase: 3

## Context

Spec §8 requires a local representation of discovered applications for caching, historical
snapshots, comparison, efficient rendering and derived analysis — while stating explicitly that
the local database must not become the ultimate source of truth.

The project has no database technology (no Room, no SQLite wrapper, no DataStore), so the usual
"reuse the existing database" rule cannot apply, and adding one would require a new dependency plus
codegen for data that is fully rebuildable from the platform in a fraction of a second.

## Decision

1. **Source of truth:** `PackageManager` (`getInstalledPackages` + `ApplicationInfo`). Every
   displayed value comes from a fresh scan; nothing in the UI is served from a stale cache.
2. **Live index:** `AppIndexStore` holds an immutable, label-sorted `List<InstalledApp>` plus a
   packageName lookup map, replaced wholesale after each scan. In memory only, no persistence of
   the full inventory.
3. **History:** only a compact `AppSnapshot` (package, label, version code, target SDK, permission
   count, last update time, APK size) is persisted, as JSON via the framework `org.json` API under
   `cacheDir`. It exists exclusively to compute added/removed/updated diffs between scans.
4. **No new dependency:** no Room, no SQLite, no DataStore, no serialization plugin. `org.json`
   ships with the Android framework.
5. **Cache writes are best effort:** a failed cache write never fails a scan; a missing or corrupt
   snapshot is silently ignored and treated as "no history".

## Consequences

* Clearing app cache/history only loses the comparison baseline, never the product's data.
* Comparison across reboots is available without a database; long-term trend history is out of
  scope for the MVP and is documented as a limitation rather than silently approximated.
* There is exactly one moment where data can be stale — between a package being uninstalled and
  the next scan. The detail screen therefore re-reads from `PackageManager` and reports
  "application removed during inspection" instead of trusting the index (spec §27).
