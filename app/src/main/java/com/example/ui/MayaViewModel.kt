package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.ai.AiEngine
import com.example.ai.AiResult
import com.example.data.db.MayaDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.Note
import com.example.data.model.Reminder
import com.example.data.repository.AppSettings
import com.example.data.repository.SettingsRepository
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ScreenDestination {
    HOME,
    SETTINGS,
    VOICE_SETTINGS,
    AI_SETTINGS,
    OTHER_SETTINGS,
    CHAT_HISTORY,
    REMINDERS,
    NOTES,
    ALARMS_TIMERS,
    AI_TOOLS,
    HELP_SUPPORT,
    ABOUT_MAYA
}

class MayaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        MayaDatabase::class.java,
        "maya_database"
    ).fallbackToDestructiveMigration().build()

    private val dao = db.mayaDao()
    private val settingsRepo = SettingsRepository(application)
    private val aiEngine = AiEngine()

    val settings: StateFlow<AppSettings> = settingsRepo.settings

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _currentScreen = MutableStateFlow(ScreenDestination.HOME)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userSpokenText = MutableStateFlow("")
    val userSpokenText: StateFlow<String> = _userSpokenText.asStateFlow()

    private val _mayaResponseText = MutableStateFlow("")
    val mayaResponseText: StateFlow<String> = _mayaResponseText.asStateFlow()

    // Voice Manager
    val voiceManager = VoiceManager(
        context = application,
        onResult = { text ->
            onUserSpeechResult(text)
        },
        onError = { err ->
            _errorMessage.value = err
        }
    )

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val rmsDb: StateFlow<Float> = voiceManager.rmsDb
    val liveTranscript: StateFlow<String> = voiceManager.partialTranscript

    // Database Flows
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val chatSessions: StateFlow<List<ChatSession>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<Reminder>> = dao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSessionMessages(_currentSessionId.value)
    }

    private fun loadSessionMessages(sessionId: String) {
        viewModelScope.launch {
            dao.getMessagesForSession(sessionId).collect { list ->
                _messages.value = list
                val lastMaya = list.lastOrNull { it.sender == "MAYA" }
                if (lastMaya != null && _mayaResponseText.value.isEmpty()) {
                    _mayaResponseText.value = lastMaya.content
                }
                val lastUser = list.lastOrNull { it.sender == "USER" }
                if (lastUser != null && _userSpokenText.value.isEmpty()) {
                    _userSpokenText.value = lastUser.content
                }
            }
        }
    }

    fun navigateTo(destination: ScreenDestination) {
        _isDrawerOpen.value = false
        _currentScreen.value = destination
    }

    fun openDrawer() {
        _isDrawerOpen.value = true
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun navigateBack() {
        _currentScreen.value = ScreenDestination.HOME
    }

    fun onMicTapped() {
        val state = voiceState.value
        if (state == VoiceState.LISTENING) {
            voiceManager.stopListening()
        } else if (state == VoiceState.SPEAKING) {
            voiceManager.stopSpeaking()
        } else {
            _errorMessage.value = null
            voiceManager.startListening(settings.value)
        }
    }

    fun onUserSpeechResult(prompt: String) {
        if (prompt.isBlank()) return
        _userSpokenText.value = prompt

        viewModelScope.launch {
            val sessionId = _currentSessionId.value

            // Insert User message
            val userMsg = ChatMessage(
                sessionId = sessionId,
                sender = "USER",
                content = prompt,
                language = settings.value.language
            )
            dao.insertMessage(userMsg)

            // Update session title if first message
            val sessionTitle = if (prompt.length > 28) prompt.take(28) + "..." else prompt
            dao.insertSession(
                ChatSession(
                    sessionId = sessionId,
                    title = sessionTitle,
                    lastMessage = prompt
                )
            )

            // Automatic intent detection for reminders and notes
            val lower = prompt.lowercase(Locale.ROOT)
            if (lower.startsWith("remind me to") || lower.startsWith("reminder:") || lower.contains("set reminder")) {
                val remTitle = prompt.replace(Regex("(?i)(remind me to|set a reminder to|set reminder)"), "").trim()
                if (remTitle.isNotBlank()) {
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    dao.insertReminder(Reminder(title = remTitle, timeLabel = timeStr))
                }
            } else if (lower.startsWith("note:") || lower.startsWith("take note") || lower.startsWith("note down")) {
                val noteBody = prompt.replace(Regex("(?i)(note:|take a note|take note|note down)"), "").trim()
                if (noteBody.isNotBlank()) {
                    val noteTitle = if (noteBody.length > 20) noteBody.take(20) + "..." else "Quick Note"
                    dao.insertNote(Note(title = noteTitle, content = noteBody))
                }
            }

            // Get conversation history
            val history = _messages.value.map { it.sender to it.content }

            // Generate AI response
            when (val result = aiEngine.generateResponse(prompt, settings.value, history)) {
                is AiResult.Success -> {
                    val responseText = result.responseText
                    _mayaResponseText.value = responseText

                    // Insert Maya message
                    val mayaMsg = ChatMessage(
                        sessionId = sessionId,
                        sender = "MAYA",
                        content = responseText,
                        language = settings.value.language
                    )
                    dao.insertMessage(mayaMsg)

                    // Speak out response
                    voiceManager.speak(responseText, settings.value)
                }
                is AiResult.Error -> {
                    _errorMessage.value = result.errorMessage
                    _mayaResponseText.value = result.fallbackText
                    voiceManager.speak(result.fallbackText, settings.value)
                }
            }
        }
    }

    fun sendTextMessage(text: String) {
        onUserSpeechResult(text)
    }

    fun startNewChat() {
        voiceManager.stopSpeaking()
        voiceManager.stopListening()
        val newId = UUID.randomUUID().toString()
        _currentSessionId.value = newId
        _userSpokenText.value = ""
        _mayaResponseText.value = ""
        _messages.value = emptyList()
        loadSessionMessages(newId)
        _isDrawerOpen.value = false
        _currentScreen.value = ScreenDestination.HOME
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _userSpokenText.value = ""
        _mayaResponseText.value = ""
        loadSessionMessages(sessionId)
        _currentScreen.value = ScreenDestination.HOME
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            dao.deleteMessagesForSession(sessionId)
            dao.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewChat()
            }
        }
    }

    fun clearAllChatHistory() {
        viewModelScope.launch {
            dao.clearAllMessages()
            dao.clearAllSessions()
            startNewChat()
        }
    }

    // Reminders
    fun addReminder(title: String, timeLabel: String) {
        viewModelScope.launch {
            dao.insertReminder(Reminder(title = title, timeLabel = timeLabel))
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            dao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            dao.deleteReminder(id)
        }
    }

    // Notes
    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            dao.insertNote(Note(title = title, content = content))
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            dao.deleteNote(id)
        }
    }

    // Settings update
    fun updateSettings(newSettings: AppSettings) {
        settingsRepo.updateSettings(newSettings)
    }

    fun previewVoice() {
        voiceManager.previewVoice(settings.value)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.cleanup()
    }
}
