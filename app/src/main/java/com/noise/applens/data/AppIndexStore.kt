package com.noise.applens.data

import android.content.Context
import com.noise.applens.domain.model.AppSnapshot
import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.domain.model.SnapshotDiff
import com.noise.applens.domain.model.SnapshotEntry
import com.noise.applens.domain.model.UpdatedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Local representation of the discovered applications (spec §8).
 *
 * Responsibilities:
 *  * in-memory index for O(1) detail lookup and stable, pre-sorted rendering
 *  * historical snapshot persisted as JSON so scans can be compared
 *  * search over the indexed data (spec §13)
 *
 * PackageManager remains the source of truth — this store is a cache and a comparison layer, and
 * it is always rebuilt from a fresh scan. See `docs/decisions/003-package-manager-source-of-truth.md`.
 */
class AppIndexStore(context: Context) {

    private val snapshotFile = File(context.cacheDir, SNAPSHOT_FILE_NAME)

    @Volatile
    private var byPackage: Map<String, InstalledApp> = emptyMap()

    @Volatile
    private var sortedApps: List<InstalledApp> = emptyList()

    /** Number of indexed applications. */
    val size: Int get() = sortedApps.size

    /** Replaces the whole index. Preserves the (already label-sorted) incoming order. */
    fun replaceAll(apps: List<InstalledApp>) {
        sortedApps = apps
        byPackage = apps.associateBy { it.packageName }
    }

    fun all(): List<InstalledApp> = sortedApps

    fun find(packageName: String): InstalledApp? = byPackage[packageName]

    fun findMany(packageNames: List<String>): List<InstalledApp> =
        packageNames.mapNotNull { byPackage[it] }

    /**
     * Case-insensitive search over application label and package name (spec §13).
     * Runs against the in-memory index, so it stays responsive with large inventories.
     */
    fun search(query: String): List<InstalledApp> {
        val needle = query.trim()
        if (needle.isEmpty()) return sortedApps
        return sortedApps.filter { app ->
            app.label.contains(needle, ignoreCase = true) ||
                app.packageName.contains(needle, ignoreCase = true)
        }
    }

    // --------------------------------------------------------------------- snapshot persistence

    /** Reads the previously persisted snapshot, or `null` when none exists / it is unreadable. */
    suspend fun loadSnapshot(): AppSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            if (!snapshotFile.exists()) return@runCatching null
            parseSnapshot(snapshotFile.readText())
        }.getOrNull()
    }

    /**
     * Writes a snapshot of [apps]. Called once per completed scan; failures are swallowed because
     * a cache write must never break the product (spec §8).
     */
    suspend fun persistSnapshot(apps: List<InstalledApp>): AppSnapshot = withContext(Dispatchers.IO) {
        val snapshot = AppSnapshot(
            takenAtMillis = System.currentTimeMillis(),
            entries = apps.map { it.toSnapshotEntry() },
        )
        runCatching {
            val tmp = File(snapshotFile.parentFile, "$SNAPSHOT_FILE_NAME.tmp")
            tmp.writeText(serializeSnapshot(snapshot))
            if (!tmp.renameTo(snapshotFile)) {
                snapshotFile.delete()
                tmp.renameTo(snapshotFile)
            }
        }
        snapshot
    }

    /** Compares the live index against [previous]. */
    fun diffAgainst(previous: AppSnapshot): SnapshotDiff {
        val previousByPackage = previous.byPackage()
        val current = byPackage

        val added = current.values.filter { it.packageName !in previousByPackage }
        val removed = previous.entries.filter { it.packageName !in current }
        val updated = current.values.mapNotNull { app ->
            val old = previousByPackage[app.packageName] ?: return@mapNotNull null
            val versionChanged = old.versionCode != app.versionCode
            val sdkChanged = old.targetSdkVersion != app.targetSdkVersion
            if (versionChanged || sdkChanged) UpdatedApp(app, old) else null
        }

        return SnapshotDiff(added = added, removed = removed, updated = updated)
    }

    private fun InstalledApp.toSnapshotEntry() = SnapshotEntry(
        packageName = packageName,
        label = label,
        versionCode = versionCode,
        targetSdkVersion = targetSdkVersion,
        permissionCount = requestedPermissions.size,
        lastUpdateTimeMillis = lastUpdateTime,
        apkSizeBytes = apkSizeBytes,
    )

    // --------------------------------------------------------------------------- serialization

    private fun serializeSnapshot(snapshot: AppSnapshot): String {
        val root = JSONObject()
        root.put(KEY_TAKEN_AT, snapshot.takenAtMillis)
        val array = JSONArray()
        snapshot.entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put(KEY_PACKAGE, entry.packageName)
                    .put(KEY_LABEL, entry.label)
                    .put(KEY_VERSION_CODE, entry.versionCode)
                    .put(KEY_TARGET_SDK, entry.targetSdkVersion)
                    .put(KEY_PERMISSION_COUNT, entry.permissionCount)
                    .put(KEY_LAST_UPDATE, entry.lastUpdateTimeMillis)
                    .put(KEY_SIZE, entry.apkSizeBytes ?: JSONObject.NULL)
            )
        }
        root.put(KEY_ENTRIES, array)
        return root.toString()
    }

    private fun parseSnapshot(raw: String): AppSnapshot {
        val root = JSONObject(raw)
        val array = root.optJSONArray(KEY_ENTRIES) ?: JSONArray()
        val entries = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val packageName = item.optString(KEY_PACKAGE).takeIf { it.isNotBlank() } ?: continue
                val size = item.optLong(KEY_SIZE, -1L).takeIf { it >= 0L }
                add(
                    SnapshotEntry(
                        packageName = packageName,
                        label = item.optString(KEY_LABEL, packageName),
                        versionCode = item.optLong(KEY_VERSION_CODE, 0L),
                        targetSdkVersion = item.optInt(KEY_TARGET_SDK, 0),
                        permissionCount = item.optInt(KEY_PERMISSION_COUNT, 0),
                        lastUpdateTimeMillis = item.optLong(KEY_LAST_UPDATE, 0L),
                        apkSizeBytes = size,
                    )
                )
            }
        }
        return AppSnapshot(takenAtMillis = root.optLong(KEY_TAKEN_AT, 0L), entries = entries)
    }

    companion object {
        /** Stored under `cacheDir`; removed automatically with the app cache. */
        const val SNAPSHOT_FILE_NAME = "applens-index-snapshot.json"

        private const val KEY_TAKEN_AT = "takenAt"
        private const val KEY_ENTRIES = "entries"
        private const val KEY_PACKAGE = "package"
        private const val KEY_LABEL = "label"
        private const val KEY_VERSION_CODE = "versionCode"
        private const val KEY_TARGET_SDK = "targetSdk"
        private const val KEY_PERMISSION_COUNT = "permissionCount"
        private const val KEY_LAST_UPDATE = "lastUpdate"
        private const val KEY_SIZE = "size"

        /** File name used for log-free diagnostics; never written to external storage. */
        fun describeCache(file: File): String =
            String.format(Locale.ROOT, "%s (%d bytes)", file.name, file.length())
    }
}
