package com.noise.applens.domain.analysis

import android.content.pm.PackageManager
import android.os.Build
import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.domain.permission.AppPermission
import com.noise.applens.domain.permission.PermissionAnalyzer
import com.noise.applens.domain.permission.PermissionCategory
import com.noise.applens.domain.review.ReviewSignal
import com.noise.applens.domain.review.SignalType
import com.noise.applens.domain.review.WhyReviewRules

/**
 * Everything the UI needs to know about one application, computed once per scan (spec §9, §13).
 *
 * All members are derived from [InstalledApp] through deterministic rules — the dashboard, list,
 * detail and comparison screens all read the same analysis, so no screen can disagree with another.
 */
data class AppAnalysis(
    val app: InstalledApp,
    /** Enriched, grouped permission list. */
    val permissions: List<AppPermission>,
    /** Deterministic reasons to review this application. */
    val signals: List<ReviewSignal>,
    /** Result of the explainable "needs review" gate. */
    val needsReview: Boolean,
) {
    val sensitivePermissions: List<AppPermission> by lazy {
        permissions.filter { it.category == PermissionCategory.SENSITIVE }
    }

    val specialPermissions: List<AppPermission> by lazy {
        permissions.filter { it.category == PermissionCategory.SPECIAL }
    }

    val sensitiveCount: Int get() = sensitivePermissions.size

    val hasBackgroundLocation: Boolean get() = signals.any { it.type == SignalType.BACKGROUND_LOCATION }
    val hasMicrophone: Boolean get() = signals.any { it.type == SignalType.MICROPHONE }
    val hasCamera: Boolean get() = signals.any { it.type == SignalType.CAMERA }
    val isOldTargetSdk: Boolean get() = signals.any { it.type == SignalType.OLD_TARGET_SDK }
    val isLarge: Boolean get() = signals.any { it.type == SignalType.LARGE_APP }
    val isStale: Boolean get() = signals.any { it.type == SignalType.STALE_UPDATE }
    val isDebuggable: Boolean get() = app.debuggable

    /** Highest priority of any signal, for badges in dense lists. */
    val topPriority: com.noise.applens.domain.review.SignalPriority?
        get() = signals.firstOrNull()?.priority
}

/** Real dashboard counters, all derived from the scanned inventory (spec §9). */
data class InventorySummary(
    val totalApps: Int,
    val needsReview: Int,
    val withSensitivePermissions: Int,
    val oldTargetSdk: Int,
    val largeApps: Int,
    val systemApps: Int,
    val userApps: Int,
    val disabledApps: Int,
    val withReadIssues: Int,
) {
    companion object {
        val EMPTY = InventorySummary(0, 0, 0, 0, 0, 0, 0, 0, 0)
    }
}

/**
 * Computes [AppAnalysis] for the whole inventory and the [InventorySummary] shown on the dashboard.
 * Permission lookups are cached inside [PermissionAnalyzer], so a 300 application inventory costs
 * roughly one binder call per unique permission, not one per application.
 */
class AppAnalysisEngine(
    packageManager: PackageManager,
    private val deviceApi: Int = Build.VERSION.SDK_INT,
) {
    private val permissionAnalyzer = PermissionAnalyzer(packageManager)
    private val rules = WhyReviewRules(deviceApi)

    fun analyzeAll(apps: List<InstalledApp>): List<AppAnalysis> =
        apps.map { analyze(it) }

    fun analyze(app: InstalledApp): AppAnalysis {
        val permissions = permissionAnalyzer.analyze(app)
        val signals = rules.evaluate(app, permissions)
        return AppAnalysis(
            app = app,
            permissions = permissions,
            signals = signals,
            needsReview = rules.needsReview(signals),
        )
    }

    fun summarize(analyses: List<AppAnalysis>): InventorySummary {
        var needsReview = 0
        var withSensitive = 0
        var oldTargetSdk = 0
        var large = 0
        var system = 0
        var user = 0
        var disabled = 0
        var readIssues = 0

        analyses.forEach { analysis ->
            if (analysis.needsReview) needsReview++
            if (analysis.sensitiveCount > 0) withSensitive++
            if (analysis.isOldTargetSdk) oldTargetSdk++
            if (analysis.isLarge) large++
            if (analysis.app.isSystemApp) system++ else user++
            if (!analysis.app.isEnabled) disabled++
            if (analysis.app.readIssues.isNotEmpty()) readIssues++
        }

        return InventorySummary(
            totalApps = analyses.size,
            needsReview = needsReview,
            withSensitivePermissions = withSensitive,
            oldTargetSdk = oldTargetSdk,
            largeApps = large,
            systemApps = system,
            userApps = user,
            disabledApps = disabled,
            withReadIssues = readIssues,
        )
    }
}
