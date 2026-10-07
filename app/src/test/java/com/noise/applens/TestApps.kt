package com.noise.applens

import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.domain.model.PermissionGrantState

/**
 * Shared fixtures for domain unit tests. Everything here is plain data — no Android APIs.
 */
object TestApps {

    fun app(
        packageName: String = "com.example.app",
        label: String = "Example",
        versionCode: Long = 1L,
        firstInstallTime: Long = 0L,
        lastUpdateTime: Long = 0L,
        apkSizeBytes: Long? = null,
        targetSdkVersion: Int = 35,
        minSdkVersion: Int? = 29,
        requestedPermissions: List<String> = emptyList(),
        permissionGrantStates: Map<String, PermissionGrantState> = emptyMap(),
        debuggable: Boolean = false,
        isSystemApp: Boolean = false,
        isEnabled: Boolean = true,
        flags: Int = 0,
        readIssues: List<String> = emptyList(),
    ): InstalledApp = InstalledApp(
        packageName = packageName,
        label = label,
        versionName = "1.0",
        versionCode = versionCode,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        apkSizeBytes = apkSizeBytes,
        targetSdkVersion = targetSdkVersion,
        minSdkVersion = minSdkVersion,
        requestedPermissions = requestedPermissions,
        permissionGrantStates = permissionGrantStates,
        debuggable = debuggable,
        isSystemApp = isSystemApp,
        isEnabled = isEnabled,
        flags = flags,
        sourceDir = "/data/app/$packageName/base.apk",
        splitSourceDirs = null,
        nativeLibraryDir = null,
        readIssues = readIssues,
    )
}
