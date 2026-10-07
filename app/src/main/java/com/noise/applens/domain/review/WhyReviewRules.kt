package com.noise.applens.domain.review

import com.noise.applens.domain.model.InstalledApp
import com.noise.applens.domain.permission.AppPermission
import com.noise.applens.domain.permission.PermissionCatalog
import com.noise.applens.domain.permission.PermissionCategory
import com.noise.applens.util.formatBytes
import java.util.concurrent.TimeUnit

/**
 * Deterministic "Why should I review this app?" rules (spec §13).
 *
 * Each rule maps observable metadata to a [ReviewSignal]. The output is stable for identical input,
 * so the same application always produces the same explanations and the same score.
 *
 * @property deviceApi Android API level of the device, used for the relative target SDK rule.
 */
class WhyReviewRules(
    private val deviceApi: Int,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    fun evaluate(app: InstalledApp, permissions: List<AppPermission>): List<ReviewSignal> {
        val requested = app.requestedPermissions.toSet()
        val signals = mutableListOf<ReviewSignal>()

        // --- Location: one signal only, strongest form wins -------------------------------
        when {
            Permission.LOCATION_BACKGROUND in requested -> signals += ReviewSignal(
                type = SignalType.BACKGROUND_LOCATION,
                title = "Requests background location",
                detail = "Can read location while the app is not in use",
                priority = SignalPriority.HIGH,
                weight = 20,
            )
            Permission.FINE_LOCATION in requested -> signals += ReviewSignal(
                type = SignalType.PRECISE_LOCATION,
                title = "Requests precise location",
                priority = SignalPriority.MEDIUM,
                weight = 10,
            )
            Permission.COARSE_LOCATION in requested -> signals += ReviewSignal(
                type = SignalType.APPROXIMATE_LOCATION,
                title = "Requests approximate location",
                priority = SignalPriority.LOW,
                weight = 4,
            )
        }

        // --- Sensors ---------------------------------------------------------------------
        if (Permission.RECORD_AUDIO in requested) signals += ReviewSignal(
            type = SignalType.MICROPHONE,
            title = "Requests microphone access",
            priority = SignalPriority.HIGH,
            weight = 14,
        )

        if (Permission.CAMERA in requested) signals += ReviewSignal(
            type = SignalType.CAMERA,
            title = "Requests camera access",
            priority = SignalPriority.MEDIUM,
            weight = 10,
        )

        // --- Personal data ---------------------------------------------------------------
        groupSignal(
            requested = requested,
            group = Permission.GROUP_CONTACTS,
            type = SignalType.CONTACTS,
            title = "Requests contacts access",
            priority = SignalPriority.MEDIUM,
            weight = 8,
            signals = signals,
        )
        groupSignal(
            requested = requested,
            group = Permission.GROUP_PHONE,
            type = SignalType.PHONE,
            title = "Requests phone access",
            priority = SignalPriority.MEDIUM,
            weight = 8,
            signals = signals,
        )
        groupSignal(
            requested = requested,
            group = Permission.GROUP_SMS,
            type = SignalType.SMS,
            title = "Requests SMS access",
            priority = SignalPriority.HIGH,
            weight = 12,
            signals = signals,
        )
        groupSignal(
            requested = requested,
            group = Permission.GROUP_CALENDAR,
            type = SignalType.CALENDAR,
            title = "Requests calendar access",
            priority = SignalPriority.LOW,
            weight = 5,
            signals = signals,
        )
        groupSignal(
            requested = requested,
            group = Permission.GROUP_BODY_SENSORS,
            type = SignalType.BODY_SENSORS,
            title = "Requests body sensor data",
            priority = SignalPriority.MEDIUM,
            weight = 6,
            signals = signals,
        )
        groupSignal(
            requested = requested,
            group = Permission.GROUP_NEARBY,
            type = SignalType.NEARBY_DEVICES,
            title = "Requests access to nearby devices",
            priority = SignalPriority.MEDIUM,
            weight = 6,
            signals = signals,
        )
        groupSignal(
            requested = requested,
            group = Permission.GROUP_MEDIA,
            type = SignalType.MEDIA_ACCESS,
            title = "Requests access to photos, videos or files",
            priority = SignalPriority.MEDIUM,
            weight = 7,
            signals = signals,
        )

        // --- Special access ---------------------------------------------------------------
        val specialAccess = requested.filter { it in PermissionCatalog.SPECIAL_ACCESS }
        if (specialAccess.isNotEmpty()) signals += ReviewSignal(
            type = SignalType.SPECIAL_ACCESS,
            title = "Holds special access",
            detail = specialAccess.joinToString(", ") { PermissionCatalog.displayName(it) },
            priority = SignalPriority.MEDIUM,
            weight = 8,
        )

        // --- Volume of sensitive access ----------------------------------------------------
        val sensitiveCount = permissions.count { it.category == PermissionCategory.SENSITIVE }
        if (sensitiveCount >= MANY_SENSITIVE_THRESHOLD) signals += ReviewSignal(
            type = SignalType.MANY_SENSITIVE_PERMISSIONS,
            title = "Requests many sensitive permissions",
            detail = "$sensitiveCount sensitive permissions in total",
            priority = SignalPriority.MEDIUM,
            weight = 8,
        )

        // --- Platform / lifecycle ----------------------------------------------------------
        val oldTargetSdk = deviceApi - TARGET_SDK_LAG
        if (app.targetSdkVersion < oldTargetSdk) signals += ReviewSignal(
            type = SignalType.OLD_TARGET_SDK,
            title = "Targets an older Android version",
            detail = "Targets API ${app.targetSdkVersion}, this device runs API $deviceApi",
            priority = SignalPriority.MEDIUM,
            weight = 10,
        )

        val size = app.apkSizeBytes
        if (size != null && size >= LARGE_APP_BYTES) signals += ReviewSignal(
            type = SignalType.LARGE_APP,
            title = "Large application size",
            detail = "APK size ${formatBytes(size)}",
            priority = SignalPriority.LOW,
            weight = 6,
        )

        if (app.debuggable) signals += ReviewSignal(
            type = SignalType.DEBUGGABLE,
            title = "Built as a debuggable application",
            detail = "Debug builds expose extra data and are not meant for normal use",
            priority = SignalPriority.HIGH,
            weight = 15,
        )

        val staleAfter = nowMillis() - STALE_UPDATE_MILLIS
        if (app.lastUpdateTime > 0L && app.lastUpdateTime < staleAfter) signals += ReviewSignal(
            type = SignalType.STALE_UPDATE,
            title = "Has not been updated in over two years",
            detail = "Last updated ${formatRelativeTimeShort(app.lastUpdateTime)}",
            priority = SignalPriority.LOW,
            weight = 5,
        )

        return signals.sortedWith(
            compareBy<ReviewSignal> { it.priority.ordinal }.thenByDescending { it.weight }
        )
    }

    /**
     * "Needs review" gate: any high priority reason, or at least two independent reasons.
     * Deterministic and explainable — never a black box (spec §14).
     */
    fun needsReview(signals: List<ReviewSignal>): Boolean =
        signals.any { it.priority == SignalPriority.HIGH } || signals.size >= 2

    private fun groupSignal(
        requested: Set<String>,
        group: Set<String>,
        type: SignalType,
        title: String,
        priority: SignalPriority,
        weight: Int,
        signals: MutableList<ReviewSignal>,
    ) {
        if (requested.none { it in group }) return
        signals += ReviewSignal(
            type = type,
            title = title,
            priority = priority,
            weight = weight,
        )
    }

    private fun formatRelativeTimeShort(timestampMillis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(nowMillis() - timestampMillis)
        return when {
            days < 1 -> "today"
            days < 30 -> "$days days ago"
            days < 365 -> "${days / 30} months ago"
            else -> "${days / 365} years ago"
        }
    }

    /** Thresholds — documented in `docs/scoring.md`, never changed silently. */
    companion object {
        const val LARGE_APP_BYTES: Long = 150L * 1024 * 1024
        const val TARGET_SDK_LAG: Int = 2
        const val MANY_SENSITIVE_THRESHOLD: Int = 5
        const val STALE_UPDATE_MILLIS: Long = 730L * 24 * 60 * 60 * 1000
    }
}

/** Permission names and groups used by the rules. */
private object Permission {
    const val CAMERA = "android.permission.CAMERA"
    const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
    const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
    const val LOCATION_BACKGROUND = "android.permission.ACCESS_BACKGROUND_LOCATION"

    val GROUP_CONTACTS = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
    )

    val GROUP_PHONE = setOf(
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.ADD_VOICEMAIL",
        "android.permission.USE_SIP",
        "android.permission.ACCEPT_HANDOVER",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
    )

    val GROUP_SMS = setOf(
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.RECEIVE_MMS",
        "android.permission.RECEIVE_WAP_PUSH",
    )

    val GROUP_CALENDAR = setOf(
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
    )

    val GROUP_BODY_SENSORS = setOf(
        "android.permission.BODY_SENSORS",
        "android.permission.BODY_SENSORS_BACKGROUND",
        "android.permission.ACTIVITY_RECOGNITION",
    )

    val GROUP_NEARBY = setOf(
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.UWB_RANGING",
    )

    val GROUP_MEDIA = setOf(
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.ACCESS_MEDIA_LOCATION",
    )
}
