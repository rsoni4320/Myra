package com.example.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

sealed class AppLaunchStatus {
    data class Success(val appName: String, val packageName: String?) : AppLaunchStatus()
    data class Failed(val appName: String, val errorMessage: String, val playStoreUrl: String?) : AppLaunchStatus()
}

class AppLauncher(
    private val context: Context,
    private val appResolver: AppResolver = AppResolver(context)
) {

    fun launch(appKey: String, rawQuery: String): AppLaunchStatus {
        val resolveResult = appResolver.resolve(appKey, rawQuery)
        return when (resolveResult) {
            is AppResolveResult.Found -> {
                try {
                    context.startActivity(resolveResult.launchIntent)
                    AppLaunchStatus.Success(resolveResult.appName, resolveResult.packageName)
                } catch (e: ActivityNotFoundException) {
                    AppLaunchStatus.Failed(
                        appName = resolveResult.appName,
                        errorMessage = "Cannot start ${resolveResult.appName}: application not available.",
                        playStoreUrl = resolveResult.packageName?.let { "https://play.google.com/store/apps/details?id=$it" }
                    )
                } catch (e: Exception) {
                    AppLaunchStatus.Failed(
                        appName = resolveResult.appName,
                        errorMessage = "Error launching ${resolveResult.appName}: ${e.localizedMessage}",
                        playStoreUrl = null
                    )
                }
            }
            is AppResolveResult.NotFound -> {
                AppLaunchStatus.Failed(
                    appName = resolveResult.appName,
                    errorMessage = "${resolveResult.appName} is not installed on this device.",
                    playStoreUrl = resolveResult.playStoreUrl
                )
            }
        }
    }

    fun openPlayStore(playStoreUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }
}
