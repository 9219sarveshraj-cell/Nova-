package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ChatMessageEntity
import com.example.model.MessageSourceBadge
import com.example.model.ScreenSnapshot
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmeraldTertiary
import com.example.ui.theme.NovaRoseError
import com.example.ui.theme.NovaVioletSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenReadingStatusBanner(
    isScreenServiceConnected: Boolean,
    isAnalyzingNow: Boolean,
    activeSnapshot: ScreenSnapshot?,
    showBanner: Boolean,
    onInspectClick: () -> Unit,
    onClearSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showBanner || (activeSnapshot == null && !isAnalyzingNow)) return

    val statusColor = when {
        isAnalyzingNow -> NovaAmberWarning
        isScreenServiceConnected -> NovaEmeraldTertiary
        else -> NovaCyanPrimary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("screen_reading_status_indicator"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = statusColor.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onInspectClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ScreenshotMonitor,
                    contentDescription = "Screen reading status",
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isAnalyzingNow) {
                            "Analyzing Screen Content Now..."
                        } else {
                            "Screen Context Ready: ${activeSnapshot?.screenTitle ?: "Active Window"}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(if (isScreenServiceConnected) "Live Accessibility" else "Inspector Context")
                            append(" • Passwords/OTPs Protected")
                            if ((activeSnapshot?.redactedSensitiveCount ?: 0) > 0) {
                                append(" (${activeSnapshot?.redactedSensitiveCount} redacted)")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            TextButton(
                onClick = onClearSnapshot,
                modifier = Modifier.testTag("detach_screen_context_button")
            ) {
                Text(
                    text = "Detach",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    isSpeakingThis: Boolean,
    onSpeakToggle: (String) -> Unit,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showScreenSnippetDetails by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        // Sender & badges row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            Text(
                text = if (message.isFromUser) "You" else "✦ NOVA AI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (message.isFromUser) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Text(
                text = "• ${message.languageBadge} • $formattedTime",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val bubbleShape = if (message.isFromUser) {
            RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        }

        val borderColor = when {
            message.isError -> NovaRoseError.copy(alpha = 0.65f)
            message.isFromUser -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        }

        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .border(width = 1.dp, color = borderColor, shape = bubbleShape),
            shape = bubbleShape,
            color = when {
                message.isError -> NovaRoseError.copy(alpha = 0.12f)
                message.isFromUser -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
            },
            tonalElevation = if (message.isFromUser) 2.dp else 4.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Attached Screen Context indicator inside message
                if (!message.attachedScreenTitle.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { showScreenSnippetDetails = !showScreenSnippetDetails }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Attached screen context",
                                    tint = NovaAmberWarning,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Screen Read: ${message.attachedScreenTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NovaAmberWarning,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (showScreenSnippetDetails) "Hide" else "Inspect",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = showScreenSnippetDetails) {
                                Text(
                                    text = message.attachedScreenSnippet.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Attached Image indicator inside message
                if (message.hasAttachedImage) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Attached image",
                                tint = NovaCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.attachedImageDescription ?: "Screenshot / Image attached",
                                style = MaterialTheme.typography.labelMedium,
                                color = NovaCyanPrimary
                            )
                        }
                    }
                }

                // Message Content
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isFromUser) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Footer for Assistant messages: Knowledge Source Badge + TTS + Copy + Retry
                if (!message.isFromUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SourceBadgePill(sourceBadgeLabel = message.sourceBadge, isError = message.isError)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSpeakToggle(message.content) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("tts_speak_message_${message.id}")
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThis) {
                                        Icons.Default.StopCircle
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    },
                                    contentDescription = if (isSpeakingThis) "Stop reading aloud" else "Read answer aloud",
                                    tint = if (isSpeakingThis) NovaEmeraldTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("copy_message_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy response text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Retry button if error or if retryPrompt is present
                    if (message.isError || !message.retryPrompt.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    onRetry(message.retryPrompt ?: "Please retry the last question.")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (message.isError) NovaRoseError else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (message.isError) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry request",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (message.isError) "Retry Request" else "Regenerate",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBadgePill(sourceBadgeLabel: String, isError: Boolean) {
    val (icon, tint) = when {
        isError -> Icons.Default.ErrorOutline to NovaRoseError
        sourceBadgeLabel == MessageSourceBadge.LIVE_WEB_SEARCH.label -> Icons.Default.Language to NovaEmeraldTertiary
        sourceBadgeLabel == MessageSourceBadge.SCREEN_CONTEXT.label -> Icons.Default.ScreenshotMonitor to NovaAmberWarning
        sourceBadgeLabel == MessageSourceBadge.IMAGE_ANALYSIS.label -> Icons.Default.Image to NovaCyanPrimary
        else -> Icons.Default.Psychology to NovaVioletSecondary
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = sourceBadgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCommandChipsRow(
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPrompts = listOf(
        "Screen par kya likha hai?",
        "Is page ko samjhao.",
        "Is question ka answer batao.",
        "Is error ko samjhao.",
        "NOVA, mujhe step by step samjhao.",
        "NOVA, Hindi mein jawab do."
    )

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        quickPrompts.forEachIndexed { index, phrase ->
            AssistChip(
                onClick = { onCommandSelected(phrase) },
                label = {
                    Text(
                        text = phrase,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (index < 4) Icons.Default.ScreenshotMonitor else Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("quick_chip_$index")
            )
        }
    }
}
