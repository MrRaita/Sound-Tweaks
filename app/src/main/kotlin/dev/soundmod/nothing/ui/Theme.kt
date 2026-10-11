@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.soundmod.nothing.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive tema. Expressive'e özgü kısımlar (MaterialExpressiveTheme, MotionScheme)
 * yalnızca bu dosyada; kararlı M3'e dönmek gerekirse burası değişir.
 */
@Composable
fun SoundModTheme(reduceMotion: Boolean, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = if (reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
        content = content
    )
}
