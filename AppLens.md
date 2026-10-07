# AppLens — Product Specification & Agent Instructions

## 1. Critical Rules

You are continuing development of an **existing Android project**.

The existing source code, project structure, Gradle configuration, dependency versions, and build setup are considered valid and must be preserved.

### NON-NEGOTIABLE RULES

1. **Do NOT recreate the project from scratch.**
2. **Do NOT replace the existing architecture unless absolutely necessary.**
3. **Do NOT change the existing Gradle version.**
4. **Do NOT change the existing Gradle Wrapper version.**
5. **Do NOT change the Android Gradle Plugin version.**
6. **Do NOT change the Kotlin version.**
7. **Do NOT change the Java/JDK version.**
8. **Do NOT change Compose/compiler/plugin versions.**
9. **Do NOT upgrade or downgrade dependencies just because newer versions exist.**
10. **Do NOT change package/application IDs.**
11. **Do NOT migrate the project to another architecture.**
12. **Do NOT introduce unnecessary modules.**
13. **Do NOT replace working code with a new implementation just because you prefer another approach.**
14. **Do NOT perform broad refactors unrelated to the current feature.**
15. **Do NOT run tests during intermediate phases.**
16. **Do NOT run unit tests, instrumentation tests, UI tests, benchmark tests, or full test suites until ALL implementation phases are completed.**
17. At the end of all phases, perform the complete verification/test pass once.
18. If an existing dependency or configuration is sufficient, reuse it.
19. If a new dependency is genuinely required, first inspect whether an existing dependency already provides the capability.
20. Never silently modify foundational project configuration.

The goal is to **extend the existing project**, not rebuild the project.

---

# 2. Product

## App Name

**AppLens**

## Product Definition

AppLens is a privacy-oriented Android utility that helps users understand what the applications installed on their phone can do, what permissions they request, how they are configured, and which applications may deserve further review.

### Core value proposition

> **"Understand what your installed apps can access — without reading Android technical details."**

The application converts low-level Android application metadata into understandable information for normal users.

AppLens is not intended to be a replacement for Android Settings.

It is an **app intelligence / privacy review tool**.

---

# 3. Problem

Most Android users have dozens or hundreds of installed applications.

They usually do not know:

* which applications request sensitive permissions
* which applications can access location
* which applications use microphone/camera permissions
* which applications target old Android versions
* how large applications are
* which applications contain many third-party libraries
* which applications may deserve a manual privacy review

Android exposes much of this information, but it is fragmented and technical.

AppLens collects this information and presents it in a simple way.

---

# 4. Product Principles

### 4.1 Local-first

All analysis is performed locally on the Android device.

No backend is required.

No user account is required.

No uploaded APKs.

No uploaded application metadata.

No cloud scanning.

### 4.2 Explain, do not scare

Never present technical information as a security fact unless it can actually be established.

For example:

BAD:

> "This application is spying on you."

GOOD:

> "This application requests background location access."

BAD:

> "This application contains malware."

GOOD:

> "This application requests permissions that may deserve review."

### 4.3 Evidence-based

Every warning or recommendation must be based on observable application metadata.

Do not invent risks.

### 4.4 User remains in control

AppLens does not uninstall applications automatically.

It does not revoke permissions automatically.

It does not modify other applications.

It provides information and, where appropriate, links the user to Android's existing system settings.

---

# 5. Main User Flow

```text
Launch App
    ↓
Scan / Load Installed Applications
    ↓
Dashboard
    ↓
Review Applications
    ↓
Select an Application
    ↓
Application Details
    ↓
Permissions
    ↓
Technical Information
    ↓
Libraries / Components
    ↓
Why Review?
```

---

# 6. Main Features

## Phase 1 — Existing Project Inspection and Foundation

Before modifying anything:

Inspect the existing project completely.

Review:

* Gradle Wrapper
* Gradle version
* AGP version
* Kotlin version
* Java/JDK configuration
* compileSdk
* targetSdk
* minSdk
* Compose setup
* dependency versions
* package structure
* architecture
* navigation
* existing UI components
* existing theme
* existing tests
* build variants
* manifest
* existing utilities
* existing repositories/data sources

### Important

Assume the current project is already correctly configured.

Do not upgrade versions.

Do not "modernize" Gradle.

Do not migrate the project.

Do not replace existing architecture.

After inspection, document the existing architecture and identify where AppLens functionality should be integrated.

Create:

```text
docs/architecture.md
```

Only document and plan at this stage.

---

# 7. Phase 2 — Installed Application Discovery

Implement real Android application discovery.

Use Android's appropriate application/package APIs.

