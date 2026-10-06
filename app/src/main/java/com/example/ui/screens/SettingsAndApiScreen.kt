package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.NovaUserSettings
import com.example.model.AiModelOption
import com.example.model.NovaLanguage
import com.example.model.ResponseDepth
import com.example.model.ThemePreference
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmeraldTertiary
import com.example.ui.theme.NovaRoseError
import com.example.ui.theme.NovaVioletSecondary
import java.util.Locale

@Composable
fun SettingsAndApiScreen(
    settings: NovaUserSettings,
    isApiKeyConfigured: Boolean,
    isInternetConnected: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onLanguageSelected: (NovaLanguage) -> Unit,
    onDepthSelected: (ResponseDepth) -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onModelSelected: (AiModelOption) -> Unit,
    onVoiceInputToggle: (Boolean) -> Unit,
    onTtsAutoReadToggle: (Boolean) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onTestTtsVoice: () -> Unit,
    onWebSearchToggle: (Boolean) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_and_api_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings & AI Engine Configuration",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customize language, voice synthesis, AI reasoning model, web grounding, and appearance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Language & Answer Style Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = NovaCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Assistant Language & Answer Style",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    NovaLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLanguageSelected(lang) }
                                .padding(vertical = 4.dp)
                                .testTag("settings_lang_${lang.code}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.language == lang,
                                onClick = { onLanguageSelected(lang) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Default Answer Depth (Concise by default, Detailed on demand):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResponseDepth.entries.forEach { depth ->
                            FilterChip(
                                selected = settings.responseDepth == depth,
                                onClick = { onDepthSelected(depth) },
                                label = { Text(depth.label) },
                                modifier = Modifier.testTag("settings_depth_${depth.id}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Voice & Text-to-Speech Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = NovaEmeraldTertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voice Input & Text-to-Speech (TTS)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Input (Speech-to-Text)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Enable microphone button & animated listening orb",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.voiceInputEnabled,
                            onCheckedChange = onVoiceInputToggle,
                            modifier = Modifier.testTag("settings_voice_input_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Read Answers Aloud (Auto TTS)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Automatically speak NOVA's answers aloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.ttsAutoReadEnabled,
                            onCheckedChange = onTtsAutoReadToggle,
                            modifier = Modifier.testTag("settings_tts_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "TTS Speech Speed: ${String.format(Locale.US, "%.1fx", settings.speechRate)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = settings.speechRate,
                        onValueChange = onSpeechRateChange,
                        valueRange = 0.6f..1.6f,
                        modifier = Modifier.testTag("settings_speech_rate_slider")
                    )

                    OutlinedButton(
                        onClick = onTestTtsVoice,
                        modifier = Modifier.testTag("test_tts_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test NOVA Voice Output")
                    }
                }
            }
        }

        // 3. AI Model & API Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = NovaVioletSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Answer Engine & API Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secure API Key Status Box (Never exposes raw key!)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isApiKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (isApiKeyConfigured) NovaEmeraldTertiary else NovaAmberWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isApiKeyConfigured) {
                                        "GEMINI_API_KEY: Configured via Secure BuildConfig"
                                    } else {
                                        "GEMINI_API_KEY: Using Default Placeholder"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isApiKeyConfigured) NovaEmeraldTertiary else NovaAmberWarning
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "API keys are managed via the Secrets panel in AI Studio (injected through BuildConfig) and are never displayed in the UI or written to logs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = NovaAmberWarning,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Prototype Security Notice: Android APKs can be decompiled. Do not share this prototype APK publicly if it contains active API keys.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Gemini AI Model:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    AiModelOption.entries.forEach { modelOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModelSelected(modelOption) }
                                .padding(vertical = 4.dp)
                                .testTag("settings_model_${modelOption.modelId}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.selectedModel == modelOption,
                                onClick = { onModelSelected(modelOption) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = modelOption.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = modelOption.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = NovaEmeraldTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Live Web Search Grounding",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = "Use Google Search grounding for current info and clearly badge responses vs general knowledge.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.webSearchGroundingEnabled,
                            onCheckedChange = onWebSearchToggle,
                            modifier = Modifier.testTag("settings_web_search_switch")
                        )
                    }
                }
            }
        }

        // 4. Appearance / Theme Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = NovaCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance & Theme Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { themePref ->
                            FilterChip(
                                selected = settings.themePreference == themePref,
                                onClick = { onThemeSelected(themePref) },
                                label = { Text(themePref.label) },
                                modifier = Modifier.testTag("settings_theme_${themePref.id}")
                            )
                        }
                    }
                }
            }
        }

        // 5. About NOVA AI & System Status Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NovaCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About NOVA AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "App Name: NOVA AI Assistant\n" +
                            "Package: com.nova.aiassistant (v1.0)\n" +
                            "Network Status: ${if (isInternetConnected) "Online" else "Offline"}\n" +
                            "Screen Reader Status: ${if (isAccessibilityGranted) "Permitted" else "Not Enabled"}\n" +
                            "Overlay Status: ${if (isOverlayGranted) "Permitted" else "Not Enabled"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onClearAllHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = NovaRoseError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Chat History")
                    }
                }
            }
        }
    }
}
