package dev.soundmod.nothing.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** "Animasyonları azalt" ayarı; efekt üreten modifier'lar bunu okur. */
val LocalReduceMotion = compositionLocalOf { false }

/** Core ikon setinde olmayan duraklat ikonu */
val PauseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 19f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineTo(6f)
        close()
        moveTo(14f, 5f)
        verticalLineToRelative(14f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        close()
    }.build()
}

/** Gruplu liste görünümü: ilk ve son öğe büyük yuvarlak, aradakiler küçük. */
fun segmentShape(index: Int, count: Int, big: Dp = 28.dp, small: Dp = 6.dp): Shape {
    val top = if (index == 0) big else small
    val bottom = if (index == count - 1) big else small
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * Liste öğeleri ilk açılışta sırayla yaylanarak belirir. Her anahtar için yalnızca bir kez (sekme
 * ya da liste yeniden oluşsa da tekrar oynamaz) ve yalnızca ilk birkaç öğede çalışır; hızlı kaydırırken
 * ekrana giren öğeler anında görünür, boş satır kalmaz.
 */
@Composable
fun Modifier.staggerIn(index: Int, seen: MutableSet<Any>, key: Any): Modifier {
    val reduce = LocalReduceMotion.current
    val animate = remember { !reduce && index < 8 && seen.add(key) }
    val p = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
            delay(index * 30L)
            p.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
        }
    }
    return this.graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        translationY = (1f - p.value) * 32f
    }
}

/** Basılınca yaylanan küçülme efekti. */
@Composable
fun Modifier.pressScale(source: InteractionSource): Modifier {
    val reduce = LocalReduceMotion.current
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed && !reduce) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "press"
    )
    return this.graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/** Çal / duraklat düğmesi; ikon yumuşakça değişir. */
@Composable
fun PlayPauseButton(playing: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick) {
        Crossfade(targetState = playing, label = "playpause") { p ->
            Icon(
                imageVector = if (p) PauseIcon else Icons.Default.PlayArrow,
                contentDescription = if (p) "Duraklat" else "Çal"
            )
        }
    }
}

/** Çalma durumu: hangi öğe çalıyor, ilerleme */
data class PlayerUi(val key: String? = null, val playing: Boolean = false, val progress: Float = 0f)