The application should discover installed applications available to the user according to the supported Android APIs and project target SDK.

Do not blindly request:

```text
QUERY_ALL_PACKAGES
```

unless the Android platform requirements and actual product need justify it.

Prefer the minimum required package visibility configuration.

For each discovered application collect information such as:

* package name
* application label
* version name
* version code
* first install time
* last update time
* application size when available
* application icon
* target SDK
* min SDK when available
* requested permissions
* granted/declared permission information where available
* debuggable state where available
* system application status
* enabled/disabled state where applicable

Do not assume every field is available on every Android version.

Handle platform differences safely.

---

# 8. Phase 3 — Local Application Index

Create a local representation of discovered applications.

The local database/cache must not become the ultimate source of truth.

Android's PackageManager remains the source of truth.

The local database is only for:

* caching
* historical snapshots
* comparison
* efficient rendering
* derived analysis

Do not persist unnecessary sensitive information.

Use the project's existing database technology if one already exists.

Do not introduce another database technology if the project already contains an adequate solution.

---

# 9. Phase 4 — AppLens Dashboard

Create the main dashboard.

Example:

```text
AppLens

Your Apps
127 installed applications

Needs Review
8 applications

Sensitive Permissions
14 applications

Old Target SDK
5 applications

Large Apps
11 applications
```

The dashboard must use real data.

### Do not

* hardcode statistics
* use fake application counts
* generate fake scan progress
* create placeholder risk values

The numbers must be derived from the actual installed applications.

---

# 10. Phase 5 — Application List

Create a searchable/filterable application list.

Each application row may display:

```text
Instagram
com.instagram.android

412 MB
Updated 3 days ago

3 sensitive permissions
```

Provide sorting/filtering where useful.

Possible filters:

* All
* Needs Review
* Sensitive Permissions
* Large Apps
* Old Target SDK
* Recently Updated
* System Apps
* User Apps

Do not add excessive filtering if it makes the UI unnecessarily complex.

---

# 11. Phase 6 — Application Details

When the user selects an application, show a detailed screen.

Example structure:

```text
Instagram

Version
412.0.x

Package
com.instagram.android

Size
412 MB

Target SDK
35

Updated
3 days ago
```

Then sections:

### Permissions

```text
Camera
Microphone
Location
Notifications
Contacts
...
```

Clearly distinguish information that represents:

* requested permissions
* granted permissions
* special permissions
* permissions that Android exposes differently on different versions

Do not misrepresent Android permission state.

---

# 12. Phase 7 — Permission Intelligence

AppLens should categorize permissions into understandable groups.

Examples:

### Sensitive

* Camera
* Microphone
* Fine Location
* Background Location
* Contacts
* Phone
* SMS
* Calendar
* Nearby Devices

### Normal / Lower sensitivity

Other common permissions may be displayed separately.

Do not label permissions as "dangerous" merely for marketing purposes.

Use Android's real permission semantics where applicable.

---

# 13. Phase 8 — "Why Review?" Intelligence

This is one of the most important features.

AppLens should explain why an application appeared in a review category.

Example:

```text
Why should I review this app?

• Requests background location
• Requests microphone permission
• Targets an older Android SDK
• Large application size
```

Another application:

```text
Why should I review this app?

• Requests camera access
• Requests contacts access
```

These explanations must be generated from deterministic rules.

No arbitrary AI-generated warnings in the MVP.

No invented security claims.

---

# 14. Review Score

AppLens may calculate a deterministic **Review Score**.

This is NOT a security score.

It represents:

> "How much this application may deserve manual review."

Example:

```text
Review Score
72 / 100
```

Possible signals:

* sensitive permissions
* background location
* microphone/camera
* older target SDK
* unusual permission combinations
* large footprint
* old installation/update history where available

### Important

The score must be explainable.

Do not create a mysterious machine-learning score.

For example:

```text
Review Score: 72

Factors:
+20 Background location
+15 Microphone
+15 Camera
+10 Old target SDK
+12 Other sensitive permissions
```

The exact scoring formula should be documented and deterministic.

---

# 15. Phase 9 — SDK / Platform Information

Show useful technical information without overwhelming normal users.

Example:

```text
Android Compatibility

Target SDK
35

Minimum SDK
26

Debuggable
No

System App
No
```

The application should adapt to Android API differences.

Where information is unavailable:

```text
Not available on this Android version
```

Do not fabricate values.

---

# 16. Phase 10 — APK / Technical Metadata

Where supported by Android APIs, expose useful technical metadata such as:

* package name
* version
* signing information summary
* APK/source paths only when appropriate
* ABI information
* install/update time
* application flags
* components where useful

