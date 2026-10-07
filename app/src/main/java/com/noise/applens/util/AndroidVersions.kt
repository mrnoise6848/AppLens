package com.noise.applens.util

/**
 * Maps API levels to their platform release names for display (spec §15).
 *
 * Unknown/future levels return `null` and are shown as `API nn` — AppLens never guesses a release
 * name it cannot know.
 */
private val RELEASE_NAMES: Map<Int, String> = mapOf(
    21 to "Android 5.0",
    22 to "Android 5.1",
    23 to "Android 6.0",
    24 to "Android 7.0",
    25 to "Android 7.1",
    26 to "Android 8.0",
    27 to "Android 8.1",
    28 to "Android 9",
    29 to "Android 10",
    30 to "Android 11",
    31 to "Android 12",
    32 to "Android 12L",
    33 to "Android 13",
    34 to "Android 14",
    35 to "Android 15",
    36 to "Android 16",
)

/** Release name for an API level, or `null` when the level is unknown. */
fun apiReleaseName(api: Int): String? = RELEASE_NAMES[api]

/** `Android 14 (API 34)`, or `API 37` for levels with no published release name. */
fun apiLabel(api: Int): String {
    val name = apiReleaseName(api) ?: return "API $api"
    return "$name (API $api)"
}

/** `Android 14` for a level, `Not available on this Android version` when unknown is not the case. */
fun apiShortName(api: Int?): String =
    if (api == null || api <= 0) "—" else (apiReleaseName(api) ?: "API $api")
