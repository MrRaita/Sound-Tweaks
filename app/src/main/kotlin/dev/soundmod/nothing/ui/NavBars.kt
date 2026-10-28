package dev.soundmod.nothing.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Wide floating bottom bar: the selected tab is shown with icon + label inside a pill, the others are icons only.
 * When the tab changes the selected pill widens and the old one narrows, so the highlight seems to flow from one place to the other.
 */
@Composable
fun ExpandingNavBar(
    tabs: List<Int>,
    current: Int,
    title: (Int) -> String,
    icon: (Int) -> ImageVector,
    reduceMotion: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = cs.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { id ->
                val selected = id == current
                val itemWeight by animateFloatAsState(
                    targetValue = if (selected) 2.4f else 1f,
                    animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.8f, stiffness = 380f),
                    label = "navweight"
                )
                val bg by animateColorAsState(
                    targetValue = if (selected) cs.primaryContainer else Color.Transparent,
                    animationSpec = if (reduceMotion) snap() else tween(220),
                    label = "navbg"
                )
                val fg by animateColorAsState(
                    targetValue = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    animationSpec = if (reduceMotion) snap() else tween(220),
                    label = "navfg"
                )
                Box(
                    Modifier
                        .weight(itemWeight)
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(role = Role.Tab) { onSelect(id) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon(id), contentDescription = title(id), tint = fg)
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn(tween(180)) + expandHorizontally(tween(220)),
                            exit = fadeOut(tween(100)) + shrinkHorizontally(tween(180))
                        ) {
                            Text(
                                title(id),
                                color = fg,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating bottom bar: a narrow pill with a shadow above the content. It has icons only; the selected tab's
 * rounded-square highlight slides between the tabs.
 */
@Composable
fun FloatingNavBar(
    tabs: List<Int>,
    current: Int,
    title: (Int) -> String,
    icon: (Int) -> ImageVector,
    reduceMotion: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val itemSize = 56.dp
    val gap = 4.dp
    val stepPx = with(LocalDensity.current) { (itemSize + gap).toPx() }
    val index = tabs.indexOf(current).coerceAtLeast(0)
    val x by animateFloatAsState(
        targetValue = index * stepPx,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "floatx"
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = cs.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 12.dp
    ) {
        Box(Modifier.padding(8.dp)) {
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), 0) }
                    .size(itemSize)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.secondaryContainer)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                tabs.forEach { id ->
                    val selected = id == current
                    val fg by animateColorAsState(
                        targetValue = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant,
                        animationSpec = if (reduceMotion) snap() else tween(200),
                        label = "floatfg"
                    )
                    Box(
                        Modifier
                            .size(itemSize)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(role = Role.Tab) { onSelect(id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon(id), contentDescription = title(id), tint = fg)
                    }
                }
            }
        }
    }
}
