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

class OpenRouterProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val providerId: String = "openrouter"
    override val displayName: String = "OpenRouter"
    override val supportedModels: List<String> = listOf(
        "meta-llama/llama-3.3-70b-instruct:free",
        "google/gemini-2.0-flash-exp:free",
        "openrouter/auto",
        "deepseek/deepseek-r1:free",
        "mistralai/mistral-7b-instruct:free"
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
                "OpenRouter API key is missing. Please enter your OpenRouter key in AI Settings.",
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
                put("model", "meta-llama/llama-3.3-70b-instruct:free")
                put("messages", messagesArray)
                put("max_tokens", maxTokens.coerceIn(50, 2048))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/maya-ai")
                .header("X-Title", "Maya AI Virtual Assistant")
                .post(bodyJson.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val err = when (code) {
                        401 -> "Invalid OpenRouter API Key. Please verify your credentials."
                        402 -> "OpenRouter credit balance depleted or insufficient."
                        429 -> "OpenRouter rate limit reached. Please wait a moment or try another model."
                        else -> "OpenRouter API responded with HTTP error $code"
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
                return@withContext ProviderResponse.Error("Empty choice returned from OpenRouter.")
            }
        } catch (e: Exception) {
            ProviderResponse.Error("Network error contacting OpenRouter: ${e.localizedMessage}")
        }
    }

    override fun createLiveSession(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        listener: LiveSessionListener
    ): LiveVoiceSession? {
        listener.onError("Real-time live voice streaming is exclusive to Gemini Live. Push-to-talk voice is active for OpenRouter.")
        return null
    }

    suspend fun fetchAvailableModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext supportedModels
        try {
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/maya-ai")
                .header("X-Title", "Maya AI Virtual Assistant")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext supportedModels
                val bodyStr = response.body?.string().orEmpty()
                val root = JSONObject(bodyStr)
                val data = root.optJSONArray("data") ?: return@withContext supportedModels
                val list = mutableListOf<String>()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val id = item.optString("id")
                    if (id.isNotBlank()) list.add(id)
                }
                if (list.isNotEmpty()) list.take(20) else supportedModels
            }
        } catch (_: Exception) {
            supportedModels
        }
    }
}
