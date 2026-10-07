package com.noise.applens.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Helpers for handing system-level actions to Android itself (spec §20, §24).
 *
 * AppLens explains the situation and then opens the official application settings page —
 * it never implements its own permission modification flow when Android already provides
 * the official UI.
 */
object SettingsIntents {

    /**
     * Opens the official "App info" page for [packageName].
     *
     * Returns `false` when no system activity can handle it (extremely rare, restricted
     * devices), so the caller can fall back to showing its own message.
     */
    fun openAppSettings(context: Context, packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
