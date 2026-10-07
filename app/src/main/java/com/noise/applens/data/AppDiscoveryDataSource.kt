package com.noise.applens.data

import android.content.Context
import com.noise.applens.domain.model.DiscoveryFailure
import com.noise.applens.domain.model.DiscoveryResult
import com.noise.applens.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Discovers the applications installed on the device (spec §7).
 *
 * Runs entirely on [Dispatchers.IO], reports **real** progress derived from the actual number of
 * packages read, and never lets a single broken package abort the scan (spec §23, §24).
 */
class AppDiscoveryDataSource(context: Context) {

    private val packageManager = context.packageManager
    private val reader = AppMetadataReader(packageManager)

    /**
     * @param onProgress invoked with `(packagesRead, packagesDiscovered)`; guaranteed to be called
     *  once at the end of the pass so the UI never gets stuck at a partial percentage.
     */
    suspend fun discover(
        onProgress: ((Int, Int) -> Unit)? = null,
    ): DiscoveryResult = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val failures = mutableListOf<DiscoveryFailure>()

        val packages = runCatching { AppMetadataReader.loadInstalledPackages(packageManager) }
            .getOrElse { error ->
                failures += DiscoveryFailure(
                    packageName = "",
                    reason = "PackageManager could not be queried: ${error.javaClass.simpleName}",
                )
                emptyList()
            }

        val total = packages.size
        var read = 0
        var lastReportedPercent = -1
        onProgress?.invoke(0, total)

        val apps = ArrayList<InstalledApp>(total)

        for (packageInfo in packages) {
            // Allow cancellation between packages without abandoning the whole pass silently.
            currentCoroutineContext().ensureActive()

            val name = packageInfo.packageName.orEmpty()
            val app = runCatching { reader.read(packageInfo) }
                .getOrElse { error ->
                    failures += DiscoveryFailure(
                        packageName = name,
                        reason = error.message ?: error.javaClass.simpleName,
                    )
                    null
                }

            if (app != null) {
                apps += app
            }

            read++
            val percent = if (total == 0) 100 else read * 100 / total
            if (percent != lastReportedPercent || read == total) {
                lastReportedPercent = percent
                onProgress?.invoke(read, total)
            }
        }

        if (total == 0) onProgress?.invoke(0, 0)

        DiscoveryResult(
            apps = apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }),
            discoveredCount = total,
            failures = failures,
            elapsedMillis = System.currentTimeMillis() - startedAt,
        )
    }
}
