package com.noise.applens.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noise.applens.domain.analysis.AppAnalysis
import com.noise.applens.domain.model.AppTechnicalInfo
import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.domain.model.LibraryInfo
import com.noise.applens.ui.components.AppIcon
import com.noise.applens.ui.components.InfoBanner
import com.noise.applens.ui.components.ScreenHeader
import com.noise.applens.ui.components.SectionTitle
import com.noise.applens.util.SettingsIntents
import com.noise.applens.util.formatBytes
import com.noise.applens.util.formatRelativeTime
import com.noise.applens.util.formatTimestamp

/**
 * Application detail screen (spec §6).
 *
 * Sections are added by their phases: summary + permissions (6, 7), review explanation and score
 * (8), SDK/platform information (9), technical metadata (10), libraries (11), settings (14).
 */
@Composable
fun AppDetailScreen(
    analysis: AppAnalysis?,
    iconLoader: (String) -> ImageBitmap?,
    loadTechnicalInfo: suspend (String) -> AppTechnicalInfo?,
    loadLibraries: suspend (String) -> List<LibraryInfo>,
    onBack: () -> Unit,
    onCompare: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = analysis?.app?.label ?: "Application",
            subtitle = analysis?.app?.packageName,
            onBack = onBack,
        )

        if (loading) {
            ReadingState()
            return
        }

        if (analysis == null) {
            RemovedState(onRetry = onRetry)
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            DetailSummary(analysis = analysis, iconLoader = iconLoader)
            CompareAction(onClick = onCompare)
            OpenSettingsAction(packageName = analysis.app.packageName)
            PermissionSection(analysis = analysis)
            SdkSection(app = analysis.app)
            TechnicalSection(
                packageName = analysis.app.packageName,
                load = loadTechnicalInfo,
            )
            LibrariesSection(
                packageName = analysis.app.packageName,
                load = loadLibraries,
            )
            WhyReviewSection(analysis = analysis)

            if (analysis.app.readIssues.isNotEmpty()) {
                InfoBanner(
                    title = "Partial information",
                    text = "Some information is unavailable on this Android version.",
                )
            }
        }
    }
}

@Composable
private fun DetailSummary(analysis: AppAnalysis, iconLoader: (String) -> ImageBitmap?) {
    val app = analysis.app

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            packageName = app.packageName,
            iconLoader = iconLoader,
            size = 64.dp,
            fallbackText = app.label,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = if (app.isSystemApp) "System application" else "User application",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    SectionTitle(text = "Overview")
    DetailRow(label = "Version", value = versionLabel(app))
    DetailRow(label = "Size", value = "${formatBytes(app.apkSizeBytes)} (APK)")
    DetailRow(label = "Target SDK", value = app.targetSdkVersion.toString())
    DetailRow(
        label = "Updated",
        value = formatRelativeTime(app.lastUpdateTime),
        secondary = formatTimestamp(app.lastUpdateTime),
    )
    DetailRow(
        label = "First installed",
        value = formatRelativeTime(app.firstInstallTime),
        secondary = formatTimestamp(app.firstInstallTime),
    )
    DetailRow(
        label = "State",
        value = if (app.isEnabled) "Enabled" else "Disabled",
    )
}

/** Entry point into the comparison flow (spec §18). */
@Composable
private fun CompareAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onClick) {
            Text("Compare with another app")
        }
    }
}

/**
 * Hands the user to Android's official application settings page (spec §20).
 *
 * AppLens does not offer its own permission modification flow: it explains the situation and
 * lets Android handle system-level actions.
 */
@Composable
private fun OpenSettingsAction(packageName: String) {
    val context = LocalContext.current
    var unavailable by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = {
                unavailable = !SettingsIntents.openAppSettings(context, packageName)
            },
        ) {
            Text("Open Android App Settings")
        }
    }
    if (unavailable) {
        Text(
            text = "Android did not open the settings page on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

/** Generic label/value row used by every technical section of the detail screen. */
@Composable
fun DetailRow(label: String, value: String, secondary: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReadingState() {
    Text(
        text = "Reading application…",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
    )
}

@Composable
private fun RemovedState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
    ) {
        Text(
            text = "This application is no longer available",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "It was removed from the device while it was being inspected. " +
                "The rest of your application inventory is unaffected.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text("Refresh inventory")
        }
    }
}

private fun versionLabel(app: InstalledApp): String {
    val name = app.versionName ?: "Unknown version"
    return "$name (${app.versionCode})"
}
