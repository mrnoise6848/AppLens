package com.noise.applens.domain.permission

/**
 * The single source of truth for how AppLens names and groups Android permissions (spec §12).
 *
 * Grouping rules, in order:
 *  1. [SPECIAL_ACCESS]     → [PermissionCategory.SPECIAL] (Android "special app access")
 *  2. [SENSITIVE]          → [PermissionCategory.SENSITIVE]
 *  3. [LOW_SENSITIVITY_RUNTIME] → [PermissionCategory.NORMAL] (runtime, but not a privacy signal)
 *  4. dangerous (protection) → [PermissionCategory.SENSITIVE]
 *  5. normal (protection)    → [PermissionCategory.NORMAL]
 *  6. signature / internal / unknown → [PermissionCategory.OTHER]
 *
 * Nothing here is a security verdict — these are presentation groupings derived from Android's
 * permission semantics (spec §4.2, §4.3).
 */
object PermissionCatalog {

    /** Android "Special app access": never granted through a runtime dialog. */
    val SPECIAL_ACCESS: Set<String> = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.MANAGE_UNKNOWN_APP_SOURCES",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.ACCESS_NOTIFICATION_POLICY",
        "android.permission.WRITE_SETTINGS",
        "android.permission.SCHEDULE_EXACT_ALARM",
    )

    /** Permissions AppLens always presents as sensitive, straight from spec §12. */
    val SENSITIVE: Set<String> = setOf(
        // Camera / microphone
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        // Location
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.ACCESS_MEDIA_LOCATION",
        // Contacts
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
        // Phone
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
        // SMS
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.RECEIVE_MMS",
        "android.permission.RECEIVE_WAP_PUSH",
        // Calendar
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        // Body sensors
        "android.permission.BODY_SENSORS",
        "android.permission.BODY_SENSORS_BACKGROUND",
        "android.permission.ACTIVITY_RECOGNITION",
        // Nearby devices
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.UWB_RANGING",
        // Files / media
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_DOCUMENTS",
    )

    /**
     * Runtime (dangerous) permissions that are not privacy signals for a normal user. They stay in
     * the normal group instead of inflating the "sensitive permissions" count (spec §4.2).
     */
    val LOW_SENSITIVITY_RUNTIME: Set<String> = setOf(
        "android.permission.POST_NOTIFICATIONS",
    )

    /** Permission names that require a background-only qualifier in explanations. */
    val BACKGROUND_ONLY: Set<String> = setOf(
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.BODY_SENSORS_BACKGROUND",
    )

    /** Human readable names for the permissions AppLens talks about. */
    private val NAMES: Map<String, String> = mapOf(
        "android.permission.CAMERA" to "Camera",
        "android.permission.RECORD_AUDIO" to "Microphone",
        "android.permission.ACCESS_FINE_LOCATION" to "Precise location",
        "android.permission.ACCESS_COARSE_LOCATION" to "Approximate location",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Background location",
        "android.permission.ACCESS_MEDIA_LOCATION" to "Media location tags",
        "android.permission.READ_CONTACTS" to "Contacts",
        "android.permission.WRITE_CONTACTS" to "Contacts (edit)",
        "android.permission.GET_ACCOUNTS" to "Accounts",
        "android.permission.READ_PHONE_STATE" to "Phone status",
        "android.permission.READ_PHONE_NUMBERS" to "Phone numbers",
        "android.permission.CALL_PHONE" to "Make calls",
        "android.permission.ANSWER_PHONE_CALLS" to "Answer calls",
        "android.permission.ADD_VOICEMAIL" to "Voicemail",
        "android.permission.USE_SIP" to "SIP calls",
        "android.permission.ACCEPT_HANDOVER" to "Call handover",
        "android.permission.READ_CALL_LOG" to "Call history",
        "android.permission.WRITE_CALL_LOG" to "Call history (edit)",
        "android.permission.PROCESS_OUTGOING_CALLS" to "Outgoing calls",
        "android.permission.READ_SMS" to "Read SMS",
        "android.permission.SEND_SMS" to "Send SMS",
        "android.permission.RECEIVE_SMS" to "Receive SMS",
        "android.permission.RECEIVE_MMS" to "Receive MMS",
        "android.permission.RECEIVE_WAP_PUSH" to "Receive WAP push",
        "android.permission.READ_CALENDAR" to "Calendar",
        "android.permission.WRITE_CALENDAR" to "Calendar (edit)",
        "android.permission.BODY_SENSORS" to "Body sensors",
        "android.permission.BODY_SENSORS_BACKGROUND" to "Body sensors (background)",
        "android.permission.ACTIVITY_RECOGNITION" to "Activity recognition",
        "android.permission.BLUETOOTH_SCAN" to "Nearby devices (Bluetooth)",
        "android.permission.BLUETOOTH_CONNECT" to "Nearby devices (connect)",
        "android.permission.BLUETOOTH_ADVERTISE" to "Nearby devices (advertise)",
        "android.permission.NEARBY_WIFI_DEVICES" to "Nearby devices (Wi-Fi)",
        "android.permission.UWB_RANGING" to "Nearby devices (UWB)",
        "android.permission.READ_MEDIA_IMAGES" to "Photos and images",
        "android.permission.READ_MEDIA_VIDEO" to "Videos",
        "android.permission.READ_MEDIA_AUDIO" to "Audio files",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to "Selected photos and videos",
        "android.permission.READ_EXTERNAL_STORAGE" to "Files and media",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "Files and media (edit)",
        "android.permission.MANAGE_DOCUMENTS" to "Documents",
        "android.permission.POST_NOTIFICATIONS" to "Notifications",
        "android.permission.INTERNET" to "Internet access",
        "android.permission.ACCESS_NETWORK_STATE" to "Network status",
        "android.permission.ACCESS_WIFI_STATE" to "Wi-Fi status",
        "android.permission.CHANGE_WIFI_STATE" to "Change Wi-Fi state",
        "android.permission.VIBRATE" to "Vibrate",
        "android.permission.FLASHLIGHT" to "Flashlight",
        "android.permission.WAKE_LOCK" to "Keep device awake",
        "android.permission.RECEIVE_BOOT_COMPLETED" to "Start after reboot",
        "android.permission.FOREGROUND_SERVICE" to "Run in the foreground",
        "android.permission.SYSTEM_ALERT_WINDOW" to "Display over other apps",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "All files access",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "Install unknown apps",
        "android.permission.MANAGE_UNKNOWN_APP_SOURCES" to "Install unknown apps",
        "android.permission.PACKAGE_USAGE_STATS" to "Usage access",
        "android.permission.ACCESS_NOTIFICATION_POLICY" to "Do not disturb access",
        "android.permission.WRITE_SETTINGS" to "Modify system settings",
        "android.permission.SCHEDULE_EXACT_ALARM" to "Exact alarms",
        "android.permission.QUERY_ALL_PACKAGES" to "See all installed apps",
    )

    /** Short explanations. Only listed where a plain-language sentence adds value. */
    private val DESCRIPTIONS: Map<String, String> = mapOf(
        "android.permission.CAMERA" to "Take photos and record video.",
        "android.permission.RECORD_AUDIO" to "Record audio with the microphone.",
        "android.permission.ACCESS_FINE_LOCATION" to "Read your precise location.",
        "android.permission.ACCESS_COARSE_LOCATION" to "Read your approximate location.",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Read your location while the app is not in use.",
        "android.permission.READ_CONTACTS" to "Read the contacts stored on this device.",
        "android.permission.WRITE_CONTACTS" to "Add or change contacts on this device.",
        "android.permission.GET_ACCOUNTS" to "List the accounts signed in on this device.",
        "android.permission.READ_PHONE_STATE" to "Read phone status and device identifiers.",
        "android.permission.CALL_PHONE" to "Start a phone call without asking you first.",
        "android.permission.READ_CALL_LOG" to "Read your call history.",
        "android.permission.READ_SMS" to "Read text messages.",
        "android.permission.SEND_SMS" to "Send text messages.",
        "android.permission.RECEIVE_SMS" to "Receive text messages.",
        "android.permission.READ_CALENDAR" to "Read your calendar entries.",
        "android.permission.BODY_SENSORS" to "Read body sensor measurements such as heart rate.",
        "android.permission.BLUETOOTH_SCAN" to "Find and connect to nearby Bluetooth devices.",
        "android.permission.NEARBY_WIFI_DEVICES" to "Find and connect to nearby Wi-Fi devices.",
        "android.permission.READ_MEDIA_IMAGES" to "Read photos and images on this device.",
        "android.permission.READ_MEDIA_VIDEO" to "Read videos on this device.",
        "android.permission.READ_MEDIA_AUDIO" to "Read audio files on this device.",
        "android.permission.SYSTEM_ALERT_WINDOW" to "Draw on top of other applications.",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "Read and write every file on this device.",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "Install applications from outside the Play Store.",
        "android.permission.PACKAGE_USAGE_STATS" to "See how often other applications are used.",
        "android.permission.WRITE_SETTINGS" to "Change system-wide settings.",
        "android.permission.SCHEDULE_EXACT_ALARM" to "Trigger alarms at an exact time.",
        "android.permission.POST_NOTIFICATIONS" to "Show notifications.",
    )

    /** Display name for a raw permission, prettified when the catalog has no entry. */
    fun displayName(permission: String): String =
        NAMES[permission] ?: permission.substringAfterLast('.').lowercase()
            .split('_')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    fun description(permission: String): String = DESCRIPTIONS[permission].orEmpty()

    fun isSpecial(permission: String): Boolean = permission in SPECIAL_ACCESS

    fun isSensitive(permission: String): Boolean = permission in SENSITIVE
}
