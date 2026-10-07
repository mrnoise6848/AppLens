package com.noise.applens.domain.model

/**
 * Advanced technical metadata for one package (spec §10, §22).
 *
 * Loaded lazily, only when the user expands the technical section, because these fields are not
 * needed for the default UI and some of them cost extra `PackageManager` round trips.
 *
 * Every field is nullable: if the platform does not report it, AppLens says so.
 */
data class AppTechnicalInfo(
    val packageName: String,
    val versionCode: Long,
    /** Absolute path of the base APK, `null` when not readable. */
    val baseApkPath: String?,
    /** Absolute paths of split APKs. */
    val splitApkPaths: List<String>,
    val apkSizeBytes: Long?,
    /** ABIs shipped with the application, empty when it has no native libraries. */
    val abis: List<String>,
    /** Package that installed this application (Play Store, sideload, another installer). */
    val installerPackageName: String?,
    val signing: SigningSummary?,
    val components: ComponentCounts?,
    /** Human readable `ApplicationInfo` flags that are actually meaningful to a user. */
    val notableFlags: List<String>,
    /** Fields that could not be read on this device/version. */
    val unavailable: List<String> = emptyList(),
)

/** Signing certificate summary — a fingerprint, never the certificate itself (spec §10). */
data class SigningSummary(
    /** SHA-256 fingerprints, one per current signer. */
    val sha256: List<String>,
    val signerCount: Int,
    val hasMultipleSigners: Boolean,
    val hasPastSigningCertificates: Boolean,
    val schemeVersion: Int,
)

/** Number of declared components. Counts only — the full component list is not useful here. */
data class ComponentCounts(
    val activities: Int,
    val services: Int,
    val receivers: Int,
    val providers: Int,
) {
    val total: Int get() = activities + services + receivers + providers
}
