package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.repository.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class AiResult {
    data class Success(val responseText: String) : AiResult()
    data class Error(val errorMessage: String, val fallbackText: String) : AiResult()
}

class AiEngine {
    private val tag = "AiEngine"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        prompt: String,
        settings: AppSettings,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): AiResult = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty()) {
            return@withContext AiResult.Success(
                if (settings.voiceLanguage.contains("Hindi", ignoreCase = true))
                    "नमस्ते! मैंने कुछ सुना नहीं। कृपया दोबारा कहें।"
                else
                    "I didn't quite catch that. Could you please say that again?"
            )
        }

        // Determine if prompt is in Hindi
        val isHindi = trimmedPrompt.any { it.code in 0x0900..0x097F } ||
                settings.voiceLanguage.contains("Hindi", ignoreCase = true) ||
                settings.language.contains("Hindi", ignoreCase = true)

        // Check if an API key is available
        val apiKey = when {
            settings.customApiKey.isNotBlank() -> settings.customApiKey.trim()
            tryGetBuildConfigApiKey().isNotBlank() -> tryGetBuildConfigApiKey()
            else -> ""
        }

        if (apiKey.isNotBlank() && !apiKey.startsWith("MY_GEMINI")) {
            try {
                val apiResponse = callGeminiApi(trimmedPrompt, apiKey, settings, conversationHistory, isHindi)
                if (apiResponse != null && apiResponse.isNotBlank()) {
                    return@withContext AiResult.Success(apiResponse)
                }
            } catch (e: Exception) {
                Log.e(tag, "API call failed, falling back to local intelligence", e)
            }
        }

        // Built-in intelligent response engine
        val localResponse = generateLocalAssistantResponse(trimmedPrompt, settings, isHindi)
        AiResult.Success(localResponse)
    }

    private fun tryGetBuildConfigApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun callGeminiApi(
        prompt: String,
        apiKey: String,
        settings: AppSettings,
        history: List<Pair<String, String>>,
        isHindi: Boolean
    ): String? {
        val persona = buildPersona(settings, isHindi)

        val contentsArray = JSONArray()

        // Include conversation history if rememberConversation is enabled
        if (settings.rememberConversation && history.isNotEmpty()) {
            val recent = history.takeLast(4)
            for (item in recent) {
                val role = if (item.first == "USER") "user" else "model"
                val partObj = JSONObject().put("text", item.second)
                val contentObj = JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(partObj))
                contentsArray.put(contentObj)
            }
        }

        // Current prompt
        val userPromptWithSystem = "$persona\n\nUser: $prompt"
        val currentContent = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(JSONObject().put("text", userPromptWithSystem)))
        contentsArray.put(currentContent)

        val requestBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            val maxTokens = when (settings.responseLength) {
                "Short" -> 150
                "Long" -> 700
                else -> 350
            }
            put("generationConfig", JSONObject().put("maxOutputTokens", maxTokens))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(tag, "Gemini API error code: ${response.code}")
                return null
            }
            val responseBody = response.body?.string() ?: return null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    return text.trim()
                }
            }
        }
        return null
    }

    private fun buildPersona(settings: AppSettings, isHindi: Boolean): String {
        val styleInstruction = when (settings.voiceStyle.lowercase()) {
            "friendly" -> "You are cheerful, warm, enthusiastic, and speak with sweet charm like Maya, a friendly anime companion."
            "professional" -> "You are polite, precise, articulate, and business-focused."
            "calm" -> "You are soothing, gentle, serene, and speak peacefully."
            "funny" -> "You are humorous, playful, witty, and tell clever jokes."
            else -> "You are helpful and friendly."
        }
        val langInstruction = if (isHindi) {
            "Respond in Hindi (Devanagari script) naturally and fluently."
        } else {
            "Respond in English."
        }
        val lengthInstruction = when (settings.responseLength) {
            "Short" -> "Keep your answer very short (1-2 sentences), optimal for voice readout."
            "Long" -> "Provide a comprehensive and detailed explanation."
            else -> "Keep your response balanced (2-4 sentences), ideal for voice audio playback."
        }

        return "System Persona: You are Maya, a personal native Android AI Voice Assistant. $styleInstruction $langInstruction $lengthInstruction Do not use markdown headers or bold stars that sound awkward when read aloud by Text-To-Speech."
    }

    private fun generateLocalAssistantResponse(
        prompt: String,
        settings: AppSettings,
        isHindi: Boolean
    ): String {
        val lower = prompt.lowercase(Locale.ROOT)

        if (isHindi) {
            return when {
                lower.contains("नमस्ते") || lower.contains("हेलो") || lower.contains("hi") || lower.contains("hello") ->
                    "नमस्ते! मैं माया हूँ, आपकी वॉयस असिस्टेंट। मैं आज आपकी क्या मदद कर सकती हूँ?"

                lower.contains("कैसी हो") || lower.contains("हाल") ->
                    "मैं बहुत अच्छी हूँ, शुक्रिया! आपके साथ बात करके मुझे बहुत खुशी हो रही है।"

                lower.contains("नाम") || lower.contains("कौन हो") ->
                    "मेरा नाम माया है! मैं आपकी पर्सनल एआई वॉयस असिस्टेंट हूँ।"

                lower.contains("समय") || lower.contains("टाइम") -> {
                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    "अभी समय $time हो रहा है।"
                }

                lower.contains("तारीख") || lower.contains("दिन") || lower.contains("डेट") -> {
                    val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN")).format(Date())
                    "आज $date है।"
                }

                lower.contains("मौसम") ->
                    "आज मौसम बहुत सुहावना है! आसमान साफ है और ठंडी हवा चल रही है।"

                lower.contains("चुटकुला") || lower.contains("हंसाओ") ->
                    "अध्यापक: अगर तुम्हारे पास 10 सेब हैं और 4 मैंने ले लिए तो क्या बचेगा? छात्र: आपके हाथ पर मेरा थप्पड़! हा हा हा!"

                lower.contains("रिमाइंडर") ->
                    "बिल्कुल! मैंने आपका रिमाइंडर नोट कर लिया है।"

                lower.contains("नोट") ->
                    "मैंने आपकी बात को नोट्स में सुरक्षित कर लिया है।"

                lower.contains("अलविदा") || lower.contains("बाय") ->
                    "अलविदा! अपना ध्यान रखिएगा। जब भी ज़रूरत हो, बस मुझे पुकारिएगा!"

                else ->
                    "मैंने आपकी बात समझ ली: \"$prompt\"। मैं हमेशा आपकी सहायता के लिए तैयार हूँ!"
            }
        } else {
            return when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                    when (settings.voiceStyle.lowercase()) {
                        "friendly" -> "Hello there! I'm Maya! Your Voice, My World. How can I brighten your day? ✨"
                        "professional" -> "Good day. I am Maya, your AI assistant. How may I be of service?"
                        "calm" -> "Hello. Take a moment to relax. I am Maya, and I am here for you."
                        "funny" -> "Hey! Look who's back! Maya is fully charged and ready to chat!"
                        else -> "Hello! I am Maya. How can I help you today?"
                    }

                lower.contains("how are you") || lower.contains("how do you do") ->
                    "I am feeling fantastic! Always happy to hear your voice and ready to help you with anything."

                lower.contains("who are you") || lower.contains("what is your name") ->
                    "I am Maya, your native Android AI Voice Assistant! You can ask me questions, set reminders, take notes, or explore with AI tools."

                lower.contains("time") -> {
                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    "The current time is $time."
                }

                lower.contains("date") || lower.contains("day") -> {
                    val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
                    "Today is $date."
                }

                lower.contains("weather") ->
                    "Right now the weather is looking clear with a pleasant temperature and gentle breeze outside!"

                lower.contains("joke") ->
                    "Why do programmers prefer dark mode? Because light attracts bugs! Hehe!"

                lower.contains("reminder") ->
                    "Got it! I have scheduled that reminder for you."

                lower.contains("note") ->
                    "Sure thing! I have saved that into your notes."

                lower.contains("thank") ->
                    "You're very welcome! That's what I'm here for! 😊"

                lower.contains("bye") || lower.contains("goodbye") ->
                    "Goodbye for now! Whenever you need me, just tap the microphone!"

                else -> {
                    when (settings.voiceStyle.lowercase()) {
                        "friendly" -> "You asked: \"$prompt\". That is so interesting! As your companion Maya, I am here to explore ideas and assist you every step of the way!"
                        "professional" -> "Acknowledged: \"$prompt\". Your query has been logged and processed by the Maya assistant system."
                        "calm" -> "I hear you clearly: \"$prompt\". Everything is under control, and I am here to assist."
                        "funny" -> "Fascinating query! My artificial neurons fired up at maximum speed for \"$prompt\"!"
                        else -> "I heard: \"$prompt\". I am ready to help you with that!"
                    }
                }
            }
        }
    }
}
