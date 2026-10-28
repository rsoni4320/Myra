package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.ScreenDestination
import com.example.ui.theme.MayaBackgroundDark
import com.example.ui.theme.MayaBorder
import com.example.ui.theme.MayaCardDark
import com.example.ui.theme.MayaCardSelected
import com.example.ui.theme.MayaPurple
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

@Composable
fun MayaDrawerContent(
    modifier: Modifier = Modifier,
    activeScreen: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit,
    onNewChat: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color(0xFF090E22))
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        // Drawer Header matching screenshot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(2.dp, MayaPurple.copy(alpha = 0.6f), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.maya_avatar),
                    contentDescription = "Maya Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Maya",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI Voice Assistant",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(
            color = MayaBorder.copy(alpha = 0.5f),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Menu items matching screenshot list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            DrawerMenuItem(
                title = "New Chat",
                icon = Icons.Outlined.ChatBubbleOutline,
                isSelected = false,
                onClick = onNewChat,
                testTag = "drawer_new_chat"
            )
            DrawerMenuItem(
                title = "Chat History",
                icon = Icons.Outlined.History,
                isSelected = activeScreen == ScreenDestination.CHAT_HISTORY,
                onClick = { onNavigate(ScreenDestination.CHAT_HISTORY) },
                testTag = "drawer_chat_history"
            )
            DrawerMenuItem(
                title = "Reminders",
                icon = Icons.Outlined.NotificationsNone,
                isSelected = activeScreen == ScreenDestination.REMINDERS,
                onClick = { onNavigate(ScreenDestination.REMINDERS) },
                testTag = "drawer_reminders"
            )
            DrawerMenuItem(
                title = "Alarms & Timers",
                icon = Icons.Outlined.AccessAlarm,
                isSelected = activeScreen == ScreenDestination.ALARMS_TIMERS,
                onClick = { onNavigate(ScreenDestination.ALARMS_TIMERS) },
                testTag = "drawer_alarms"
            )
            DrawerMenuItem(
                title = "Notes",
                icon = Icons.Outlined.Description,
                isSelected = activeScreen == ScreenDestination.NOTES,
                onClick = { onNavigate(ScreenDestination.NOTES) },
                testTag = "drawer_notes"
            )
            DrawerMenuItem(
                title = "AI Tools",
                icon = Icons.Outlined.AutoAwesome,
                isSelected = activeScreen == ScreenDestination.AI_TOOLS,
                onClick = { onNavigate(ScreenDestination.AI_TOOLS) },
                testTag = "drawer_ai_tools"
            )
            DrawerMenuItem(
                title = "Voice Settings",
                icon = Icons.Outlined.MicNone,
                isSelected = activeScreen == ScreenDestination.VOICE_SETTINGS,
                onClick = { onNavigate(ScreenDestination.VOICE_SETTINGS) },
                testTag = "drawer_voice_settings"
            )
            DrawerMenuItem(
                title = "Settings",
                icon = Icons.Outlined.Settings,
                isSelected = activeScreen == ScreenDestination.SETTINGS,
                onClick = { onNavigate(ScreenDestination.SETTINGS) },
                testTag = "drawer_settings"
            )
            DrawerMenuItem(
                title = "Help & Support",
                icon = Icons.Outlined.QuestionMark,
                isSelected = activeScreen == ScreenDestination.HELP_SUPPORT,
                onClick = { onNavigate(ScreenDestination.HELP_SUPPORT) },
                testTag = "drawer_help"
            )
            DrawerMenuItem(
                title = "About Maya",
                icon = Icons.Outlined.Info,
                isSelected = activeScreen == ScreenDestination.ABOUT_MAYA,
                onClick = { onNavigate(ScreenDestination.ABOUT_MAYA) },
                testTag = "drawer_about"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Exit Maya button at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MayaCardDark)
                .clickable { onExit() }
                .padding(vertical = 12.dp)
                .testTag("drawer_exit_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Exit Maya",
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exit Maya",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val background = if (isSelected) MayaCardSelected else Color.Transparent
    val contentColor = if (isSelected) MayaPurple else TextWhite
    val shape = RoundedCornerShape(10.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(shape)
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
