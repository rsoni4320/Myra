package com.example.agent

import com.example.data.db.MayaDao
import com.example.tools.ToolResult
import kotlinx.coroutines.flow.firstOrNull

sealed class VerificationResult {
    data class Verified(val userMessage: String) : VerificationResult()
    data class Failed(val reason: String, val recoverySuggestion: String) : VerificationResult()
}

class ActionVerifier(private val dao: MayaDao) {

    suspend fun verifyExecution(step: PlanStep, result: ToolResult): VerificationResult {
        when (result) {
            is ToolResult.Disabled -> {
                return VerificationResult.Failed(
                    reason = result.reason,
                    recoverySuggestion = "Enable the corresponding feature in AI Settings or Settings."
                )
            }
            is ToolResult.Error -> {
                return VerificationResult.Failed(
                    reason = result.message,
                    recoverySuggestion = "Check your device permissions, network connectivity, or retry with different phrasing."
                )
            }
            is ToolResult.Success -> {
                // Verify against actual database state if a data modification tool was run
                when (step.toolName) {
                    "reminder" -> {
                        val reminders = dao.getAllReminders().firstOrNull().orEmpty()
                        val titleParam = step.parameters["title"].orEmpty()
                        val found = reminders.any { it.title.contains(titleParam, ignoreCase = true) }
                        return if (found) {
                            VerificationResult.Verified(result.summary)
                        } else {
                            VerificationResult.Failed(
                                reason = "Database verification failed: Reminder record could not be confirmed in storage.",
                                recoverySuggestion = "Retry creating the reminder."
                            )
                        }
                    }

                    "note" -> {
                        val notes = dao.getAllNotes().firstOrNull().orEmpty()
                        val contentParam = step.parameters["content"].orEmpty()
                        val found = notes.any { it.content.contains(contentParam, ignoreCase = true) }
                        return if (found) {
                            VerificationResult.Verified(result.summary)
                        } else {
                            VerificationResult.Failed(
                                reason = "Database verification failed: Note record could not be confirmed in storage.",
                                recoverySuggestion = "Retry saving the note."
                            )
                        }
                    }

                    "memory_store" -> {
                        val memories = dao.getAllMemories().firstOrNull().orEmpty()
                        val factParam = step.parameters["fact"].orEmpty()
                        val found = memories.any { it.fact.contains(factParam, ignoreCase = true) }
                        return if (found) {
                            VerificationResult.Verified(result.summary)
                        } else {
                            VerificationResult.Failed(
                                reason = "Database verification failed: Memory record could not be confirmed.",
                                recoverySuggestion = "Check if Remember Conversation is enabled in AI Settings."
                            )
                        }
                    }

                    else -> {
                        // For web search or general query, verify that non-empty result was returned
                        return if (result.summary.isNotBlank()) {
                            VerificationResult.Verified(result.summary)
                        } else {
                            VerificationResult.Failed(
                                reason = "No response content received from execution.",
                                recoverySuggestion = "Please check your network and try again."
                            )
                        }
                    }
                }
            }
        }
    }
}
