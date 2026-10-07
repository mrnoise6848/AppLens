package com.noise.applens.data

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import android.util.LruCache

/**
 * Bounded cache of application icons keyed by package name (spec §23).
 *
 * Icons are loaded on demand only — never up front for the whole inventory — and the cache evicts
 * least-recently-used entries so a 300+ application list stays within a fixed memory budget.
 */
class AppIconCache(
    private val packageManager: PackageManager,
    maxIcons: Int = DEFAULT_MAX_ICONS,
) {
    private val cache = object : LruCache<String, ImageBitmap>(maxIcons) {}

    /** Returns a cached icon, loading it synchronously on the calling thread when missing. */
    fun getOrLoad(packageName: String): ImageBitmap? = cache.get(packageName) ?: load(packageName)
        ?.also { cache.put(packageName, it) }

    fun invalidate(packageName: String) {
        cache.remove(packageName)
    }

    fun clear() {
        cache.evictAll()
    }

    private fun load(packageName: String): ImageBitmap? = runCatching {
        val drawable = packageManager.getApplicationIcon(packageName)
        drawable.toBitmap().asImageBitmap()
    }.getOrNull()

    companion object {
        const val DEFAULT_MAX_ICONS = 150
    }
}
