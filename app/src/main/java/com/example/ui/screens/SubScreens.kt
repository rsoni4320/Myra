package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MayaViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.MayaBackgroundDark
import com.example.ui.theme.MayaCardDark
import com.example.ui.theme.MayaPurple
import com.example.ui.theme.MayaRed
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1. CHAT HISTORY SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(viewModel: MayaViewModel) {
    BackHandler { viewModel.navigateBack() }
    val sessions by viewModel.chatSessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat History", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )
        },
        containerColor = MayaBackgroundDark
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "No chats",
                        tint = TextGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No past conversations found", color = TextGray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.startNewChat() }) {
                        Text("Start a New Chat with Maya", color = MayaPurple)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectSession(session.sessionId) }
                            .testTag("chat_session_${session.sessionId}"),
                        colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title.ifBlank { "Conversation" },
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val dateStr = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
                                    .format(Date(session.createdAt))
                                Text(text = dateStr, color = TextGray, fontSize = 12.sp)
                            }
                            IconButton(onClick = { viewModel.deleteSession(session.sessionId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. REMINDERS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: MayaViewModel) {
    BackHandler { viewModel.navigateBack() }
    val reminders by viewModel.reminders.collectAsState()
    var newTitle by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("09:00 AM") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MayaPurple,
                contentColor = TextWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        },
        containerColor = MayaBackgroundDark
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No reminders yet. Tap + or tell Maya to remind you!", color = TextGray, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Checkbox(
                                checked = reminder.isCompleted,
                                onCheckedChange = { viewModel.toggleReminder(reminder) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MayaPurple,
                                    uncheckedColor = TextGray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    color = if (reminder.isCompleted) TextGray else TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(text = reminder.timeLabel, color = MayaPurple, fontSize = 12.sp)
                            }
                            IconButton(onClick = { viewModel.deleteReminder(reminder.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextGray)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Reminder", color = TextWhite) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Reminder task") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MayaPurple
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newTime,
                            onValueChange = { newTime = it },
                            label = { Text("Time (e.g. 10:30 AM)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MayaPurple
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                viewModel.addReminder(newTitle.trim(), newTime.trim())
                                newTitle = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MayaPurple)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = MayaCardDark
            )
        }
    }
}

// 3. NOTES SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: MayaViewModel) {
    BackHandler { viewModel.navigateBack() }
    val notes by viewModel.notes.collectAsState()
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice & Text Notes", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MayaPurple,
                contentColor = TextWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        containerColor = MayaBackgroundDark
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No notes recorded yet. Say 'Take note...' or tap +", color = TextGray, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = note.title,
                                    color = TextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextGray)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = note.content, color = Color(0xFFD1D5DB), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.createdAt))
                            Text(text = dateStr, color = TextGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Note", color = TextWhite) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text("Title") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MayaPurple
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = noteContent,
                            onValueChange = { noteContent = it },
                            label = { Text("Note content") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MayaPurple
                            ),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (noteTitle.isNotBlank() || noteContent.isNotBlank()) {
                                viewModel.addNote(
                                    if (noteTitle.isBlank()) "Quick Note" else noteTitle.trim(),
                                    noteContent.trim()
                                )
                                noteTitle = ""
                                noteContent = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MayaPurple)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = MayaCardDark
            )
        }
    }
}

