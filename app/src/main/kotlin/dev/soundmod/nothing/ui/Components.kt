package dev.soundmod.nothing.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Listelerin alt boşluğu: yüzen çubukta çubuğun altında kalmasın diye büyür. */
val LocalListBottomPadding = compositionLocalOf { 24.dp }

/** Listelerin üst boşluğu: başlık listenin üstüne bindiği için ilk satır başlığın altından başlar. */
val LocalListTopPadding = compositionLocalOf { 0.dp }

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
 * Liste satırları için giriş efekti: yalnızca liste ilk açılırken (kaydırma yokken) satırlar sırayla yukarı kayarak
 * belirir. Kaydırma sırasında satırlara hiçbir efekt uygulanmaz.
 */
@Composable
fun Modifier.staggerIn(index: Int, listState: LazyListState): Modifier {
    val reduce = LocalReduceMotion.current
    val animate = !reduce && !listState.isScrollInProgress
    val entry = remember { Animatable(if (animate) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (animate) {
            if (index < 8) delay(index * 25L)
            entry.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 300f))
        }
    }
    return this.graphicsLayer {
        val e = entry.value
        if (e != 0f) {
            translationY = e * 26.dp.toPx()
            alpha = 1f - e.coerceIn(0f, 1f) * 0.8f
        }
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

/**
 * Çalma durumu. İlerleme sürekli güncellenmez (arka plan iş yükü yok): çalma konumu ve bu konumun alındığı
 * an (SystemClock.elapsedRealtime) tutulur; çubuk kareden kareye bu iki değerden kendi hesaplanır.
 */
data class PlayerUi(
    val key: String? = null,
    val playing: Boolean = false,
    val durationMs: Int = 0,
    val anchorPosMs: Int = 0,
    val anchorTimeMs: Long = 0L
)
