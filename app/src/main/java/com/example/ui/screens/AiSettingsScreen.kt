package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MayaViewModel
import com.example.ui.theme.MayaBackgroundDark
import com.example.ui.theme.MayaCardDark
import com.example.ui.theme.MayaCardSelected
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaPurple
import com.example.ui.theme.SwitchTrackActive
import com.example.ui.theme.SwitchTrackInactive
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MayaViewModel
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val settings by viewModel.settings.collectAsState()
    var apiKeyInput by remember(settings.customApiKey) { mutableStateOf(settings.customApiKey) }
    var openRouterKeyInput by remember(settings.openRouterApiKey) { mutableStateOf(settings.openRouterApiKey) }
    var openCodeKeyInput by remember(settings.openCodeApiKey) { mutableStateOf(settings.openCodeApiKey) }
    var openCodeEndpointInput by remember(settings.openCodeEndpoint) { mutableStateOf(settings.openCodeEndpoint) }

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
                            text = "AI Settings",
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
                        modifier = Modifier.testTag("ai_settings_back")
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
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // AI Model Header
                Text(
                    text = "AI Model",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 4 Model Cards matching Screen 5
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val models = listOf(
                        ModelCardInfo("GPT-4o", Color(0xFFAB7AFF)),
                        ModelCardInfo("Gemini Pro", Color(0xFF4A90E2)),
                        ModelCardInfo("Claude 3", Color(0xFFDE7E4E)),
                        ModelCardInfo("Llama 3", Color(0xFFC7CBD1))
                    )

                    models.forEach { item ->
                        val isSelected = settings.aiModel.equals(item.name, ignoreCase = true)
                        val bg = if (isSelected) MayaCardSelected else MayaCardDark
                        val border = if (isSelected) MayaPurple else Color(0xFF1E284D)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(1.5.dp, border, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateSettings(settings.copy(aiModel = item.name))
                                }
                                .padding(vertical = 14.dp, horizontal = 4.dp)
                                .testTag("model_card_${item.name}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ModelIcon(name = item.name, tint = item.tint)

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = item.name,
                                color = if (isSelected) TextWhite else TextGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Configuration status banner
                val selectedModel = settings.aiModel
                val hasApiKey = settings.customApiKey.isNotBlank()
                val statusText = when {
                    selectedModel.contains("Gemini", ignoreCase = true) ->
                        "Gemini 3.8 Live & Flash active. Real-time voice intelligence connected."
                    selectedModel.contains("GPT", ignoreCase = true) ->
                        if (hasApiKey) "OpenAI GPT-4o REST provider configured." else "OpenAI GPT-4o requires your API key below."
                    else ->
                        if (hasApiKey) "$selectedModel endpoint configured." else "$selectedModel requires external API key configuration below."
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selectedModel.contains("Gemini")) MayaCardSelected else MayaCardDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (selectedModel.contains("Gemini")) "✦" else "ℹ", color = MayaCyan, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(statusText, color = TextWhite, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Response Length Section
                Text(
                    text = "Response Length",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Segmented 3-button pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MayaCardDark)
                        .padding(4.dp)
                ) {
                    val options = listOf("Short", "Balanced", "Long")
                    options.forEach { option ->
                        val isSelected = settings.responseLength.equals(option, ignoreCase = true)
                        val bg = if (isSelected) MayaCardSelected else Color.Transparent
                        val border = if (isSelected) MayaPurple else Color.Transparent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateSettings(settings.copy(responseLength = option))
                                }
                                .padding(vertical = 8.dp)
                                .testTag("response_length_$option"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) TextWhite else TextGray,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Feature Switches
                AiFeatureSwitch(
                    title = "Remember Conversation",
                    subtitle = null,
                    checked = settings.rememberConversation,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings(settings.copy(rememberConversation = checked))
                    },
                    testTag = "ai_toggle_remember"
                )

                AiFeatureSwitch(
                    title = "Web Search",
                    subtitle = "Allow AI to search the web for answers",
                    checked = settings.webSearch,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings(settings.copy(webSearch = checked))
                    },
                    testTag = "ai_toggle_web_search"
                )

                AiFeatureSwitch(
                    title = "Image Understanding",
                    subtitle = "Allow AI to analyze images",
                    checked = settings.imageUnderstanding,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings(settings.copy(imageUnderstanding = checked))
                    },
                    testTag = "ai_toggle_image"
                )

                AiFeatureSwitch(
                    title = "Document Reading",
                    subtitle = "Allow AI to read documents (PDF, TXT)",
                    checked = settings.documentReading,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings(settings.copy(documentReading = checked))
                    },
                    testTag = "ai_toggle_document"
                )

                AiFeatureSwitch(
                    title = "Code Generation",
                    subtitle = "Allow AI to write and analyze code",
                    checked = settings.codeGeneration,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings(settings.copy(codeGeneration = checked))
                    },
                    testTag = "ai_toggle_code"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // OpenRouter Configuration
                Text(
                    text = "OpenRouter Provider",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Base URL: https://openrouter.ai/api/v1 (Llama 3.3, Gemini 2.0 Flash, DeepSeek)",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = openRouterKeyInput,
                    onValueChange = {
                        openRouterKeyInput = it
                        viewModel.updateSettings(settings.copy(openRouterApiKey = it))
                    },
                    placeholder = { Text("Paste OpenRouter API Key (sk-or-...)", color = TextGray) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Key, contentDescription = "OpenRouter Key", tint = MayaPurple)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("openrouter_api_key_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = MayaPurple,
                        unfocusedBorderColor = Color(0xFF1E284D),
                        focusedContainerColor = MayaCardDark,
                        unfocusedContainerColor = MayaCardDark
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // OpenCode Configuration
                Text(
                    text = "OpenCode Provider",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Configurable endpoint for OpenCode-compatible inference services.",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = openCodeKeyInput,
                    onValueChange = {
                        openCodeKeyInput = it
                        viewModel.updateSettings(settings.copy(openCodeApiKey = it))
                    },
                    placeholder = { Text("Paste OpenCode API Key...", color = TextGray) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Key, contentDescription = "OpenCode Key", tint = MayaCyan)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opencode_api_key_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = MayaCyan,
                        unfocusedBorderColor = Color(0xFF1E284D),
                        focusedContainerColor = MayaCardDark,
                        unfocusedContainerColor = MayaCardDark
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = openCodeEndpointInput,
                    onValueChange = {
                        openCodeEndpointInput = it
                        viewModel.updateSettings(settings.copy(openCodeEndpoint = it))
                    },
                    placeholder = { Text("Endpoint: https://opencode.ai/api/v1", color = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opencode_endpoint_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = MayaCyan,
                        unfocusedBorderColor = Color(0xFF1E284D),
                        focusedContainerColor = MayaCardDark,
                        unfocusedContainerColor = MayaCardDark
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Optional Custom Gemini/OpenAI API Key input
                Text(
                    text = "Direct Gemini / OpenAI Key (Optional)",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Direct key for official Gemini or OpenAI endpoints.",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        viewModel.updateSettings(settings.copy(customApiKey = it))
                    },
                    placeholder = { Text("Paste direct API key here...", color = TextGray) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Key, contentDescription = "API Key", tint = MayaPurple)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_api_key_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = MayaPurple,
                        unfocusedBorderColor = Color(0xFF1E284D),
                        focusedContainerColor = MayaCardDark,
                        unfocusedContainerColor = MayaCardDark
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

private data class ModelCardInfo(val name: String, val tint: Color)

@Composable
private fun ModelIcon(name: String, tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)

        when (name) {
            "GPT-4o" -> {
                // OpenAI Swirl style
                drawCircle(color = tint.copy(alpha = 0.2f), radius = w * 0.45f)
                val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                for (i in 0 until 6) {
                    val angle = (i * 60f) * (Math.PI / 180f).toFloat()
                    val p1 = Offset(c.x + cos(angle) * (w * 0.15f), c.y + sin(angle) * (h * 0.15f))
                    val p2 = Offset(c.x + cos(angle + 0.8f) * (w * 0.42f), c.y + sin(angle + 0.8f) * (h * 0.42f))
                    drawLine(color = tint, start = p1, end = p2, strokeWidth = stroke.width, cap = stroke.cap)
                }
            }
            "Gemini Pro" -> {
                // 4-point sparkle star
                val path = Path().apply {
                    moveTo(c.x, 2f)
                    quadraticTo(c.x, c.y, w - 2f, c.y)
                    quadraticTo(c.x, c.y, c.x, h - 2f)
                    quadraticTo(c.x, c.y, 2f, c.y)
                    quadraticTo(c.x, c.y, c.x, 2f)
                }
                drawPath(path, color = tint)
            }
            "Claude 3" -> {
                // Radiant starburst rays
                val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                for (i in 0 until 8) {
                    val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                    val start = Offset(c.x + cos(angle) * 4.dp.toPx(), c.y + sin(angle) * 4.dp.toPx())
                    val end = Offset(c.x + cos(angle) * (w * 0.45f), c.y + sin(angle) * (h * 0.45f))
                    drawLine(color = tint, start = start, end = end, strokeWidth = stroke.width, cap = stroke.cap)
                }
            }
            else -> {
                // Meta ribbon / infinity loop
                val path = Path().apply {
                    moveTo(w * 0.25f, h * 0.5f)
                    cubicTo(w * 0.15f, h * 0.2f, w * 0.45f, h * 0.2f, c.x, c.y)
                    cubicTo(w * 0.55f, h * 0.8f, w * 0.85f, h * 0.8f, w * 0.75f, h * 0.5f)
                    cubicTo(w * 0.85f, h * 0.2f, w * 0.55f, h * 0.2f, c.x, c.y)
                    cubicTo(w * 0.45f, h * 0.8f, w * 0.15f, h * 0.8f, w * 0.25f, h * 0.5f)
                }
                drawPath(path, color = tint, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun AiFeatureSwitch(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhite,
                checkedTrackColor = SwitchTrackActive,
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = SwitchTrackInactive
            )
        )
    }
}
