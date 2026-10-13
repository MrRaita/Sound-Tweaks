@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.soundmod.nothing.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 Expressive tema. Expressive'e özgü kısımlar (MaterialExpressiveTheme, MotionScheme)
 * yalnızca bu dosyada; kararlı M3'e dönmek gerekirse burası değişir.
 *
 * @param themeMode 0 = sistem, 1 = açık, 2 = koyu
 * @param dynamic   true ise (Android 12+) sistem/duvar kağıdı renkleri; değilse seçilen palet
 */
@Composable
fun SoundModTheme(
    themeMode: Int,
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
    val scheme = remember(useDynamic, dark, palette) {
        if (useDynamic) {
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            Palettes.scheme(Palettes.list[palette.coerceIn(0, Palettes.list.lastIndex)], dark)
        }
    }

    // Durum/gezinti çubuğu simgeleri uygulama temasına uysun (sistem temasından bağımsız seçilebildiği için)
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
