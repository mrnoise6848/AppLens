package com.noise.applens.domain.analysis

import com.noise.applens.domain.model.PermissionGrantState
import com.noise.applens.domain.permission.AppPermission
import com.noise.applens.domain.permission.PermissionCategory

/**
 * Side-by-side comparison of two applications (spec §18).
 *
 * The goal is not to declare a winner — rows are neutral facts and the UI only marks differences.
 */
data class ComparisonTable(
    val left: AppAnalysis,
    val right: AppAnalysis,
    /** Scalar facts such as size, target SDK, score. */
    val facts: List<ComparisonFact>,
    /** Union of sensitive/special permissions requested by either application. */
    val permissionRows: List<PermissionComparisonRow>,
    /** True when at least one fact or permission differs. */
    val hasDifferences: Boolean,
)

/** One scalar row. [differs] drives the subtle emphasis, nothing is judged. */
data class ComparisonFact(
    val label: String,
    val left: String,
    val right: String,
    val differs: Boolean,
)

/** Presence/state of one permission on one side. */
enum class ComparisonCell(val symbol: String, val description: String) {
    GRANTED("✓", "Granted"),
    REQUESTED("○", "Requested, not granted"),
    UNKNOWN("?", "Not available on this Android version"),
    ABSENT("—", "Not requested");

    companion object {
        fun of(permission: AppPermission?): ComparisonCell = when {
            permission == null -> ABSENT
            permission.grantState == PermissionGrantState.GRANTED -> GRANTED
            permission.grantState == PermissionGrantState.DENIED -> REQUESTED
            else -> UNKNOWN
        }
    }
}

data class PermissionComparisonRow(
    val label: String,
    val category: PermissionCategory,
    val left: ComparisonCell,
    val right: ComparisonCell,
) {
    val differs: Boolean get() = left != right
}

/**
 * Builds the comparison table from two analyses. Pure and deterministic: the same pair always
 * produces the same rows in the same order.
 */
object AppComparisonBuilder {

    fun build(left: AppAnalysis, right: AppAnalysis): ComparisonTable {
        val facts = buildFacts(left, right)
        val permissionRows = buildPermissionRows(left, right)

        return ComparisonTable(
            left = left,
            right = right,
            facts = facts,
            permissionRows = permissionRows,
            hasDifferences = facts.any { it.differs } || permissionRows.any { it.differs },
        )
    }

    private fun buildFacts(left: AppAnalysis, right: AppAnalysis): List<ComparisonFact> {
        fun fact(label: String, l: String, r: String) =
            ComparisonFact(label = label, left = l, right = r, differs = l != r)

        val a = left.app
        val b = right.app

        return listOf(
            fact("Version", a.versionName ?: "—", b.versionName ?: "—"),
            fact("APK size", formatSize(a.apkSizeBytes), formatSize(b.apkSizeBytes)),
            fact("Target SDK", a.targetSdkVersion.toString(), b.targetSdkVersion.toString()),
            fact("Minimum SDK", a.minSdkVersion?.toString() ?: "—", b.minSdkVersion?.toString() ?: "—"),
            fact("Updated", relative(a.lastUpdateTime), relative(b.lastUpdateTime)),
            fact("First installed", relative(a.firstInstallTime), relative(b.firstInstallTime)),
            fact("Requested permissions", a.requestedPermissions.size.toString(), b.requestedPermissions.size.toString()),
            fact("Sensitive permissions", left.sensitiveCount.toString(), right.sensitiveCount.toString()),
            fact("Review Score", left.score.value.toString(), right.score.value.toString()),
            fact("Needs review", yesNo(left.needsReview), yesNo(right.needsReview)),
            fact("System app", yesNo(a.isSystemApp), yesNo(b.isSystemApp)),
            fact("Debuggable", yesNo(a.debuggable), yesNo(b.debuggable)),
            fact("Type", if (a.isSystemApp) "System" else "User", if (b.isSystemApp) "System" else "User"),
        )
    }

    /** Sensitive and special permissions requested by either side, sensitive group first. */
    private fun buildPermissionRows(
        left: AppAnalysis,
        right: AppAnalysis,
    ): List<PermissionComparisonRow> {
        fun comparable(analysis: AppAnalysis): Map<String, AppPermission> =
            analysis.permissions
                .filter { it.category == PermissionCategory.SENSITIVE || it.category == PermissionCategory.SPECIAL }
                .associateBy { it.permission }

        val leftMap = comparable(left)
        val rightMap = comparable(right)

        return (leftMap.keys + rightMap.keys)
            .distinct()
            .mapNotNull { permission ->
                val leftPermission = leftMap[permission] ?: rightMap[permission] ?: return@mapNotNull null
                PermissionComparisonRow(
                    label = leftPermission.displayName,
                    category = leftPermission.category,
                    left = ComparisonCell.of(leftMap[permission]),
                    right = ComparisonCell.of(rightMap[permission]),
                )
            }
            .sortedWith(
                compareBy<PermissionComparisonRow> { it.category.ordinal }
                    .thenBy { it.label.lowercase() }
            )
    }

    private fun formatSize(bytes: Long?): String =
        if (bytes == null || bytes < 0) "—" else com.noise.applens.util.formatBytes(bytes)

    private fun relative(timestamp: Long): String =
        if (timestamp <= 0L) "—" else com.noise.applens.util.formatRelativeTime(timestamp)

    private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"
}
