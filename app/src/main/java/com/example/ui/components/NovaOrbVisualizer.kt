package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.NovaAmberWarning
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmeraldTertiary
import com.example.ui.theme.NovaRoseError
import com.example.ui.theme.NovaVioletSecondary
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun NovaOrbVisualizer(
    state: AssistantState,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 128.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nova_orb_transition")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 2600
                    AssistantState.LISTENING, AssistantState.SPEAKING -> 4000
                    AssistantState.ANALYZING_SCREEN -> 3200
                    else -> 9000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 650
                    AssistantState.SPEAKING -> 550
                    AssistantState.THINKING -> 800
                    AssistantState.ANALYZING_SCREEN -> 750
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_wave_phase"
    )

    val primaryOrbColor = when (state) {
        AssistantState.IDLE -> NovaCyanPrimary
        AssistantState.LISTENING -> NovaEmeraldTertiary
        AssistantState.ANALYZING_SCREEN -> NovaAmberWarning
        AssistantState.THINKING -> NovaVioletSecondary
        AssistantState.SPEAKING -> NovaCyanPrimary
        AssistantState.ERROR -> NovaRoseError
    }

    val secondaryOrbColor = when (state) {
        AssistantState.IDLE -> NovaVioletSecondary
        AssistantState.LISTENING -> NovaCyanPrimary
        AssistantState.ANALYZING_SCREEN -> NovaCyanPrimary
        AssistantState.THINKING -> NovaCyanPrimary
        AssistantState.SPEAKING -> NovaVioletSecondary
        AssistantState.ERROR -> NovaAmberWarning
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(orbSize)
            .testTag("nova_ai_orb")
            .semantics {
                contentDescription = "NOVA AI Orb: ${state.statusLabel}. Tap to toggle voice assistant."
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = orbSize / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = min(size.width, size.height)
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val dynamicBoost = if (state == AssistantState.LISTENING) audioLevel * 0.25f else 0f
            val baseRadius = (minDim * 0.28f) * (pulseScale + dynamicBoost)

            // Outer atmospheric radial halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryOrbColor.copy(alpha = 0.42f),
                        secondaryOrbColor.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = minDim * 0.49f
                ),
                radius = minDim * 0.49f,
                center = centerOffset
            )

            // Rotating futuristic cybernetic orbital arcs
            rotate(degrees = rotationAngle, pivot = centerOffset) {
                val arcRadius = minDim * 0.40f
                val arcTopLeft = Offset(centerOffset.x - arcRadius, centerOffset.y - arcRadius)
                val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryOrbColor.copy(alpha = 0.85f),
                            secondaryOrbColor.copy(alpha = 0.9f),
                            Color.Transparent
                        ),
                        center = centerOffset
                    ),
                    startAngle = 15f,
                    sweepAngle = 135f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = minDim * 0.025f, cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryOrbColor.copy(alpha = 0.85f),
                            primaryOrbColor.copy(alpha = 0.9f),
                            Color.Transparent
                        ),
                        center = centerOffset
                    ),
                    startAngle = 195f,
                    sweepAngle = 135f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = minDim * 0.025f, cap = StrokeCap.Round)
                )
            }

            // Counter-rotating inner harmonic ring
            rotate(degrees = -rotationAngle * 1.3f, pivot = centerOffset) {
                val innerArcRadius = minDim * 0.34f
                val innerTopLeft = Offset(centerOffset.x - innerArcRadius, centerOffset.y - innerArcRadius)
                val innerSize = Size(innerArcRadius * 2f, innerArcRadius * 2f)
                drawArc(
                    color = secondaryOrbColor.copy(alpha = 0.55f),
                    startAngle = 60f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = minDim * 0.015f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryOrbColor.copy(alpha = 0.55f),
                    startAngle = 240f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = minDim * 0.015f, cap = StrokeCap.Round)
                )
            }

            // Harmonic waveform ring when listening, speaking, or thinking
            if (state != AssistantState.IDLE) {
                val wavePath = Path()
                val points = 72
                val waveAmp = minDim * (0.03f + dynamicBoost * 0.14f)
                val ringBase = minDim * 0.31f
                for (i in 0..points) {
                    val angle = (i.toFloat() / points) * (2f * Math.PI).toFloat()
                    val harmonic = sin(angle * 6f + wavePhase) * cos(angle * 3f - wavePhase)
                    val r = ringBase + harmonic * waveAmp
                    val x = centerOffset.x + r * cos(angle)
                    val y = centerOffset.y + r * sin(angle)
                    if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                }
                wavePath.close()
                drawPath(
                    path = wavePath,
                    color = primaryOrbColor.copy(alpha = 0.78f),
                    style = Stroke(width = minDim * 0.018f, cap = StrokeCap.Round)
                )
            }

            // Core glowing neural sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryOrbColor,
                        secondaryOrbColor.copy(alpha = 0.9f),
                        Color(0xFF0A1124)
                    ),
                    center = Offset(
                        centerOffset.x - baseRadius * 0.18f,
                        centerOffset.y - baseRadius * 0.18f
                    ),
                    radius = baseRadius * 1.15f
                ),
                radius = baseRadius,
                center = centerOffset
            )
        }
    }
}
