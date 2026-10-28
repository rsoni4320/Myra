package com.example.ai

import com.example.data.repository.AppSettings

class ProviderFactory(
    private val geminiProvider: GeminiProvider = GeminiProvider(),
    private val openRouterProvider: OpenRouterProvider = OpenRouterProvider(),
    private val openCodeProvider: OpenCodeProvider = OpenCodeProvider(),
    private val openAiProvider: OpenAiProvider = OpenAiProvider(),
    private val localOfflineProvider: LocalOfflineProvider = LocalOfflineProvider()
) {
    fun getProvider(settings: AppSettings, hasDefaultGeminiKey: Boolean): AiProvider {
        val model = settings.aiModel

        return when {
            // OpenRouter selected
            model.contains("OpenRouter", ignoreCase = true) ||
            model.contains("Llama", ignoreCase = true) ||
            model.contains("DeepSeek", ignoreCase = true) ||
            model.contains("Mistral", ignoreCase = true) -> {
                if (settings.openRouterApiKey.isNotBlank() || settings.customApiKey.isNotBlank()) {
                    openRouterProvider
                } else {
                    localOfflineProvider
                }
            }

            // OpenCode selected
            model.contains("OpenCode", ignoreCase = true) -> {
                if (settings.openCodeApiKey.isNotBlank() || settings.customApiKey.isNotBlank()) {
                    openCodeProvider
                } else {
                    localOfflineProvider
                }
            }

            // OpenAI GPT-4o
            model.contains("GPT", ignoreCase = true) || model.contains("OpenAI", ignoreCase = true) -> {
                if (settings.customApiKey.isNotBlank()) {
                    openAiProvider
                } else if (settings.openRouterApiKey.isNotBlank()) {
                    openRouterProvider
                } else {
                    localOfflineProvider
                }
            }

            // Gemini Live / Gemini Pro / Default
            model.contains("Gemini", ignoreCase = true) || model.contains("Claude", ignoreCase = true) -> {
                if (settings.customApiKey.isNotBlank() || hasDefaultGeminiKey) {
                    geminiProvider
                } else if (settings.openRouterApiKey.isNotBlank()) {
                    openRouterProvider
                } else {
                    localOfflineProvider
                }
            }

            else -> {
                if (settings.openRouterApiKey.isNotBlank()) openRouterProvider
                else if (settings.customApiKey.isNotBlank() || hasDefaultGeminiKey) geminiProvider
                else localOfflineProvider
            }
        }
    }

    fun getActiveApiKey(settings: AppSettings, provider: AiProvider, buildConfigGeminiKey: String): String {
        return when (provider.providerId) {
            "openrouter" -> {
                if (settings.openRouterApiKey.isNotBlank()) settings.openRouterApiKey.trim()
                else settings.customApiKey.trim()
            }
            "opencode" -> {
                if (settings.openCodeApiKey.isNotBlank()) settings.openCodeApiKey.trim()
                else settings.customApiKey.trim()
            }
            "openai" -> settings.customApiKey.trim()
            "gemini" -> {
                if (settings.customApiKey.isNotBlank()) settings.customApiKey.trim()
                else buildConfigGeminiKey.trim()
            }
            else -> ""
        }
    }
}
