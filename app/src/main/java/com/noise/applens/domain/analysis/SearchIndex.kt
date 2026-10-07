package com.noise.applens.domain.analysis

import java.util.Locale

/**
 * Precomputed, normalized search index over the analysed inventory (spec §13).
 *
 * Built once per completed scan and reused for every keystroke, so search never re-normalizes the
 * inventory and stays instant with hundreds of applications. Search runs entirely over local data —
 * no `PackageManager` call is made while the user types.
 */
class SearchIndex private constructor(
    private val rows: List<Row>,
    private val all: List<AppAnalysis>,
) {

    private data class Row(
        val analysis: AppAnalysis,
        val label: String,
        val packageName: String,
    )

    val size: Int get() = all.size

    /** Whole inventory in analysis (label) order. */
    fun all(): List<AppAnalysis> = all

    /**
     * Ranked results for [query]: exact package, exact label, label prefix, package prefix,
     * word prefix, label substring, package substring — best first, label order as the tie-break.
     * Returns an empty list when nothing matches.
     */
    fun ranked(query: String): List<AppAnalysis> {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return all

        return rows.mapNotNull { row ->
            val score = scoreOf(row, needle)
            if (score < 0) null else Scored(score, row.analysis)
        }.sorted()
            .map { it.analysis }
    }

    private fun scoreOf(row: Row, needle: String): Int = when {
        row.packageName == needle -> SCORE_EXACT_PACKAGE
        row.label == needle -> SCORE_EXACT_LABEL
        row.label.startsWith(needle) -> SCORE_LABEL_PREFIX
        row.packageName.startsWith(needle) -> SCORE_PACKAGE_PREFIX
        row.label.split(*WORD_SEPARATORS).any { it.startsWith(needle) } -> SCORE_WORD_PREFIX
        row.label.contains(needle) -> SCORE_LABEL_CONTAINS
        row.packageName.contains(needle) -> SCORE_PACKAGE_CONTAINS
        else -> NO_MATCH
    }

    private data class Scored(val score: Int, val analysis: AppAnalysis) : Comparable<Scored> {
        override fun compareTo(other: Scored): Int {
            val byScore = other.score.compareTo(score)
            if (byScore != 0) return byScore
            return String.CASE_INSENSITIVE_ORDER.compare(analysis.app.label, other.analysis.app.label)
        }
    }

    companion object {
        private const val NO_MATCH = -1
        private const val SCORE_EXACT_PACKAGE = 100
        private const val SCORE_EXACT_LABEL = 95
        private const val SCORE_LABEL_PREFIX = 80
        private const val SCORE_PACKAGE_PREFIX = 70
        private const val SCORE_WORD_PREFIX = 60
        private const val SCORE_LABEL_CONTAINS = 40
        private const val SCORE_PACKAGE_CONTAINS = 30

        private val WORD_SEPARATORS = charArrayOf(' ', '-', '.', '_', '/')

        /** Builds the index. Called once per scan, off the UI thread. */
        fun build(analyses: List<AppAnalysis>): SearchIndex {
            val rows = ArrayList<Row>(analyses.size)
            analyses.forEach { analysis ->
                rows += Row(
                    analysis = analysis,
                    label = analysis.app.label.lowercase(Locale.ROOT),
                    packageName = analysis.app.packageName.lowercase(Locale.ROOT),
                )
            }
            return SearchIndex(rows = rows, all = analyses)
        }

        /** Empty index used before the first scan completes. */
        fun empty(): SearchIndex = SearchIndex(rows = emptyList(), all = emptyList())
    }
}
