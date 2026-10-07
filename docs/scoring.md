# AppLens — Review Score

> Status: implemented (Phase 8)
> Code: `domain/score/ReviewScore.kt`, `domain/review/WhyReviewRules.kt`

## What the score is

> "How much this application may deserve manual review."

It is **not** a security score, a malware score or a trust rating. An application with a high score
is an application with more reasons for a human to look at it — nothing more.

## Rules

1. Every reason comes from a deterministic rule over observable metadata
   (`PackageInfo` / `ApplicationInfo` / the declared permission list).
2. No machine learning, no randomness, no network lookup, no hardcoded application names.
3. Identical metadata always produces the identical score and the identical explanation.
4. Every point shown to the user is listed as a factor. Nothing contributes silently.
5. The score is capped at 100 for display; the underlying sum may exceed it.

```text
Review Score = min(100, Σ factor weights)
```

## Factors

| # | Signal | Trigger | Weight | Priority |
|---|---|---|---|---|
| 1 | Background location | `ACCESS_BACKGROUND_LOCATION` declared | 20 | HIGH |
| 2 | Debuggable | `ApplicationInfo.FLAG_DEBUGGABLE` set | 15 | HIGH |
| 3 | Microphone | `RECORD_AUDIO` declared | 14 | HIGH |
| 4 | SMS | any of read/send/receive SMS, MMS, WAP push | 12 | HIGH |
| 5 | Precise location | `ACCESS_FINE_LOCATION` (no background) | 10 | MEDIUM |
| 6 | Camera | `CAMERA` declared | 10 | MEDIUM |
| 7 | Old target SDK | `targetSdk < deviceApi − 2` | 10 | MEDIUM |
| 8 | Contacts | read/write contacts or get accounts | 8 | MEDIUM |
| 9 | Phone | phone state, call log, call, SIP, voicemail… | 8 | MEDIUM |
| 10 | Special access | any Android "special app access" permission | 8 | MEDIUM |
| 11 | Many sensitive permissions | ≥ 5 sensitive permissions declared | 8 | MEDIUM |
| 12 | Media / file access | read media, external storage, media location | 7 | MEDIUM |
| 13 | Body sensors | body sensors, background body sensors, activity recognition | 6 | MEDIUM |
| 14 | Nearby devices | Bluetooth scan/connect/advertise, nearby Wi-Fi, UWB | 6 | MEDIUM |
| 15 | Large application | APK ≥ 150 MB | 6 | LOW |
| 16 | Calendar | read/write calendar | 5 | LOW |
| 17 | Stale update | not updated in over 24 months | 5 | LOW |
| 18 | Approximate location | `ACCESS_COARSE_LOCATION` only | 4 | LOW |

Location rules are mutually exclusive: background wins over precise, precise over approximate.

### Thresholds

| Constant | Value | Rationale |
|---|---|---|
| `LARGE_APP_BYTES` | 150 MB | APK size (not app data, which would need usage access) |
| `TARGET_SDK_LAG` | 2 | "two Android releases behind the device" |
| `MANY_SENSITIVE_THRESHOLD` | 5 | many individual sensitive permissions |
| `STALE_UPDATE_MILLIS` | 730 days | two years without an update |
| `MAX_SCORE` | 100 | display cap |
| Bands | <20 / <50 / <75 / 100 | few / a few / worth a closer look / worth a careful review |

Thresholds live as named constants next to the rules; changing one is a documented decision, never a
silent tweak.

## "Needs review" gate

Separate from the score, so the dashboard number stays explainable:

```text
needsReview = (any HIGH priority signal) OR (2 or more signals)
```

Examples:

```text
Camera only                      → 1 MEDIUM signal  → not in the review list
Camera + contacts                → 2 signals        → in the review list
Background location only         → 1 HIGH signal    → in the review list
```

## Score breakdown UI

The detail screen shows `Review Score`, its band, and each factor with its `+N` contribution, so the
number is never mysterious (spec §14).

## What the score deliberately ignores

* whether a permission is currently granted (the score describes *requested* capability)
* application popularity, download counts, developer identity
* any behaviour observed at runtime — AppLens does not instrument other applications
* anything that cannot be derived from `PackageManager` locally
