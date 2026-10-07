package com.noise.applens.state

/**
 * Which slot the comparison screen is currently choosing an application for.
 */
enum class CompareSide { FIRST, SECOND }

/**
 * State of the comparison screen (spec §18).
 *
 * Held in the ViewModel so the selected pair and the open picker survive rotation.
 */
data class CompareUiState(
    val first: String? = null,
    val second: String? = null,
    /** Non-null while the user is picking an application for that slot. */
    val selecting: CompareSide? = null,
) {
    /** Both applications chosen → the table can be rendered. */
    val isComplete: Boolean get() = first != null && second != null && first != second

    /** `true` when a picker should be shown instead of the table. */
    val isPicking: Boolean get() = selecting != null
}
