package com.noise.applens.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noise.applens.domain.model.LibraryConfidence
import com.noise.applens.domain.model.LibraryInfo
import com.noise.applens.ui.components.SectionTitle

/**
 * Library / SDK overview (spec §17).
 *
 * Compact by default: name plus confidence, with the evidence behind the same disclosure used for
 * technical metadata. Detected, Likely and Unknown are always shown as words, never colour alone.
 */
@Composable
fun LibrariesSection(
    packageName: String,
    load: suspend (String) -> List<LibraryInfo>,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(packageName) { mutableStateOf(false) }
    var libraries by remember(packageName) { mutableStateOf<List<LibraryInfo>?>(null) }
    var loading by remember(packageName) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(
                text = "Libraries / SDKs",
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expanded) "Hide" else "Show",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (!expanded) return@Column

        LaunchedEffect(packageName) {
            if (libraries == null && !loading) {
                loading = true
                libraries = load(packageName)
                loading = false
            }
        }

        val items = libraries
        when {
            loading -> Text(
                text = "Reading package metadata…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            items.isNullOrEmpty() -> Text(
                text = "No third-party libraries could be identified from package metadata.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            else -> {
                items.forEach { library -> LibraryRow(library = library) }
                Text(
                    text = "Detected from manifest and package metadata only. AppLens does not " +
                        "decompile APKs, so libraries without a manifest footprint cannot be listed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryRow(library: LibraryInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = library.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = library.confidence.label,
                style = MaterialTheme.typography.labelSmall,
                color = when (library.confidence) {
                    LibraryConfidence.DETECTED -> MaterialTheme.colorScheme.primary
                    LibraryConfidence.LIKELY -> MaterialTheme.colorScheme.tertiary
                    LibraryConfidence.UNKNOWN -> MaterialTheme.colorScheme.outline
                },
            )
        }
        library.evidence.forEach { evidence ->
            Text(
                text = evidence,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 17.dp),
            )
        }
    }
}
