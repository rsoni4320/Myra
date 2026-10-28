package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.ActionVerifier
import com.example.agent.PlanStep
import com.example.agent.PolicyEvaluation
import com.example.agent.SecurityPolicyEngine
import com.example.agent.StepRiskLevel
import com.example.agent.TaskPlanner
import com.example.agent.VerificationResult
import com.example.ai.AiEngine
import com.example.ai.AiResult
import com.example.ai.LocalOfflineProvider
import com.example.ai.ProviderResponse
import com.example.data.db.MayaDatabase
import com.example.data.model.Reminder
import com.example.data.repository.AppSettings
import com.example.data.repository.SettingsRepository
import com.example.tools.AssistantTool
import com.example.tools.MemoryStoreTool
import com.example.tools.ReminderTool
import com.example.tools.ToolRegistry
import com.example.tools.ToolResult
import com.example.tools.WebSearchTool
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.room.Room

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: MayaDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MayaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `read string from context verifies Maya app name`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Maya", appName)
    }

    // 1. Task Planning Tests
    @Test
    fun `task planner plans reminder with medium risk`() {
        val planner = TaskPlanner()
        val plan = planner.createPlan("Remind me to buy groceries")
        assertEquals(1, plan.steps.size)
        val step = plan.steps[0]
        assertEquals("reminder", step.toolName)
        assertEquals(StepRiskLevel.MEDIUM, step.riskLevel)
        assertFalse(step.requiresUserConfirmation)
    }

    @Test
    fun `task planner flags destructive action with high risk and required confirmation`() {
        val planner = TaskPlanner()
        val plan = planner.createPlan("Clear all history")
        assertEquals(1, plan.steps.size)
        val step = plan.steps[0]
        assertEquals(StepRiskLevel.HIGH, step.riskLevel)
        assertTrue(step.requiresUserConfirmation)
        assertTrue(plan.requiresConfirmation)
    }

    // 2. Security Policy Engine Tests
    @Test
    fun `policy engine blocks unconfirmed high-risk action`() {
        val policy = SecurityPolicyEngine()
        val highRiskStep = PlanStep(
            stepId = "1",
            description = "Delete all databases",
            toolName = "clear_history",
            parameters = emptyMap(),
            riskLevel = StepRiskLevel.HIGH,
            requiresUserConfirmation = true
        )

        val evalUnconfirmed = policy.evaluateStep(highRiskStep, userExplicitlyConfirmed = false)
        assertTrue(evalUnconfirmed is PolicyEvaluation.RequiresConfirmation)

        val evalConfirmed = policy.evaluateStep(highRiskStep, userExplicitlyConfirmed = true)
        assertTrue(evalConfirmed is PolicyEvaluation.Allowed)
    }

    @Test
    fun `policy engine redacts secrets from logs`() {
        val policy = SecurityPolicyEngine()
        val raw = "Calling API with AIzaSyD12345678901234567890123456789012 and sk-proj-1234567890abcdef1234567890"
        val sanitized = policy.sanitizeForLogging(raw)
        assertFalse("Raw API key must not be present", sanitized.contains("AIzaSyD"))
        assertFalse("Raw OpenAI key must not be present", sanitized.contains("sk-proj-"))
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    // 3. Tool Validation & Feature Toggles Tests
    @Test
    fun `web search tool returns disabled when setting is off`() = runBlocking {
        val tool = WebSearchTool(OkHttpClient())
        val dao = database.mayaDao()
        val settings = AppSettings(webSearch = false)

        val result = tool.execute(mapOf("query" to "Android Compose"), context, dao, settings)
        assertTrue(result is ToolResult.Disabled)
    }

    @Test
    fun `memory store tool saves fact and checks disabled setting`() = runBlocking {
        val tool = MemoryStoreTool()
        val dao = database.mayaDao()
        val settingsDisabled = AppSettings(rememberConversation = false)

        val disabledRes = tool.execute(mapOf("fact" to "I like tea", "action" to "save"), context, dao, settingsDisabled)
        assertTrue(disabledRes is ToolResult.Disabled)

        val settingsEnabled = AppSettings(rememberConversation = true)
        val enabledRes = tool.execute(mapOf("fact" to "I like tea", "action" to "save"), context, dao, settingsEnabled)
        assertTrue(enabledRes is ToolResult.Success)
    }

    // 4. Action Verifier Tests
    @Test
    fun `action verifier confirms database state for reminders`() = runBlocking {
        val dao = database.mayaDao()
        val verifier = ActionVerifier(dao)

        val step = PlanStep(
            stepId = "rem_1",
            description = "Doctor appointment",
            toolName = "reminder",
            parameters = mapOf("title" to "Doctor appointment"),
            riskLevel = StepRiskLevel.MEDIUM
        )

        // Before record is inserted, verification fails
        val preVerification = verifier.verifyExecution(step, ToolResult.Success("Created"))
        assertTrue(preVerification is VerificationResult.Failed)

        // Insert record
        dao.insertReminder(Reminder(title = "Doctor appointment", timeLabel = "10:00 AM"))

        // After record is inserted, verification succeeds
        val postVerification = verifier.verifyExecution(step, ToolResult.Success("Created"))
        assertTrue(postVerification is VerificationResult.Verified)
    }

    // 5. Provider Failure & Offline Engine Tests
    @Test
    fun `local offline provider responds gracefully without internet`() = runBlocking {
        val provider = LocalOfflineProvider()
        val resp = provider.generateText(
            prompt = "What time is it?",
            systemInstruction = "You are Maya",
            history = emptyList(),
            maxTokens = 100,
            apiKey = ""
        )
        assertTrue(resp is ProviderResponse.Success)
        val text = (resp as ProviderResponse.Success).text
        assertTrue(text.contains("time"))
    }

    @Test
    fun `ai engine responds to english and hindi greetings`() = runBlocking {
        val dao = database.mayaDao()
        val registry = ToolRegistry(context, dao)
        val engine = AiEngine(registry, dao)
        val settings = AppSettings(voiceStyle = "Friendly")

        val resultEng = engine.generateResponse("Hello Maya!", settings)
        assertTrue(resultEng is AiResult.Success)

        val resultHindi = engine.generateResponse("नमस्ते माया", settings.copy(voiceLanguage = "Hindi (हिंदी)"))
        assertTrue(resultHindi is AiResult.Success)
    }
}
