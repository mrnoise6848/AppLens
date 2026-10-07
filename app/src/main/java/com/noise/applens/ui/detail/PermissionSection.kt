package com.noise.applens.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noise.applens.domain.analysis.AppAnalysis
import com.noise.applens.domain.model.PermissionGrantState
import com.noise.applens.domain.permission.AppPermission
import com.noise.applens.domain.permission.PermissionCategory
import com.noise.applens.ui.components.SectionTitle
import com.noise.applens.util.pluralize

/**
 * Permission intelligence section (spec §11, §12).
 *
 * Permissions are presented in the groups AppLens defines (sensitive / special access / normal /
 * other), each row carries Android's own grant state as text rather than colour alone, and every
 * grouping is derived from Android's protection level — nothing is labelled "dangerous" for effect.
 */
@Composable
fun PermissionSection(analysis: AppAnalysis, modifier: Modifier = Modifier) {
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

    SectionTitle(text = "Permissions · ${pluralize(permissions.size, "requested permission")}")

    Text(
        text = permissionSummary(analysis),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 6.dp),
    )

    Column(modifier = modifier) {
        PermissionCategory.entries.forEach { category ->
            val group = permissions.filter { it.category == category }
            if (group.isNotEmpty()) {
                PermissionGroup(
                    category = category,
                    permissions = group,
                    title = groupTitle(category, group.size),
                    description = groupDescription(category),
                )
            }
        }
    }
}

private fun permissionSummary(analysis: AppAnalysis): String {
    val sensitive = analysis.sensitiveCount
    val special = analysis.specialPermissions.size
    val normal = analysis.permissions.count { it.category == PermissionCategory.NORMAL }
    val other = analysis.permissions.count { it.category == PermissionCategory.OTHER }

    return buildList {
        if (sensitive > 0) add(pluralize(sensitive, "sensitive permission"))
        if (special > 0) add(pluralize(special, "special access"))
        if (normal > 0) add(pluralize(normal, "other permission", "other permissions"))
        if (other > 0) add(pluralize(other, "system permission", "system permissions"))
    }.joinToString(" · ")
}

private fun groupTitle(category: PermissionCategory, count: Int): String = when (category) {
    PermissionCategory.SENSITIVE -> "Sensitive · $count"
    PermissionCategory.SPECIAL -> "Special access · $count"
    PermissionCategory.NORMAL -> "Other permissions · $count"
    PermissionCategory.OTHER -> "System permissions · $count"
}

private fun groupDescription(category: PermissionCategory): String = when (category) {
    PermissionCategory.SENSITIVE ->
        "Access to personal data or device sensors. Android asks the user before granting these."
    PermissionCategory.SPECIAL ->
        "Granted through a dedicated Android settings screen, not through a permission dialog."
    PermissionCategory.NORMAL ->
        "Granted automatically when the application is installed."
    PermissionCategory.OTHER ->
        "Signature or system level permissions; not chosen by the user."
}

@Composable
private fun PermissionGroup(
    category: PermissionCategory,
    permissions: List<AppPermission>,
    title: String,
    description: String,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = when (category) {
                PermissionCategory.SENSITIVE -> MaterialTheme.colorScheme.primary
                PermissionCategory.SPECIAL -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp),
        )
        permissions.forEach { permission -> PermissionRow(permission = permission) }
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
                    .background(color = grantColor(permission.grantState)),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = permission.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = grantLabel(permission),
                style = MaterialTheme.typography.labelSmall,
                color = when (permission.grantState) {
                    PermissionGrantState.GRANTED -> MaterialTheme.colorScheme.primary
                    PermissionGrantState.DENIED -> MaterialTheme.colorScheme.outline
                    PermissionGrantState.UNKNOWN -> MaterialTheme.colorScheme.tertiary
                },
            )
        }

        if (permission.isBackgroundOnly) {
            Text(
                text = "Applies in the background",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = 17.dp),
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

/** Colour is never the only signal — [grantLabel] always spells the state out (spec §26). */
private fun grantLabel(permission: AppPermission): String = when {
    permission.category == PermissionCategory.SPECIAL -> "Android settings"
    permission.grantState == PermissionGrantState.GRANTED -> "Granted"
    permission.grantState == PermissionGrantState.DENIED -> "Not granted"
    else -> "Not available on this Android version"
}

@Composable
private fun grantColor(state: PermissionGrantState): androidx.compose.ui.graphics.Color =
    when (state) {
        PermissionGrantState.GRANTED -> MaterialTheme.colorScheme.primary
        PermissionGrantState.DENIED -> MaterialTheme.colorScheme.outline
        PermissionGrantState.UNKNOWN -> MaterialTheme.colorScheme.tertiary
    }
