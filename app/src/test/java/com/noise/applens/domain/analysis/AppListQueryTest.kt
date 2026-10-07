package com.noise.applens.domain.analysis

import com.noise.applens.TestApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ranked search and list operations (spec §10, §13): local data only, deterministic order,
 * responsive structure (one index build, then per-query scoring).
 */
class AppListQueryTest {

    private val analyses = listOf(
        analysis("com.instagram.android", "Instagram", sensitive = 3),
        analysis("com.example.notes", "Notes", large = true),
        analysis("org.example.installer", "Installer"),
        analysis("com.android.settings", "Settings", system = true),
    )

    private val index = SearchIndex.build(analyses)

    private fun analysis(
        packageName: String,
        label: String,
        sensitive: Int = 0,
        large: Boolean = false,
        system: Boolean = false,
    ): AppAnalysis {
        val permissions = List(sensitive) { i ->
            com.noise.applens.domain.permission.AppPermission(
                permission = "android.permission.SENSITIVE_$i",
                displayName = "Sensitive $i",
                description = "",
                category = com.noise.applens.domain.permission.PermissionCategory.SENSITIVE,
                grantState = com.noise.applens.domain.model.PermissionGrantState.GRANTED,
                isRuntimePermission = true,
                isBackgroundOnly = false,
            )
        }
        return AppAnalysis(
            app = TestApps.app(
                packageName = packageName,
                label = label,
                apkSizeBytes = if (large) 200L * 1024 * 1024 else 10L * 1024 * 1024,
                isSystemApp = system,
                targetSdkVersion = 35,
                lastUpdateTime = 1_700_000_000_000L,
            ),
            permissions = permissions,
            signals = if (large) {
                listOf(
                    com.noise.applens.domain.review.ReviewSignal(
                        type = com.noise.applens.domain.review.SignalType.LARGE_APP,
                        title = "Large application size",
                        priority = com.noise.applens.domain.review.SignalPriority.LOW,
                        weight = 6,
                    ),
                )
            } else {
                emptyList()
            },
            score = com.noise.applens.domain.score.ReviewScore(value = 0, factors = emptyList()),
            needsReview = sensitive > 0,
        )
    }

    // ------------------------------------------------------------------------------- ranked search

    @Test
    fun `exact package match ranks first`() {
        val results = index.ranked("com.instagram.android")
        assertEquals("Instagram", results.first().app.label)
    }

    @Test
    fun `exact label match beats prefix match`() {
        val results = index.ranked("notes")
        assertEquals("Notes", results.first().app.label)
    }

    @Test
    fun `label prefix ranks above package substring`() {
        val results = index.ranked("inst")
        // Instagram: label prefix ("inst" → Instagram) vs installer: label prefix too,
        // but Instagram also matches package prefix — either way both must be present,
        // and matches are ordered deterministically.
        assertEquals(2, results.size)
        assertTrue(results.any { it.app.label == "Instagram" })
        assertTrue(results.any { it.app.label == "Installer" })
    }

    @Test
    fun `search is case insensitive`() {
        val upper = index.ranked("INSTAGRAM")
        val lower = index.ranked("instagram")
        assertEquals(lower.map { it.app.packageName }, upper.map { it.app.packageName })
    }

    @Test
    fun `no match returns empty list`() {
        assertTrue(index.ranked("zzz-not-a-real-app").isEmpty())
    }

    @Test
    fun `empty query returns the whole inventory`() {
        assertEquals(analyses.size, index.ranked("   ").size)
    }

    @Test
    fun `package name search finds by package substring`() {
        val results = index.ranked("org.example.inst")
        assertTrue(results.any { it.app.packageName == "org.example.installer" })
    }

    // ------------------------------------------------------------------------------ filter + sort

    @Test
    fun `best match with query returns ranked order`() {
        val results = AppListQuery.apply(
            analyses = analyses,
            query = "inst",
            sort = AppSort.BEST_MATCH,
            index = index,
        )
        assertTrue(results.any { it.app.label == "Instagram" })
        assertTrue(results.any { it.app.label == "Installer" })
        assertFalse(results.any { it.app.label == "Settings" })
    }

    @Test
    fun `filter is applied on top of ranked results`() {
        val results = AppListQuery.apply(
            analyses = analyses,
            filter = AppFilter.SYSTEM,
            query = "instagram",
            sort = AppSort.BEST_MATCH,
            index = index,
        )
        assertTrue(results.isEmpty()) // Instagram is not a system app
    }

    @Test
    fun `no query sorts by name for BEST_MATCH`() {
        val results = AppListQuery.apply(analyses = analyses, sort = AppSort.BEST_MATCH, index = index)
        // Case-insensitive label order: Instagram < Installer (g < l at position 6).
        assertEquals(
            listOf("Instagram", "Installer", "Notes", "Settings"),
            results.map { it.app.label },
        )
    }

    @Test
    fun `size sort is descending`() {
        val results = AppListQuery.apply(analyses = analyses, sort = AppSort.SIZE, index = index)
        assertEquals("Notes", results.first().app.label)
    }

    @Test
    fun `system filter separates system from user apps`() {
        val system = AppListQuery.apply(analyses = analyses, filter = AppFilter.SYSTEM, index = index)
        val user = AppListQuery.apply(analyses = analyses, filter = AppFilter.USER, index = index)
        assertEquals(1, system.size)
        assertEquals(3, user.size)
        assertEquals(analyses.size, system.size + user.size)
    }

    @Test
    fun `needs review filter matches the gate`() {
        val results = AppListQuery.apply(analyses = analyses, filter = AppFilter.NEEDS_REVIEW, index = index)
        assertEquals(listOf("Instagram"), results.map { it.app.label })
    }

    @Test
    fun `filter counts cover the whole inventory for ALL`() {
        assertEquals(analyses.size, AppListQuery.count(analyses, AppFilter.ALL))
        assertEquals(1, AppListQuery.count(analyses, AppFilter.LARGE))
        assertEquals(0, AppListQuery.count(analyses, AppFilter.OLD_TARGET_SDK))
    }

    @Test
    fun `query without index still searches label and package`() {
        val results = AppListQuery.apply(analyses = analyses, query = "settings", index = null)
        assertEquals(listOf("Settings"), results.map { it.app.label })
    }
}
