package com.noise.applens.state

import com.noise.applens.domain.analysis.AppFilter
import com.noise.applens.domain.analysis.AppSort

/**
 * State of the application list screen (filter, query, sort).
 *
 * Held in the ViewModel so a rotation does not throw away the user's search and scroll context.
 */
data class ListUiState(
    val filter: AppFilter = AppFilter.ALL,
    val query: String = "",
    val sort: AppSort = AppSort.BEST_MATCH,
    /** Set by an entry point that should place the cursor in the search field. */
    val focusSearch: Boolean = false,
) {
    /** `true` when the search box has text the user can clear. */
    val hasQuery: Boolean get() = query.isNotBlank()
}
