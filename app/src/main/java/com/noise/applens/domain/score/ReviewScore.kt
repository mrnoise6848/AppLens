package com.noise.applens.domain.score

import com.noise.applens.domain.review.ReviewSignal
import com.noise.applens.domain.review.SignalType

/**
 * Deterministic, explainable "how much does this application deserve manual review" score
 * (spec §14).
 *
 * This is **not** a security score. It is the weighted sum of the observable review signals, capped
 * at 100, and every point is traceable to a factor shown in the UI. There is no randomness, no
 * learned model and no hidden input: identical metadata always yields the identical score.
 */
data class ReviewScore(
    /** `0..100`. */
    val value: Int,
    /** Contributing factors, largest contribution first. */
    val factors: List<ScoreFactor>,
) {
    val band: ScoreBand get() = ScoreBand.from(value)
}

/** One line of the score breakdown. */
data class ScoreFactor(
    val type: SignalType,
    /** Human readable factor label, e.g. "Background location". */
    val label: String,
    /** Points contributed, always positive. */
    val delta: Int,
    /** Optional supporting evidence from the signal. */
    val detail: String? = null,
)

/** Plain-language bands, deliberately phrased as review effort — never as a verdict. */
enum class ScoreBand(val label: String) {
    FEW("Few reasons to review"),
    SOME("A few reasons to review"),
    WORTH("Worth a closer look"),
    HIGH("Worth a careful review");

    companion object {
        fun from(value: Int): ScoreBand = when {
            value < BAND_SOME -> FEW
            value < BAND_WORTH -> SOME
            value < BAND_HIGH -> WORTH
            else -> HIGH
        }

        const val BAND_SOME = 20
        const val BAND_WORTH = 50
        const val BAND_HIGH = 75
    }
}

object ReviewScoreCalculator {

    /** Hard cap so the displayed value is always a valid `0..100` number (spec §14). */
    const val MAX_SCORE = 100

    fun calculate(signals: List<ReviewSignal>): ReviewScore {
        val factors = signals
            .map { signal ->
                ScoreFactor(
                    type = signal.type,
                    label = signal.title,
                    delta = signal.weight,
                    detail = signal.detail,
                )
            }
            .sortedWith(compareByDescending<ScoreFactor> { it.delta }.thenBy { it.label })

        val total = factors.sumOf { it.delta }.coerceAtMost(MAX_SCORE)
        return ReviewScore(value = total, factors = factors)
    }
}
