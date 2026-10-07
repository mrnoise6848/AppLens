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
import com.noise.applens.domain.model.AppTechnicalInfo
import com.noise.applens.ui.components.SectionTitle
import com.noise.applens.util.formatBytes

/**
 * Expandable technical metadata section (spec §10).
 *
 * Loaded on first expansion only: the extra `PackageManager` queries (signing certificates and
 * component counts) are never paid for applications the user does not open. Only the expanded
 * state survives process death; the payload is cheap to reload.
 */
@Composable
fun TechnicalSection(
    packageName: String,
    load: suspend (String) -> AppTechnicalInfo?,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(packageName) { mutableStateOf(false) }
    var info by remember(packageName) { mutableStateOf<AppTechnicalInfo?>(null) }
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
                text = "Technical information",
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
            if (info == null && !loading) {
                loading = true
                info = load(packageName)
                loading = false
            }
        }

        val technicalInfo = info
        when {
            loading -> Text(
                text = "Reading technical information…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            technicalInfo == null -> Text(
                text = "Technical information is not available on this Android version.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            else -> TechnicalRows(info = technicalInfo)
        }
    }
}

@Composable
private fun TechnicalRows(info: AppTechnicalInfo) {
    DetailRow(label = "Package", value = info.packageName, secondary = "versionCode ${info.versionCode}")
    DetailRow(label = "APK size", value = "${formatBytes(info.apkSizeBytes)} (base + splits)")

    DetailRow(
        label = "Base APK",
        value = info.baseApkPath ?: "Not available",
        secondary = if (info.splitApkPaths.isEmpty()) {
            "No split APKs"
        } else {
            "${info.splitApkPaths.size} split APK(s)"
        },
    )

    info.splitApkPaths.forEach { path ->
        DetailRow(label = "Split APK", value = path)
    }

    DetailRow(
        label = "ABIs",
        value = if (info.abis.isEmpty()) "Not available" else info.abis.joinToString(", "),
        secondary = if (info.abis.isEmpty()) "No native libraries reported" else null,
    )

    DetailRow(
        label = "Installed by",
        value = info.installerPackageName ?: "Not available on this Android version",
    )

    val signing = info.signing
    if (signing != null) {
        DetailRow(
            label = "Signers",
            value = if (signing.signerCount == 1) "1 signer" else "${signing.signerCount} signers",
            secondary = if (signing.hasMultipleSigners) "Multiple signers" else "Single signer",
        )
        signing.sha256.forEach { fingerprint ->
            DetailRow(label = "SHA-256", value = shorten(fingerprint))
        }
        if (signing.hasPastSigningCertificates) {
            DetailRow(label = "Certificate rotation", value = "Past certificates present")
        }
        DetailRow(label = "Signing scheme", value = "v${signing.schemeVersion}")
    }

    val components = info.components
    if (components != null) {
        DetailRow(
            label = "Components",
            value = components.total.toString(),
            secondary = "${components.activities} activities · ${components.services} services · " +
                "${components.receivers} receivers · ${components.providers} providers",
        )
    }

    DetailRow(
        label = "Flags",
        value = if (info.notableFlags.isEmpty()) "None" else info.notableFlags.joinToString(", "),
    )

    if (info.unavailable.isNotEmpty()) {
        Text(
            text = "Not available on this Android version: ${info.unavailable.joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
    }
}

/** `0123456789ABCDEF…ABCDEF` — enough to compare certificates without flooding the screen. */
private fun shorten(fingerprint: String): String =
    if (fingerprint.length <= 24) fingerprint else "${fingerprint.take(24)}…${fingerprint.takeLast(6)}"
