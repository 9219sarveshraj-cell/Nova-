package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ScreenshotMonitor
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ThemePreference
import com.example.ui.screens.AssistantChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PermissionsAndPrivacyScreen
import com.example.ui.screens.ScreenAndCommandsHubScreen
import com.example.ui.screens.SettingsAndApiScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NovaViewModel

enum class NovaDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ASSISTANT(
        route = "assistant",
        label = "Assistant",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    SCREEN_COMMANDS(
        route = "screen_commands",
        label = "Screen & Cmds",
        selectedIcon = Icons.Filled.ScreenshotMonitor,
        unselectedIcon = Icons.Outlined.ScreenshotMonitor
    ),
    HISTORY(
        route = "history",
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    PRIVACY_PERMS(
        route = "privacy_perms",
        label = "Privacy",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaAppRoot()
        }
    }
}

@Composable
fun NovaAppRoot(novaViewModel: NovaViewModel = viewModel()) {
    val settings by novaViewModel.settingsState.collectAsStateWithLifecycle()
    val messages by novaViewModel.currentMessages.collectAsStateWithLifecycle()
    val allSessions by novaViewModel.allSessions.collectAsStateWithLifecycle()
    val currentSessionId by novaViewModel.currentSessionId.collectAsStateWithLifecycle()
    val assistantState by novaViewModel.assistantState.collectAsStateWithLifecycle()
    val isListening by novaViewModel.isListening.collectAsStateWithLifecycle()
    val partialTranscript by novaViewModel.partialTranscript.collectAsStateWithLifecycle()
    val audioLevel by novaViewModel.audioLevel.collectAsStateWithLifecycle()
    val isSpeaking by novaViewModel.isSpeaking.collectAsStateWithLifecycle()
    val speakingMessageText by novaViewModel.speakingMessageText.collectAsStateWithLifecycle()
    val isThinking by novaViewModel.isThinking.collectAsStateWithLifecycle()
    val isScreenServiceConnected by novaViewModel.isScreenServiceConnected.collectAsStateWithLifecycle()
    val isAnalyzingScreenNow by novaViewModel.isAnalyzingScreenNow.collectAsStateWithLifecycle()
    val activeScreenSnapshot by novaViewModel.activeScreenSnapshot.collectAsStateWithLifecycle()
    val attachScreenToNextPrompt by novaViewModel.attachScreenContextToNextPrompt.collectAsStateWithLifecycle()
    val attachedBitmap by novaViewModel.attachedBitmap.collectAsStateWithLifecycle()
    val attachedImageLabel by novaViewModel.attachedImageLabel.collectAsStateWithLifecycle()
    val statusBannerMessage by novaViewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val voiceStatusMessage by novaViewModel.voiceStatusMessage.collectAsStateWithLifecycle()
    val isFloatingOverlayRunning by novaViewModel.isFloatingOverlayRunning.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (settings.themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> systemDark
    }

    var currentDestination by rememberSaveable { mutableStateOf(NovaDestination.ASSISTANT) }

    // Ensure Back press from any secondary tab returns to the Assistant screen
    BackHandler(enabled = currentDestination != NovaDestination.ASSISTANT) {
        currentDestination = NovaDestination.ASSISTANT
    }

    MyApplicationTheme(darkTheme = useDarkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 680.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            NovaDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${dest.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    // Quick Floating Assistant Button on secondary tabs to jump to NOVA Voice/Chat
                    if (currentDestination != NovaDestination.ASSISTANT) {
                        FloatingActionButton(
                            onClick = {
                                currentDestination = NovaDestination.ASSISTANT
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("floating_assistant_quick_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Open NOVA AI Assistant"
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NovaDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_rail_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentDestination) {
                            NovaDestination.ASSISTANT -> {
                                AssistantChatScreen(
                                    messages = messages,
                                    settings = settings,
                                    assistantState = assistantState,
                                    isListening = isListening,
                                    partialTranscript = partialTranscript,
                                    audioLevel = audioLevel,
                                    isSpeaking = isSpeaking,
                                    speakingMessageText = speakingMessageText,
                                    isThinking = isThinking,
                                    isScreenServiceConnected = isScreenServiceConnected,
                                    isAnalyzingScreenNow = isAnalyzingScreenNow,
                                    activeScreenSnapshot = activeScreenSnapshot,
                                    attachScreenToNextPrompt = attachScreenToNextPrompt,
                                    attachedBitmap = attachedBitmap,
                                    attachedImageLabel = attachedImageLabel,
                                    statusBannerMessage = statusBannerMessage,
                                    voiceStatusMessage = voiceStatusMessage,
                                    onSendMessage = { prompt ->
                                        novaViewModel.sendMessage(prompt)
                                    },
                                    onRetryPrompt = { prompt ->
                                        novaViewModel.retryPrompt(prompt)
                                    },
                                    onToggleVoice = {
                                        novaViewModel.toggleVoiceListening()
                                    },
                                    onToggleSpeakMessage = { text ->
                                        novaViewModel.toggleSpeakMessage(text)
                                    },
                                    onStopSpeaking = {
                                        novaViewModel.stopSpeaking()
                                    },
                                    onToggleAttachScreen = {
                                        novaViewModel.toggleAttachScreenContext()
                                    },
                                    onTriggerScreenCommand = { phrase ->
                                        novaViewModel.triggerScreenReadCommand(phrase)
                                    },
                                    onAttachImageUri = { uri ->
                                        novaViewModel.attachImageFromUri(uri)
                                    },
                                    onAttachCameraBitmap = { bmp ->
                                        novaViewModel.attachCameraBitmap(bmp)
                                    },
                                    onClearAttachedImage = {
                                        novaViewModel.clearAttachedImage()
                                    },
                                    onLanguageChange = { lang ->
                                        novaViewModel.setLanguage(lang)
                                    },
                                    onDepthChange = { depth ->
                                        novaViewModel.setResponseDepth(depth)
                                    },
                                    onNewChat = {
                                        novaViewModel.createNewChatSession()
                                    },
                                    onClearChat = {
                                        novaViewModel.clearCurrentSessionMessages()
                                    },
                                    onDismissStatusBanner = {
                                        novaViewModel.clearStatusBannerMessage()
                                    },
                                    onNavigateToScreenHub = {
                                        currentDestination = NovaDestination.SCREEN_COMMANDS
                                    }
                                )
                            }

                            NovaDestination.SCREEN_COMMANDS -> {
                                ScreenAndCommandsHubScreen(
                                    isAccessibilityConnected = isScreenServiceConnected || novaViewModel.isAccessibilityGranted(),
                                    activeSnapshot = activeScreenSnapshot,
                                    onRefreshLiveScreen = {
                                        novaViewModel.refreshLiveScreenCapture()
                                    },
                                    onSelectSampleSnapshot = { sample ->
                                        novaViewModel.selectSampleScreenSnapshot(sample)
                                    },
                                    onExecuteCommandInChat = { preset ->
                                        currentDestination = NovaDestination.ASSISTANT
                                        if (preset.requiresScreenContext) {
                                            novaViewModel.triggerScreenReadCommand(preset.phrase)
                                        } else {
                                            novaViewModel.sendMessage(preset.phrase)
                                        }
                                    }
                                )
                            }

                            NovaDestination.HISTORY -> {
                                HistoryScreen(
                                    sessions = allSessions,
                                    currentSessionId = currentSessionId,
                                    onSelectSession = { sessionId ->
                                        novaViewModel.selectSession(sessionId)
                                        currentDestination = NovaDestination.ASSISTANT
                                    },
                                    onCreateNewSession = {
                                        novaViewModel.createNewChatSession()
                                        currentDestination = NovaDestination.ASSISTANT
                                    },
                                    onDeleteSession = { sessionId ->
                                        novaViewModel.deleteSession(sessionId)
                                    },
                                    onDeleteAllHistory = {
                                        novaViewModel.deleteAllConversationHistory()
                                    }
                                )
                            }

                            NovaDestination.PRIVACY_PERMS -> {
                                PermissionsAndPrivacyScreen(
                                    settings = settings,
                                    isAccessibilityConnected = isScreenServiceConnected || novaViewModel.isAccessibilityGranted(),
                                    isFloatingOverlayRunning = isFloatingOverlayRunning,
                                    onStrictRedactionToggle = { enabled ->
                                        novaViewModel.setStrictPrivacyRedactionEnabled(enabled)
                                    },
                                    onShowScreenIndicatorToggle = { enabled ->
                                        novaViewModel.setShowScreenReadingIndicator(enabled)
                                    },
                                    onToggleFloatingAssistant = { enabled ->
                                        novaViewModel.toggleFloatingAssistantOverlay(enabled)
                                    },
                                    onDeleteAllHistory = {
                                        novaViewModel.deleteAllConversationHistory()
                                    }
                                )
                            }

                            NovaDestination.SETTINGS -> {
                                SettingsAndApiScreen(
                                    settings = settings,
                                    isApiKeyConfigured = novaViewModel.isApiKeyConfigured(),
                                    isInternetConnected = novaViewModel.isInternetConnected(),
                                    isAccessibilityGranted = isScreenServiceConnected || novaViewModel.isAccessibilityGranted(),
                                    isOverlayGranted = novaViewModel.isOverlayPermissionGranted(),
                                    onLanguageSelected = { lang ->
                                        novaViewModel.setLanguage(lang)
                                    },
                                    onDepthSelected = { depth ->
                                        novaViewModel.setResponseDepth(depth)
                                    },
                                    onThemeSelected = { theme ->
                                        novaViewModel.setThemePreference(theme)
                                    },
                                    onModelSelected = { model ->
                                        novaViewModel.setSelectedModel(model)
                                    },
                                    onVoiceInputToggle = { enabled ->
                                        novaViewModel.setVoiceInputEnabled(enabled)
                                    },
                                    onTtsAutoReadToggle = { enabled ->
                                        novaViewModel.setTtsAutoReadEnabled(enabled)
                                    },
                                    onSpeechRateChange = { rate ->
                                        novaViewModel.setSpeechRate(rate)
                                    },
                                    onTestTtsVoice = {
                                        novaViewModel.toggleSpeakMessage(
                                            "Namaste! I am NOVA AI Assistant. Voice synthesis is working smoothly."
                                        )
                                    },
                                    onWebSearchToggle = { enabled ->
                                        novaViewModel.setWebSearchGroundingEnabled(enabled)
                                    },
                                    onClearAllHistory = {
                                        novaViewModel.deleteAllConversationHistory()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
