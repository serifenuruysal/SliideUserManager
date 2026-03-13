package com.sliide.usermanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Premium "Posh" Colour tokens - Slate & Indigo
// Reverted to deeper "Midnight" Slate for high-end feel.
// ---------------------------------------------------------------------------

private val Slate950 = Color(0xFF020617) // Midnight Slate
private val Slate900 = Color(0xFF0F172A) // Deep Slate
private val Slate800 = Color(0xFF1E293B) // Surface Slate
private val Slate50  = Color(0xFFF8FAFC)

private val Indigo600 = Color(0xFF4F46E5) // Signature Indigo
private val Indigo700 = Color(0xFF4338CA)
private val Amber500 = Color(0xFFF59E0B)

private val PoshDarkColors = darkColorScheme(
    primary          = Indigo600,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary        = Amber500,
    background       = Slate950,
    surface          = Slate900,
    surfaceVariant   = Slate800,
    onBackground     = Color.White,
    onSurface        = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
)

private val PoshLightColors = lightColorScheme(
    primary          = Indigo700,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Indigo700,
    secondary        = Amber500,
    background       = Slate50,
    surface          = Color.White,
    surfaceVariant   = Color(0xFFF1F5F9),
    onBackground     = Slate950,
    onSurface        = Slate950,
    onSurfaceVariant = Color(0xFF64748B),
    error            = Color(0xFFE11D48),
)

private val PoshTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 28.sp, letterSpacing = (-0.25).sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 0.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 16.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 0.5.sp),
)

@Composable
fun SliideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PoshDarkColors else PoshLightColors,
        typography  = PoshTypography,
        content     = content
    )
}
