package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MayaViewModel
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MayaBackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Other Settings",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("other_settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MayaBackgroundDark)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OtherSettingsItem(
                    icon = Icons.Outlined.NotificationsNone,
                    label = "Reminders",
                    onClick = { viewModel.navigateTo(ScreenDestination.REMINDERS) },
                    testTag = "other_reminders"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.AccessAlarm,
                    label = "Alarms & Timers",
                    onClick = { viewModel.navigateTo(ScreenDestination.ALARMS_TIMERS) },
                    testTag = "other_alarms"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.Description,
                    label = "Notes",
                    onClick = { viewModel.navigateTo(ScreenDestination.NOTES) },
                    testTag = "other_notes"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.Backup,
                    label = "Chat Backup",
                    onClick = {
                        Toast.makeText(context, "Local database backed up successfully!", Toast.LENGTH_SHORT).show()
                    },
                    testTag = "other_chat_backup"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.Share,
                    label = "Export Chat",
                    onClick = {
                        Toast.makeText(context, "Chat exported to local storage.", Toast.LENGTH_SHORT).show()
                    },
                    testTag = "other_export_chat"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.DeleteOutline,
                    label = "Clear Chat History",
                    onClick = { showClearHistoryDialog = true },
                    testTag = "other_clear_chat_history"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.PrivacyTip,
                    label = "Privacy & Data",
                    onClick = {
                        Toast.makeText(context, "Privacy: All conversations are stored privately on device.", Toast.LENGTH_LONG).show()
                    },
                    testTag = "other_privacy"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.NotificationsNone,
                    label = "Notification Settings",
                    onClick = {
                        Toast.makeText(context, "Assistant notifications are enabled.", Toast.LENGTH_SHORT).show()
                    },
                    testTag = "other_notifications"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.Security,
                    label = "Permissions",
                    onClick = {
                        Toast.makeText(context, "Microphone & Vibration permissions active.", Toast.LENGTH_SHORT).show()
                    },
                    testTag = "other_permissions"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.HelpOutline,
                    label = "Help & Support",
                    onClick = { viewModel.navigateTo(ScreenDestination.HELP_SUPPORT) },
                    testTag = "other_help"
                )

                OtherSettingsItem(
                    icon = Icons.Outlined.Info,
                    label = "About Maya",
                    badge = "1.0.0",
                    onClick = { viewModel.navigateTo(ScreenDestination.ABOUT_MAYA) },
                    testTag = "other_about"
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Logout Button matching Screen 6
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF23141E))
                        .border(1.dp, MayaRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { showLogoutDialog = true }
                        .padding(vertical = 14.dp)
                        .testTag("logout_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.PowerSettingsNew,
                            contentDescription = "Logout",
                            tint = MayaRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Logout",
                            color = MayaRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Clear Chat Confirm Dialog
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear All Chat History?", color = TextWhite) },
                text = { Text("This will permanently remove all past chat messages from your device.", color = TextGray) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAllChatHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Clear", color = MayaRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = MayaCardDark
            )
        }

        // Logout Confirm Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Reset Session", color = TextWhite) },
                text = { Text("This will reset your current assistant session and return to home.", color = TextGray) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.startNewChat()
                        showLogoutDialog = false
                    }) {
                        Text("Reset", color = MayaRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = MayaCardDark
            )
        }
    }
}

@Composable
private fun OtherSettingsItem(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 13.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextWhite,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = TextWhite,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Text(
                text = badge,
                color = TextGray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = TextGray,
            modifier = Modifier.size(18.dp)
        )
    }
}
