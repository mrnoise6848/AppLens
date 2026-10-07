package com.noise.applens.data

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.noise.applens.domain.model.PermissionGrantState

/**
 * Resolves the grant state of a declared permission for an arbitrary package.
 *
 * Android exposes this differently across versions, so the result is tri-state: when the platform
 * does not answer reliably we report [PermissionGrantState.UNKNOWN] instead of misrepresenting the
 * permission state (spec §11, §24). Uses only APIs available from `minSdk 29` upwards.
 */
internal class PermissionGrantResolver(private val packageManager: PackageManager) {

    /**
     * Permissions that are never granted through the runtime permission dialog and cannot be read
     * back with `checkPermission`. They are surfaced as "special" in the UI instead of being shown
     * as denied (spec §11).
     */
    private val specialPermissions = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.MANAGE_UNKNOWN_APP_SOURCES",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.ACCESS_NOTIFICATION_POLICY",
        "android.permission.WRITE_SETTINGS",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.USE_EXACT_ALARM",
        "android.permission.REQUEST_DELETE_PACKAGES",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.QUERY_ALL_PACKAGES",
    )

    fun isSpecial(permission: String): Boolean = permission in specialPermissions

    /**
     * @param requested permissions declared in the manifest, in declaration order.
     * @param rawFlags `PackageInfo.requestedPermissionsFlags` in the same order, `null` when the
     *  platform did not report flags.
     */
    fun resolve(
        packageName: String,
        requested: Array<String>?,
        rawFlags: IntArray?,
    ): Map<String, PermissionGrantState> {
        if (requested.isNullOrEmpty()) return emptyMap()

        return requested.associateWith { permission ->
            val index = requested.indexOfFirst { it == permission }
            val flagGranted = rawFlags != null &&
                index in rawFlags.indices &&
                (rawFlags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0

            resolveOne(packageName, permission, flagGranted)
        }
    }

    private fun resolveOne(
        packageName: String,
        permission: String,
        flagGranted: Boolean,
    ): PermissionGrantState {
        if (isSpecial(permission)) return PermissionGrantState.UNKNOWN

        val checked = runCatching {
            packageManager.checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED
        }.getOrNull() ?: return if (flagGranted) PermissionGrantState.GRANTED else PermissionGrantState.UNKNOWN

        return when {
            checked -> PermissionGrantState.GRANTED
            flagGranted -> PermissionGrantState.GRANTED
            else -> PermissionGrantState.DENIED
        }
    }
}
