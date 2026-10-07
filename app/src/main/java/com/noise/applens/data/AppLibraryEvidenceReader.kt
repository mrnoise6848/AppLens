package com.noise.applens.data

import android.content.pm.PackageManager
import android.os.Build
import com.noise.applens.domain.model.AppLibraryEvidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Collects the metadata evidence used for library detection (spec §17).
 *
 * Only manifest-declared facts are read: component class names, declared/requested permissions,
 * the application class and native library file names. No APK is opened, read as a binary or
 * decompiled.
 */
class AppLibraryEvidenceReader(private val packageManager: PackageManager) {

    suspend fun read(packageName: String): AppLibraryEvidence? = withContext(Dispatchers.IO) {
        runCatching {
            val packageInfo = load(packageName)
            val applicationInfo = packageInfo.applicationInfo ?: return@runCatching null

            val components = buildList {
                packageInfo.activities?.forEach { add(it.name) }
                packageInfo.services?.forEach { add(it.name) }
                packageInfo.receivers?.forEach { add(it.name) }
                packageInfo.providers?.forEach { add(it.name) }
            }.filterNotNull()

            AppLibraryEvidence(
                componentClassNames = components,
                definedPermissions = packageInfo.permissions?.map { it.name }.orEmpty().filterNotNull(),
                requestedPermissions = packageInfo.requestedPermissions?.toList().orEmpty(),
                applicationClassName = applicationInfo.className?.takeIf { it.isNotBlank() },
                nativeLibraryNames = readNativeLibraries(applicationInfo.nativeLibraryDir),
            )
        }.getOrNull()
    }

    private fun load(packageName: String): android.content.pm.PackageInfo {
        val flags = PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS or
            AppMetadataReader.packageInfoFlags()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags)
        }
    }

    private fun readNativeLibraries(nativeLibraryDir: String?): List<String> {
        if (nativeLibraryDir == null) return emptyList()
        return runCatching {
            File(nativeLibraryDir).list()?.toList().orEmpty()
        }.getOrDefault(emptyList())
    }
}
