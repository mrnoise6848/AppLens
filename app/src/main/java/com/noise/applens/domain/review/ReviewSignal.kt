package com.noise.applens.domain.review

/**
 * A single, evidence-based reason why an application deserves a manual review (spec §8, §13).
 *
 * Every signal is produced by a deterministic rule from observable application metadata — never by
 * a model, never by a guess (spec §4.3).
 */
data class ReviewSignal(
    val type: SignalType,
    /** Short sentence shown in lists, e.g. "Requests background location". */
    val title: String,
    /** Optional supporting evidence, e.g. "Targets API 29, this device runs API 36". */
    val detail: String? = null,
    val priority: SignalPriority,
    /** Contribution to the deterministic Review Score (spec §14). */
    val weight: Int,
)

/** How strongly a signal should drive the "needs review" decision. */
enum class SignalPriority { HIGH, MEDIUM, LOW }

/** Closed set of review reasons — keeps rules, UI and scoring in sync. */
enum class SignalType {
    BACKGROUND_LOCATION,
    PRECISE_LOCATION,
    APPROXIMATE_LOCATION,
    MICROPHONE,
    CAMERA,
    CONTACTS,
    PHONE,
    SMS,
    CALENDAR,
    BODY_SENSORS,
    NEARBY_DEVICES,
    MEDIA_ACCESS,
    SPECIAL_ACCESS,
    MANY_SENSITIVE_PERMISSIONS,
    OLD_TARGET_SDK,
    LARGE_APP,
    DEBUGGABLE,
    STALE_UPDATE,
}
