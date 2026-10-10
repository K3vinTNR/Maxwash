package org.umn.maxwash.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MaxwashColors = lightColorScheme(
    primary = WashTeal, onPrimary = Color.White,
    primaryContainer = WashSoftBlue, onPrimaryContainer = WashTeal,
    secondary = WashGreen, onSecondary = Color.White,
    secondaryContainer = WashMint, onSecondaryContainer = WashGreen,
    tertiary = WashBlue, onTertiary = Color.White,
    tertiaryContainer = WashLavender, onTertiaryContainer = WashNavy,
    background = WashBackground, onBackground = WashNavy,
    surface = Color.White, onSurface = WashNavy,
    surfaceVariant = WashSoftBlue, onSurfaceVariant = WashMuted,
    outline = WashMuted, outlineVariant = WashBorder,
    error = WashError, onError = Color.White,
    errorContainer = Color(0xFFFFEDEF), onErrorContainer = WashError
)

val MaxwashShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MaxwashTheme(content: @Composable () -> Unit) {
    // The supplied mockups only define light mode; retain this palette on all devices.
    MaterialTheme(
        colorScheme = MaxwashColors,
        typography = Typography,
        shapes = MaxwashShapes,
        content = content
    )
}