Do not expose unnecessarily complex technical details to the default UI.

Advanced technical information can be placed behind an expandable section.

---

# 17. Phase 11 — Library / Dependency Inspection

Where realistically possible from Android application/package metadata, provide a lightweight library/component overview.

For example:

```text
Libraries / SDKs

Firebase
OkHttp
AndroidX
...
```

Do not pretend to identify libraries with certainty when only indirect evidence exists.

Clearly distinguish:

```text
Detected
Likely
Unknown
```

Prefer deterministic package/component/resource evidence.

Do not implement a heavy APK decompiler or reverse-engineering engine unless it is genuinely required and practical.

This feature should remain lightweight and reliable.

---

# 18. Phase 12 — Application Comparison

Allow users to compare two installed applications.

Example:

```text
Instagram          TikTok

Permissions        Permissions
Camera             Camera
Microphone         Microphone
Location           Location
Contacts           None

Target SDK         Target SDK
35                 35

Size
412 MB             280 MB
```

The goal is not to declare a winner.

The goal is to make differences obvious.

---

# 19. Phase 13 — Search

Provide fast application search.

Search by:

* application name
* package name

Search should operate on locally available indexed data.

The UI must remain responsive with a large number of installed applications.

---

# 20. Phase 14 — Android Settings Integration

For relevant information, allow the user to open Android's official application settings page.

For example:

```text
Open Android App Settings
```

Do not implement custom permission modification flows when Android already provides the official UI.

The app should help users understand the situation and then let Android handle system-level actions.

---

# 21. Privacy Requirements

AppLens must be local-first.

### Must NOT

* upload installed application lists
* upload package names to a server
* upload APK files
* upload permission information
* require an account
* include analytics by default
* include ads in the MVP
* send sensitive logs externally

Avoid logging:

* complete package inventories
* sensitive permission details
* user-specific application data

Use appropriate debug logging only during development.

---

# 22. Architecture

Use the architecture already present in the project.

Do not migrate the whole application to a different architecture.

Where the existing structure is insufficient, extend it minimally.

Preferred logical separation:

```text
Presentation
    ↓
Domain
    ↓
Data
    ↓
Android APIs
```

Possible domain concepts:

```text
InstalledApp
AppPermission
AppRiskSignal
AppReviewSummary
AppTechnicalInfo
LibraryInfo
AppComparison
```

Possible responsibilities:

```text
AppDiscoveryDataSource
AppMetadataReader
PermissionAnalyzer
ReviewScoreCalculator
LibraryAnalyzer
AppRepository
```

Do not create abstractions that are not necessary.

---

# 23. Performance

The application must be efficient with large application collections.

The app should remain responsive with:

```text
50 apps
100 apps
200 apps
300+ apps
```

Avoid:

* blocking the main thread
* loading all icons at once
* excessive recomposition
* repeatedly querying PackageManager unnecessarily
* repeatedly recalculating expensive metadata
* unbounded memory use

Use caching where justified.

Use background execution for expensive work.

---

# 24. Error Handling

Handle:

* package disappeared during scanning
* app uninstalled while viewing it
* permission/API information unavailable
* PackageManager errors
* unsupported Android API behavior
* missing metadata
* invalid/unknown values

One broken application must not terminate the entire scan.

Example:

```text
Some application information could not be read.
The rest of your application inventory is available.
```

---

# 25. Android Version Compatibility

The implementation must account for Android API differences.

Do not use APIs without checking the project's minimum SDK.

Use compatibility checks where required.

Do not raise minSdk merely to simplify implementation.

Do not change targetSdk/compileSdk unless the existing project genuinely cannot implement the required functionality.

If a required feature cannot be implemented with the current configuration:

**STOP and report the exact blocker before changing foundational versions.**

Do not silently upgrade the project.

---

# 26. UI/UX Requirements

The UI should be:

* modern
* clean
* simple
* fast
* understandable to normal users
* technically credible
* portfolio-quality

Avoid:

* excessive cards
* unnecessary animations
* noisy dashboards
* giant technical tables
* fake security warnings
* scary red alerts everywhere

Use visual hierarchy.

Important findings should be visible immediately.

Technical information should be progressively disclosed.

---

# 27. Empty / Edge States

Support:

### No applications found

```text
No applications available to analyze.
```

### Permission / package visibility limitation

Explain what the app can and cannot inspect.

### Scan in progress

Show real progress based on actual work.

### Partial information

Clearly explain:

```text
Some information is unavailable on this Android version.
```

### Application removed during inspection

Recover gracefully.

---

# 28. Documentation

Create and maintain:

```text
docs/architecture.md
docs/privacy.md
docs/scoring.md
docs/android-api-compatibility.md
docs/decisions/
```

