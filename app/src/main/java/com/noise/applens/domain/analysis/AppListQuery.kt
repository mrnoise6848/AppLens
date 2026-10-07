package com.noise.applens.domain.analysis

/**
 * Pure list operations over the analysed inventory (spec §10, §13).
 *
 * Stateless and side-effect free so the same call always produces the same list — filtering,
 * searching and sorting never touch `PackageManager` and never block the UI thread.
 */
object AppListQuery {

    /**
     * Applications matching [filter], then [query], ordered by [sort].
     *
     * With [AppSort.BEST_MATCH] (the default) and a non-empty [query], results come from the
     * precomputed [index] in relevance order; every other sort re-orders the matched set.
     */
    fun apply(
        analyses: List<AppAnalysis>,
        filter: AppFilter = AppFilter.ALL,
        query: String = "",
        sort: AppSort = AppSort.BEST_MATCH,
        index: SearchIndex? = null,
    ): List<AppAnalysis> {
        val needle = query.trim()
        val bestMatch = sort == AppSort.BEST_MATCH

        if (needle.isNotEmpty() && bestMatch && index != null) {
            return filter.filterBy(index.ranked(needle))
        }

        val searched = if (needle.isEmpty()) analyses else analyses.search(needle)
        return sort.sort(filter.filterBy(searched))
    }

    /** Number of applications a filter would show — used for the chip counters. */
    fun count(analyses: List<AppAnalysis>, filter: AppFilter): Int =
        analyses.count { filter.matches(it) }

    private fun AppFilter.matches(analysis: AppAnalysis): Boolean = when (this) {
        AppFilter.ALL -> true
        AppFilter.NEEDS_REVIEW -> analysis.needsReview
        AppFilter.SENSITIVE -> analysis.sensitiveCount > 0
        AppFilter.LARGE -> analysis.isLarge
        AppFilter.OLD_TARGET_SDK -> analysis.isOldTargetSdk
        AppFilter.RECENTLY_UPDATED -> analysis.app.lastUpdateTime >= thirtyDaysAgo()
        AppFilter.SYSTEM -> analysis.app.isSystemApp
        AppFilter.USER -> !analysis.app.isSystemApp
    }

    private fun AppFilter.filterBy(analyses: List<AppAnalysis>): List<AppAnalysis> =
        if (this == AppFilter.ALL) analyses else analyses.filter { matches(it) }

    private fun List<AppAnalysis>.search(query: String): List<AppAnalysis> {
        if (query.isEmpty()) return this
        return filter { analysis ->
            analysis.app.label.contains(query, ignoreCase = true) ||
                analysis.app.packageName.contains(query, ignoreCase = true)
        }
    }

    private fun AppSort.sort(analyses: List<AppAnalysis>): List<AppAnalysis> = when (this) {
        // Best match with no query is simply name order.
        AppSort.BEST_MATCH, AppSort.NAME -> analyses.sortedWith(labelComparator)
        AppSort.SIZE -> analyses.sortedWith(
            compareByDescending<AppAnalysis> { it.app.sizeSortKey }.then(labelComparator)
        )
        AppSort.RECENTLY_UPDATED -> analyses.sortedWith(
            compareByDescending<AppAnalysis> { it.app.lastUpdateTime }.then(labelComparator)
        )
        AppSort.SENSITIVE -> analyses.sortedWith(
            compareByDescending<AppAnalysis> { it.sensitiveCount }.then(labelComparator)
        )
        AppSort.OLDEST_TARGET -> analyses.sortedWith(
            compareBy<AppAnalysis> { it.app.targetSdkVersion }.then(labelComparator)
        )
    }

    private val labelComparator: Comparator<AppAnalysis> =
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.app.label }

    /** Reference point for the "recently updated" filter. */
    private fun thirtyDaysAgo(nowMillis: Long = System.currentTimeMillis()): Long =
        nowMillis - 30L * 24 * 60 * 60 * 1000
}
