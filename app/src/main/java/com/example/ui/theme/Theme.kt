package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val NovaDarkColorScheme = darkColorScheme(
    primary = NovaCyanPrimary,
    onPrimary = OnNovaCyanPrimary,
    primaryContainer = NovaCyanContainer,
    onPrimaryContainer = OnNovaCyanContainer,
    secondary = NovaVioletSecondary,
    onSecondary = OnNovaVioletSecondary,
    secondaryContainer = NovaVioletContainer,
    onSecondaryContainer = OnNovaVioletContainer,
    tertiary = NovaEmeraldTertiary,
    onTertiary = OnNovaEmeraldTertiary,
    tertiaryContainer = NovaEmeraldContainer,
    onTertiaryContainer = OnNovaEmeraldContainer,
    background = NovaObsidianBg,
    onBackground = NovaTextPrimaryDark,
    surface = NovaObsidianSurface,
    onSurface = NovaTextPrimaryDark,
    surfaceVariant = NovaObsidianCard,
    onSurfaceVariant = NovaTextSecondaryDark,
    surfaceContainerHigh = NovaObsidianElevated,
    outline = NovaOutlineDark,
    error = NovaRoseError
)

private val NovaLightColorScheme = lightColorScheme(
    primary = NovaCyanLightPrimary,
    onPrimary = OnNovaCyanLightPrimary,
    primaryContainer = NovaCyanLightContainer,
    onPrimaryContainer = OnNovaCyanLightContainer,
    secondary = NovaVioletLightSecondary,
    onSecondary = OnNovaVioletLightSecondary,
    secondaryContainer = NovaVioletLightContainer,
    onSecondaryContainer = OnNovaVioletLightContainer,
    tertiary = NovaEmeraldLightTertiary,
    onTertiary = OnNovaEmeraldLightTertiary,
    tertiaryContainer = NovaEmeraldLightContainer,
    onTertiaryContainer = OnNovaEmeraldLightContainer,
    background = NovaLightBg,
    onBackground = NovaTextPrimaryLight,
    surface = NovaLightSurface,
    onSurface = NovaTextPrimaryLight,
    surfaceVariant = NovaLightCard,
    onSurfaceVariant = NovaTextSecondaryLight,
    surfaceContainerHigh = NovaLightElevated,
    outline = NovaOutlineLight,
    error = NovaRoseError
)

val NovaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NovaDarkColorScheme else NovaLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NovaShapes,
        content = content
    )
}
