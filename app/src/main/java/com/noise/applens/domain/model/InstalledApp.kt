package com.noise.applens.domain.model

/**
 * Immutable local record of one installed application.
 *
 * [PackageManager] is always the source of truth; this model is a snapshot used for rendering,
 * caching and derived analysis (spec §8). Every field that is not guaranteed on every supported
 * Android version is nullable / optional — never fabricate a value (spec §15, §25).
 *
 * @property packageName unique package identifier.
 * @property label user visible application label; falls back to the package name when unavailable.
 * @property versionName declared version name, `null` when the package does not declare one.
 * @property versionCode monotonically increasing version code (`longVersionCode`).
 * @property firstInstallTime first install timestamp (ms epoch), `0` when unknown.
 * @property lastUpdateTime last update timestamp (ms epoch), `0` when unknown.
 * @property apkSizeBytes total size of the base + split APK files on disk. This is **not** the
 *  app data size (that would require usage access); `null` when the files cannot be read.
 * @property targetSdkVersion target SDK declared by the application.
 * @property minSdkVersion minimum SDK declared by the application, `null` when not reported.
 * @property requestedPermissions permissions declared in the manifest, in declaration order.
 * @property permissionGrantStates resolved grant state per requested permission.
 *  See [PermissionGrantState] — `UNKNOWN` is used whenever the platform does not expose the state.
 * @property debuggable whether the `android:debuggable` flag is set.
 * @property isSystemApp whether the application is installed on the system image.
 * @property isEnabled whether the application component is currently enabled.
 * @property flags raw [android.content.pm.ApplicationInfo] flags, kept for technical details.
 * @property sourceDir base APK path, `null` when not readable.
 * @property splitSourceDirs split APK paths when the app uses split APKs.
 * @property nativeLibraryDir native library directory, `null` for pure-Java apps / when unavailable.
 * @property readIssues human readable notes about fields that could not be read for this package.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val apkSizeBytes: Long?,
    val targetSdkVersion: Int,
    val minSdkVersion: Int?,
    val requestedPermissions: List<String>,
    val permissionGrantStates: Map<String, PermissionGrantState>,
    val debuggable: Boolean,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val flags: Int,
    val sourceDir: String?,
    val splitSourceDirs: List<String>?,
    val nativeLibraryDir: String?,
    val readIssues: List<String> = emptyList(),
) {
    /** Permissions explicitly granted to this package right now. */
    val grantedPermissions: Set<String>
        get() = permissionGrantStates.filterValues { it == PermissionGrantState.GRANTED }.keys

    /** Permissions declared but reported as denied. */
    val deniedPermissions: Set<String>
        get() = permissionGrantStates.filterValues { it == PermissionGrantState.DENIED }.keys

    /** Declared permissions whose grant state the platform did not expose. */
    val unresolvedPermissions: Set<String>
        get() = permissionGrantStates.filterValues { it == PermissionGrantState.UNKNOWN }.keys

    /** Size in bytes for sorting/filtering; unknown sizes sort last. */
    val sizeSortKey: Long get() = apkSizeBytes ?: -1L
}

/** Whether a declared permission is currently granted — `UNKNOWN` instead of a wrong guess. */
enum class PermissionGrantState { GRANTED, DENIED, UNKNOWN }

/** Result of a full discovery pass (spec §7). */
data class DiscoveryResult(
    /** Successfully read applications, sorted by label. */
    val apps: List<InstalledApp>,
    /** Number of packages the platform returned before reading them. */
    val discoveredCount: Int,
    /** Packages that could not be read; one failure must not abort the scan (spec §24). */
    val failures: List<DiscoveryFailure>,
    /** Wall clock duration of the pass in milliseconds. */
    val elapsedMillis: Long,
)

/** A single package that could not be turned into an [InstalledApp]. */
data class DiscoveryFailure(
    val packageName: String,
    val reason: String,
)
