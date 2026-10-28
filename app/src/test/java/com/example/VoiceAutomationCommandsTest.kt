package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.PlanStep
import com.example.agent.TaskPlanner
import com.example.ai.OpenCodeProvider
import com.example.ai.OpenRouterProvider
import com.example.ai.ProviderFactory
import com.example.android.AndroidActionExecutor
import com.example.android.AndroidExecutionResult
import com.example.android.AppResolver
import com.example.commands.ParsedVoiceCommand
import com.example.commands.VoiceCommandParser
import com.example.data.repository.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceAutomationCommandsTest {

    private lateinit var context: Context
    private val parser = VoiceCommandParser()
    private val planner = TaskPlanner(parser)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // Command 1: "Open YouTube"
    @Test
    fun `test command 1 - Open YouTube`() {
        val parsed = parser.parse("Open YouTube")
        assertTrue(parsed is ParsedVoiceCommand.LaunchApp)
        assertEquals("youtube", (parsed as ParsedVoiceCommand.LaunchApp).appKey)

        val plan = planner.createPlan("Open YouTube")
        assertEquals(1, plan.steps.size)
        assertEquals("app_launcher", plan.steps[0].toolName)
        assertEquals("youtube", plan.steps[0].parameters["app_key"])
    }

    // Command 2: "YouTube kholo" (Hindi/Hinglish)
    @Test
    fun `test command 2 - YouTube kholo`() {
        val parsed = parser.parse("YouTube kholo")
        assertTrue(parsed is ParsedVoiceCommand.LaunchApp)
        assertEquals("youtube", (parsed as ParsedVoiceCommand.LaunchApp).appKey)

        val plan = planner.createPlan("YouTube kholo")
        assertEquals(1, plan.steps.size)
        assertEquals("app_launcher", plan.steps[0].toolName)
        assertEquals("youtube", plan.steps[0].parameters["app_key"])
    }

    // Command 3: "Open Gallery"
    @Test
    fun `test command 3 - Open Gallery`() {
        val parsed = parser.parse("Open Gallery")
        assertTrue(parsed is ParsedVoiceCommand.LaunchApp)
        assertEquals("gallery", (parsed as ParsedVoiceCommand.LaunchApp).appKey)

        val plan = planner.createPlan("Open Gallery")
        assertEquals("app_launcher", plan.steps[0].toolName)
        assertEquals("gallery", plan.steps[0].parameters["app_key"])
    }

    // Command 4: "Open Calculator"
    @Test
    fun `test command 4 - Open Calculator`() {
        val parsed = parser.parse("Open Calculator")
        assertTrue(parsed is ParsedVoiceCommand.LaunchApp)
        assertEquals("calculator", (parsed as ParsedVoiceCommand.LaunchApp).appKey)

        val plan = planner.createPlan("Open Calculator")
        assertEquals("app_launcher", plan.steps[0].toolName)
        assertEquals("calculator", plan.steps[0].parameters["app_key"])
    }

    // Command 5: "Open Chrome"
    @Test
    fun `test command 5 - Open Chrome`() {
        val parsed = parser.parse("Open Chrome")
        assertTrue(parsed is ParsedVoiceCommand.LaunchApp)
        assertEquals("chrome", (parsed as ParsedVoiceCommand.LaunchApp).appKey)

        val plan = planner.createPlan("Open Chrome")
        assertEquals("app_launcher", plan.steps[0].toolName)
        assertEquals("chrome", plan.steps[0].parameters["app_key"])
    }

    // Command 6: "Open Settings"
    @Test
    fun `test command 6 - Open Settings`() {
        val parsed = parser.parse("Open Settings")
        assertTrue(parsed is ParsedVoiceCommand.LaunchApp)
        assertEquals("settings", (parsed as ParsedVoiceCommand.LaunchApp).appKey)

        val plan = planner.createPlan("Open Settings")
        assertEquals("app_launcher", plan.steps[0].toolName)
        assertEquals("settings", plan.steps[0].parameters["app_key"])
    }

    // Command 7: "Search Google for Kotlin tutorials"
    @Test
    fun `test command 7 - Search Google for Kotlin tutorials`() {
        val parsed = parser.parse("Search Google for Kotlin tutorials")
        assertTrue(parsed is ParsedVoiceCommand.WebSearch)
        assertEquals("Kotlin tutorials", (parsed as ParsedVoiceCommand.WebSearch).searchQuery)

        val plan = planner.createPlan("Search Google for Kotlin tutorials")
        assertEquals(1, plan.steps.size)
        assertEquals("web_search_action", plan.steps[0].toolName)
        assertEquals("Kotlin tutorials", plan.steps[0].parameters["query"])
    }

    // Command 8: "Set a timer for one minute"
    @Test
    fun `test command 8 - Set a timer for one minute`() {
        val parsed = parser.parse("Set a timer for one minute")
        assertTrue(parsed is ParsedVoiceCommand.SetTimer)
        assertEquals(60, (parsed as ParsedVoiceCommand.SetTimer).durationSeconds)

        val plan = planner.createPlan("Set a timer for one minute")
        assertEquals(1, plan.steps.size)
        assertEquals("system_timer", plan.steps[0].toolName)
        assertEquals("60", plan.steps[0].parameters["duration_seconds"])
    }

    // Command 9: "Answer this question in Hindi"
    @Test
    fun `test command 9 - Answer this question in Hindi`() {
        val parsed = parser.parse("Answer this question in Hindi")
        assertTrue(parsed is ParsedVoiceCommand.AnswerInLanguage)
        assertEquals("Hindi", (parsed as ParsedVoiceCommand.AnswerInLanguage).targetLanguage)

        val plan = planner.createPlan("Answer this question in Hindi")
        assertEquals(1, plan.steps.size)
        assertEquals("conversational_ai", plan.steps[0].toolName)
        assertEquals("Hindi", plan.steps[0].parameters["target_language"])
    }

    // Command 10: "Stop speaking"
    @Test
    fun `test command 10 - Stop speaking`() {
        val parsed = parser.parse("Stop speaking")
        assertTrue(parsed is ParsedVoiceCommand.StopSpeaking)

        val plan = planner.createPlan("Stop speaking")
        assertEquals(1, plan.steps.size)
        assertEquals("stop_speech", plan.steps[0].toolName)
    }

    // Hinglish and Other Apps
    @Test
    fun `test Hinglish and popular app commands`() {
        val calcHinglish = parser.parse("calculator kholo")
        assertTrue(calcHinglish is ParsedVoiceCommand.LaunchApp)
        assertEquals("calculator", (calcHinglish as ParsedVoiceCommand.LaunchApp).appKey)

        val waHinglish = parser.parse("WhatsApp kholo")
        assertTrue(waHinglish is ParsedVoiceCommand.LaunchApp)
        assertEquals("whatsapp", (waHinglish as ParsedVoiceCommand.LaunchApp).appKey)

        val cameraEng = parser.parse("Open Camera")
        assertTrue(cameraEng is ParsedVoiceCommand.LaunchApp)
        assertEquals("camera", (cameraEng as ParsedVoiceCommand.LaunchApp).appKey)

        val hindiStop = parser.parse("bolna band karo")
        assertTrue(hindiStop is ParsedVoiceCommand.StopSpeaking)
    }

    // Android Action Executor URL validation
    @Test
    fun `test android action executor safe url enforcement`() {
        val executor = AndroidActionExecutor(context)
        val safeResult = executor.openUrl("https://android.com")
        assertTrue(safeResult is AndroidExecutionResult.Success)

        val dangerousResult = executor.openUrl("javascript:alert(1)")
        assertTrue(dangerousResult is AndroidExecutionResult.Failure)
    }

    // OpenRouter Provider Configuration & Routing
    @Test
    fun `test OpenRouter provider routing and validation`() = runBlocking {
        val provider = OpenRouterProvider()
        assertEquals("openrouter", provider.providerId)
        assertTrue(provider.supportedModels.contains("meta-llama/llama-3.3-70b-instruct:free"))

        // When key is blank, it returns clear error
        val errResp = provider.generateText("Hello", "System", emptyList(), 100, "")
        assertTrue(errResp is com.example.ai.ProviderResponse.Error)
        assertTrue((errResp as com.example.ai.ProviderResponse.Error).message.contains("OpenRouter API key is missing"))

        val factory = ProviderFactory()
        val settings = AppSettings(aiModel = "Llama 3", openRouterApiKey = "sk-or-test")
        val resolved = factory.getProvider(settings, hasDefaultGeminiKey = false)
        assertEquals("openrouter", resolved.providerId)
    }

    // OpenCode Provider Configuration & Routing
    @Test
    fun `test OpenCode provider routing and validation`() = runBlocking {
        val provider = OpenCodeProvider()
        assertEquals("opencode", provider.providerId)
        assertTrue(provider.supportedModels.contains("opencode-assistant"))

        // When key is blank, it returns clear error
        val errResp = provider.generateText("Code", "System", emptyList(), 100, "")
        assertTrue(errResp is com.example.ai.ProviderResponse.Error)
        assertTrue((errResp as com.example.ai.ProviderResponse.Error).message.contains("OpenCode API key is missing"))

        val factory = ProviderFactory()
        val settings = AppSettings(aiModel = "OpenCode Assistant", openCodeApiKey = "opencode-key")
        val resolved = factory.getProvider(settings, hasDefaultGeminiKey = false)
        assertEquals("opencode", resolved.providerId)
    }

    // Accessibility Automation Tests
    @Test
    fun `test accessibility scroll up and down commands`() {
        val parsedUp = parser.parse("screen scroll up karo")
        assertTrue(parsedUp is ParsedVoiceCommand.Scroll)
        assertTrue((parsedUp as ParsedVoiceCommand.Scroll).scrollUp)

        val parsedDown = parser.parse("is page ko neeche scroll karo")
        assertTrue(parsedDown is ParsedVoiceCommand.Scroll)
        assertFalse((parsedDown as ParsedVoiceCommand.Scroll).scrollUp)
    }

    @Test
    fun `test accessibility tap and go back commands`() {
        val parsedTap = parser.parse("pehli video par tap karo")
        assertTrue(parsedTap is ParsedVoiceCommand.Tap)
        assertEquals("video", (parsedTap as ParsedVoiceCommand.Tap).label)

        val parsedBack = parser.parse("wapas jao")
        assertTrue(parsedBack is ParsedVoiceCommand.GoBack)
    }
}
