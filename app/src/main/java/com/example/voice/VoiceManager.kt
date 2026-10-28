package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.repository.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class VoiceManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val tag = "VoiceManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
        } catch (e: Exception) {
            Log.w(tag, "ToneGenerator init failed", e)
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _voiceState.value = VoiceState.IDLE
                }
            })
            Log.d(tag, "TTS initialized successfully")
        } else {
            Log.e(tag, "TTS initialization failed with status: $status")
        }
    }

    fun startListening(settings: AppSettings) {
        // Stop any current speaking
        stopSpeaking()

        if (settings.vibration) {
            vibrate(40)
        }
        if (settings.soundEffects) {
            playChime()
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            _voiceState.value = VoiceState.ERROR
            return
        }

        destroyRecognizer()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.LISTENING
                        _partialTranscript.value = ""
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsDb.value = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceState.value = VoiceState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        _rmsDb.value = 0f
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy. Please try again."
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Speech recognition error ($error)"
                        }
                        _voiceState.value = VoiceState.IDLE
                        onError(message)
                    }

                    override fun onResults(results: Bundle?) {
                        _rmsDb.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        _partialTranscript.value = text
                        if (text.isNotEmpty()) {
                            _voiceState.value = VoiceState.PROCESSING
                            onResult(text)
                        } else {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotEmpty()) {
                            _partialTranscript.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                val selectedLocale = when {
                    settings.voiceLanguage.contains("Hindi", ignoreCase = true) ||
                    settings.language.contains("Hindi", ignoreCase = true) -> Locale("hi", "IN")
                    settings.voiceLanguage.contains("India", ignoreCase = true) -> Locale("en", "IN")
                    else -> Locale.US
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLocale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLocale.toLanguageTag())
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognizer", e)
            _voiceState.value = VoiceState.ERROR
            onError("Failed to start speech recognition: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping recognizer", e)
        }
    }

    fun speak(text: String, settings: AppSettings) {
        if (!isTtsReady || tts == null) {
            Log.w(tag, "TTS not ready yet")
            return
        }

        try {
            // Configure pitch and speed
            // voicePitch: 0.5 to 2.0 (default 1.0)
            // voiceStyle tweaks pitch slightly
            var pitch = settings.voicePitch
            var rate = settings.speechSpeed

            when (settings.voiceStyle.lowercase()) {
                "friendly" -> pitch *= 1.15f // higher, cheerful anime-like pitch
                "calm" -> {
                    pitch *= 0.95f
                    rate *= 0.9f
                }
                "professional" -> {
                    pitch *= 1.0f
                    rate *= 1.05f
                }
                "funny" -> pitch *= 1.3f
            }

            tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
            tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))

            // Choose language: Hindi if text contains Devanagari or settings indicate Hindi
            val containsHindi = text.any { it.code in 0x0900..0x097F }
            val targetLocale = if (containsHindi || settings.voiceLanguage.contains("Hindi", ignoreCase = true)) {
                Locale("hi", "IN")
            } else {
                Locale.US
            }

            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default locale
                tts?.setLanguage(Locale.US)
            }

            val params = Bundle().apply {
                putFloat(
                    TextToSpeech.Engine.KEY_PARAM_VOLUME,
                    (settings.volume / 100f).coerceIn(0f, 1f)
                )
            }

            _voiceState.value = VoiceState.SPEAKING
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "MAYA_UTTERANCE_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(tag, "Error speaking with TTS", e)
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            if (_voiceState.value == VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.IDLE
            }
        } catch (e: Exception) {
            Log.w(tag, "Error stopping TTS", e)
        }
    }

    fun previewVoice(settings: AppSettings) {
        val previewText = when {
            settings.voiceLanguage.contains("Hindi", ignoreCase = true) ||
            settings.language.contains("Hindi", ignoreCase = true) ->
                "नमस्ते! मैं माया हूँ, आपकी वॉयस असिस्टेंट। मेरी आवाज़ कैसी लग रही है?"
            settings.voiceStyle.equals("Friendly", ignoreCase = true) ->
                "Hey there! I'm Maya, your personal voice assistant! It's so wonderful to talk with you! ✨"
            settings.voiceStyle.equals("Professional", ignoreCase = true) ->
                "Greetings. I am Maya, your AI voice assistant, configured and ready for your commands."
            settings.voiceStyle.equals("Calm", ignoreCase = true) ->
                "Hello... Take a deep breath. I am Maya, here to assist you whenever you need."
            settings.voiceStyle.equals("Funny", ignoreCase = true) ->
                "Beep boop! Just kidding, I'm Maya! Ready to conquer the universe or just check the weather?"
            else ->
                "Hello, I am Maya! Your Voice, My World."
        }
        speak(previewText, settings)
    }

    private fun playChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (e: Exception) {
            Log.w(tag, "Error playing tone", e)
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w(tag, "Error triggering vibration", e)
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(tag, "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
    }

    fun cleanup() {
        destroyRecognizer()
        tts?.stop()
        tts?.shutdown()
        tts = null
        toneGenerator?.release()
        toneGenerator = null
    }
}
