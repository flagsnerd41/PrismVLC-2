package com.flagsnerd.prism.ui

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.flagsnerd.prism.R
import com.flagsnerd.prism.data.Store

/** Retro Blendy - used sparingly: wordmark and big titles only. */
val Display = FontFamily(Font(R.font.retro_blendy))

class PaletteDef(
    val id: String,
    val label: String,
    val primary: Long,
    val onPrimary: Long,
    val container: Long,
    val onContainer: Long,
    val surface: Long,
    val onSurface: Long,
    val variant: Long,
    val onVariant: Long,
)

val Palettes = listOf(
    PaletteDef("violet", "Violet", 0xFFD0BCFF, 0xFF381E72, 0xFF4F378B, 0xFFEADDFF, 0xFF141218, 0xFFE6E0E9, 0xFF2B2930, 0xFFCAC4D0),
    PaletteDef("ocean", "Ocean", 0xFF7CC4FF, 0xFF00344F, 0xFF004B70, 0xFFC7E7FF, 0xFF0B1620, 0xFFE3EAF0, 0xFF1B2B38, 0xFFB7C6D2),
    PaletteDef("sunset", "Sunset", 0xFFFFB68A, 0xFF512400, 0xFF723600, 0xFFFFDCC7, 0xFF1C120C, 0xFFF2E3DA, 0xFF342119, 0xFFD6C2B8),
    PaletteDef("forest", "Forest", 0xFF7ADB8A, 0xFF003915, 0xFF00531F, 0xFF96F8A3, 0xFF0C1710, 0xFFE0EBE1, 0xFF1C2B20, 0xFFB9CCBC),
    PaletteDef("rose", "Rose", 0xFFFFB1C8, 0xFF650033, 0xFF8D0F4B, 0xFFFFD9E3, 0xFF1D1014, 0xFFF3E1E6, 0xFF35202A, 0xFFD8C0C8),
    PaletteDef("amoled", "AMOLED", 0xFFFFFFFF, 0xFF000000, 0xFF2A2A2A, 0xFFFFFFFF, 0xFF000000, 0xFFF2F2F2, 0xFF1A1A1A, 0xFFBDBDBD),
)

private fun schemeOf(p: PaletteDef): ColorScheme {
    val surface = Color(p.surface)
    val variant = Color(p.variant)
    return darkColorScheme(
        primary = Color(p.primary),
        onPrimary = Color(p.onPrimary),
        primaryContainer = Color(p.container),
        onPrimaryContainer = Color(p.onContainer),
        secondary = Color(p.primary),
        onSecondary = Color(p.onPrimary),
        secondaryContainer = Color(p.container),
        onSecondaryContainer = Color(p.onContainer),
        tertiary = Color(p.primary),
        background = surface,
        onBackground = Color(p.onSurface),
        surface = surface,
        onSurface = Color(p.onSurface),
        surfaceVariant = variant,
        onSurfaceVariant = Color(p.onVariant),
        surfaceContainerLowest = surface,
        surfaceContainerLow = lerp(surface, variant, 0.3f),
        surfaceContainer = lerp(surface, variant, 0.55f),
        surfaceContainerHigh = lerp(surface, variant, 0.8f),
        surfaceContainerHighest = variant,
        outline = Color(p.onVariant).copy(alpha = 0.5f),
        outlineVariant = Color(p.onVariant).copy(alpha = 0.2f),
    )
}

fun schemeFor(id: String, ctx: Context): ColorScheme {
    if (id == "you" && Build.VERSION.SDK_INT >= 31) return dynamicDarkColorScheme(ctx)
    val def = Palettes.firstOrNull { it.id == id } ?: Palettes.first()
    return schemeOf(def)
}

@Composable
fun PrismTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val id = Store.palette
    val scheme = remember(id) { schemeFor(id, ctx) }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
