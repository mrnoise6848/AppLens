package com.noise.applens.domain.analysis

/**
 * Predefined list filters (spec §10) and sort orders for the application list.
 *
 * Labels are part of the domain because the same strings are reused for chips, dashboard counts and
 * accessibility descriptions.
 */
enum class AppFilter(val label: String) {
    ALL("All"),
    NEEDS_REVIEW("Needs review"),
    SENSITIVE("Sensitive permissions"),
    LARGE("Large apps"),
    OLD_TARGET_SDK("Old target SDK"),
    RECENTLY_UPDATED("Recently updated"),
    SYSTEM("System apps"),
    USER("User apps");

    val isDefault: Boolean get() = this == ALL
}

/** Sort orders offered on the application list (spec §10). */
enum class AppSort(val label: String) {
    NAME("Name (A–Z)"),
    SIZE("Largest size"),
    RECENTLY_UPDATED("Recently updated"),
    SENSITIVE("Most sensitive permissions"),
    OLDEST_TARGET("Oldest target SDK");
}
