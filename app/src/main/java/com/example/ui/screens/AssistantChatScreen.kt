package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NovaUserSettings
import com.example.model.AssistantState
import com.example.model.NovaLanguage
import com.example.model.ResponseDepth
import com.example.model.ScreenSnapshot
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.NovaOrbVisualizer
import com.example.ui.components.QuickCommandChipsRow
import com.example.ui.components.ScreenReadingStatusBanner
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmeraldTertiary
import com.example.ui.theme.NovaRoseError
import com.example.ui.theme.NovaVioletSecondary

@Composable
fun AssistantChatScreen(
    messages: List<ChatMessageEntity>,
    settings: NovaUserSettings,
    assistantState: AssistantState,
    isListening: Boolean,
    partialTranscript: String,
    audioLevel: Float,
    isSpeaking: Boolean,
    speakingMessageText: String?,
    isThinking: Boolean,
    isScreenServiceConnected: Boolean,
    isAnalyzingScreenNow: Boolean,
    activeScreenSnapshot: ScreenSnapshot?,
    attachScreenToNextPrompt: Boolean,
    attachedBitmap: Bitmap?,
    attachedImageLabel: String?,
    statusBannerMessage: String?,
    voiceStatusMessage: String?,
    onSendMessage: (String) -> Unit,
    onRetryPrompt: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onToggleSpeakMessage: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleAttachScreen: () -> Unit,
    onTriggerScreenCommand: (String) -> Unit,
    onAttachImageUri: (android.net.Uri) -> Unit,
    onAttachCameraBitmap: (Bitmap?) -> Unit,
    onClearAttachedImage: () -> Unit,
    onLanguageChange: (NovaLanguage) -> Unit,
    onDepthChange: (ResponseDepth) -> Unit,
    onNewChat: () -> Unit,
    onClearChat: () -> Unit,
    onDismissStatusBanner: () -> Unit,
    onNavigateToScreenHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var inputText by rememberSaveable { mutableStateOf("") }
    var isOrbExpanded by rememberSaveable { mutableStateOf(true) }
    var showMicRationaleDialog by remember { mutableStateOf(false) }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var showScreenPermissionDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo Picker launcher (Zero-permission Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAttachImageUri(uri)
        }
    }

    // Camera Preview launcher
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onAttachCameraBitmap(bitmap)
        }
    }

    // Runtime permission launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleVoice()
        } else {
            showMicRationaleDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraCaptureLauncher.launch(null)
        } else {
            showCameraRationaleDialog = true
        }
    }

    fun handleMicButtonClick() {
        val hasMicPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMicPerm) {
            onToggleVoice()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun handleCameraButtonClick() {
        val hasCamPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCamPerm) {
            cameraCaptureLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Futuristic Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isOrbExpanded = !isOrbExpanded }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when (assistantState) {
                                        AssistantState.LISTENING -> NovaEmeraldTertiary
                                        AssistantState.ANALYZING_SCREEN -> NovaAmberWarning
                                        AssistantState.THINKING -> NovaVioletSecondary
                                        AssistantState.SPEAKING -> NovaCyanPrimary
                                        AssistantState.ERROR -> NovaRoseError
                                        AssistantState.IDLE -> NovaCyanPrimary
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NOVA AI",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isOrbExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle AI Orb panel",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = assistantState.statusLabel + " • " + settings.selectedModel.displayName.substringBefore(" ("),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNewChat,
                            modifier = Modifier.testTag("new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "Start new conversation",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear conversation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick Language & Response Depth Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NovaLanguage.entries.forEach { lang ->
                        val selected = settings.language == lang
                        FilterChip(
                            selected = selected,
                            onClick = { onLanguageChange(lang) },
                            label = {
                                Text(
                                    text = lang.shortBadge,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    ResponseDepth.entries.take(2).forEach { depth ->
                        val selected = settings.responseDepth == depth
                        FilterChip(
                            selected = selected,
                            onClick = { onDepthChange(depth) },
                            label = {
                                Text(
                                    text = depth.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("depth_chip_${depth.id}")
                        )
                    }
                }
            }
        }

        // Screen-Reading Active Status Indicator Banner
        ScreenReadingStatusBanner(
            isScreenServiceConnected = isScreenServiceConnected,
            isAnalyzingNow = isAnalyzingScreenNow,
            activeSnapshot = if (attachScreenToNextPrompt || isAnalyzingScreenNow) activeScreenSnapshot else null,
            showBanner = settings.showScreenReadingIndicator,
            onInspectClick = onNavigateToScreenHub,
            onClearSnapshot = onToggleAttachScreen
        )

        // Status / Voice notice banner
        val activeNotice = voiceStatusMessage ?: statusBannerMessage
        AnimatedVisibility(visible = !activeNotice.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeNotice.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissStatusBanner,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss notice",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Collapsible Futuristic Animated AI Orb Hero Panel
        AnimatedVisibility(visible = isOrbExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_nova_hero_banner),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xCC070B14),
                                    Color(0xE60B1222)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NovaOrbVisualizer(
                        state = assistantState,
                        audioLevel = audioLevel,
                        onClick = { handleMicButtonClick() },
                        orbSize = 92.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = assistantState.statusLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isListening && partialTranscript.isNotBlank()) {
                                "\"$partialTranscript\""
                            } else {
                                assistantState.subLabel
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCCE4FF),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isListening) NovaEmeraldTertiary else NovaCyanPrimary.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .clickable { handleMicButtonClick() }
                                    .testTag("orb_wake_voice_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isListening) Color.Black else NovaCyanPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isListening) "Listening..." else "Wake NOVA",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isListening) Color.Black else NovaCyanPrimary
                                    )
                                }
                            }

                            if (isSpeaking) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = NovaRoseError.copy(alpha = 0.25f),
                                    modifier = Modifier
                                        .clickable { onStopSpeaking() }
                                        .testTag("stop_tts_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Stop speaking",
                                            tint = NovaRoseError,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Stop TTS",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = NovaRoseError
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = NovaAmberWarning.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .clickable {
                                            onTriggerScreenCommand("Screen par kya likha hai?")
                                        }
                                        .testTag("orb_quick_screen_read_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ScreenshotMonitor,
                                            contentDescription = null,
                                            tint = NovaAmberWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Read Screen",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = NovaAmberWarning
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isThinking || isAnalyzingScreenNow) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = if (isAnalyzingScreenNow) NovaAmberWarning else NovaCyanPrimary
            )
        }

        // Chat Messages List + Quick Command Chips
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list"),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(
                        text = "Quick Voice & Screen Commands:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    QuickCommandChipsRow(
                        onCommandSelected = { commandPhrase ->
                            onSendMessage(commandPhrase)
                        }
                    )
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatMessageBubble(
                    message = msg,
                    isSpeakingThis = isSpeaking && speakingMessageText == msg.content,
                    onSpeakToggle = onToggleSpeakMessage,
                    onRetry = onRetryPrompt
                )
            }
        }

        // Attached Image / Screenshot Preview Drawer
        AnimatedVisibility(visible = attachedBitmap != null) {
            if (attachedBitmap != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    bitmap = attachedBitmap.asImageBitmap(),
                                    contentDescription = "Attached screenshot preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, NovaCyanPrimary, RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = attachedImageLabel ?: "Attached Image Ready",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap a quick prompt below or type your question",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = onClearAttachedImage,
                                modifier = Modifier.testTag("remove_attached_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attached image"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Is image ko samjhao",
                                "Is question ka answer batao",
                                "Extract & translate text"
                            ).forEach { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.clickable {
                                        onSendMessage(prompt)
                                    }
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Composer & Large Microphone Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Attach Screenshot / Image (Photo Picker)
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("attach_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Select screenshot or image",
                            tint = if (attachedBitmap != null) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Capture Photo with Camera
                    IconButton(
                        onClick = { handleCameraButtonClick() },
                        modifier = Modifier.testTag("camera_capture_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture photo with camera",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Toggle Screen Reading Context
                    IconButton(
                        onClick = {
                            onToggleAttachScreen()
                        },
                        modifier = Modifier.testTag("toggle_screen_read_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenshotMonitor,
                            contentDescription = "Attach on-screen content",
                            tint = if (attachScreenToNextPrompt) NovaAmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Chat Input Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isListening) {
                                    "Listening... Speak now"
                                } else {
                                    "Ask NOVA in Hindi, Hinglish, or English..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() || attachedBitmap != null) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    // Send Button (when text or image present)
                    if (inputText.isNotBlank() || attachedBitmap != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onSendMessage(inputText)
                                    inputText = ""
                                    focusManager.clearFocus()
                                }
                                .testTag("send_message_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Large Futuristic Microphone Button
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isListening -> NovaEmeraldTertiary
                            !settings.voiceInputEnabled -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = if (isListening) Color.White else MaterialTheme.colorScheme.secondary,
                                shape = CircleShape
                            )
                            .clickable { handleMicButtonClick() }
                            .testTag("mic_voice_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isListening) {
                                    Icons.Default.Stop
                                } else if (!settings.voiceInputEnabled) {
                                    Icons.Default.MicOff
                                } else {
                                    Icons.Default.Mic
                                },
                                contentDescription = "Activate voice input",
                                tint = if (isListening) {
                                    Color.Black
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Microphone Permission Rationale Dialog
    if (showMicRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showMicRationaleDialog = false },
            title = { Text("Microphone Permission Required") },
            text = {
                Text(
                    "NOVA AI Assistant needs Microphone access ONLY when you tap the Voice button to convert your spoken Hindi, Hinglish, or English questions into text. NOVA never records audio in the background."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMicRationaleDialog = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) {
                    Text("Grant Microphone")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicRationaleDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Camera Permission Rationale Dialog
    if (showCameraRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraRationaleDialog = false },
            title = { Text("Camera Permission Required") },
            text = {
                Text(
                    "NOVA AI uses the camera only when you choose to take a snapshot of a document, question, or error screen for visual explanation. You can also attach existing screenshots without any permission using the Gallery button."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraRationaleDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Allow Camera")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Screen Reading Permission Dialog
    if (showScreenPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showScreenPermissionDialog = false },
            title = { Text("Enable Screen Understanding") },
            text = {
                Text(
                    "To read live text from other apps on your phone, enable 'NOVA AI Assistant' in Android Accessibility Settings.\n\n" +
                        "Privacy Guarantee:\n" +
                        "• NOVA only reads the screen when you explicitly ask.\n" +
                        "• Passwords, banking PINs, and OTPs are automatically skipped and redacted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScreenPermissionDialog = false
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                ) {
                    Text("Open Accessibility Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScreenPermissionDialog = false }) {
                    Text("Use Sample Screen Context")
                }
            }
        )
    }

    // Clear Current Chat Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Current Conversation?") },
            text = { Text("This will remove all messages in the current chat session.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearChat()
                    }
                ) {
                    Text("Clear Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
