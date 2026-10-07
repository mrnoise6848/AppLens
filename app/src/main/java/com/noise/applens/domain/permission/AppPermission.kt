package com.noise.applens.domain.permission

import com.noise.applens.domain.model.PermissionGrantState

/** Coarse, user-facing grouping of a single declared permission (spec §12). */
enum class PermissionCategory {
    /** Access to personal data or a sensor; the category AppLens highlights. */
    SENSITIVE,

    /** Android "special app access" — granted through a system settings screen, not a dialog. */
    SPECIAL,

    /** Install-time / low-sensitivity permission. */
    NORMAL,

    /** Signature, internal or otherwise unknown permission the platform did not classify. */
    OTHER,
}

/**
 * A declared permission enriched with Android's own semantics plus an AppLens grouping.
 *
 * [category] is the AppLens presentation layer; [isRuntimePermission] keeps Android's real
 * protection level visible so the UI never claims a permission is "dangerous" without evidence.
 */
data class AppPermission(
    /** Raw permission name, e.g. `android.permission.CAMERA`. */
    val permission: String,
    /** Human readable name, e.g. `Camera`. */
    val displayName: String,
    /** Short, non-alarmist explanation. Empty when the platform provides nothing useful. */
    val description: String,
    val category: PermissionCategory,
    val grantState: PermissionGrantState,
    /** `true` when Android classifies the permission as dangerous (runtime consent). */
    val isRuntimePermission: Boolean,
    /** `true` when the permission only applies while the app is in the background. */
    val isBackgroundOnly: Boolean,
) {
    val isGranted: Boolean get() = grantState == PermissionGrantState.GRANTED

    /** `true` when the platform did not tell us whether it is granted. */
    val grantIsUnknown: Boolean get() = grantState == PermissionGrantState.UNKNOWN
}
