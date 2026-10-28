package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    // General
    val language: String = "English (US)",
    val theme: String = "Dark",
    val accentColor: Long = 0xFF7C4DFF,
    val vibration: Boolean = true,
    val soundEffects: Boolean = true,

    // Voice
    val voiceLanguage: String = "English (US)",
    val voiceStyle: String = "Friendly",
    val speechSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val volume: Int = 80,
    val wakeWord: Boolean = true,

    // AI
    val aiModel: String = "GPT-4o",
    val responseLength: String = "Balanced",
    val rememberConversation: Boolean = true,
    val webSearch: Boolean = true,
    val imageUnderstanding: Boolean = true,
    val documentReading: Boolean = true,
    val codeGeneration: Boolean = true,
    val customApiKey: String = ""
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("maya_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            language = prefs.getString("language", "English (US)") ?: "English (US)",
            theme = prefs.getString("theme", "Dark") ?: "Dark",
            accentColor = prefs.getLong("accentColor", 0xFF7C4DFF),
            vibration = prefs.getBoolean("vibration", true),
            soundEffects = prefs.getBoolean("soundEffects", true),
            voiceLanguage = prefs.getString("voiceLanguage", "English (US)") ?: "English (US)",
            voiceStyle = prefs.getString("voiceStyle", "Friendly") ?: "Friendly",
            speechSpeed = prefs.getFloat("speechSpeed", 1.0f),
            voicePitch = prefs.getFloat("voicePitch", 1.0f),
            volume = prefs.getInt("volume", 80),
            wakeWord = prefs.getBoolean("wakeWord", true),
            aiModel = prefs.getString("aiModel", "GPT-4o") ?: "GPT-4o",
            responseLength = prefs.getString("responseLength", "Balanced") ?: "Balanced",
            rememberConversation = prefs.getBoolean("rememberConversation", true),
            webSearch = prefs.getBoolean("webSearch", true),
            imageUnderstanding = prefs.getBoolean("imageUnderstanding", true),
            documentReading = prefs.getBoolean("documentReading", true),
            codeGeneration = prefs.getBoolean("codeGeneration", true),
            customApiKey = prefs.getString("customApiKey", "") ?: ""
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit().apply {
            putString("language", newSettings.language)
            putString("theme", newSettings.theme)
            putLong("accentColor", newSettings.accentColor)
            putBoolean("vibration", newSettings.vibration)
            putBoolean("soundEffects", newSettings.soundEffects)
            putString("voiceLanguage", newSettings.voiceLanguage)
            putString("voiceStyle", newSettings.voiceStyle)
            putFloat("speechSpeed", newSettings.speechSpeed)
            putFloat("voicePitch", newSettings.voicePitch)
            putInt("volume", newSettings.volume)
            putBoolean("wakeWord", newSettings.wakeWord)
            putString("aiModel", newSettings.aiModel)
            putString("responseLength", newSettings.responseLength)
            putBoolean("rememberConversation", newSettings.rememberConversation)
            putBoolean("webSearch", newSettings.webSearch)
            putBoolean("imageUnderstanding", newSettings.imageUnderstanding)
            putBoolean("documentReading", newSettings.documentReading)
            putBoolean("codeGeneration", newSettings.codeGeneration)
            putString("customApiKey", newSettings.customApiKey)
            apply()
        }
        _settings.value = newSettings
    }
}
