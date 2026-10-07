package com.noise.applens.domain.library

import com.noise.applens.domain.model.AppLibraryEvidence
import com.noise.applens.domain.model.LibraryConfidence
import com.noise.applens.domain.model.LibraryInfo

/**
 * Deterministic library/SDK detection from package metadata (spec §17).
 *
 * Evidence strength decides the confidence:
 *  * a class declared in the manifest with an SDK-owned namespace → [LibraryConfidence.DETECTED]
 *  * only a permission namespace owned by the SDK               → [LibraryConfidence.LIKELY]
 *  * only a native library file name                            → [LibraryConfidence.UNKNOWN]
 *
 * Every evidence string is shown to the user, so the classification can be audited. Signatures are
 * checked most-specific-first, so `com.google.android.gms.ads` is reported as AdMob rather than as
 * generic Google Play services.
 */
class LibraryAnalyzer {

    fun analyze(evidence: AppLibraryEvidence): List<LibraryInfo> {
        if (evidence.isEmpty) return emptyList()

        val hits = linkedMapOf<String, MutableList<String>>()

        fun record(index: Int, item: String) {
            hits.getOrPut(SIGNATURES[index].name) { mutableListOf() }.add(item)
        }

        // 1. Component class names and the application class — strongest evidence.
        (evidence.componentClassNames + listOfNotNull(evidence.applicationClassName)).forEach { className ->
            val index = SIGNATURES.indexOfFirst { signature ->
                signature.classPrefixes.any { prefix -> className.startsWith(prefix) }
            }
            if (index >= 0) record(index, "declares $className")
        }

        // 2. Permissions declared or requested with an SDK-owned namespace.
        (evidence.definedPermissions + evidence.requestedPermissions).forEach { permission ->
            val index = SIGNATURES.indexOfFirst { signature ->
                signature.permissionPrefixes.any { prefix -> permission.startsWith(prefix) }
            }
            if (index >= 0) record(index, "uses permission $permission")
        }

        // 3. Native library file names — weakest evidence.
        evidence.nativeLibraryNames.forEach { libName ->
            val index = SIGNATURES.indexOfFirst { signature ->
                signature.nativeTokens.any { token -> libName.contains(token, ignoreCase = true) }
            }
            if (index >= 0) record(index, "ships $libName")
        }

        return hits.map { (name, items) ->
            val index = SIGNATURES.indexOfFirst { it.name == name }
            LibraryInfo(
                name = name,
                confidence = confidenceFor(items, classBacked = index >= 0 && items.any { it.startsWith("declares") }),
                evidence = items.distinct().sorted(),
            )
        }.sortedWith(
            compareBy<LibraryInfo> { it.confidence.ordinal }.thenBy { it.name.lowercase() },
        )
    }

    private fun confidenceFor(evidence: List<String>, classBacked: Boolean): LibraryConfidence = when {
        classBacked -> LibraryConfidence.DETECTED
        evidence.any { it.startsWith("uses permission") } -> LibraryConfidence.LIKELY
        else -> LibraryConfidence.UNKNOWN
    }

    private data class LibrarySignature(
        val name: String,
        val classPrefixes: List<String>,
        val permissionPrefixes: List<String> = emptyList(),
        val nativeTokens: List<String> = emptyList(),
    )

    companion object {
        /**
         * Ordered most specific first: the first signature that matches an item owns it, so
         * overlapping namespaces do not double count.
         */
        private val SIGNATURES = listOf(
            LibrarySignature(
                name = "AdMob",
                classPrefixes = listOf("com.google.android.gms.ads"),
                nativeTokens = listOf("google_mobile_ads", "gms.ads"),
            ),
            LibrarySignature(
                name = "React Native",
                classPrefixes = listOf("com.facebook.react"),
                nativeTokens = listOf("reactnativejni", "react_nativemodule"),
            ),
            LibrarySignature(
                name = "Facebook / Meta SDK",
                classPrefixes = listOf(
                    "com.facebook.ads",
                    "com.facebook.login",
                    "com.facebook.internal",
                    "com.facebook.CustomTab",
                    "com.facebook.FacebookSdk",
                    "com.facebook.appevents",
                ),
                permissionPrefixes = listOf("com.facebook."),
            ),
            LibrarySignature(
                name = "Crashlytics",
                classPrefixes = listOf(
                    "com.crashlytics",
                    "com.google.firebase.crashlytics",
                ),
                nativeTokens = listOf("crashlytics"),
            ),
            LibrarySignature(
                name = "Firebase",
                classPrefixes = listOf("com.google.firebase"),
                permissionPrefixes = listOf("com.google.android.c2dm"),
                nativeTokens = listOf("firebase"),
            ),
            LibrarySignature(
                name = "Google Play services",
                classPrefixes = listOf("com.google.android.gms"),
                nativeTokens = listOf("gmscore", "measurement"),
            ),
            LibrarySignature(
                name = "Unity",
                classPrefixes = listOf("com.unity3d"),
                nativeTokens = listOf("unity", "il2cpp"),
            ),
            LibrarySignature(
                name = "Flutter",
                classPrefixes = listOf("io.flutter"),
                nativeTokens = listOf("flutter"),
            ),
            LibrarySignature(
                name = "Cordova",
                classPrefixes = listOf("org.apache.cordova"),
                nativeTokens = listOf("cordova"),
            ),
            LibrarySignature(
                name = "ExoPlayer / Media3",
                classPrefixes = listOf("androidx.media3", "com.google.android.exoplayer2"),
            ),
            LibrarySignature(
                name = "AndroidX",
                classPrefixes = listOf("androidx.", "com.google.android.material"),
            ),
            LibrarySignature(
                name = "OkHttp",
                classPrefixes = listOf("okhttp3", "com.squareup.okhttp"),
            ),
            LibrarySignature(
                name = "Retrofit",
                classPrefixes = listOf("retrofit2"),
            ),
            LibrarySignature(
                name = "Glide",
                classPrefixes = listOf("com.bumptech.glide"),
            ),
            LibrarySignature(
                name = "Play Billing",
                classPrefixes = listOf("com.android.billingclient"),
            ),
            LibrarySignature(
                name = "Stripe",
                classPrefixes = listOf("com.stripe"),
            ),
            LibrarySignature(
                name = "AppsFlyer",
                classPrefixes = listOf("com.appsflyer"),
            ),
            LibrarySignature(
                name = "Adjust",
                classPrefixes = listOf("com.adjust"),
            ),
            LibrarySignature(
                name = "OneSignal",
                classPrefixes = listOf("com.onesignal"),
            ),
            LibrarySignature(
                name = "Braze",
                classPrefixes = listOf("com.braze"),
            ),
            LibrarySignature(
                name = "Amplitude",
                classPrefixes = listOf("com.amplitude"),
            ),
            LibrarySignature(
                name = "AppLovin",
                classPrefixes = listOf("com.applovin"),
            ),
            LibrarySignature(
                name = "TikTok / ByteDance SDK",
                classPrefixes = listOf("com.bytedance", "com.ss.android"),
            ),
        )
    }
}
