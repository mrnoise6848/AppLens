package com.noise.applens.data

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.noise.applens.domain.model.InstalledApp
import java.io.File

/**
 * Extracts the AppLens field set from a single [PackageInfo] (spec §7).
 *
 * Every read is defensive: a field that the current Android version does not expose becomes `null`
 * and is recorded in [InstalledApp.readIssues] instead of being fabricated (spec §15, §24).
 */
internal class AppMetadataReader(private val packageManager: PackageManager) {

    private val grantResolver = PermissionGrantResolver(packageManager)

    fun read(packageInfo: PackageInfo): InstalledApp {
        val applicationInfo = packageInfo.applicationInfo
            ?: throw IllegalStateException("No application info for ${packageInfo.packageName}")

        val issues = mutableListOf<String>()

        val label = runCatching {
            packageManager.getApplicationLabel(applicationInfo)?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: packageInfo.packageName
        }.getOrElse {
            issues += "Label could not be read"
            packageInfo.packageName
        }

        val versionName = packageInfo.versionName?.takeIf { it.isNotBlank() }

        val apkSize = runCatching { readApkSize(applicationInfo) }
            .getOrElse { null }
            ?.also { if (it < 0L) issues += "Application size is not available" }

        val grantStates = grantResolver.resolve(
            packageName = packageInfo.packageName,
            requested = packageInfo.requestedPermissions,
            rawFlags = packageInfo.requestedPermissionsFlags,
        )

        val requestedPermissions = packageInfo.requestedPermissions?.toList().orEmpty()

        return InstalledApp(
            packageName = packageInfo.packageName,
            label = label,
            versionName = versionName,
            versionCode = packageInfo.longVersionCode,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            apkSizeBytes = apkSize,
            targetSdkVersion = applicationInfo.targetSdkVersion,
            minSdkVersion = readMinSdk(applicationInfo, issues),
            requestedPermissions = requestedPermissions,
            permissionGrantStates = grantStates,
            debuggable = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0,
            isSystemApp = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 ||
                applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
            isEnabled = applicationInfo.enabled,
            flags = applicationInfo.flags,
            sourceDir = applicationInfo.sourceDir,
            splitSourceDirs = applicationInfo.splitSourceDirs?.toList(),
            nativeLibraryDir = applicationInfo.nativeLibraryDir,
            readIssues = issues,
        )
    }

    /**
     * Size of the on-disk APK files (base + splits). This is the size Android reports without
     * usage access; app data size would require `PACKAGE_USAGE_STATS`, which AppLens does not
     * request (spec §21). Returns `null` when nothing can be read.
     */
    private fun readApkSize(applicationInfo: android.content.pm.ApplicationInfo): Long? {
        val base = applicationInfo.sourceDir ?: return null
        var total = 0L
        var readable = false

        val baseFile = File(base)
        if (baseFile.exists()) {
            total += baseFile.length()
            readable = true
        }
        applicationInfo.splitSourceDirs?.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                total += file.length()
                readable = true
            }
        }
        return if (readable && total > 0) total else null
    }

    private fun readMinSdk(
        applicationInfo: android.content.pm.ApplicationInfo,
        issues: MutableList<String>,
    ): Int? = runCatching {
        // ApplicationInfo.minSdkVersion is only reported on supported releases; guard anyway.
        applicationInfo.minSdkVersion.takeIf { it > 0 }
    }.getOrElse {
        issues += "Minimum SDK is not available on this Android version"
        null
    }

    companion object {
        /** Flags needed to populate the AppLens field set. */
        @Suppress("DEPRECATION")
        fun packageInfoFlags(): Int = PackageManager.GET_PERMISSIONS

        /** API-level safe wrapper around [PackageManager.getInstalledPackages]. */
        @Suppress("DEPRECATION")
        fun loadInstalledPackages(packageManager: PackageManager): List<PackageInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(packageInfoFlags().toLong())
                )
            } else {
                packageManager.getInstalledPackages(packageInfoFlags())
            }
    }
}
