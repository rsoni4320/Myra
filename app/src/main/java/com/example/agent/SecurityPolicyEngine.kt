package com.example.agent

import android.util.Log

sealed class PolicyEvaluation {
    object Allowed : PolicyEvaluation()
    data class RequiresConfirmation(val warning: String, val step: PlanStep) : PolicyEvaluation()
    data class Blocked(val reason: String) : PolicyEvaluation()
}

class SecurityPolicyEngine {
    private val tag = "SecurityPolicy"
    private var lastActionTimestamp = 0L
    private var actionCountInWindow = 0
    private val rateLimitWindowMs = 60000L
    private val maxActionsPerWindow = 25

    fun evaluateStep(step: PlanStep, userExplicitlyConfirmed: Boolean): PolicyEvaluation {
        // 1. Rate Limiting Check
        val now = System.currentTimeMillis()
        if (now - lastActionTimestamp > rateLimitWindowMs) {
            actionCountInWindow = 0
            lastActionTimestamp = now
        }
        actionCountInWindow++
        if (actionCountInWindow > maxActionsPerWindow) {
            return PolicyEvaluation.Blocked("Action rate limit exceeded. Please wait a minute before making more automated requests.")
        }

        // 2. Sensitive / Irreversible Action Confirmation Check
        if (step.requiresUserConfirmation && !userExplicitlyConfirmed) {
            val warning = when (step.riskLevel) {
                StepRiskLevel.HIGH -> "Warning: This action (\"${step.description}\") is irreversible or affects sensitive data. Do you wish to proceed?"
                else -> "Please confirm you wish to execute: ${step.description}"
            }
            return PolicyEvaluation.RequiresConfirmation(warning, step)
        }

        // 3. Argument Validation & Sanitization
        for ((key, value) in step.parameters) {
            if (value.length > 5000) {
                return PolicyEvaluation.Blocked("Parameter '$key' exceeds maximum permissible length of 5000 characters.")
            }
        }

        return PolicyEvaluation.Allowed
    }

    /**
     * Redacts any detected API keys, passwords, or tokens from logs or error outputs.
     */
    fun sanitizeForLogging(text: String): String {
        return text
            .replace(Regex("AIza[0-9A-Za-z-_]{30,}"), "[REDACTED]")
            .replace(Regex("sk-[a-zA-Z0-9_-]{15,}"), "[REDACTED]")
            .replace(Regex("(?i)(key|token|password)=[^&\\s]+"), "$1=[REDACTED]")
    }

    fun logSecure(message: String) {
        Log.i(tag, sanitizeForLogging(message))
    }
}
