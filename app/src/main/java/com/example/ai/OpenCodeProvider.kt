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

class OpenCodeProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build(),
    private val defaultEndpoint: String = "https://opencode.ai/api/v1"
) : AiProvider {

    override val providerId: String = "opencode"
    override val displayName: String = "OpenCode"
    override val supportedModels: List<String> = listOf(
        "opencode-base",
        "opencode-code-70b",
        "opencode-assistant"
    )
    override val isLiveSpeechSupported: Boolean = false

    override suspend fun generateText(
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>>,
        maxTokens: Int,
        apiKey: String
    ): ProviderResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ProviderResponse.Error(
                "OpenCode API key is missing. Please configure it in AI Settings.",
                isRecoverable = false
            )
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
                put("model", "opencode-assistant")
                put("messages", messagesArray)
                put("max_tokens", maxTokens.coerceIn(50, 2048))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val cleanEndpoint = defaultEndpoint.trimEnd('/')
            val fullUrl = if (cleanEndpoint.endsWith("/chat/completions")) cleanEndpoint else "$cleanEndpoint/chat/completions"

            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "Maya-AI-Assistant/1.0")
                .post(bodyJson.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val err = when (code) {
                        401 -> "Invalid OpenCode API Key. Please verify credentials in AI Settings."
                        404 -> "OpenCode endpoint not found: $fullUrl"
                        429 -> "OpenCode rate limit exceeded. Please wait a few seconds."
                        else -> "OpenCode API returned HTTP $code"
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
                return@withContext ProviderResponse.Error("Empty response returned from OpenCode.")
            }
        } catch (e: Exception) {
            ProviderResponse.Error("Network error contacting OpenCode: ${e.localizedMessage}")
        }
    }

    override fun createLiveSession(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        listener: LiveSessionListener
    ): LiveVoiceSession? {
        listener.onError("Real-time live voice streaming is exclusive to Gemini Live. Push-to-talk voice is active for OpenCode.")
        return null
    }

    suspend fun testConnection(apiKey: String, endpoint: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "API Key is required to test OpenCode."
        try {
            val cleanEndpoint = endpoint.trimEnd('/')
            val fullUrl = if (cleanEndpoint.endsWith("/models")) cleanEndpoint else "$cleanEndpoint/models"
            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    "OpenCode connected successfully (HTTP ${response.code})."
                } else {
                    "OpenCode returned HTTP ${response.code}."
                }
            }
        } catch (e: Exception) {
            "OpenCode connection test failed: ${e.localizedMessage}"
        }
    }
}
