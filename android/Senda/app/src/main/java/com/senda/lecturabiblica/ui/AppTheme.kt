package com.senda.lecturabiblica.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AccentPalette(val id: String, val name: String, val color: Color, val darkColor: Color)

val accentPalettes = listOf(
    AccentPalette("bosque", "Bosque", Color(0xFF176B50), Color(0xFF8DDBB9)),
    AccentPalette("cielo", "Cielo", Color(0xFF2466A8), Color(0xFF9CCAFF)),
    AccentPalette("coral", "Coral", Color(0xFFA83E4C), Color(0xFFFFB2BA)),
    AccentPalette("violeta", "Violeta", Color(0xFF7153A7), Color(0xFFD4BBFF)),
    AccentPalette("mango", "Mango", Color(0xFF8A5900), Color(0xFFFFBA47)),
    AccentPalette("fucsia", "Fucsia", Color(0xFFA10069), Color(0xFFFFAFD7)),
)

@Composable
fun SendaTheme(mode: String, accentId: String, content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val accent = accentPalettes.firstOrNull { it.id == accentId } ?: accentPalettes.first()
    val darkAccentContainer = accent.color.copy(alpha = .52f).compositeOver(Color(0xFF27312B))
    val lightAccentContainer = accent.color.copy(alpha = .16f).compositeOver(Color(0xFFF7FAF7))
    val scheme = if (dark) {
        darkColorScheme(
            primary = accent.darkColor,
            onPrimary = Color(0xFF00382A),
            primaryContainer = darkAccentContainer,
            onPrimaryContainer = Color(0xFFE1F5EA),
            secondary = accent.darkColor,
            secondaryContainer = darkAccentContainer,
            onSecondaryContainer = Color(0xFFE1F5EA),
            tertiary = Color(0xFFE4C78A),
            background = Color(0xFF101512),
            surface = Color(0xFF151B17),
            surfaceTint = accent.darkColor,
            surfaceVariant = Color(0xFF27312B),
            onSurface = Color(0xFFE4EAE5),
            onSurfaceVariant = Color(0xFFC0C9C2),
            outline = Color(0xFF89938C),
            error = Color(0xFFFFB4AB),
            errorContainer = Color(0xFF72221D),
        )
    } else {
        lightColorScheme(
            primary = accent.color,
            onPrimary = Color.White,
            primaryContainer = lightAccentContainer,
            onPrimaryContainer = Color(0xFF10281F),
            secondary = accent.color,
            secondaryContainer = lightAccentContainer,
            onSecondaryContainer = Color(0xFF10281F),
            tertiary = Color(0xFF705D28),
            background = Color(0xFFF8FAF7),
            surface = Color(0xFFFEFFFC),
            surfaceTint = accent.color,
            surfaceVariant = Color(0xFFE7EEE8),
            onSurface = Color(0xFF172019),
            onSurfaceVariant = Color(0xFF46514A),
            outline = Color(0xFF747E77),
            error = Color(0xFFBA1A1A),
            errorContainer = Color(0xFFFFDAD6),
        )
    }
    MaterialTheme(colorScheme = scheme, typography = sendaTypography, shapes = sendaShapes, content = content)
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha + background.alpha * (1 - alpha)
    return Color(
        red = (red * alpha + background.red * background.alpha * (1 - alpha)) / a,
        green = (green * alpha + background.green * background.alpha * (1 - alpha)) / a,
        blue = (blue * alpha + background.blue * background.alpha * (1 - alpha)) / a,
        alpha = a,
    )
}
