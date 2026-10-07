package com.noise.applens.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noise.applens.domain.analysis.AppAnalysis
import com.noise.applens.domain.analysis.AppFilter
import com.noise.applens.domain.analysis.AppListQuery
import com.noise.applens.domain.analysis.AppSort
import com.noise.applens.state.ListUiState
import com.noise.applens.ui.components.AppIcon
import com.noise.applens.ui.components.ScreenHeader
import com.noise.applens.util.formatBytes
import com.noise.applens.util.formatRelativeTime
import com.noise.applens.util.pluralize

/**
 * Searchable, filterable and sortable application list (spec §10, §13).
 *
 * Filtering runs on the in-memory index, so it stays instant with hundreds of applications, and
 * icons are loaded lazily per visible row.
 */
@Composable
fun AppListScreen(
    analyses: List<AppAnalysis>,
    listState: ListUiState,
    iconLoader: (String) -> ImageBitmap?,
    onBack: () -> Unit,
    onFilterChange: (AppFilter) -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (AppSort) -> Unit,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val results = remember(analyses, listState) {
        AppListQuery.apply(
            analyses = analyses,
            filter = listState.filter,
            query = listState.query,
            sort = listState.sort,
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Applications",
            subtitle = if (results.size == analyses.size) {
                pluralize(analyses.size, "installed application")
            } else {
                "${results.size} of ${analyses.size} applications"
            },
            onBack = onBack,
        )

        OutlinedTextField(
            value = listState.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search by name or package") },
            singleLine = true,
        )

        FilterChipRow(
            selected = listState.filter,
            analyses = analyses,
            onFilterChange = onFilterChange,
            modifier = Modifier.padding(top = 10.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (listState.hasQuery) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text("Clear search")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            SortMenu(sort = listState.sort, onSortChange = onSortChange)
        }

        if (results.isEmpty()) {
            EmptyResults(hasQuery = listState.hasQuery, onClear = { onQueryChange("") })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(results, key = { it.app.packageName }) { analysis ->
                    AppRow(analysis = analysis, onClick = { onOpenApp(analysis.app.packageName) }, iconLoader = iconLoader)
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    selected: AppFilter,
    analyses: List<AppAnalysis>,
    onFilterChange: (AppFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppFilter.entries.forEach { filter ->
            val count = remember(analyses, filter) { AppListQuery.count(analyses, filter) }
            AppChip(
                label = "${filter.label} ($count)",
                selected = filter == selected,
                onClick = { onFilterChange(filter) },
            )
        }
    }
}

/** Pill-shaped filter control; kept dependency-free because the project has no icon set. */
@Composable
private fun AppChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun SortMenu(sort: AppSort, onSortChange: (AppSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Sort: ${sort.label}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (option == sort) FontWeight.Medium else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyResults(hasQuery: Boolean, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
    ) {
        Text(
            text = if (hasQuery) "No applications match your search." else "No applications in this filter.",
            style = MaterialTheme.typography.titleMedium,
        )
        if (hasQuery) {
            TextButton(onClick = onClear, modifier = Modifier.padding(top = 4.dp)) {
                Text("Clear search")
            }
        }
    }
}

@Composable
private fun AppRow(
    analysis: AppAnalysis,
    onClick: () -> Unit,
    iconLoader: (String) -> ImageBitmap?,
) {
    val app = analysis.app
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            packageName = app.packageName,
            iconLoader = iconLoader,
            fallbackText = app.label,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = formatBytes(app.apkSizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = rowMetadata(analysis),
                style = MaterialTheme.typography.bodySmall,
                color = if (analysis.needsReview) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "Updated 3 days ago · 3 sensitive permissions", prefixed with "Needs review" when relevant. */
private fun rowMetadata(analysis: AppAnalysis): String {
    val segments = mutableListOf<String>()
    if (analysis.needsReview) segments += "Needs review"
    segments += "Updated ${formatRelativeTime(analysis.app.lastUpdateTime)}"
    if (analysis.sensitiveCount > 0) {
        segments += pluralize(analysis.sensitiveCount, "sensitive permission")
    }
    return segments.joinToString(" · ")
}
