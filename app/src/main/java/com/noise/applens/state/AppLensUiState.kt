package com.noise.applens.state

import com.noise.applens.domain.analysis.AppAnalysis
import com.noise.applens.domain.analysis.InventorySummary
import com.noise.applens.domain.model.AppSnapshot
import com.noise.applens.domain.model.DiscoveryFailure
import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.domain.model.SnapshotDiff
import kotlin.math.roundToInt

/** Lifecycle of the inventory scan. Progress values are always derived from real work. */
sealed interface ScanPhase {
    /** Nothing has been requested yet. */
    data object Idle : ScanPhase

    /** A scan is running; [read] packages have been read out of [total] discovered packages. */
    data class Scanning(val read: Int, val total: Int) : ScanPhase {
        /** `0f..1f`, `0f` while the package count is still unknown. */
        val progress: Float
            get() = if (total <= 0) 0f else read.toFloat() / total.toFloat()

        /** Percentage for display, `0` until the first package has been read. */
        val percent: Int
            get() = (progress * 100).roundToInt()
    }

    /** The scan finished. */
    data class Ready(
        /** Number of applications that could not be read. */
        val failureCount: Int = 0,
        /** Duration of the scan in milliseconds. */
        val elapsedMillis: Long = 0L,
    ) : ScanPhase

    /** The scan could not be performed at all. */
    data class Failed(val message: String) : ScanPhase
}

/**
 * Immutable UI state of the application. Every count shown to the user is derived from [apps],
 * never from a placeholder (spec §9).
 */
data class AppLensUiState(
    val scan: ScanPhase = ScanPhase.Idle,
    /** Current inventory, label-sorted. Empty until the first scan completes. */
    val apps: List<InstalledApp> = emptyList(),
    /** Deterministic per-application analysis, aligned 1:1 with [apps]. */
    val analyses: List<AppAnalysis> = emptyList(),
    /** Dashboard counters, all derived from [analyses]. `null` until the first completed scan. */
    val summary: InventorySummary? = null,
    /** Packages that could not be read during the last scan. */
    val failures: List<DiscoveryFailure> = emptyList(),
    /** Snapshot taken by the previous run, used for change detection only. */
    val previousSnapshot: AppSnapshot? = null,
    /** Difference between the previous snapshot and the current inventory. */
    val diff: SnapshotDiff? = null,
) {
    /** `true` while a scan is in flight. */
    val isScanning: Boolean get() = scan is ScanPhase.Scanning

    /** `true` once at least one completed scan produced results. */
    val isReady: Boolean get() = scan is ScanPhase.Ready && apps.isNotEmpty()

    /**
     * `true` when some packages failed but the rest of the inventory is usable — drives the
     * partial-information state required by spec §24.
     */
    val hasPartialInformation: Boolean get() = failures.isNotEmpty()
}
