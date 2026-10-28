package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val providerId: String = "openai"
    override val displayName: String = "OpenAI"
    override val supportedModels: List<String> = listOf("GPT-4o", "gpt-4o", "gpt-4o-mini")
    override val isLiveSpeechSupported: Boolean = false

    override suspend fun generateText(
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>>,
        maxTokens: Int,
        apiKey: String
    ): ProviderResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ProviderResponse.Error("OpenAI API key is missing. Please configure it in AI Settings.", isRecoverable = false)
        }

        try {
            val messagesArray = JSONArray()

            if (systemInstruction.isNotBlank()) {
                messagesArray.put(JSONObject().put("role", "system").put("content", systemInstruction))
            }

            for ((sender, content) in history.takeLast(6)) {
                val role = if (sender == "USER") "user" else "assistant"
                messagesArray.put(JSONObject().put("role", role).put("content", content))
            }

            messagesArray.put(JSONObject().put("role", "user").put("content", prompt))

            val bodyJson = JSONObject().apply {
                put("model", "gpt-4o")
                put("messages", messagesArray)
                put("max_tokens", maxTokens.coerceIn(50, 2048))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(bodyJson.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val err = when (code) {
                        401 -> "Invalid OpenAI API Key. Please verify your credentials."
                        429 -> "OpenAI quota exceeded or rate limit reached."
                        else -> "OpenAI API responded with HTTP error $code"
                    }
                    return@withContext ProviderResponse.Error(err)
                }

                val bodyStr = response.body?.string().orEmpty()
                val root = JSONObject(bodyStr)
                val choices = root.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.optJSONObject("message")
                    val content = message?.optString("content").orEmpty().trim()
                    return@withContext ProviderResponse.Success(content)
                }
                return@withContext ProviderResponse.Error("Empty choice returned from OpenAI.")
            }
        } catch (e: Exception) {
            ProviderResponse.Error("Network error contacting OpenAI: ${e.localizedMessage}")
        }
    }

    override fun createLiveSession(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        listener: LiveSessionListener
    ): LiveVoiceSession? {
        listener.onError("Real-time live voice streaming is currently exclusive to Gemini Live (gemini-3.8-live). Push-to-talk fallback active for GPT-4o.")
        return null
    }
}
