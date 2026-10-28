package com.example.agent

import com.example.commands.ParsedVoiceCommand
import com.example.commands.VoiceCommandParser
import java.util.Locale

enum class StepRiskLevel {
    LOW,        // Read-only queries, calculations, local formatting, app launch
    MEDIUM,     // Creating reminders, saving notes, changing non-destructive settings, timers, alarms
    HIGH        // Deleting data, clearing history, sending external communication, irreversible actions
}

data class PlanStep(
    val stepId: String,
    val description: String,
    val toolName: String,
    val parameters: Map<String, String>,
    val riskLevel: StepRiskLevel,
    val requiresUserConfirmation: Boolean = riskLevel == StepRiskLevel.HIGH
)

data class ExecutionPlan(
    val goal: String,
    val steps: List<PlanStep>,
    val requiresConfirmation: Boolean = steps.any { it.requiresUserConfirmation }
)

class TaskPlanner(
    private val voiceCommandParser: VoiceCommandParser = VoiceCommandParser()
) {

    fun createPlan(userGoal: String): ExecutionPlan {
        val trimmed = userGoal.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val steps = mutableListOf<PlanStep>()

        // 1. High Risk operations (Explicit Data Clearing / Deletion)
        if (lower.contains("clear all history") || lower.contains("delete chat") || lower.contains("clear memory")) {
            steps.add(
                PlanStep(
                    stepId = "step_destructive_1",
                    description = "Clear all stored conversations from device database",
                    toolName = "clear_history",
                    parameters = mapOf("scope" to "all"),
                    riskLevel = StepRiskLevel.HIGH,
                    requiresUserConfirmation = true
                )
            )
            return ExecutionPlan(goal = trimmed, steps = steps)
        }

        // 2. Parse via VoiceCommandParser
        when (val parsed = voiceCommandParser.parse(trimmed)) {
            is ParsedVoiceCommand.StopSpeaking -> {
                steps.add(
                    PlanStep(
                        stepId = "step_stop_1",
                        description = "Halt all active voice readout and audio synthesis",
                        toolName = "stop_speech",
                        parameters = emptyMap(),
                        riskLevel = StepRiskLevel.LOW
                    )
                )
            }

            is ParsedVoiceCommand.LaunchApp -> {
                steps.add(
                    PlanStep(
                        stepId = "step_launch_1",
                        description = "Launch application \"${parsed.appKey}\"",
                        toolName = "app_launcher",
                        parameters = mapOf("app_key" to parsed.appKey, "raw_query" to parsed.rawAppQuery),
                        riskLevel = StepRiskLevel.LOW
                    )
                )
            }

            is ParsedVoiceCommand.WebSearch -> {
                steps.add(
                    PlanStep(
                        stepId = "step_search_1",
                        description = "Search Google for: \"${parsed.searchQuery}\"",
                        toolName = "web_search_action",
                        parameters = mapOf("query" to parsed.searchQuery),
                        riskLevel = StepRiskLevel.LOW
                    )
                )
            }

            is ParsedVoiceCommand.OpenUrl -> {
                steps.add(
                    PlanStep(
                        stepId = "step_open_url_1",
                        description = "Open web URL: \"${parsed.url}\"",
                        toolName = "open_url",
                        parameters = mapOf("url" to parsed.url),
                        riskLevel = StepRiskLevel.LOW
                    )
                )
            }

            is ParsedVoiceCommand.SetTimer -> {
                steps.add(
                    PlanStep(
                        stepId = "step_timer_1",
                        description = "Set countdown timer for ${parsed.durationSeconds} seconds",
                        toolName = "system_timer",
                        parameters = mapOf(
                            "duration_seconds" to parsed.durationSeconds.toString(),
                            "raw_text" to parsed.rawText
                        ),
                        riskLevel = StepRiskLevel.MEDIUM
                    )
                )
            }

            is ParsedVoiceCommand.SetAlarm -> {
                steps.add(
                    PlanStep(
                        stepId = "step_alarm_1",
                        description = "Set system alarm for ${parsed.hour}:${String.format(Locale.ROOT, "%02d", parsed.minute)}",
                        toolName = "system_alarm",
                        parameters = mapOf(
                            "hour" to parsed.hour.toString(),
                            "minute" to parsed.minute.toString(),
                            "raw_text" to parsed.rawText
                        ),
                        riskLevel = StepRiskLevel.MEDIUM
                    )
                )
            }

            is ParsedVoiceCommand.AnswerInLanguage -> {
                steps.add(
                    PlanStep(
                        stepId = "step_conversation_1",
                        description = "Answer query in ${parsed.targetLanguage}",
                        toolName = "conversational_ai",
                        parameters = mapOf(
                            "prompt" to parsed.prompt,
                            "target_language" to parsed.targetLanguage
                        ),
                        riskLevel = StepRiskLevel.LOW
                    )
                )
            }

            is ParsedVoiceCommand.Conversational -> {
                // Secondary check for Reminders, Notes, and Memory before conversational fallback
                when {
                    lower.contains("remind me") || lower.contains("set reminder") -> {
                        val task = trimmed.replace(Regex("(?i)(remind me to|set a reminder to|set reminder)"), "").trim()
                        steps.add(
                            PlanStep(
                                stepId = "step_reminder_1",
                                description = "Schedule reminder for \"$task\"",
                                toolName = "reminder",
                                parameters = mapOf("title" to task),
                                riskLevel = StepRiskLevel.MEDIUM
                            )
                        )
                    }

                    lower.contains("take note") || lower.contains("note down") || lower.startsWith("note:") -> {
                        val content = trimmed.replace(Regex("(?i)(take a note|take note|note down|note:)"), "").trim()
                        steps.add(
                            PlanStep(
                                stepId = "step_note_1",
                                description = "Save note \"$content\"",
                                toolName = "note",
                                parameters = mapOf("title" to "Quick Note", "content" to content),
                                riskLevel = StepRiskLevel.MEDIUM
                            )
                        )
                    }

                    lower.contains("remember that") || lower.startsWith("remember:") -> {
                        val fact = trimmed.replace(Regex("(?i)(remember that|remember:)"), "").trim()
                        steps.add(
                            PlanStep(
                                stepId = "step_memory_1",
                                description = "Store personal memory fact: \"$fact\"",
                                toolName = "memory_store",
                                parameters = mapOf("fact" to fact, "action" to "save"),
                                riskLevel = StepRiskLevel.MEDIUM
                            )
                        )
                    }

                    else -> {
                        steps.add(
                            PlanStep(
                                stepId = "step_conversation_1",
                                description = "Synthesize conversational answer for: \"$trimmed\"",
                                toolName = "conversational_ai",
                                parameters = mapOf("prompt" to trimmed),
                                riskLevel = StepRiskLevel.LOW
                            )
                        )
                    }
                }
            }
        }

        return ExecutionPlan(goal = trimmed, steps = steps)
    }
}
