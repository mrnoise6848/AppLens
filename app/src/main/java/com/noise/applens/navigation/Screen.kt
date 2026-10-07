package com.noise.applens.navigation

import com.noise.applens.domain.analysis.AppFilter

/** Screens of the application, with their arguments. */
sealed interface Screen {
    data object Dashboard : Screen

    data class AppList(
        val filter: AppFilter = AppFilter.ALL,
    ) : Screen

    data class AppDetail(val packageName: String) : Screen

    /** Starts from [first] when a specific application is the entry point. */
    data class Compare(val first: String? = null, val second: String? = null) : Screen
}
