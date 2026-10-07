package com.noise.applens.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.noise.applens.domain.model.AppTechnicalInfo
import com.noise.applens.domain.model.ComponentCounts
import com.noise.applens.domain.model.SigningSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Reads advanced technical metadata for a single package (spec §10).
 *
 * Runs off the main thread, isolates every optional read behind `runCatching` and records what was
 * unavailable instead of returning a fabricated value.
 */
class AppTechnicalInfoReader(private val packageManager: PackageManager) {

    suspend fun read(packageName: String): AppTechnicalInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val packageInfo = load(packageName, signingFlags())
            val applicationInfo = packageInfo.applicationInfo ?: return@runCatching null
            val unavailable = mutableListOf<String>()

            val base = applicationInfo.sourceDir
            val splits = applicationInfo.splitSourceDirs?.toList().orEmpty()
            val size = readApkSize(base, splits)

            val abis = readAbis(applicationInfo)
            if (abis.isEmpty()) unavailable += "Native libraries (none reported)"

            val installer = readInstaller(packageName).also {
                if (it == null) unavailable += "Installer package"
            }

            val signing = readSigning(packageInfo).also {
                if (it == null) unavailable += "Signing information"
            }

            val components = readComponents(packageName).also {
                if (it == null) unavailable += "Component counts"
            }

            AppTechnicalInfo(
                packageName = packageInfo.packageName,
                versionCode = packageInfo.longVersionCode,
                baseApkPath = base,
                splitApkPaths = splits,
                apkSizeBytes = size,
                abis = abis,
                installerPackageName = installer,
                signing = signing,
                components = components,
                notableFlags = notableFlags(applicationInfo.flags),
                unavailable = unavailable,
            )
        }.getOrNull()
    }

    private fun load(packageName: String, flags: Int): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags)
        }

    private fun signingFlags(): Int =
        PackageManager.GET_SIGNING_CERTIFICATES or AppMetadataReader.packageInfoFlags()

    private fun componentFlags(): Int =
        PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS

    private fun readApkSize(base: String?, splits: List<String>): Long? {
        if (base == null) return null
        var total = 0L
        var readable = false
        val files = listOfNotNull(base) + splits
        files.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                total += file.length()
                readable = true
            }
        }
        return if (readable && total > 0) total else null
    }

    /** ABIs from the extracted native library directory, falling back to sibling `lib/<abi>` dirs. */
    private fun readAbis(applicationInfo: ApplicationInfo): List<String> {
        val found = linkedSetOf<String>()

        applicationInfo.nativeLibraryDir?.substringAfterLast('/')?.let { segment ->
            normalizeAbi(segment)?.let { found += it }
        }

        val base = applicationInfo.sourceDir
        if (base != null) {
            val libDir = File(File(base).parentFile, "lib")
            if (libDir.isDirectory) {
                libDir.listFiles()?.forEach { dir ->
                    if (dir.isDirectory) normalizeAbi(dir.name)?.let { found += it }
                }
            }
        }
        return found.toList()
    }

    private fun normalizeAbi(name: String): String? = when (name) {
        "arm64" -> "arm64-v8a"
        "arm" -> "armeabi-v7a"
        "x86" -> "x86"
        "x86_64" -> "x86_64"
        "riscv64" -> "riscv64"
        else -> name.takeIf { it.contains('-') || it.contains('_') }
    }

    private fun readInstaller(packageName: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun readSigning(packageInfo: PackageInfo): SigningSummary? {
        val signingInfo = packageInfo.signingInfo ?: return null
        val signers = runCatching { signingInfo.apkContentsSigners }.getOrNull() ?: return null
        if (signers.isEmpty()) return null

        val fingerprints = signers.map { signature ->
            sha256Hex(runCatching { signature.toByteArray() }.getOrNull() ?: return null)
        }

        return SigningSummary(
            sha256 = fingerprints,
            signerCount = signers.size,
            hasMultipleSigners = runCatching { signingInfo.hasMultipleSigners() }.getOrNull() ?: false,
            hasPastSigningCertificates = runCatching { signingInfo.hasPastSigningCertificates() }
                .getOrNull() ?: false,
            schemeVersion = runCatching { signingInfo.schemeVersion }.getOrNull() ?: 0,
        )
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { byte -> "%02X".format(byte) }
    }

    private fun readComponents(packageName: String): ComponentCounts? = runCatching {
        val info = load(packageName, componentFlags())
        ComponentCounts(
            activities = info.activities?.size ?: 0,
            services = info.services?.size ?: 0,
            receivers = info.receivers?.size ?: 0,
            providers = info.providers?.size ?: 0,
        )
    }.getOrNull()

    /** Only flags a user can act on; everything else stays hidden (spec §10). */
    private fun notableFlags(flags: Int): List<String> = buildList {
        if (flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) add("debuggable")
        if (flags and ApplicationInfo.FLAG_TEST_ONLY != 0) add("test only")
        if (flags and ApplicationInfo.FLAG_PERSISTENT != 0) add("persistent")
        if (flags and ApplicationInfo.FLAG_LARGE_HEAP != 0) add("large heap")
        if (flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0) add("allows backup")
        if (flags and ApplicationInfo.FLAG_SYSTEM != 0) add("system")
        if (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) add("updated system app")
        if (flags and ApplicationInfo.FLAG_STOPPED != 0) add("stopped")
        if (flags and ApplicationInfo.FLAG_SUSPENDED != 0) add("suspended")
        if (flags and ApplicationInfo.FLAG_HAS_CODE != 0) add("has code")
    }
}
