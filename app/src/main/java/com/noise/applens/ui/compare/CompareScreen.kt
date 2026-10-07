package com.noise.applens.ui.compare

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noise.applens.domain.analysis.AppAnalysis
import com.noise.applens.domain.analysis.AppComparisonBuilder
import com.noise.applens.domain.analysis.AppListQuery
import com.noise.applens.domain.analysis.ComparisonTable
import com.noise.applens.domain.permission.PermissionCategory
import com.noise.applens.state.CompareSide
import com.noise.applens.state.CompareUiState
import com.noise.applens.ui.components.AppIcon
import com.noise.applens.ui.components.ScreenHeader
import com.noise.applens.ui.components.SectionTitle

/**
 * Two-application comparison (spec §18).
 *
 * Every row is a neutral fact and the only visual treatment is on values that actually differ —
 * the screen never declares a winner.
 */
@Composable
fun CompareScreen(
    analyses: List<AppAnalysis>,
    state: CompareUiState,
    iconLoader: (String) -> ImageBitmap?,
    onBack: () -> Unit,
    onPickSide: (CompareSide) -> Unit,
    onPickApp: (String) -> Unit,
    onClosePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Compare",
            subtitle = if (state.isComplete) "Side by side" else "Choose two applications",
            onBack = onBack,
        )

        when {
            state.isPicking -> AppPicker(
                analyses = analyses,
                side = state.selecting ?: CompareSide.FIRST,
                iconLoader = iconLoader,
                onSelect = onPickApp,
                onCancel = onClosePicker,
            )

            state.isComplete -> {
                val left = analyses.find { it.app.packageName == state.first }
                val right = analyses.find { it.app.packageName == state.second }
                if (left == null || right == null) {
                    MissingApp(onPickSide = onPickSide)
                } else {
                    ComparisonContent(
                        table = remember(left, right) { AppComparisonBuilder.build(left, right) },
                        onPickSide = onPickSide,
                        iconLoader = iconLoader,
                    )
                }
            }

            else -> SlotPicker(state = state, onPickSide = onPickSide)
        }
    }
}

@Composable
private fun SlotPicker(state: CompareUiState, onPickSide: (CompareSide) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = "Pick two applications to see how they differ.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Slot(
                title = "First",
                packageName = state.first,
                modifier = Modifier.weight(1f),
                onClick = { onPickSide(CompareSide.FIRST) },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Slot(
                title = "Second",
                packageName = state.second,
                modifier = Modifier.weight(1f),
                onClick = { onPickSide(CompareSide.SECOND) },
            )
        }
        if (state.first != null && state.first == state.second) {
            Text(
                text = "Choose two different applications.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Slot(
    title: String,
    packageName: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = packageName ?: "Choose application",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (packageName == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MissingApp(onPickSide: (CompareSide) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            text = "One of these applications is no longer installed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onPickSide(CompareSide.SECOND) }) {
            Text("Choose another application")
        }
    }
}

@Composable
private fun ComparisonContent(
    table: ComparisonTable,
    onPickSide: (CompareSide) -> Unit,
    iconLoader: (String) -> ImageBitmap?,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        ComparisonHeader(table = table, onPickSide = onPickSide, iconLoader = iconLoader)

        Text(
            text = "✓ granted · ○ requested, not granted · ? not available · — not requested",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
        )

        SectionTitle(text = "Differences")
        if (!table.hasDifferences) {
            Text(
                text = "These two applications match on every compared field.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
        } else {
            table.facts.filter { it.differs }.forEach { fact ->
                ComparisonRow(label = fact.label, left = fact.left, right = fact.right, emphasize = true)
            }
        }

        SectionTitle(text = "All facts")
        table.facts.forEach { fact ->
            ComparisonRow(
                label = fact.label,
                left = fact.left,
                right = fact.right,
                emphasize = fact.differs,
            )
        }

        SectionTitle(text = "Permissions")
        var lastCategory: PermissionCategory? = null
        table.permissionRows.forEach { row ->
            if (row.category != lastCategory) {
                Text(
                    text = categoryLabel(row.category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
                lastCategory = row.category
            }
            ComparisonRow(
                label = row.label,
                left = row.left.symbol,
                right = row.right.symbol,
                emphasize = row.differs,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
private fun ComparisonHeader(
    table: ComparisonTable,
    onPickSide: (CompareSide) -> Unit,
    iconLoader: (String) -> ImageBitmap?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable { onPickSide(CompareSide.FIRST) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(
                packageName = table.left.app.packageName,
                iconLoader = iconLoader,
                size = 40.dp,
                fallbackText = table.left.app.label,
            )
            Text(
                text = table.left.app.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Change",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "vs",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f).clickable { onPickSide(CompareSide.SECOND) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(
                packageName = table.right.app.packageName,
                iconLoader = iconLoader,
                size = 40.dp,
                fallbackText = table.right.app.label,
            )
            Text(
                text = table.right.app.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Change",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ComparisonRow(label: String, left: String, right: String, emphasize: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = left,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.7f),
        )
        Text(
            text = right,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.7f),
        )
    }
}

private fun categoryLabel(category: PermissionCategory): String = when (category) {
    PermissionCategory.SENSITIVE -> "Sensitive"
    PermissionCategory.SPECIAL -> "Special access"
    PermissionCategory.NORMAL -> "Other"
    PermissionCategory.OTHER -> "System"
}

/** Application picker used for both slots; searches name and package like the list screen. */
@Composable
private fun AppPicker(
    analyses: List<AppAnalysis>,
    side: CompareSide,
    iconLoader: (String) -> ImageBitmap?,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(analyses, query) { AppListQuery.apply(analyses, query = query) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (side == CompareSide.FIRST) {
                "Choose the first application"
            } else {
                "Choose the second application"
            },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search by name or package") },
            singleLine = true,
        )

        if (results.isEmpty()) {
            Text(
                text = "No applications match your search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(results, key = { it.app.packageName }) { analysis ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(analysis.app.packageName) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(
                            packageName = analysis.app.packageName,
                            iconLoader = iconLoader,
                            size = 36.dp,
                            fallbackText = analysis.app.label,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = analysis.app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = analysis.app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }

        TextButton(onClick = onCancel, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text("Cancel")
        }
    }
}
