package com.noise.applens.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Application icon loaded on demand from the bounded [com.noise.applens.data.AppIconCache].
 *
 * Loading happens off the main thread and only for rows that are actually composed, so a 300+
 * application list never loads 300 icons at once (spec §23).
 */
@Composable
fun AppIcon(
    packageName: String,
    iconLoader: (String) -> ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    fallbackText: String = "",
) {
    var bitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(packageName) { mutableStateOf(false) }

    LaunchedEffect(packageName) {
        if (bitmap == null && !failed) {
            val loaded = withContext(Dispatchers.IO) { iconLoader(packageName) }
            if (loaded != null) bitmap = loaded else failed = true
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (fallbackText.isNotBlank()) {
            Text(
                text = fallbackText.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
