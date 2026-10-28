package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MayaViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.MayaDrawerContent
import com.example.ui.screens.AboutMayaScreen
import com.example.ui.screens.AiSettingsScreen
import com.example.ui.screens.AiToolsScreen
import com.example.ui.screens.AlarmsTimersScreen
import com.example.ui.screens.ChatHistoryScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.OtherSettingsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.theme.MayaBackgroundDark
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MayaApp(onExitApp = { finish() })
            }
        }
    }
}

@Composable
fun MayaApp(
    viewModel: MayaViewModel = viewModel(),
    onExitApp: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDrawerOpenState by viewModel.isDrawerOpen.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Sync ViewModel drawer state with DrawerState
    LaunchedEffect(isDrawerOpenState) {
        if (isDrawerOpenState && drawerState.isClosed) {
            drawerState.open()
        } else if (!isDrawerOpenState && drawerState.isOpen) {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen && isDrawerOpenState) {
            viewModel.closeDrawer()
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
            viewModel.closeDrawer()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == ScreenDestination.HOME,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF090E22)
            ) {
                MayaDrawerContent(
                    activeScreen = currentScreen,
                    onNavigate = { destination ->
                        scope.launch {
                            drawerState.close()
                            viewModel.navigateTo(destination)
                        }
                    },
                    onNewChat = {
                        scope.launch {
                            drawerState.close()
                            viewModel.startNewChat()
                        }
                    },
                    onExit = {
                        onExitApp()
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MayaBackgroundDark)
                .statusBarsPadding()
        ) {
            when (currentScreen) {
                ScreenDestination.HOME -> HomeScreen(viewModel = viewModel)
                ScreenDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                ScreenDestination.VOICE_SETTINGS -> VoiceSettingsScreen(viewModel = viewModel)
                ScreenDestination.AI_SETTINGS -> AiSettingsScreen(viewModel = viewModel)
                ScreenDestination.OTHER_SETTINGS -> OtherSettingsScreen(viewModel = viewModel)
                ScreenDestination.CHAT_HISTORY -> ChatHistoryScreen(viewModel = viewModel)
                ScreenDestination.REMINDERS -> RemindersScreen(viewModel = viewModel)
                ScreenDestination.NOTES -> NotesScreen(viewModel = viewModel)
                ScreenDestination.ALARMS_TIMERS -> AlarmsTimersScreen(viewModel = viewModel)
                ScreenDestination.AI_TOOLS -> AiToolsScreen(viewModel = viewModel)
                ScreenDestination.HELP_SUPPORT -> HelpSupportScreen(viewModel = viewModel)
                ScreenDestination.ABOUT_MAYA -> AboutMayaScreen(viewModel = viewModel)
            }
        }
    }
}
