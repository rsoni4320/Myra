package com.example.agent

import java.util.Locale

enum class StepRiskLevel {
    LOW,        // Read-only queries, calculations, local formatting
    MEDIUM,     // Creating reminders, saving notes, changing non-destructive settings
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

class TaskPlanner {

    fun createPlan(userGoal: String): ExecutionPlan {
        val trimmed = userGoal.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val steps = mutableListOf<PlanStep>()

        when {
            // Reminders
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

            // Notes
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

            // Web Search
            lower.contains("search for") || lower.contains("google:") || lower.startsWith("search:") -> {
                val query = trimmed.replace(Regex("(?i)(search the web for|search for|search:|google:)"), "").trim()
                steps.add(
                    PlanStep(
                        stepId = "step_search_1",
                        description = "Query online sources for: \"$query\"",
                        toolName = "web_search",
                        parameters = mapOf("query" to query),
                        riskLevel = StepRiskLevel.LOW
                    )
                )
            }

            // Memory
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

            // Clear/Delete operations (High Risk - Requires Confirmation)
            lower.contains("clear all history") || lower.contains("delete chat") || lower.contains("clear memory") -> {
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
            }

            // General conversational query
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

        return ExecutionPlan(goal = trimmed, steps = steps)
    }
}
