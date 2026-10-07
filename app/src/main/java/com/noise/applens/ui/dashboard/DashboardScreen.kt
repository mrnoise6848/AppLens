package com.noise.applens.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noise.applens.domain.analysis.AppFilter
import com.noise.applens.domain.analysis.InventorySummary
import com.noise.applens.state.AppLensUiState
import com.noise.applens.state.ScanPhase
import com.noise.applens.ui.components.InfoBanner
import com.noise.applens.ui.components.SectionTitle
import com.noise.applens.util.formatDuration
import com.noise.applens.util.pluralize

/**
 * AppLens home screen (spec §9).
 *
 * Every number on this screen comes from [InventorySummary], which is computed from the real
 * installed-application inventory — there are no hardcoded counts and no fake progress.
 */
@Composable
fun DashboardScreen(
    state: AppLensUiState,
    onOpenApps: (AppFilter) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
    ) {
        item { DashboardHeader() }

        when (val scan = state.scan) {
            ScanPhase.Idle -> item { ScanningState(percent = 0, read = 0, total = 0) }

            is ScanPhase.Scanning -> item {
                ScanningState(percent = scan.percent, read = scan.read, total = scan.total)
            }

            is ScanPhase.Failed -> item {
                ScanFailedState(message = scan.message, onRetry = onRetry)
            }

            is ScanPhase.Ready -> readyContent(state, scan, onOpenApps)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.readyContent(
    state: AppLensUiState,
    scan: ScanPhase.Ready,
    onOpenApps: (AppFilter) -> Unit,
) {
    val summary = state.summary
    if (summary == null || summary.totalApps == 0) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(
                    text = "No applications available to analyze.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "This device did not report any installed applications. " +
                        "Package visibility can be limited by the system.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        return
    }

    item { InventoryHero(summary = summary, scan = scan) }

    item {
        if (state.failures.isNotEmpty()) {
            InfoBanner(
                title = "Partial information",
                text = "${pluralize(state.failures.size, "application", "applications")} could not be " +
                    "read. The rest of your application inventory is available.",
            )
        }
    }

    item {
        SectionTitle(text = "Worth a closer look")
        DashboardMetricRow(
            title = "Needs review",
            subtitle = "Several review signals combined",
            count = summary.needsReview,
            accent = MaterialTheme.colorScheme.tertiary,
            onClick = { onOpenApps(AppFilter.NEEDS_REVIEW) },
        )
        DashboardMetricRow(
            title = "Sensitive permissions",
            subtitle = "Camera, microphone, location, contacts…",
            count = summary.withSensitivePermissions,
            accent = MaterialTheme.colorScheme.secondary,
            onClick = { onOpenApps(AppFilter.SENSITIVE) },
        )
        DashboardMetricRow(
            title = "Old target SDK",
            subtitle = "Built for an older Android version",
            count = summary.oldTargetSdk,
            accent = MaterialTheme.colorScheme.primary,
            onClick = { onOpenApps(AppFilter.OLD_TARGET_SDK) },
        )
        DashboardMetricRow(
            title = "Large apps",
            subtitle = "APK of 150 MB or more",
            count = summary.largeApps,
            accent = MaterialTheme.colorScheme.outline,
            onClick = { onOpenApps(AppFilter.LARGE) },
        )
    }

    item {
        SectionTitle(text = "Inventory")
        Text(
            text = "${summary.systemApps} system · ${summary.userApps} user" +
                if (summary.disabledApps > 0) " · ${summary.disabledApps} disabled" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp),
        )
    }

    val diff = state.diff
    if (diff != null && diff.hasChanges) {
        item {
            SectionTitle(text = "Since your last scan")
            Text(
                text = buildString {
                    if (diff.added.isNotEmpty()) append("${diff.added.size} added")
                    if (diff.removed.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append("${diff.removed.size} removed")
                    }
                    if (diff.updated.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append("${diff.updated.size} updated")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun DashboardHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            text = "AppLens",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Understand what your installed apps can access. " +
                "Everything is analysed on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun InventoryHero(summary: InventorySummary, scan: ScanPhase.Ready) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            text = summary.totalApps.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = pluralize(summary.totalApps, "installed application"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Scanned in ${formatDuration(scan.elapsedMillis)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun ScanningState(percent: Int, read: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            progress = { if (total <= 0) 0f else read.toFloat() / total.toFloat() },
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
        )
        Text(
            text = if (total <= 0) {
                "Reading installed applications…"
            } else {
                "Reading applications — $read of $total ($percent%)"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun ScanFailedState(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = "Applications could not be read",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Android did not return the application list. $message",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) {
            Text("Try again")
        }
    }
}

/**
 * One tappable dashboard metric. Uses a colour accent and a number instead of a card, to keep the
 * screen quiet (spec §26: avoid excessive cards and noisy dashboards).
 */
@Composable
private fun DashboardMetricRow(
    title: String,
    subtitle: String,
    count: Int,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box10(accent = accent)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (count == 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun Box10(accent: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color = accent, shape = CircleShape),
    )
}
