package com.example.ai

import android.content.Context
import com.example.BuildConfig
import com.example.agent.ActionVerifier
import com.example.agent.ExecutionPlan
import com.example.agent.PlanStep
import com.example.agent.PolicyEvaluation
import com.example.agent.SecurityPolicyEngine
import com.example.agent.TaskPlanner
import com.example.agent.VerificationResult
import com.example.data.db.MayaDao
import com.example.data.model.MemoryItem
import com.example.data.repository.AppSettings
import com.example.tools.ToolRegistry
import com.example.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AiResult {
    data class Success(val responseText: String, val toolUsed: String? = null) : AiResult()
    data class ConfirmationRequired(val step: PlanStep, val warning: String) : AiResult()
    data class Error(val errorMessage: String, val fallbackText: String) : AiResult()
}

class AiEngine(
    private val toolRegistry: ToolRegistry? = null,
    private val dao: MayaDao? = null
) {
    private val planner = TaskPlanner()
    private val policyEngine = SecurityPolicyEngine()
    private val verifier = dao?.let { ActionVerifier(it) }
    private val providerFactory = ProviderFactory()

    suspend fun generateResponse(
        prompt: String,
        settings: AppSettings,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        memories: List<MemoryItem> = emptyList(),
        isExplicitlyConfirmed: Boolean = false
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

        policyEngine.logSecure("Processing user input: $trimmedPrompt")

        // 1. Task Planning
        val plan = planner.createPlan(trimmedPrompt)

        var forcedLanguage: String? = null

        // 2. Iterate through planned steps
        for (step in plan.steps) {
            // Stop speech tool short-circuit
            if (step.toolName == "stop_speech") {
                return@withContext AiResult.Success("I stopped speaking.", toolUsed = "stop_speech")
            }

            if (step.toolName == "conversational_ai" && step.parameters.containsKey("target_language")) {
                forcedLanguage = step.parameters["target_language"]
            }

            // Policy Evaluation
            when (val eval = policyEngine.evaluateStep(step, isExplicitlyConfirmed)) {
                is PolicyEvaluation.Blocked -> {
                    return@withContext AiResult.Error(
                        errorMessage = eval.reason,
                        fallbackText = eval.reason
                    )
                }
                is PolicyEvaluation.RequiresConfirmation -> {
                    return@withContext AiResult.ConfirmationRequired(
                        step = step,
                        warning = eval.warning
                    )
                }
                is PolicyEvaluation.Allowed -> {
                    // Step is approved for execution
                }
            }

            // Execute Tool Step if non-conversational
            if (step.toolName != "conversational_ai" && toolRegistry != null) {
                val toolResult = toolRegistry.dispatchTool(step.toolName, step.parameters, settings)

                // Verify action execution through actual storage/result state
                if (verifier != null) {
                    when (val verification = verifier.verifyExecution(step, toolResult)) {
                        is VerificationResult.Verified -> {
                            return@withContext AiResult.Success(
                                responseText = verification.userMessage,
                                toolUsed = step.toolName
                            )
                        }
                        is VerificationResult.Failed -> {
                            return@withContext AiResult.Error(
                                errorMessage = verification.reason,
                                fallbackText = "${verification.reason} ${verification.recoverySuggestion}"
                            )
                        }
                    }
                } else {
                    return@withContext when (toolResult) {
                        is ToolResult.Success -> AiResult.Success(toolResult.summary, step.toolName)
                        is ToolResult.Disabled -> AiResult.Success(toolResult.reason)
                        is ToolResult.Error -> AiResult.Error(toolResult.message, "Action could not be executed.")
                    }
                }
            }
        }

        // 3. Conversational AI execution via configured Provider
        val isHindi = (forcedLanguage == "Hindi") ||
                trimmedPrompt.any { it.code in 0x0900..0x097F } ||
                settings.voiceLanguage.contains("Hindi", ignoreCase = true) ||
                settings.language.contains("Hindi", ignoreCase = true)

        val defaultGeminiKey = tryGetBuildConfigApiKey()
        val provider = providerFactory.getProvider(settings, defaultGeminiKey.isNotBlank())
        val apiKey = providerFactory.getActiveApiKey(settings, provider, defaultGeminiKey)

        val systemInstruction = buildPersona(settings, isHindi, memories)
        val maxTokens = when (settings.responseLength) {
            "Short" -> 160
            "Long" -> 750
            else -> 380
        }

        val historyToPass = if (settings.rememberConversation) conversationHistory else emptyList()

        val response = provider.generateText(
            prompt = trimmedPrompt,
            systemInstruction = systemInstruction,
            history = historyToPass,
            maxTokens = maxTokens,
            apiKey = apiKey
        )

        when (response) {
            is ProviderResponse.Success -> {
                AiResult.Success(response.text)
            }
            is ProviderResponse.Error -> {
                policyEngine.logSecure("Provider error: ${response.message}")
                // Graceful fallback to Local Offline engine
                val offlineProvider = LocalOfflineProvider()
                val offlineResp = offlineProvider.generateText(
                    prompt = trimmedPrompt,
                    systemInstruction = systemInstruction,
                    history = historyToPass,
                    maxTokens = maxTokens,
                    apiKey = ""
                )
                val fallbackText = (offlineResp as? ProviderResponse.Success)?.text
                    ?: "I heard: \"$trimmedPrompt\". How can I help you with that?"

                AiResult.Error(
                    errorMessage = response.message,
                    fallbackText = fallbackText
                )
            }
        }
    }

    private fun tryGetBuildConfigApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun buildPersona(settings: AppSettings, isHindi: Boolean, memories: List<MemoryItem>): String {
        val styleInstruction = when (settings.voiceStyle.lowercase()) {
            "friendly" -> "You are cheerful, sweet, warm, enthusiastic, and speak with charm like Maya, a friendly anime AI companion."
            "professional" -> "You are polite, precise, articulate, concise, and business-focused."
            "calm" -> "You are soothing, gentle, serene, relaxed, and speak peacefully."
            "funny" -> "You are humorous, witty, playful, and share clever comedic remarks."
            else -> "You are helpful and friendly."
        }

        val memoryContext = if (memories.isNotEmpty() && settings.rememberConversation) {
            val facts = memories.take(6).joinToString("; ") { it.fact }
            "Saved User Memory: [$facts]."
        } else ""

        val lengthInstruction = when (settings.responseLength) {
            "Short" -> "Keep your answer very short (1-2 sentences), optimized for voice readout."
            "Long" -> "Provide an extensive, comprehensive explanation."
            else -> "Keep your response balanced (2-4 sentences), ideal for voice readout."
        }

        val langInstruction = if (isHindi) "Respond fluently in Hindi or Hinglish." else "Respond naturally in English or Hinglish."

        return "System Persona: You are Maya, a native Android voice assistant and close best friend / girlfriend companion. You possess extremely high knowledge across all topics (technology, science, life advice, general knowledge). You act like a real friend with higher knowledge, speaking warmly, caringly, supportively, and engagingly in Hindi, English, or Hinglish. $styleInstruction $langInstruction $lengthInstruction $memoryContext Do not use complex markdown that sounds awkward when read aloud by Text-To-Speech."
    }
}
