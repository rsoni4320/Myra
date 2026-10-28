package com.example.android

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class AndroidExecutionResult {
    data class Success(val message: String, val extraData: String? = null) : AndroidExecutionResult()
    data class Failure(val error: String, val recoverySuggestion: String? = null) : AndroidExecutionResult()
}

class AndroidActionExecutor(
    private val context: Context,
    private val launcher: AppLauncher = AppLauncher(context)
) {

    fun launchApp(appKey: String, rawQuery: String): AndroidExecutionResult {
        return when (val status = launcher.launch(appKey, rawQuery)) {
            is AppLaunchStatus.Success -> {
                AndroidExecutionResult.Success(
                    message = "Opening ${status.appName} now.",
                    extraData = status.packageName
                )
            }
            is AppLaunchStatus.Failed -> {
                val suggestion = status.playStoreUrl?.let { "You can install it from Google Play Store." }
                AndroidExecutionResult.Failure(
                    error = status.errorMessage,
                    recoverySuggestion = suggestion
                )
            }
        }
    }

    fun searchWeb(query: String): AndroidExecutionResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return AndroidExecutionResult.Failure("Search query cannot be empty.")
        }

        return try {
            val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString())
            val searchUri = Uri.parse("https://www.google.com/search?q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AndroidExecutionResult.Success("Searching Google for \"$trimmed\".")
        } catch (e: Exception) {
            // Fallback to web search action intent
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, trimmed)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                AndroidExecutionResult.Success("Searching web for \"$trimmed\".")
            } catch (ex: Exception) {
                AndroidExecutionResult.Failure("Could not open web browser: ${ex.localizedMessage}")
            }
        }
    }

    fun openUrl(rawUrl: String): AndroidExecutionResult {
        val trimmed = rawUrl.trim()
        val colonIdx = trimmed.indexOf(':')
        if (colonIdx > 0 && !trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            val rawScheme = trimmed.substring(0, colonIdx).lowercase()
            return AndroidExecutionResult.Failure("Blocked insecure URL scheme: $rawScheme. Only http and https are allowed.")
        }

        val safeUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }

        val uri = Uri.parse(safeUrl)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return AndroidExecutionResult.Failure("Blocked insecure URL scheme: $scheme. Only http and https are allowed.")
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AndroidExecutionResult.Success("Opening $safeUrl.")
        } catch (e: Exception) {
            AndroidExecutionResult.Failure("Failed to open URL: ${e.localizedMessage}")
        }
    }

    fun setTimer(durationSeconds: Int, label: String = "Maya Assistant Timer"): AndroidExecutionResult {
        val seconds = durationSeconds.coerceIn(1, 86400)
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val minutes = seconds / 60
            val secRemaining = seconds % 60
            val timeDesc = if (minutes > 0) "$minutes minute${if (minutes > 1) "s" else ""}${if (secRemaining > 0) " $secRemaining seconds" else ""}" else "$seconds seconds"
            AndroidExecutionResult.Success("Setting a timer for $timeDesc.")
        } catch (e: Exception) {
            AndroidExecutionResult.Failure("Could not set system timer: ${e.localizedMessage}. Make sure a clock app is installed.")
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "Maya Assistant Alarm"): AndroidExecutionResult {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, h)
                putExtra(AlarmClock.EXTRA_MINUTES, m)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val amPm = if (h >= 12) "PM" else "AM"
            val displayHour = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            val formattedTime = String.format("%d:%02d %s", displayHour, m, amPm)
            AndroidExecutionResult.Success("Setting an alarm for $formattedTime.")
        } catch (e: Exception) {
            AndroidExecutionResult.Failure("Could not set system alarm: ${e.localizedMessage}")
        }
    }
}
