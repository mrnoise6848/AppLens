# 001 — Preserve the existing project architecture

* Status: **Accepted**
* Date: 2026-10-07
* Phase: 1

## Context

AppLens is specified as an extension of an existing Android project. The repository already contains a
working Gradle/AGP/Kotlin/Compose configuration (AGP 9.4.1, Gradle 9.8.0, Kotlin 2.2.10, JDK 11,
compileSdk/targetSdk 37, minSdk 29) and a single-module Jetpack Compose template.

## Decision

1. Keep the build environment byte-identical: no Gradle wrapper, Gradle, AGP, Kotlin, JDK, Compose or
   SDK version changes.
2. Keep the single `:app` module. No new Gradle modules.
3. Keep the Compose-only, single-Activity presentation style.
4. Add AppLens functionality as **new files** under `domain/`, `data/`, `state/`, `navigation/` and
   `ui/<feature>/`; modify existing files only where the template must host the new app root
   (`MainActivity.kt`) or where the palette/typography must be adjusted for legibility.
5. Do not migrate to MVVM-with-Hilt, MVI, Clean Architecture modules, or any other architecture "for
   cleanliness". The layering is logical (packages), not modular.

## Consequences

* No build-infra risk, no version drift, no dependency churn.
* Manual dependency wiring in a single composition root — acceptable for a one-module app.
* State persistence across process death is limited to the file-based snapshot cache; this is an
  accepted MVP limitation rather than a reason to add a database or ViewModel artifact.
