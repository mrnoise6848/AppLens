package com.noise.applens.domain.model

/**
 * Historical snapshot of the inventory, persisted locally so the app can compare the current state
 * with the previous scan (spec §8).
 *
 * The snapshot is **not** a source of truth — it is only used for caching, history and comparison.
 * It intentionally stores the minimum needed for that: identity, version and the derived counts,
 * never full permission lists or any device-identifying data (spec §21).
 */
data class AppSnapshot(
    /** Wall clock time the snapshot was written (ms epoch). */
    val takenAtMillis: Long,
    val entries: List<SnapshotEntry>,
) {
    fun byPackage(): Map<String, SnapshotEntry> = entries.associateBy { it.packageName }
}

/** One application as recorded in a previous snapshot. */
data class SnapshotEntry(
    val packageName: String,
    val label: String,
    val versionCode: Long,
    val targetSdkVersion: Int,
    val permissionCount: Int,
    val lastUpdateTimeMillis: Long,
    val apkSizeBytes: Long?,
)

/** Difference between the previous snapshot and the freshly scanned inventory. */
data class SnapshotDiff(
    /** Packages present now but not in the snapshot. */
    val added: List<InstalledApp>,
    /** Packages present in the snapshot but no longer installed. */
    val removed: List<SnapshotEntry>,
    /** Packages whose version or target SDK changed since the snapshot. */
    val updated: List<UpdatedApp>,
) {
    val hasChanges: Boolean
        get() = added.isNotEmpty() || removed.isNotEmpty() || updated.isNotEmpty()
}

/** An application that changed between the snapshot and the current scan. */
data class UpdatedApp(
    val current: InstalledApp,
    val previous: SnapshotEntry,
)

/** Reason an update was detected. */
enum class UpdateKind { VERSION_CHANGED, TARGET_SDK_CHANGED }
