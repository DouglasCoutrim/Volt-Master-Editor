package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VoltMasterColorScheme = darkColorScheme(
    primary = ElectricNeonGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003810),
    onPrimaryContainer = ElectricGreenBright,
    secondary = MetallicSilver,
    onSecondary = Color.Black,
    secondaryContainer = DarkBorder,
    onSecondaryContainer = MetallicSilverLight,
    tertiary = ElectricGreenBright,
    background = DeepBlack,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = MetallicSilver,
    outline = DarkBorder,
    outlineVariant = Color(0xFF38383A),
    error = ErrorRed,
    onError = Color.Black
)

@Composable
fun VoltMasterTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DeepBlack.toArgb()
                window.navigationBarColor = DeepBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = VoltMasterColorScheme,
        typography = Typography,
        content = content
    )
}

