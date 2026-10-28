package com.example.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import java.util.Locale

sealed class AppResolveResult {
    data class Found(val launchIntent: Intent, val appName: String, val packageName: String?) : AppResolveResult()
    data class NotFound(val appName: String, val candidatePackage: String?, val playStoreUrl: String?) : AppResolveResult()
}

class AppResolver(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    private val knownAppPackages = mapOf(
        "youtube" to listOf("com.google.android.youtube"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2", "com.sec.android.app.popupcalculator"),
        "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d", "com.sec.android.gallery3d"),
        "chrome" to listOf("com.android.chrome", "org.chromium.chrome"),
        "settings" to listOf("com.android.settings"),
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "maps" to listOf("com.google.android.apps.maps"),
        "camera" to listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2"),
        "gmail" to listOf("com.google.android.gm")
    )

    fun resolve(appKey: String, rawQuery: String): AppResolveResult {
        val key = appKey.lowercase(Locale.ROOT)
        val displayTitle = key.replaceFirstChar { it.titlecase(Locale.ROOT) }

        // 1. Check known packages for specific launcher intent
        val candidatePkgs = knownAppPackages[key] ?: emptyList()
        for (pkg in candidatePkgs) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return AppResolveResult.Found(intent, displayTitle, pkg)
            }
        }

        // 2. Try Standard Android Category Intents (covers any OEM app)
        val categoryIntent = when (key) {
            "calculator" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
            "gallery" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)
            "chrome" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
            "maps" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MAPS)
            "gmail" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL)
            "camera" -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            else -> null
        }

        if (categoryIntent != null) {
            categoryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val activities = packageManager.queryIntentActivities(categoryIntent, 0)
            if (activities.isNotEmpty()) {
                val resolvedActivity = activities[0].activityInfo
                val explicitIntent = Intent(categoryIntent).apply {
                    setClassName(resolvedActivity.packageName, resolvedActivity.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return AppResolveResult.Found(explicitIntent, displayTitle, resolvedActivity.packageName)
            }
        }

        // 3. Fallback: Search all installed launchable apps by label
        val matchedApp = findInstalledAppByLabel(rawQuery.ifEmpty { key })
        if (matchedApp != null) {
            val intent = packageManager.getLaunchIntentForPackage(matchedApp.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return AppResolveResult.Found(intent, matchedApp.label, matchedApp.packageName)
            }
        }

        // 4. Not installed: construct Play Store fallback
        val primaryCandidate = candidatePkgs.firstOrNull()
        val playStoreUrl = primaryCandidate?.let { "https://play.google.com/store/apps/details?id=$it" }
        return AppResolveResult.NotFound(
            appName = displayTitle,
            candidatePackage = primaryCandidate,
            playStoreUrl = playStoreUrl
        )
    }

    private data class InstalledApp(val label: String, val packageName: String)

    private fun findInstalledAppByLabel(query: String): InstalledApp? {
        val lowerQuery = query.lowercase(Locale.ROOT)
        return try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val pkgApps = packageManager.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in pkgApps) {
                val label = resolveInfo.loadLabel(packageManager).toString()
                if (label.lowercase(Locale.ROOT).contains(lowerQuery) ||
                    lowerQuery.contains(label.lowercase(Locale.ROOT))) {
                    return InstalledApp(label, resolveInfo.activityInfo.packageName)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
