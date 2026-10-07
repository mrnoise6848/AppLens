# 002 — Package visibility configuration

* Status: **Accepted**
* Date: 2026-10-07
* Phase: 2

## Context

AppLens must enumerate the applications installed on the device (spec §7). Since Android 11 (API 30)
the package visibility filtering model restricts which packages `PackageManager` reports to an app.
The project targets SDK 37 with `minSdk 29`, so the filtering model applies on almost every device the
app will run on.

Two viable options exist:

| Option | Configuration | What is visible |
|---|---|---|
| A | `<queries><intent>ACTION_MAIN + CATEGORY_LAUNCHER</intent></queries>` | only apps that expose a launcher activity |
| B | `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>` | every installed package |

## Decision

**Option B** — declare `android.permission.QUERY_ALL_PACKAGES`.

Rationale:

1. The product is an application inventory / privacy review tool. A launcher-only view would hide
   background services, provider-only packages and most system packages, which are precisely the
   packages users most often want to inspect. The dashboard numbers would be wrong by design.
2. The spec explicitly asks for *system application status*, *target SDK*, *requested permissions*
   for the discovered set (spec §7), which requires broad visibility.
3. There is no narrower `<queries>` declaration that expresses "all packages"; repeating intent
   filters would be guesswork and still incomplete.
4. The permission is a normal (install-time) permission: no runtime prompt, no settings screen, and
   no data leaves the device — the app still never uploads the package list (spec §21).

## Consequences

* Google Play may require justification for `QUERY_ALL_PACKAGES`. AppLens' core functionality is
  reviewing installed applications, which is in the "security / privacy review" family Google
  describes as an allowed use case. If Play distribution becomes a goal, the store listing and the
  declaration form must state this explicitly; sideloaded/portfolio distribution is unaffected.
* The permission is documented in `docs/privacy.md` with the statement that the resulting package
  inventory never leaves the device.
* No other permission is added in this phase. In particular AppLens does **not** request
  `PACKAGE_USAGE_STATS` (would require the user to grant usage access and would only be needed for
  app-data size), `QUERY_ALL_PACKAGES`-adjacent storage permissions, or any network permission.
