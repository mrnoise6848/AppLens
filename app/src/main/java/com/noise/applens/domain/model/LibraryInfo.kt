package com.noise.applens.domain.model

/**
 * A library/SDK identified from package metadata (spec §17).
 *
 * AppLens never decompiles an APK. Everything here is derived from deterministic evidence that
 * Android exposes: declared component class names, declared/requested permissions, the application
 * class and the native library directory.
 */
data class LibraryInfo(
    /** Display name, e.g. `Firebase`. */
    val name: String,
    val confidence: LibraryConfidence,
    /** Human readable evidence, one entry per matched signal. */
    val evidence: List<String>,
)

/**
 * How sure AppLens is about a library. The UI must show this — the goal is not to pretend
 * certainty where only indirect evidence exists (spec §17).
 */
enum class LibraryConfidence(val label: String) {
    /** An SDK-authored class is declared in the application manifest. */
    DETECTED("Detected"),

    /** Indirect evidence only (for example a permission namespace owned by the SDK). */
    LIKELY("Likely"),

    /** Weak evidence, e.g. a native library file name that could come from anywhere. */
    UNKNOWN("Unknown");
}

/**
 * Raw evidence collected for one package, before classification. Kept separate from the analyzer
 * so the analyzer stays pure and testable.
 */
data class AppLibraryEvidence(
    /** Fully qualified class names of declared activities, services, receivers and providers. */
    val componentClassNames: List<String>,
    /** Permissions this package declares. */
    val definedPermissions: List<String>,
    /** Permissions this package requests. */
    val requestedPermissions: List<String>,
    /** `ApplicationInfo.className`, when the app overrides the application class. */
    val applicationClassName: String?,
    /** File names inside the native library directory, e.g. `libfirebase.so`. */
    val nativeLibraryNames: List<String>,
) {
    val isEmpty: Boolean
        get() = componentClassNames.isEmpty() &&
            definedPermissions.isEmpty() &&
            requestedPermissions.isEmpty() &&
            applicationClassName == null &&
            nativeLibraryNames.isEmpty()
}
