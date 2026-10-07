package com.noise.applens.domain.permission

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.domain.model.PermissionGrantState
import java.util.concurrent.ConcurrentHashMap

/**
 * Turns the raw permission list of an application into grouped, explained [AppPermission] entries
 * (spec §7, §12).
 *
 * Classification uses Android's own protection level where the platform exposes it, combined with
 * the explicit [PermissionCatalog] groupings. Lookups are cached per permission name so scanning
 * hundreds of applications costs one `PackageManager` round trip per unique permission.
 *
 * Must be called off the main thread: `getPermissionInfo` is a binder call.
 */
class PermissionAnalyzer(private val packageManager: PackageManager) {

    private val protectionCache = ConcurrentHashMap<String, Int>()

    /** Returns the enriched permission list, sensitive entries first. */
    fun analyze(app: InstalledApp): List<AppPermission> {
        if (app.requestedPermissions.isEmpty()) return emptyList()

        return app.requestedPermissions
            .distinct()
            .map { permission -> buildPermission(permission, app) }
            .sortedWith(
                compareBy<AppPermission> { it.category.sortOrder }
                    .thenBy { it.displayName.lowercase() }
            )
    }

    /** Convenience: only the entries AppLens presents as sensitive. */
    fun sensitiveOf(app: InstalledApp): List<AppPermission> =
        analyze(app).filter { it.category == PermissionCategory.SENSITIVE }

    private fun buildPermission(permission: String, app: InstalledApp): AppPermission {
        val protection = protectionLevel(permission)
        val isRuntime = protection == PermissionInfo.PROTECTION_DANGEROUS

        return AppPermission(
            permission = permission,
            displayName = PermissionCatalog.displayName(permission),
            description = PermissionCatalog.description(permission),
            category = categoryOf(permission, protection),
            grantState = app.permissionGrantStates[permission] ?: PermissionGrantState.UNKNOWN,
            isRuntimePermission = isRuntime,
            isBackgroundOnly = permission in PermissionCatalog.BACKGROUND_ONLY,
        )
    }

    private fun categoryOf(permission: String, protection: Int?): PermissionCategory = when {
        PermissionCatalog.isSpecial(permission) -> PermissionCategory.SPECIAL
        PermissionCatalog.isSensitive(permission) -> PermissionCategory.SENSITIVE
        permission in PermissionCatalog.LOW_SENSITIVITY_RUNTIME -> PermissionCategory.NORMAL
        protection == PermissionInfo.PROTECTION_DANGEROUS -> PermissionCategory.SENSITIVE
        protection == PermissionInfo.PROTECTION_NORMAL -> PermissionCategory.NORMAL
        // signature / internal / unknown: not something a user chose, do not imply otherwise
        else -> PermissionCategory.OTHER
    }

    /** Protection base level, or `null` when the platform did not return permission info. */
    private fun protectionLevel(permission: String): Int? {
        val cached = protectionCache[permission]
        if (cached != null) return if (cached == UNKNOWN_PROTECTION) null else cached

        val level = runCatching { packageManager.getPermissionInfo(permission, 0) }
            .map { it.protection }
            .getOrElse { UNKNOWN_PROTECTION }

        protectionCache.putIfAbsent(permission, level)
        return if (level == UNKNOWN_PROTECTION) null else level
    }

    companion object {
        private const val UNKNOWN_PROTECTION = -1
    }
}

private val PermissionCategory.sortOrder: Int
    get() = when (this) {
        PermissionCategory.SENSITIVE -> 0
        PermissionCategory.SPECIAL -> 1
        PermissionCategory.NORMAL -> 2
        PermissionCategory.OTHER -> 3
    }
