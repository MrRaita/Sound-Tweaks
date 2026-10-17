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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/** Listelerin alt boşluğu: yüzen çubukta çubuğun altında kalmasın diye büyür. */
val LocalListBottomPadding = compositionLocalOf { 24.dp }

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

/** Kaydırmadan türeyen çekme/itme değeri (−1..1). Çubuklar ve liste satırları bunu okur. */
val LocalPull = staticCompositionLocalOf<() -> Float> { { 0f } }

/**
 * Çekme-itme fiziği. Kaydırma hızı ani bir değer üretir; bırakılınca gevşek bir yayla (az sönümlü) sıfıra
 * döner, böylece taşıp birkaç kez sallanır. Pozitif değer: içerik yukarı kayıyor.
 */
class PullPhysics(private val scope: CoroutineScope) {
    val pull = Animatable(0f)
    var enabled = true
    private var job: Job? = null

    fun onDelta(dy: Float) {
        if (!enabled || dy == 0f) return
        val target = (-dy / 38f).coerceIn(-1f, 1f)
        val next = pull.value + (target - pull.value) * 0.45f
        job?.cancel()
        job = scope.launch {
            pull.snapTo(next)
            pull.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 140f))
        }
    }
}

/**
 * Çubuk bükülmesi. İçerik yukarı kayarken çubuğun üstü yukarı uzar, altı yukarı çekilip daralır; aşağı kayarken
 * tersi olur. Sönümü az yay sayesinde bırakınca taşıp sallanır.
 */
fun Modifier.elasticBar(pull: () -> Float): Modifier = this.graphicsLayer {
    val p = pull()
    val a = abs(p)
    scaleY = 1f + a * 0.16f
    scaleX = 1f - a * 0.025f
    transformOrigin = TransformOrigin(0.5f, if (p >= 0f) 1f else 0f)
    translationY = -p * 12.dp.toPx()
}

/**
 * Liste satırları için çekme-itme: ekrana girerken kaydırma yönünden az sönümlü bir yayla gelir (aşırı salınım
 * taşırır), kaydırma sürerken de satırlar yönün tersine gecikip geri sallanır. Her satırın genliği biraz farklıdır;
 * böylece hareket tek parça değil, dalgalı ve kaotik görünür.
 */
@Composable
fun Modifier.staggerIn(index: Int, listState: LazyListState): Modifier {
    val reduce = LocalReduceMotion.current
    val pull = LocalPull.current
    val entry = remember { Animatable(if (reduce) 0f else 1f) }
    // Giriş yönü: içerik yukarı kayıyorsa satır aşağıdan, aşağı kayıyorsa yukarıdan gelir
    val dir = remember { if (pull() < -0.05f) -1f else 1f }
    LaunchedEffect(Unit) {
        if (!reduce) {
            if (!listState.isScrollInProgress && index < 8) delay(index * 25L)
            entry.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 280f))
        }
    }
    val phase = 1f + 0.3f * sin(index * 1.9f)
    return this.graphicsLayer {
        val e = entry.value
        val p = if (reduce) 0f else pull()
        translationY = dir * e * 26.dp.toPx() + p * 9.dp.toPx() * phase
        alpha = 1f - e.coerceIn(0f, 1f) * 0.8f
        scaleY = 1f + abs(p) * 0.035f * phase
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
