@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.mrraita.soundtwks.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 Expressive theme. The Expressive-specific parts (MaterialExpressiveTheme, MotionScheme)
 * live only in this file; if we ever need to go back to stable M3, only this file changes.
 *
 * @param themeMode 0 = system, 1 = light, 2 = dark
 * @param oled      if true, the dark theme (including when the system is dark) uses pure black surfaces
 * @param dynamic   if true (Android 12+) system/wallpaper colors are used; otherwise the selected palette
 */
@Composable
fun SoundTweaksTheme(
    themeMode: Int,
    oled: Boolean,
    dynamic: Boolean,
    palette: Int,
    reduceMotion: Boolean,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val useDynamic = dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = remember(useDynamic, dark, palette, oled) {
        val base = if (useDynamic) {
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            Palettes.scheme(Palettes.list[palette.coerceIn(0, Palettes.list.lastIndex)], dark)
        }
        if (dark && oled) base.toOled() else base
    }

    // Make the status/navigation bar icons follow the app theme (it can be chosen independently of the system theme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = if (reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
        content = content
    )
}

/** Pulls the background and surfaces to pure black; slightly lighter blacks are used so cards stay distinguishable. */
private fun ColorScheme.toOled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF171717),
    surfaceContainerHighest = Color(0xFF1F1F1F),
    surfaceBright = Color(0xFF262626)
)
