# 005 — Evidence-based risk language

* Status: **Accepted**
* Date: 2026-10-07
* Phase: 8 (scoring), applied across all UI text

## Context

Spec §14 requires an explainable review score and explicitly forbids unsupported security claims:
no "safe/unsafe" verdicts, no malware detection claims, no trust ratings. The product explains
*observable metadata*; it must never sound like it judged an application.

## Decision

1. **The score is review effort, not risk.** It is named "Review Score" and banded as
   "Few reasons to review" … "Worth a careful review" (`ScoreBand`), never as
   safe / dangerous / infected.
2. **Every point is a visible factor.** `ReviewScoreCalculator` returns `ScoreFactor` entries
   (label + delta + evidence) that the detail screen renders as `+N` lines. Nothing contributes
   silently (spec §14).
3. **Signals cite metadata, not behaviour.** A signal says "declares background location",
   "target SDK is 3 behind this device", "APK ≥ 150 MB" — facts read from `PackageManager`.
   AppLens does not instrument other apps, so it never claims to know what an app *does*.
4. **Deterministic rules only.** No ML, no randomness, no hardcoded app names, no network lookup.
   Identical metadata → identical score and identical explanation.
5. **"Needs review" is a gate, not the score.** `any HIGH signal OR ≥ 2 signals`, kept separate
   so the dashboard count stays explainable without arithmetic on a capped number.
6. **Unknown stays unknown.** Where the platform cannot answer (special-access grants, missing
   fields on old API levels), the UI shows "Unknown" / "Not available on this Android version"
   instead of a convenient default.

## Consequences

* AppLens can honestly state it makes no security determinations; the language throughout the UI
  ("worth a closer look", "reasons to review") supports that.
* Scoring changes must be recorded in `docs/scoring.md` with the weight and rationale — a weight
  tweak that is not documented is a regression.
* The app cannot answer "is this app safe?", by design.
