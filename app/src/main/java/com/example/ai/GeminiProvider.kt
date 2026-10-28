package com.example.ai

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val providerId: String = "google_gemini"
    override val displayName: String = "Gemini AI"
    override val supportedModels: List<String> = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-3.8-live", "Gemini Pro")
    override val isLiveSpeechSupported: Boolean = true

    override suspend fun generateText(
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>>,
        maxTokens: Int,
        apiKey: String
    ): ProviderResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ProviderResponse.Error("Gemini API key is not configured.", isRecoverable = false)
        }

        try {
            val contentsArray = JSONArray()

            // System instruction if any
            if (systemInstruction.isNotBlank()) {
                val sysContent = JSONObject().put("role", "user").put(
                    "parts", JSONArray().put(JSONObject().put("text", "System Instruction: $systemInstruction"))
                )
                contentsArray.put(sysContent)
                contentsArray.put(
                    JSONObject().put("role", "model").put(
                        "parts", JSONArray().put(JSONObject().put("text", "Understood. I will follow your instructions."))
                    )
                )
            }

            // History
            for ((sender, content) in history.takeLast(6)) {
                val role = if (sender == "USER") "user" else "model"
                contentsArray.put(
                    JSONObject().put("role", role).put(
                        "parts", JSONArray().put(JSONObject().put("text", content))
                    )
                )
            }

            // Current prompt
            contentsArray.put(
                JSONObject().put("role", "user").put(
                    "parts", JSONArray().put(JSONObject().put("text", prompt))
                )
            )

            val bodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().put("maxOutputTokens", maxTokens.coerceIn(50, 2048)))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val err = when (code) {
                        400 -> "Invalid API request format or model parameter."
                        401, 403 -> "Invalid or unauthorized Gemini API key. Please verify in AI Settings."
                        429 -> "Gemini API rate limit reached. Please wait a moment."
                        else -> "Gemini API responded with HTTP $code"
                    }
                    return@withContext ProviderResponse.Error(err)
                }

                val bodyStr = response.body?.string().orEmpty()
                val root = JSONObject(bodyStr)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val cand = candidates.getJSONObject(0)
                    val contentObj = cand.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text").trim()
                        return@withContext ProviderResponse.Success(text)
                    }
                }
                return@withContext ProviderResponse.Error("Empty candidate returned from Gemini API.")
            }
        } catch (e: Exception) {
            ProviderResponse.Error("Network error contacting Gemini: ${e.localizedMessage}")
        }
    }

    override fun createLiveSession(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        listener: LiveSessionListener
    ): LiveVoiceSession {
        return GeminiLiveSession(client, apiKey, modelName, systemInstruction, listener)
    }

    private class GeminiLiveSession(
        private val client: OkHttpClient,
        private val apiKey: String,
        private val modelName: String,
        private val systemInstruction: String,
        private val listener: LiveSessionListener
    ) : LiveVoiceSession {

        private val tag = "GeminiLiveSession"
        private var webSocket: WebSocket? = null
        private var isClosed = false
        private val scope = CoroutineScope(Dispatchers.IO)

        init {
            connect()
        }

        private fun connect() {
            val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
            val request = Request.Builder().url(url).build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(tag, "Gemini Live WebSocket opened")
                    // Send initial session setup message
                    try {
                        val setupMsg = JSONObject().apply {
                            put("setup", JSONObject().apply {
                                put("model", "models/gemini-2.0-flash-exp")
                                put("generationConfig", JSONObject().apply {
                                    put("responseModalities", JSONArray().put("AUDIO"))
                                    put("speechConfig", JSONObject().apply {
                                        put("voiceConfig", JSONObject().apply {
                                            put("prebuiltVoiceConfig", JSONObject().put("voiceName", "Aoede"))
                                        })
                                    })
                                })
                                if (systemInstruction.isNotBlank()) {
                                    put("systemInstruction", JSONObject().apply {
                                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                                    })
                                }
                            })
                        }
                        webSocket.send(setupMsg.toString())
                        listener.onConnected()
                    } catch (e: Exception) {
                        Log.e(tag, "Error sending setup message", e)
                        listener.onError("Live setup error: ${e.message}")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val root = JSONObject(text)
                        val serverContent = root.optJSONObject("serverContent")
                        if (serverContent != null) {
                            if (serverContent.optBoolean("interrupted")) {
                                listener.onInterrupted()
                            }
                            val modelTurn = serverContent.optJSONObject("modelTurn")
                            val parts = modelTurn?.optJSONArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    val part = parts.getJSONObject(i)
                                    val textPart = part.optString("text")
                                    if (textPart.isNotBlank()) {
                                        listener.onTextChunkReceived(textPart)
                                    }
                                    val inlineData = part.optJSONObject("inlineData")
                                    if (inlineData != null) {
                                        val dataBase64 = inlineData.optString("data")
                                        if (dataBase64.isNotBlank()) {
                                            val pcmAudio = Base64.decode(dataBase64, Base64.DEFAULT)
                                            listener.onAudioChunkReceived(pcmAudio)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to parse Live message: ${e.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(tag, "WebSocket failure: ${t.message}")
                    listener.onError("Live session disconnected: ${t.localizedMessage}")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onClosed()
                }
            })
        }

        override fun sendAudioChunk(audioPcm16: ByteArray) {
            if (isClosed) return
            scope.launch {
                try {
                    val base64Data = Base64.encodeToString(audioPcm16, Base64.NO_WRAP)
                    val realtimeInput = JSONObject().apply {
                        put("realtimeInput", JSONObject().apply {
                            put("mediaChunks", JSONArray().put(JSONObject().apply {
                                put("mimeType", "audio/pcm;rate=16000")
                                put("data", base64Data)
                            }))
                        })
                    }
                    webSocket?.send(realtimeInput.toString())
                } catch (e: Exception) {
                    Log.w(tag, "Error sending audio chunk", e)
                }
            }
        }

        override fun sendText(text: String) {
            if (isClosed) return
            scope.launch {
                try {
                    val clientContent = JSONObject().apply {
                        put("clientContent", JSONObject().apply {
                            put("turns", JSONArray().put(JSONObject().apply {
                                put("role", "user")
                                put("parts", JSONArray().put(JSONObject().put("text", text)))
                            }))
                            put("turnComplete", true)
                        })
                    }
                    webSocket?.send(clientContent.toString())
                } catch (e: Exception) {
                    Log.w(tag, "Error sending text", e)
                }
            }
        }

        override fun interrupt() {
            // Signal interruption to local audio and remote stream
            listener.onInterrupted()
        }

        override fun close() {
            isClosed = true
            webSocket?.close(1000, "Normal closure")
            webSocket = null
        }
    }
}
