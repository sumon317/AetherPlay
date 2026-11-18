package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00242B),
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = OnCyanContainerDark,
    secondary = WarmCoral,
    onSecondary = Color(0xFF2B0800),
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = OnCoralContainerDark,
    tertiary = AmberGold,
    onTertiary = Color(0xFF2B1800),
    background = ObsidianBg,
    onBackground = PureMist,
    surface = ObsidianSurface,
    onSurface = PureMist,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = Color(0xFF070A11),
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2C3854),
    outline = GlassOutlineDark,
    outlineVariant = Color(0xFF1E283D),
    error = DangerRedDark,
    onError = Color.White
)

private val StudioLightColorScheme = lightColorScheme(
    primary = CyanDeep,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = OnCyanContainerLight,
    secondary = CoralDeep,
    onSecondary = Color.White,
    secondaryContainer = CoralContainerLight,
    onSecondaryContainer = OnCoralContainerLight,
    tertiary = AmberDeep,
    onTertiary = Color.White,
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
    surfaceContainerHighest = Color(0xFFD0DAEC),
    outline = GlassOutlineLight,
    outlineVariant = Color(0xFFE2E8F0),
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
fun resolveIsDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun AetherPlayTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = resolveIsDarkTheme(themeMode)

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> StudioDarkColorScheme
        else -> StudioLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            content()
        }
    }
}
