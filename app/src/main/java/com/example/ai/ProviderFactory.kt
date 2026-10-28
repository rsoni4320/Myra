package com.example.ai

class ProviderFactory(
    private val geminiProvider: GeminiProvider = GeminiProvider(),
    private val openAiProvider: OpenAiProvider = OpenAiProvider(),
    private val localOfflineProvider: LocalOfflineProvider = LocalOfflineProvider()
) {
    fun getProvider(modelName: String, hasApiKey: Boolean): AiProvider {
        if (!hasApiKey) {
            return localOfflineProvider
        }
        return when {
            modelName.contains("Gemini", ignoreCase = true) || modelName.contains("Live", ignoreCase = true) ->
                geminiProvider
            modelName.contains("GPT", ignoreCase = true) || modelName.contains("OpenAI", ignoreCase = true) ->
                openAiProvider
            else ->
                geminiProvider
        }
    }
}
