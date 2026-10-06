package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.NovaUserSettings
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmeraldTertiary
import com.example.ui.theme.NovaRoseError

@Composable
fun PermissionsAndPrivacyScreen(
    settings: NovaUserSettings,
    isAccessibilityConnected: Boolean,
    isFloatingOverlayRunning: Boolean,
    onStrictRedactionToggle: (Boolean) -> Unit,
    onShowScreenIndicatorToggle: (Boolean) -> Unit,
    onToggleFloatingAssistant: (Boolean) -> Unit,
    onDeleteAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    // Refresh permission states when returning from Android System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasMicPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("permissions_and_privacy_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Permissions & Privacy Center",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Transparent, explicit permission control. NOVA never records audio or screen activity in the background.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Core Privacy Commitments Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = NovaCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOVA Zero-Surveillance Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Never secretly records microphone audio or captures the screen.\n" +
                            "• Never accesses passwords, banking PINs, OTPs, or private credentials.\n" +
                            "• Always displays a clear indicator whenever screen content is analyzed.\n" +
                            "• Stores API keys via BuildConfig and never logs or exposes raw keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 1. Microphone Permission Card
        item {
            PermissionStatusCard(
                icon = Icons.Default.Mic,
                title = "Microphone (Voice Assistant)",
                isGranted = hasMicPermission,
                whyRequired = "Required only when you tap the Microphone or AI Orb to speak your questions in Hindi, Hinglish, or English.",
                actionLabel = if (hasMicPermission) "Granted" else "Grant Microphone",
                onActionClick = {
                    if (!hasMicPermission) {
                        micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                testTag = "perm_card_microphone"
            )
        }

        // 2. Screen Reading Accessibility Service Card
        item {
            PermissionStatusCard(
                icon = Icons.Default.ScreenshotMonitor,
                title = "Accessibility Screen Understanding",
                isGranted = isAccessibilityConnected,
                whyRequired = "Required only when you ask NOVA commands like \"Screen par kya likha hai?\" or \"Is error ko samjhao\". Automatically skips password and OTP fields.",
                actionLabel = if (isAccessibilityConnected) "Configure in Settings" else "Enable in Accessibility",
                onActionClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (_: Exception) {
                    }
                },
                testTag = "perm_card_accessibility"
            )
        }

        // 3. Floating Assistant Overlay Permission Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("perm_card_overlay")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BubbleChart,
                                contentDescription = null,
                                tint = if (hasOverlayPermission) NovaEmeraldTertiary else NovaAmberWarning
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Floating Assistant Overlay",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasOverlayPermission) {
                                        if (isFloatingOverlayRunning) "Overlay Bubble Active" else "Permission Granted (Ready)"
                                    } else {
                                        "Optional Permission Not Granted"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasOverlayPermission) NovaEmeraldTertiary else NovaAmberWarning
                                )
                            }
                        }

                        Switch(
                            checked = settings.floatingAssistantEnabled && hasOverlayPermission,
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled && !hasOverlayPermission) {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                } else {
                                    onToggleFloatingAssistant(wantEnabled)
                                }
                            },
                            modifier = Modifier.testTag("floating_assistant_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Displays an optional draggable NOVA AI button over other apps so you can open NOVA Assistant with one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.testTag("open_overlay_settings_button")
                    ) {
                        Text(if (hasOverlayPermission) "Manage Overlay Permission" else "Grant Overlay Permission")
                    }
                }
            }
        }

        // 4. Camera Permission Card
        item {
            PermissionStatusCard(
                icon = Icons.Default.CameraAlt,
                title = "Camera (Document & Error Snapshot)",
                isGranted = hasCameraPermission,
                whyRequired = "Optional permission to capture a photo of a printed question, diagram, or error screen. Selecting existing screenshots uses the zero-permission Photo Picker.",
                actionLabel = if (hasCameraPermission) "Granted" else "Allow Camera",
                onActionClick = {
                    if (!hasCameraPermission) {
                        cameraPermLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                testTag = "perm_card_camera"
            )
        }

        // Privacy Controls Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NovaCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy & Security Safeguards",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
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
                                text = "Redact Passwords, OTPs & Banking PINs",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically scrubs sensitive credentials and blocks banking/UPI windows before screen reading.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.strictPrivacyRedactionEnabled,
                            onCheckedChange = onStrictRedactionToggle,
                            modifier = Modifier.testTag("privacy_redaction_switch")
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
                                text = "Show Screen-Reading Active Indicator",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Always show a top banner whenever screen content is attached or being analyzed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.showScreenReadingIndicator,
                            onCheckedChange = onShowScreenIndicatorToggle,
                            modifier = Modifier.testTag("screen_indicator_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDeleteAllHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = NovaRoseError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("privacy_delete_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All Stored Conversation History")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    whyRequired: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) NovaEmeraldTertiary else NovaAmberWarning
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = (if (isGranted) NovaEmeraldTertiary else NovaAmberWarning).copy(alpha = 0.16f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = if (isGranted) "Status: Granted" else "Status: Ask When Needed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGranted) NovaEmeraldTertiary else NovaAmberWarning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) NovaEmeraldTertiary else NovaAmberWarning)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = whyRequired,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onActionClick,
                enabled = !isGranted || title.contains("Accessibility"),
                modifier = Modifier.testTag("${testTag}_action")
            ) {
                Text(actionLabel)
            }
        }
    }
}
