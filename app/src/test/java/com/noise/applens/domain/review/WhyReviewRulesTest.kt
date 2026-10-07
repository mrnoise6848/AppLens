package com.noise.applens.domain.review

import com.noise.applens.TestApps
import com.noise.applens.domain.permission.AppPermission
import com.noise.applens.domain.permission.PermissionCategory
import com.noise.applens.domain.model.PermissionGrantState
import com.noise.applens.domain.score.ReviewScoreCalculator
import com.noise.applens.domain.score.ScoreBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Determinism and explainability of the "why review" rules and the score (spec §13, §14).
 */
class WhyReviewRulesTest {

    private val now = 1_700_000_000_000L
    private val rules = WhyReviewRules(deviceApi = 35, nowMillis = { now })

    private fun permissions(vararg names: String): List<AppPermission> = names.map {
        AppPermission(
            permission = it,
            displayName = it,
            description = "",
            category = PermissionCategory.SENSITIVE,
            grantState = PermissionGrantState.GRANTED,
            isRuntimePermission = true,
            isBackgroundOnly = false,
        )
    }

    @Test
    fun `identical metadata produces identical signals`() {
        val app = TestApps.app(
            requestedPermissions = listOf("android.permission.CAMERA"),
            lastUpdateTime = now - 1000L,
        )
        val first = rules.evaluate(app, permissions("android.permission.CAMERA"))
        val second = rules.evaluate(app, permissions("android.permission.CAMERA"))
        assertEquals(first, second)
    }

    @Test
    fun `background location wins over precise and approximate`() {
        val app = TestApps.app(
            requestedPermissions = listOf(
                "android.permission.ACCESS_BACKGROUND_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
            ),
        )
        val signals = rules.evaluate(app, permissions())
        assertEquals(1, signals.size)
        assertEquals(SignalType.BACKGROUND_LOCATION, signals[0].type)
    }

    @Test
    fun `precise location wins over approximate`() {
        val app = TestApps.app(
            requestedPermissions = listOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
            ),
        )
        val signals = rules.evaluate(app, permissions())
        assertEquals(1, signals.size)
        assertEquals(SignalType.PRECISE_LOCATION, signals[0].type)
    }

    @Test
    fun `high priority signal alone needs review`() {
        val signals = listOf(
            ReviewSignal(SignalType.MICROPHONE, "mic", priority = SignalPriority.HIGH, weight = 14),
        )
        assertTrue(rules.needsReview(signals))
    }

    @Test
    fun `two medium signals need review`() {
        val signals = listOf(
            ReviewSignal(SignalType.CAMERA, "cam", priority = SignalPriority.MEDIUM, weight = 10),
            ReviewSignal(SignalType.CONTACTS, "contacts", priority = SignalPriority.MEDIUM, weight = 8),
        )
        assertTrue(rules.needsReview(signals))
    }

    @Test
    fun `single medium signal does not need review`() {
        val signals = listOf(
            ReviewSignal(SignalType.CAMERA, "cam", priority = SignalPriority.MEDIUM, weight = 10),
        )
        assertFalse(rules.needsReview(signals))
    }

    @Test
    fun `old target sdk rule uses device api minus lag`() {
        // deviceApi 35, lag 2 → anything below API 33 triggers.
        val old = rules.evaluate(TestApps.app(targetSdkVersion = 32), permissions())
        val current = rules.evaluate(TestApps.app(targetSdkVersion = 33), permissions())
        assertTrue(old.any { it.type == SignalType.OLD_TARGET_SDK })
        assertFalse(current.any { it.type == SignalType.OLD_TARGET_SDK })
    }

    @Test
    fun `large app rule uses the documented 150 MB threshold`() {
        val justUnder = rules.evaluate(TestApps.app(apkSizeBytes = WhyReviewRules.LARGE_APP_BYTES - 1), permissions())
        val atLimit = rules.evaluate(TestApps.app(apkSizeBytes = WhyReviewRules.LARGE_APP_BYTES), permissions())
        assertFalse(justUnder.any { it.type == SignalType.LARGE_APP })
        assertTrue(atLimit.any { it.type == SignalType.LARGE_APP })
    }

    @Test
    fun `unknown size does not raise the large-app signal`() {
        val signals = rules.evaluate(TestApps.app(apkSizeBytes = null), permissions())
        assertFalse(signals.any { it.type == SignalType.LARGE_APP })
    }

    @Test
    fun `score equals the sum of factor weights and is capped at 100`() {
        val signals = listOf(
            ReviewSignal(SignalType.BACKGROUND_LOCATION, "bg", priority = SignalPriority.HIGH, weight = 60),
            ReviewSignal(SignalType.DEBUGGABLE, "dbg", priority = SignalPriority.HIGH, weight = 60),
        )
        val score = ReviewScoreCalculator.calculate(signals)
        assertEquals(100, score.value)
        assertEquals(2, score.factors.size)
        assertTrue(score.factors.all { it.delta > 0 })
    }

    @Test
    fun `every factor contributes exactly its weight`() {
        val signals = listOf(
            ReviewSignal(SignalType.CAMERA, "cam", priority = SignalPriority.MEDIUM, weight = 10),
            ReviewSignal(SignalType.LARGE_APP, "big", priority = SignalPriority.LOW, weight = 6),
        )
        val score = ReviewScoreCalculator.calculate(signals)
        assertEquals(16, score.value)
        assertEquals(16, score.factors.sumOf { it.delta })
    }

    @Test
    fun `score bands follow the documented thresholds`() {
        assertEquals(ScoreBand.FEW, ScoreBand.from(0))
        assertEquals(ScoreBand.FEW, ScoreBand.from(19))
        assertEquals(ScoreBand.SOME, ScoreBand.from(20))
        assertEquals(ScoreBand.SOME, ScoreBand.from(49))
        assertEquals(ScoreBand.WORTH, ScoreBand.from(50))
        assertEquals(ScoreBand.WORTH, ScoreBand.from(74))
        assertEquals(ScoreBand.HIGH, ScoreBand.from(75))
        assertEquals(ScoreBand.HIGH, ScoreBand.from(100))
    }

    @Test
    fun `signals are ordered by priority then weight`() {
        val app = TestApps.app(
            requestedPermissions = listOf(
                "android.permission.RECORD_AUDIO",
                "android.permission.CAMERA",
                "android.permission.READ_CALENDAR",
            ),
            lastUpdateTime = now - 1000L,
        )
        val signals = rules.evaluate(app, permissions())
        val priorities = signals.map { it.priority.ordinal }
        assertEquals(priorities.sorted(), priorities)
    }
}
