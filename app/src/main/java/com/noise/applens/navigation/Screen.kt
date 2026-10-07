package com.noise.applens.navigation

/** Predefined list filters (spec §10). */
enum class AppFilter(val label: String) {
    ALL("All"),
    NEEDS_REVIEW("Needs review"),
    SENSITIVE("Sensitive permissions"),
    LARGE("Large apps"),
    OLD_TARGET_SDK("Old target SDK"),
    RECENTLY_UPDATED("Recently updated"),
    SYSTEM("System apps"),
    USER("User apps");

    /** `true` when this filter is the default "All" entry. */
    val isDefault: Boolean get() = this == ALL
}

/** Screens of the application, with their arguments. */
sealed interface Screen {
    data object Dashboard : Screen

    data class AppList(
        val filter: AppFilter = AppFilter.ALL,
        val query: String = "",
    ) : Screen

    data class AppDetail(val packageName: String) : Screen

    /** Starts from [first] when a specific application is the entry point. */
    data class Compare(val first: String? = null, val second: String? = null) : Screen
}
