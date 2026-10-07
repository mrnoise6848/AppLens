package com.noise.applens.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Small formatting helpers shared by every screen. Never locale-dependent for machine values. */

private const val KB = 1024.0
private const val MB = KB * 1024
private const val GB = MB * 1024

/** `1.2 MB`, `412 MB`, `1.4 GB` — or `—` when the value is unknown. */
fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes < 0) return "—"
    return when {
        bytes >= GB -> String.format(Locale.US, "%.2f GB", bytes / GB)
        bytes >= MB -> String.format(Locale.US, "%.0f MB", bytes / MB)
        bytes >= KB -> String.format(Locale.US, "%.0f KB", bytes / KB)
        else -> "$bytes B"
    }
}

/** Human friendly relative time such as `3 days ago`. `—` for unknown/zero timestamps. */
fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (timestampMillis <= 0L) return "—"
    val delta = (nowMillis - timestampMillis).coerceAtLeast(0L)

    return when {
        delta < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        delta < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(delta)} min ago"
        delta < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(delta)} h ago"
        delta < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(delta)} days ago"
        delta < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(delta) / 7} weeks ago"
        delta < TimeUnit.DAYS.toMillis(365) -> "${TimeUnit.MILLISECONDS.toDays(delta) / 30} months ago"
        else -> "${TimeUnit.MILLISECONDS.toDays(delta) / 365} years ago"
    }
}

/** Absolute date for technical sections: `2026-10-07 13:10`. */
fun formatTimestamp(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timestampMillis))
}

/** Scan duration: `840 ms`, `1.4 s`, `2 min 5 s`. */
fun formatDuration(millis: Long): String = when {
    millis < 1000 -> "$millis ms"
    millis < 60_000 -> String.format(Locale.US, "%.1f s", millis / 1000.0)
    else -> "${millis / 60_000} min ${(millis % 60_000) / 1000} s"
}

/** Number of sensitive/other items for compact labels: `1 permission`, `12 permissions`. */
fun pluralize(count: Int, singular: String, plural: String = "${singular}s"): String =
    if (count == 1) "1 $singular" else "$count $plural"

/** Rounds a 0..1 ratio into a percentage. */
fun percentOf(ratio: Float): Int = (ratio.coerceIn(0f, 1f) * 100).roundToInt()
