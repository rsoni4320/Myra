package com.example.ai

sealed class ProviderResponse {
    data class Success(val text: String, val tokensUsed: Int = 0) : ProviderResponse()
    data class Error(val message: String, val isRecoverable: Boolean = true) : ProviderResponse()
}

interface LiveSessionListener {
    fun onConnected()
    fun onAudioChunkReceived(audioData: ByteArray)
    fun onTextChunkReceived(textChunk: String)
    fun onInterrupted()
    fun onError(errorMessage: String)
    fun onClosed()
}

interface LiveVoiceSession {
    fun sendAudioChunk(audioPcm16: ByteArray)
    fun sendText(text: String)
    fun interrupt()
    fun close()
}

interface AiProvider {
    val providerId: String
    val displayName: String
    val supportedModels: List<String>
    val isLiveSpeechSupported: Boolean

    suspend fun generateText(
        prompt: String,
        systemInstruction: String,
        history: List<Pair<String, String>>,
        maxTokens: Int,
        apiKey: String
    ): ProviderResponse

    fun createLiveSession(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        listener: LiveSessionListener
    ): LiveVoiceSession?
}
