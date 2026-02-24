package com.runanywhere.startup_hackathon20.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Green Theme
private val LightGreenColorScheme = lightColorScheme(
    primary = Green80,
    onPrimary = White,
    primaryContainer = GreenLight,
    onPrimaryContainer = DarkGray,

    secondary = GreenDark,
    onSecondary = White,
    secondaryContainer = GreenLight,
    onSecondaryContainer = DarkGray,

    tertiary = GreenDark,
    onTertiary = White,

    background = GreenBackground,
    onBackground = Black,

    surface = GreenSurface,
    onSurface = Black,

    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,

    error = androidx.compose.ui.graphics.Color(0xFFEF4444),
    onError = White
)

// Dark Theme (Indigo/Slate)
private val DarkColorScheme = darkColorScheme(
    primary = DarkSecondary,
    onPrimary = White,
    primaryContainer = DarkPrimary,
    onPrimaryContainer = White,

    secondary = DarkTertiary,
    onSecondary = Black,
    secondaryContainer = DarkPrimary,
    onSecondaryContainer = White,

    tertiary = DarkTertiary,
    onTertiary = Black,

    background = DarkBackground,
    onBackground = White,

    surface = DarkSurface,
    onSurface = White,

    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF334155),
    onSurfaceVariant = LightGray,

    error = androidx.compose.ui.graphics.Color(0xFFF87171),
    onError = Black
)

// ==================== VIBRANT NEON THEME (DEFAULT) ====================
private val NeonColorScheme = lightColorScheme(
    primary = NeonPrimary,
    onPrimary = White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE879F9),
    onPrimaryContainer = Black,

    secondary = NeonSecondary,
    onSecondary = Black,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF37E5F7),
    onSecondaryContainer = Black,

    tertiary = NeonTertiary,
    onTertiary = White,

    background = NeonBackground,
    onBackground = Black,

    surface = NeonSurface,
    onSurface = Black,

    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,

    error = androidx.compose.ui.graphics.Color(0xFFEF4444),
    onError = White
)

// ==================== VIBRANT ORANGE THEME ====================
private val OrangeColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFB399),
    onPrimaryContainer = Black,

    secondary = OrangeSecondary,
    onSecondary = Black,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD460),
    onSecondaryContainer = Black,

    tertiary = OrangeTertiary,
    onTertiary = White,

    background = OrangeBackground,
    onBackground = Black,

    surface = OrangeSurface,
    onSurface = Black,

    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,

    error = androidx.compose.ui.graphics.Color(0xFFEF4444),
    onError = White
)

// ==================== VIBRANT TEAL THEME ====================
private val TealColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF3DE5E5),
    onPrimaryContainer = Black,

    secondary = TealSecondary,
    onSecondary = Black,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF37E5F7),
    onSecondaryContainer = Black,

    tertiary = TealTertiary,
    onTertiary = White,

    background = TealBackground,
    onBackground = Black,

    surface = TealSurface,
    onSurface = Black,

    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,

    error = androidx.compose.ui.graphics.Color(0xFFEF4444),
    onError = White
)

@Composable
fun Startup_hackathon20Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use our custom theme
    themeMode: String = "neon", // "neon", "orange", "teal", "green", "dark"
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == "dark" -> DarkColorScheme
        themeMode == "green" -> LightGreenColorScheme
        themeMode == "orange" -> OrangeColorScheme
        themeMode == "teal" -> TealColorScheme
        else -> NeonColorScheme // Default to vibrant neon theme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}