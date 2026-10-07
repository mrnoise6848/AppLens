package com.noise.applens.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noise.applens.domain.analysis.AppAnalysis
import com.noise.applens.domain.score.ReviewScoreCalculator
import com.noise.applens.ui.components.SectionTitle
import com.noise.applens.ui.components.ScoreBadge
import com.noise.applens.util.pluralize

/**
 * "Why should I review this app?" plus the Review Score breakdown (spec §8, §13, §14).
 *
 * Every bullet is a deterministic rule output, and every score point is listed as a factor, so the
 * user can always see *why* the application was flagged. No invented security claims are made.
 */
@Composable
fun WhyReviewSection(analysis: AppAnalysis, modifier: Modifier = Modifier) {
    val signals = analysis.signals

    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = "Why review this app?")

        if (signals.isEmpty()) {
            Text(
                text = "No review signals found. This application requests nothing AppLens " +
                    "flags for a manual look.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
            return
        }

        ScoreHeader(analysis = analysis)

        signals.forEach { signal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        text = signal.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (signal.detail != null) {
                        Text(
                            text = signal.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "+${signal.weight}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Text(
            text = "${pluralize(signals.size, "signal")} · score is the sum of these factors",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun ScoreHeader(analysis: AppAnalysis) {
    val score = analysis.score

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Review Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = score.band.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (analysis.needsReview) {
                Text(
                    text = "Included in your “needs review” list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        ScoreBadge(value = score.value, max = ReviewScoreCalculator.MAX_SCORE)
    }
}