Suggested decisions:

```text
001-preserve-existing-project-architecture.md
002-packagemanager-as-source-of-truth.md
003-local-first-design.md
004-deterministic-review-scoring.md
005-evidence-based-risk-language.md
```

---

# 29. README

The README must start with the problem, not the technology stack.

Recommended structure:

```text
# AppLens

Understand what your installed apps can access.

## The Problem

Most users have dozens or hundreds of installed applications,
but Android application information is scattered across system settings.

## The Solution

AppLens analyzes installed applications locally and explains:

- permissions
- technical metadata
- review signals
- SDK information
- application differences

## Screenshots

## Features

## Architecture

## Privacy

## Performance

## Limitations

## Testing

## Roadmap

## License
```

Do not claim functionality that the implementation does not provide.

---

# 30. Development Workflow

Follow this workflow exactly.

## Step 1 — Inspect

Read the existing source tree and configuration.

Identify:

* existing architecture
* existing screens
* existing dependencies
* existing utilities
* current build configuration
* current Android API levels

## Step 2 — Architecture Review

Determine where AppLens functionality belongs.

Do not rewrite working code.

## Step 3 — Implementation Plan

Create an implementation plan before large changes.

## Step 4 — Implement

Implement one phase at a time.

## Step 5 — Static / Code Review

Review your changes without running the test suite.

Check:

* architecture
* correctness
* API compatibility
* lifecycle handling
* performance
* unnecessary dependencies
* incorrect assumptions

## Step 6 — Continue to Next Phase

Do NOT run tests.

Repeat until all phases are complete.

---

# 31. IMPORTANT TESTING RULE

## DO NOT RUN TESTS UNTIL ALL FEATURES ARE IMPLEMENTED

During Phases 1 through the final implementation phase:

**Do not run:**

* unit tests
* instrumentation tests
* UI tests
* benchmark tests
* integration tests
* full Gradle test tasks

You may inspect existing tests and update them if required, but **do not execute them**.

The reason is to avoid repeatedly spending time running incomplete intermediate test suites.

Testing happens only after all implementation phases are complete.

---

# 32. Final Verification — ONLY AFTER ALL PHASES

After every planned feature has been implemented:

Run the complete verification process.

At this point perform:

### Build

```text
./gradlew assembleDebug
```

### Unit tests

Run all relevant unit tests.

### Instrumentation tests

Run relevant Android instrumentation tests.

### UI tests

Run relevant Compose/UI tests.

### Static analysis

Run the project's existing lint/static analysis tasks.

### Manual validation

Verify on a real Android device or emulator:

1. Application discovery
2. Large application list
3. Search
4. Sorting/filtering
5. Permission analysis
6. Review score
7. Why Review explanations
8. Application details
9. SDK metadata
10. Settings integration
11. App comparison
12. Uninstall/change while scanning
13. Partial metadata
14. Android version differences
15. Empty states
16. Performance with many installed applications

---

# 33. Final Definition of Done

The project is considered complete only when:

* existing Gradle version is preserved
* existing AGP version is preserved
* existing Kotlin version is preserved
* existing Java/JDK version is preserved
* existing project architecture is preserved unless a documented minimal extension was required
* no unnecessary dependencies were introduced
* real installed-app discovery works
* real PackageManager data is used
* permissions are analyzed correctly
* sensitive permission categories work
* deterministic review explanations work
* review score is explainable
* search works
* application details work
* SDK information works
* application comparison works
* Android settings integration works
* Android API differences are handled
* no fake statistics exist
* no fake scan progress exists
* no unsupported security claims exist
* no application data is uploaded
* app remains responsive with a large application list
* all documentation is updated
* README is complete
* final test suite passes
* final lint/static analysis passes
* final APK builds successfully

---

# 34. Final Reporting Format

After implementation and ONLY after the final verification pass, report:

## Implemented

* ...

## Build

* ...

## Tests

* ...

## Static Analysis

* ...

## Manual Validation

* ...

## Performance

* ...

## Privacy Validation

* ...

## Known Limitations

* ...

## Architecture Notes

* ...

## Files Changed

* ...

## Final Status

* Complete / Incomplete

Do not claim success for any item that was not actually verified.

---

# 35. Final Instruction

Remember:

This is an **existing project**.

The objective is to build **AppLens on top of the current source**, not to replace the project.

Preserve the existing Gradle and build environment.

Preserve working code.

Avoid unnecessary rewrites.

Avoid unnecessary dependencies.

Do not run tests until every implementation phase is finished.

When all implementation work is complete, perform one comprehensive final verification pass.
