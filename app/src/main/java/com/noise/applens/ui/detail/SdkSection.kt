package com.noise.applens.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.ui.components.SectionTitle
import com.noise.applens.util.apiLabel
import com.noise.applens.util.apiShortName

/**
 * Android compatibility information (spec §15).
 *
 * Values the platform does not report are shown as unavailable rather than filled in with a guess.
 */
@Composable
fun SdkSection(app: InstalledApp, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = "Android compatibility")

        DetailRow(
            label = "Target SDK",
            value = apiLabel(app.targetSdkVersion),
            secondary = targetSdkNote(app),
        )

        val minSdk = app.minSdkVersion
        DetailRow(
            label = "Minimum SDK",
            value = if (minSdk == null || minSdk <= 0) {
                "Not available on this Android version"
            } else {
                apiLabel(minSdk)
            },
            secondary = if (minSdk == null || minSdk <= 0) null else apiShortName(minSdk),
        )

        DetailRow(
            label = "Debuggable",
            value = if (app.debuggable) "Yes" else "No",
            secondary = if (app.debuggable) {
                "Debug builds are not intended for normal use"
            } else {
                null
            },
        )

        DetailRow(
            label = "System app",
            value = if (app.isSystemApp) "Yes" else "No",
        )

        DetailRow(
            label = "Enabled",
            value = if (app.isEnabled) "Yes" else "No",
            secondary = if (app.isEnabled) null else "Disabled on this device",
        )
    }
}

/** Explains how far behind the device the application's target SDK is, in plain language. */
private fun targetSdkNote(app: InstalledApp): String {
    val deviceApi = android.os.Build.VERSION.SDK_INT
    val gap = deviceApi - app.targetSdkVersion
    return when {
        gap <= 0 -> "Matches this device (API $deviceApi)"
        gap == 1 -> "One release behind this device (API $deviceApi)"
        gap < 10 -> "$gap releases behind this device (API $deviceApi)"
        else -> "Built for a much older Android version (API $deviceApi)"
    }
}
