package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.local.ThemeMode

val LocalIsDarkTheme = staticCompositionLocalOf { true }

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioBlue,
    onPrimary = Color(0xFF082038),
    primaryContainer = StudioBlueContainerDark,
    onPrimaryContainer = OnStudioBlueContainerDark,
    secondary = SageTeal,
    onSecondary = Color(0xFF072622),
    secondaryContainer = SageTealContainerDark,
    onSecondaryContainer = OnSageTealContainerDark,
    tertiary = SoftLavender,
    onTertiary = Color(0xFF1A1745),
    tertiaryContainer = Color(0xFF2D2B4C),
    onTertiaryContainer = Color(0xFFE4E2FF),
    background = ObsidianBg,
    onBackground = PureMist,
    surface = ObsidianSurface,
    onSurface = PureMist,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = Color(0xFF090B0E),
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2F3542),
    outline = GlassOutlineDark,
    outlineVariant = Color(0xFF212630),
    error = DangerRedDark,
    onError = Color(0xFF2B0B11)
)

private val StudioLightColorScheme = lightColorScheme(
    primary = StudioBlueDeep,
    onPrimary = Color.White,
    primaryContainer = StudioBlueContainerLight,
    onPrimaryContainer = OnStudioBlueContainerLight,
    secondary = TealDeep,
    onSecondary = Color.White,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = OnTealContainerLight,
    tertiary = LavenderDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3E0FC),
    onTertiaryContainer = Color(0xFF1A1745),
    background = DaylightBg,
    onBackground = InkDark,
    surface = DaylightSurface,
    onSurface = InkDark,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = SlateDark,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = DaylightBg,
    surfaceContainer = DaylightSurfaceVariant,
    surfaceContainerHigh = DaylightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFD4DBE8),
    outline = GlassOutlineLight,
    outlineVariant = Color(0xFFE3E7EF),
    error = DangerRedLight,
    onError = Color.White
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AetherPlayTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> StudioDarkColorScheme
        else -> StudioLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(view, isDark, useDynamicColor) {
            val activity = view.context.findThemeActivity()
            val window = activity?.window
            if (window != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                val legacyDarkScrim = android.graphics.Color.argb(0x80, 0x1B, 0x1B, 0x1B)
                val statusBarStyle = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        legacyDarkScrim
                    )
                }
                val navigationBarStyle = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        legacyDarkScrim
                    )
                }
                (activity as? ComponentActivity)?.enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = navigationBarStyle
                )
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
            onDispose { }
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

private tailrec fun Context.findThemeActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findThemeActivity()
    else -> null
}
