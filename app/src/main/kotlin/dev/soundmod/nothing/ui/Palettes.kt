package dev.soundmod.nothing.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Hazır renk paleti: bir ana ton (hue) ve doygunluk çarpanından şema üretilir. */
data class Palette(
    val name: String,
    val hue: Float,
    /** 0 = tamamen nötr, 1 = tam renkli */
    val sat: Float = 1f,
    /** Verilirse üçüncül (vurgu) renk bu tonda ve canlı olur */
    val tertiaryHue: Float? = null
)

object Palettes {
    val list = listOf(
        Palette("Okyanus", 212f),
        Palette("Turkuaz", 178f),
        Palette("Orman", 140f),
        Palette("Zeytin", 78f),
        Palette("Kehribar", 38f),
        Palette("Mercan", 10f),
        Palette("Gül", 338f),
        Palette("Erik", 305f),
        Palette("Lavanta", 265f),
        Palette("Gece", 235f, 0.7f),
        Palette("Nothing", 0f, 0f, 4f),
        Palette("Grafit", 210f, 0.2f)
    )

    private fun c(h: Float, s: Float, l: Float): Color =
        Color.hsl(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))

    /** Seçim kutusunda gösterilen örnek renk */
    fun swatch(p: Palette): Color =
        if (p.tertiaryHue != null) c(p.tertiaryHue, 0.85f, 0.50f) else c(p.hue, 0.30f + 0.35f * p.sat, 0.45f)

    fun scheme(p: Palette, dark: Boolean): ColorScheme {
        val h = p.hue
        val k = p.sat
        val h2 = h + 25f
        val th = p.tertiaryHue ?: (h + 60f)
        val sP = 0.62f * k
        val sS = 0.30f * k
        val sT = if (p.tertiaryHue != null) 0.80f else 0.40f * k
        val n = 0.04f + 0.06f * k // nötr yüzeylerin hafif tonu
        return if (!dark) lightColorScheme(
            primary = c(h, sP, 0.38f), onPrimary = Color.White,
            primaryContainer = c(h, 0.75f * k, 0.88f), onPrimaryContainer = c(h, 0.70f * k + 0.1f, 0.14f),
            secondary = c(h2, sS, 0.38f), onSecondary = Color.White,
            secondaryContainer = c(h2, 0.40f * k, 0.88f), onSecondaryContainer = c(h2, 0.45f * k + 0.1f, 0.14f),
            tertiary = c(th, sT, 0.40f), onTertiary = Color.White,
            tertiaryContainer = c(th, sT + 0.10f, 0.88f), onTertiaryContainer = c(th, sT + 0.15f, 0.14f),
            background = c(h, n, 0.98f), onBackground = c(h, n, 0.10f),
            surface = c(h, n, 0.98f), onSurface = c(h, n, 0.10f),
            surfaceVariant = c(h, n + 0.06f, 0.90f), onSurfaceVariant = c(h, n + 0.04f, 0.30f),
            surfaceTint = c(h, sP, 0.38f),
            inverseSurface = c(h, n + 0.02f, 0.20f), inverseOnSurface = c(h, n, 0.95f),
            inversePrimary = c(h, 0.65f * k, 0.78f),
            outline = c(h, n, 0.46f), outlineVariant = c(h, n + 0.04f, 0.80f),
            surfaceBright = c(h, n, 0.98f), surfaceDim = c(h, n + 0.02f, 0.87f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = c(h, n, 0.96f),
            surfaceContainer = c(h, n + 0.02f, 0.94f),
            surfaceContainerHigh = c(h, n + 0.03f, 0.92f),
            surfaceContainerHighest = c(h, n + 0.04f, 0.90f)
        ) else darkColorScheme(
            primary = c(h, 0.60f * k + 0.05f, 0.80f), onPrimary = c(h, 0.70f * k, 0.18f),
            primaryContainer = c(h, 0.55f * k, 0.28f), onPrimaryContainer = c(h, 0.75f * k, 0.90f),
            secondary = c(h2, 0.35f * k, 0.80f), onSecondary = c(h2, 0.40f * k, 0.18f),
            secondaryContainer = c(h2, 0.30f * k, 0.28f), onSecondaryContainer = c(h2, 0.45f * k, 0.90f),
            tertiary = c(th, sT + 0.10f, 0.80f), onTertiary = c(th, sT, 0.18f),
            tertiaryContainer = c(th, sT, 0.28f), onTertiaryContainer = c(th, sT + 0.10f, 0.90f),
            background = c(h, n, 0.08f), onBackground = c(h, n, 0.90f),
            surface = c(h, n, 0.08f), onSurface = c(h, n, 0.90f),
            surfaceVariant = c(h, n + 0.04f, 0.28f), onSurfaceVariant = c(h, n + 0.04f, 0.80f),
            surfaceTint = c(h, 0.60f * k + 0.05f, 0.80f),
            inverseSurface = c(h, n, 0.90f), inverseOnSurface = c(h, n, 0.20f),
            inversePrimary = c(h, sP, 0.38f),
            outline = c(h, n, 0.60f), outlineVariant = c(h, n + 0.02f, 0.28f),
            surfaceBright = c(h, n, 0.24f), surfaceDim = c(h, n, 0.06f),
            surfaceContainerLowest = c(h, n, 0.04f),
            surfaceContainerLow = c(h, n, 0.10f),
            surfaceContainer = c(h, n, 0.12f),
            surfaceContainerHigh = c(h, n, 0.16f),
            surfaceContainerHighest = c(h, n, 0.20f)
        )
    }
}
