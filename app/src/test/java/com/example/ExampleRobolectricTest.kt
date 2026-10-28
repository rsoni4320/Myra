package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiEngine
import com.example.ai.AiResult
import com.example.data.repository.AppSettings
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies Maya app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Maya", appName)
    }

    @Test
    fun `ai engine responds to english greetings`() = runBlocking {
        val engine = AiEngine()
        val settings = AppSettings(voiceStyle = "Friendly")
        val result = engine.generateResponse("Hello Maya!", settings)
        assertTrue(result is AiResult.Success)
        val text = (result as AiResult.Success).responseText
        assertTrue("Expected greeting response, got: $text", text.contains("Maya") || text.contains("Hello"))
    }

    @Test
    fun `ai engine responds to hindi greetings`() = runBlocking {
        val engine = AiEngine()
        val settings = AppSettings(voiceLanguage = "Hindi (हिंदी)")
        val result = engine.generateResponse("नमस्ते माया", settings)
        assertTrue(result is AiResult.Success)
        val text = (result as AiResult.Success).responseText
        assertTrue("Expected Hindi response, got: $text", text.contains("नमस्ते") || text.contains("माया"))
    }

    @Test
    fun `settings repository persists preferences correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)

        val updated = repo.settings.value.copy(
            voiceStyle = "Funny",
            speechSpeed = 1.3f,
            voiceLanguage = "Hindi (हिंदी)"
        )
        repo.updateSettings(updated)

        assertEquals("Funny", repo.settings.value.voiceStyle)
        assertEquals(1.3f, repo.settings.value.speechSpeed, 0.01f)
        assertEquals("Hindi (हिंदी)", repo.settings.value.voiceLanguage)
    }
}