// 4. ALARMS & TIMERS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsTimersScreen(viewModel: MayaViewModel) {
    BackHandler { viewModel.navigateBack() }
    val context = LocalContext.current
    var timerSeconds by remember { mutableIntStateOf(300) } // 5 mins
    var isTimerRunning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms & Timers", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )
        },
        containerColor = MayaBackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = "Timer",
                        tint = MayaPurple,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val mins = timerSeconds / 60
                    val secs = timerSeconds % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", mins, secs),
                        color = TextWhite,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { timerSeconds = 300 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E284D))
                        ) { Text("5m") }
                        Button(
                            onClick = { timerSeconds = 900 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E284D))
                        ) { Text("15m") }
                        Button(
                            onClick = { timerSeconds = 1800 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E284D))
                        ) { Text("30m") }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            isTimerRunning = !isTimerRunning
                            Toast.makeText(
                                context,
                                if (isTimerRunning) "Timer started!" else "Timer paused",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) MayaRed else MayaPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isTimerRunning) "Pause Timer" else "Start Timer", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// 5. AI TOOLS SCREEN (Multimodal, Code, Document & Translation)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(viewModel: MayaViewModel) {
    BackHandler { viewModel.navigateBack() }
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var toolInput by remember { mutableStateOf("") }

    // Photo picker (zero storage permissions)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeSelectedImage(uri)
            viewModel.navigateBack()
            Toast.makeText(context, "Image selected for analysis", Toast.LENGTH_SHORT).show()
        }
    }

    // Document picker (zero external leak)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "Document"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        fileName = cursor.getString(nameIdx)
                    }
                }

                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readLines().take(50).joinToString("\n")
                }.orEmpty()

                viewModel.readSelectedDocument(fileName, content)
                viewModel.navigateBack()
                Toast.makeText(context, "Document loaded safely on-device", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read document: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Tools & Intelligence", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )
        },
        containerColor = MayaBackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Assistant Tools & Multimodal Workflows",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Prompt Box
            OutlinedTextField(
                value = toolInput,
                onValueChange = { toolInput = it },
                label = { Text("Prompt or question for Maya...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = MayaPurple
                ),
                minLines = 3
            )

            // Text Tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (toolInput.isNotBlank()) {
                            viewModel.sendTextMessage("Translate to Hindi: $toolInput")
                            viewModel.navigateBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E284D))
                ) {
                    Text("Translate (HI)", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (toolInput.isNotBlank()) {
                            viewModel.sendTextMessage("Summarize concisely: $toolInput")
                            viewModel.navigateBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MayaPurple)
                ) {
                    Text("Summarize", fontSize = 12.sp)
                }
            }

            // Web Search & Code Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (toolInput.isNotBlank()) {
                            viewModel.sendTextMessage("Search: $toolInput")
                            viewModel.navigateBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E284D))
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Web Search", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (toolInput.isNotBlank()) {
                            viewModel.sendTextMessage("Write code for: $toolInput")
                            viewModel.navigateBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E284D))
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Code", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Write Code", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Multimodal Section
            Text(
                text = "Multimodal & Document Analysis",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            // Image Understanding Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (settings.imageUnderstanding) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            Toast.makeText(context, "Image Understanding is disabled in AI Settings", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Image Analysis", tint = MayaPurple, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Analyze Image (Photo Picker)", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Pick an image without broad storage permissions", color = TextGray, fontSize = 12.sp)
                    }
                }
            }

            // Document Reading Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (settings.documentReading) {
                            documentPickerLauncher.launch(arrayOf("text/plain", "application/pdf"))
                        } else {
                            Toast.makeText(context, "Document Reading is disabled in AI Settings", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = "Document Analysis", tint = MayaPurple, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read Document (TXT/PDF)", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Inspects text locally without leaking files externally", color = TextGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// 6. USER-CONTROLLED PRIVACY & MEMORY SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyMemoryScreen(viewModel: MayaViewModel) {
    BackHandler { viewModel.navigateBack() }
    val memories by viewModel.memories.collectAsState()
    var newFact by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Memory Store", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MayaPurple,
                contentColor = TextWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Memory")
            }
        },
        containerColor = MayaBackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Privacy", tint = MayaPurple, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "User-controlled memory is opt-in, stored strictly on-device in Room, and completely inspectable and deletable.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            if (memories.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No personal memories stored yet.\nSay 'Remember that...' or tap + to add.", color = TextGray, fontSize = 14.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stored Memory Facts (${memories.size})", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    TextButton(onClick = { viewModel.clearAllMemories() }) {
                        Text("Clear All", color = MayaRed, fontSize = 13.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memories, key = { it.id }) { memory ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = memory.fact, color = TextWhite, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "${memory.category} • ${memory.source}", color = TextGray, fontSize = 11.sp)
                                }
                                IconButton(onClick = { viewModel.deleteMemory(memory.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextGray)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Memory Fact", color = TextWhite) },
                text = {
                    OutlinedTextField(
                        value = newFact,
                        onValueChange = { newFact = it },
                        label = { Text("Fact for Maya to remember") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = MayaPurple
                        ),
                        singleLine = false,
                        minLines = 2
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFact.isNotBlank()) {
                                viewModel.addMemory(newFact.trim())
                                newFact = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MayaPurple)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = MayaCardDark
            )
        }
    }
}

// 7. HELP & SUPPORT SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(viewModel: MayaViewModel) {
    BackHandler { viewModel.navigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )
        },
        containerColor = MayaBackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Voice Commands Guide",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            HelpCard(
                title = "Push-to-Talk Microphone",
                description = "Tap the circular glowing microphone button at the bottom of the home screen to speak. Tap again to finish or stop speaking."
            )

            HelpCard(
                title = "Hindi & English Support",
                description = "Maya natively understands both Hindi (हिंदी) and English. Try asking: 'नमस्ते माया', 'आज का मौसम कैसा है', 'एक चुटकुला सुनाओ', or 'What time is it?'"
            )

            HelpCard(
                title = "Voice Reminders & Notes",
                description = "Say 'Remind me to call Mom at 5pm' or 'Take note buy milk' and Maya will automatically persist it in your local Room database!"
            )

            HelpCard(
                title = "Voice Customization",
                description = "In Voice Settings, you can switch between Friendly, Professional, Calm, and Funny voice styles, as well as adjust speed, pitch, and preview Maya's voice."
            )
        }
    }
}

@Composable
private fun HelpCard(title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MayaCardDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = MayaPurple, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, color = Color(0xFFD1D5DB), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

// 8. ABOUT MAYA SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMayaScreen(viewModel: MayaViewModel) {
    BackHandler { viewModel.navigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Maya", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )
        },
        containerColor = MayaBackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Maya AI Voice Assistant", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Version 1.0.0", color = MayaPurple, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MayaCardDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• Architecture: Clean MVVM + Kotlin Coroutines & Flow", color = TextWhite, fontSize = 13.sp)
                    Text("• UI Framework: Jetpack Compose & Material 3", color = TextWhite, fontSize = 13.sp)
                    Text("• Local Database: Android Jetpack Room (SQLite)", color = TextWhite, fontSize = 13.sp)
                    Text("• Voice Engine: Android SpeechRecognizer + TextToSpeech", color = TextWhite, fontSize = 13.sp)
                    Text("• Intelligence: Gemini AI Cloud Engine + Local Personality Engine", color = TextWhite, fontSize = 13.sp)
                    Text("• Multilingual: Native English and Hindi (हिंदी) processing", color = TextWhite, fontSize = 13.sp)
                    Text("• Security: Zero hardcoded secrets, zero-permission Photo Picker", color = TextWhite, fontSize = 13.sp)
                }
            }
        }
    }
}
