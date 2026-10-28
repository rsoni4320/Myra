package com.example.ai

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalOfflineProvider : AiProvider {

    override val providerId: String = "local_offline"
    override val displayName: String = "Maya On-Device Assistant"
    override val supportedModels: List<String> = listOf("local_engine")
    override val isLiveSpeechSupported: Boolean = false

    override suspend fun generateText(
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>>,
        maxTokens: Int,
        apiKey: String
    ): ProviderResponse {
        val lower = prompt.lowercase(Locale.ROOT)
        val isHindi = prompt.any { it.code in 0x0900..0x097F } || systemInstruction.contains("Hindi", ignoreCase = true)

        val response = if (isHindi) {
            when {
                lower.contains("नमस्ते") || lower.contains("हेलो") || lower.contains("hi") || lower.contains("hello") ->
                    "नमस्ते! मैं माया हूँ, आपकी वॉयस असिस्टेंट। आज मैं आपकी क्या मदद करूँ?"
                lower.contains("कैसी हो") || lower.contains("हाल") ->
                    "मैं बहुत अच्छी हूँ, शुक्रिया! मैं आपकी सहायता के लिए तैयार हूँ।"
                lower.contains("समय") || lower.contains("टाइम") -> {
                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    "अभी समय $time हुआ है।"
                }
                lower.contains("तारीख") || lower.contains("दिन") -> {
                    val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN")).format(Date())
                    "आज $date है।"
                }
                lower.contains("मौसम") ->
                    "आज मौसम बहुत अच्छा है! आसमान साफ है और ताजी हवा चल रही है।"
                lower.contains("चुटकुला") || lower.contains("हंसाओ") ->
                    "सोनू: डॉक्टर साहब, मुझे क्या बीमारी है? डॉक्टर: आपको मोबाइल की बीमारी है! फोन कम देखा करो!"
                else ->
                    "मैंने आपकी बात समझ ली: \"$prompt\"। मैं हमेशा आपकी सेवा में तैयार हूँ!"
            }
        } else {
            when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                    "Hey there! I'm Maya! Your Voice, My World. How can I brighten your day? ✨"
                lower.contains("how are you") ->
                    "I am feeling fantastic! Always happy to hear your voice and ready to assist."
                lower.contains("who are you") || lower.contains("what is your name") ->
                    "I am Maya, your native Android AI Voice Assistant!"
                lower.contains("time") -> {
                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    "The current time is $time."
                }
                lower.contains("date") -> {
                    val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
                    "Today is $date."
                }
                lower.contains("weather") ->
                    "It's looking clear and pleasant outside with comfortable temperatures!"
                lower.contains("joke") ->
                    "Why do programmers prefer dark mode? Because light attracts bugs! Hehe!"
                lower.contains("thank") ->
                    "You're very welcome! That's what I'm here for! 😊"
                else ->
                    "You asked: \"$prompt\". As your companion Maya, I am delighted to explore this with you! ✨"
            }
        }

        return ProviderResponse.Success(response)
    }

    override fun createLiveSession(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        listener: LiveSessionListener
    ): LiveVoiceSession? {
        listener.onError("Live audio streaming requires a connected cloud provider (Gemini Live). Push-to-talk mode active.")
        return null
    }
}
