package com.noise.applens.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noise.applens.domain.analysis.AppAnalysis
import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.domain.model.PermissionGrantState
import com.noise.applens.domain.permission.AppPermission
import com.noise.applens.domain.permission.PermissionCategory
import com.noise.applens.ui.components.AppIcon
import com.noise.applens.ui.components.InfoBanner
import com.noise.applens.ui.components.ScreenHeader
import com.noise.applens.ui.components.SectionTitle
import com.noise.applens.util.formatBytes
import com.noise.applens.util.formatRelativeTime
import com.noise.applens.util.formatTimestamp
import com.noise.applens.util.pluralize

/**
 * Application detail screen (spec §6).
 *
 * Phase 6 covers the summary facts and the permission state; the sensitivity grouping, review
 * explanation, SDK/technical sections and settings integration are added by the following phases.
 */
@Composable
fun AppDetailScreen(
    analysis: AppAnalysis?,
    iconLoader: (String) -> ImageBitmap?,
    onBack: () -> Unit,
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
            PermissionSection(analysis = analysis)

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
    DetailRow(label = "Size", value = formatBytes(app.apkSizeBytes) + " (APK)")
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

/**
 * Permissions grouped by what Android actually reports about them: requested permissions that are
 * granted, requested but not granted, special access, and state the platform does not expose
 * (spec §6, §11).
 */
@Composable
private fun PermissionSection(analysis: AppAnalysis) {
    val permissions = analysis.permissions
    if (permissions.isEmpty()) {
        SectionTitle(text = "Permissions")
        Text(
            text = "This application requests no permissions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        return
    }

    SectionTitle(
        text = "Permissions · ${pluralize(permissions.size, "requested permission")}",
    )

    val granted = permissions.filter { it.grantState == PermissionGrantState.GRANTED }
    val denied = permissions.filter { it.grantState == PermissionGrantState.DENIED }
    val special = permissions.filter { it.category == PermissionCategory.SPECIAL }
    val unknown = permissions.filter {
        it.grantState == PermissionGrantState.UNKNOWN && it.category != PermissionCategory.SPECIAL
    }

    if (granted.isNotEmpty()) PermissionGroup(title = "Granted", permissions = granted)
    if (denied.isNotEmpty()) PermissionGroup(title = "Requested, not granted", permissions = denied)
    if (special.isNotEmpty()) PermissionGroup(title = "Special access", permissions = special)
    if (unknown.isNotEmpty()) {
        PermissionGroup(title = "Not available on this Android version", permissions = unknown)
    }
}

@Composable
private fun PermissionGroup(title: String, permissions: List<AppPermission>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = permissions.size.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
        )
    }

    permissions.forEach { permission ->
        PermissionRow(permission = permission)
    }
}

@Composable
private fun PermissionRow(permission: AppPermission) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        color = when (permission.grantState) {
                            PermissionGrantState.GRANTED -> MaterialTheme.colorScheme.primary
                            PermissionGrantState.DENIED -> MaterialTheme.colorScheme.outline
                            PermissionGrantState.UNKNOWN -> MaterialTheme.colorScheme.tertiary
                        },
                    ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = permission.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        if (permission.description.isNotEmpty()) {
            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 17.dp),
            )
        }
        Text(
            text = permission.permission,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 17.dp),
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
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
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
