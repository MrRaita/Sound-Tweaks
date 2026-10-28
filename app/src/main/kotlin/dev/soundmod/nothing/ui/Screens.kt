@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.soundmod.nothing.ui

import dev.soundmod.nothing.BuildConfig
import dev.soundmod.nothing.core.SoundPaths
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build as WrenchIcon
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import dev.soundmod.nothing.core.RootInfo
import dev.soundmod.nothing.core.SoundCatalog
import dev.soundmod.nothing.core.SoundSlot
import dev.soundmod.nothing.core.SystemSound

/** All actions the UI calls (collected in one place). */
data class UiActions(
    val refresh: () -> Unit,
    val refreshSys: () -> Unit,
    val reloadLibrary: () -> Unit,
    val ensureLibrary: () -> Unit,
    val playLibrary: (String) -> Unit,
    val playSlot: (SoundSlot) -> Unit,
    val testFx: (Int) -> Unit,
    val pickSystem: (SoundSlot, SystemSound) -> Unit,
    val pickFile: (SoundSlot) -> Unit,
    val reset: (SoundSlot) -> Unit,
    val setReduceMotion: (Boolean) -> Unit,
    val setNormalize: (Boolean) -> Unit,
    val setTargetDb: (Int) -> Unit,
    val reapplyAll: () -> Unit,
    val setThemeMode: (Int) -> Unit,
    val setOled: (Boolean) -> Unit,
    val setDynamic: (Boolean) -> Unit,
    val setPalette: (Int) -> Unit,
    val restartSysUiNow: () -> Unit,
    val setSystemToggle: (SysToggle, Boolean) -> Unit,
    val buildReport: () -> Unit,
    val clearReport: () -> Unit,
    val dismissIntro: () -> Unit,
    val showIntro: () -> Unit,
    val setDebugLog: (Boolean) -> Unit,
    val setForceLock: (Boolean) -> Unit,
    val confirmRestartPrompt: () -> Unit,
    val dismissRestartPrompt: () -> Unit,
    val applySilent: (SoundSlot) -> Unit,
    val setSlotNorm: (String, Int, Int) -> Unit,
    val reapplySlot: (SoundSlot) -> Unit,
    val setSkipRestartWarning: (Boolean) -> Unit,
    val moveTabTo: (Int, Int) -> Unit,
    val setTabHidden: (Int, Boolean) -> Unit,
    val setNavStyle: (Int) -> Unit,
    val setNoHpWarning: (Boolean) -> Unit
)

private val TAB_TITLES = listOf("Sounds", "Library", "Advanced", "Settings")

private fun tabIcon(id: Int): ImageVector = when (id) {
    0 -> Icons.Default.Notifications
    1 -> Icons.Default.Search
    2 -> Icons.Default.WrenchIcon
    else -> Icons.Default.Settings
}

// The header overlays the list, so the inner padding Scaffold provides is deliberately unused (lists take their own top padding)
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SoundModScreen(
    state: UiState,
    player: State<PlayerUi>,
    snackbar: SnackbarHostState,
    a: UiActions
) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = TAB_TITLES
    // Settings (3) is always visible; the others follow the user's order, unless hidden
    val visibleTabs = state.tabOrder.filter { it == 3 || it !in state.hiddenTabs }
    val current = if (tab in visibleTabs) tab else visibleTabs.first()
    val listStates = List(4) { rememberLazyListState() }
    var showRestartDialog by remember { mutableStateOf(false) }
    val onRestartClick: () -> Unit = {
        if (state.skipRestartWarning) {
            a.restartSysUiNow()
        } else {
            showRestartDialog = true
        }
    }
    val density = LocalDensity.current

    // The top header has two states (wide / narrow): it shrinks with a springy transition once the scroll threshold is passed and opens again when scrolled back to the top.
    // Between the thresholds (hysteresis) the state does not change; the header never rests half collapsed.
    val collapse = remember { Animatable(0f) }
    val collapseHigh = with(density) { 24.dp.toPx() }
    val collapseLow = with(density) { 6.dp.toPx() }
    LaunchedEffect(current, state.reduceMotion) {
        var last = 0f
        snapshotFlow {
            val ls = listStates[current]
            if (ls.firstVisibleItemIndex > 0) Float.MAX_VALUE else ls.firstVisibleItemScrollOffset.toFloat()
        }
            .map { offset ->
                last = when {
                    offset > collapseHigh -> 1f
                    offset < collapseLow -> 0f
                    else -> last
                }
                last
            }
            .distinctUntilChanged()
            .collectLatest { target ->
                if (state.reduceMotion) collapse.snapTo(target)
                else collapse.animateTo(target, spring(dampingRatio = 0.85f, stiffness = 450f))
            }
    }
    val collapseFn = remember(collapse) { { collapse.value } }

    LaunchedEffect(current) {
        if (current == 1) a.ensureLibrary()
        if (current == 2) a.refreshSys()
    }

    // The bars float above the list; lists extend to the bottom of the screen and the last rows never end up under the bar
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottom = 96.dp + navInset
    // The header overlays the list (the list shows behind the corners); lists start with a gap as tall as the expanded header
    var headerPx by remember { mutableIntStateOf(0) }
    val headerInset = with(density) { headerPx.toDp() }
    // Current header height (so the Library's fixed search/filter area stays attached to the header)
    val statusTopPx = WindowInsets.statusBars.getTop(density)
    val collapsedBandInt = with(density) { 56.dp.roundToPx() }
    val headerHeightFn = remember(collapse, statusTopPx, collapsedBandInt) {
        {
            val c = collapse.value.coerceIn(0f, 1f)
            val expanded = headerPx
            val collapsed = statusTopPx + collapsedBandInt
            (expanded + (collapsed - expanded) * c).roundToInt()
        }
    }

    CompositionLocalProvider(
        LocalReduceMotion provides state.reduceMotion,
        LocalListBottomPadding provides listBottom,
        LocalListTopPadding provides headerInset,
        LocalHeaderHeight provides headerHeightFn
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                Column {
                    Header(titles[current], collapseFn, state.reduceMotion, onRestartClick) { headerPx = it }
                    if (state.busy) {
                        if (state.reduceMotion) LinearProgressIndicator(Modifier.fillMaxWidth())
                        else LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.padding(bottom = 88.dp)) }
        ) { pad ->
            Box(
                Modifier
                    .fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        if (state.reduceMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            val dir = if (visibleTabs.indexOf(targetState) > visibleTabs.indexOf(initialState)) 1 else -1
                            (slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = 300f)) { it * dir / 3 } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(180)) { -it * dir / 3 } + fadeOut(tween(120)))
                        }
                    },
                    label = "tabs"
                ) { t ->
                    when (t) {
                        0 -> SoundsTab(state, player, a, listStates[0])
                        1 -> LibraryTab(state, player, a, listStates[1])
                        2 -> AdvancedTab(state, a, listStates[2])
                        else -> SettingsTab(state, a, listStates[3], onRestartClick)
                    }
                }

                // Both styles float; the only difference is width (in the wide bar the selected tab expands with its label)
                val barModifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                if (state.navStyle == 1) {
                    FloatingNavBar(
                        tabs = visibleTabs,
                        current = current,
                        title = { titles[it] },
                        icon = { tabIcon(it) },
                        reduceMotion = state.reduceMotion,
                        onSelect = { tab = it },
                        modifier = barModifier
                    )
                } else {
                    ExpandingNavBar(
                        tabs = visibleTabs,
                        current = current,
                        title = { titles[it] },
                        icon = { tabIcon(it) },
                        reduceMotion = state.reduceMotion,
                        onSelect = { tab = it },
                        modifier = barModifier
                    )
                }
                if (state.busy && !state.reduceMotion) {
                    LoadingIndicator(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 88.dp + navInset)
                            .size(48.dp)
                    )
                }
            }
        }
    }

    if (showRestartDialog) {
        RestartConfirmDialog(
            onConfirm = { dontShowAgain ->
                showRestartDialog = false
                if (dontShowAgain) a.setSkipRestartWarning(true)
                a.restartSysUiNow()
            },
            onDismiss = { showRestartDialog = false }
        )
    }
    state.restartPrompt?.let { RestartPromptDialog(it, a.confirmRestartPrompt, a.dismissRestartPrompt) }
    if (state.showIntro) IntroDialog(a.dismissIntro)
    state.report?.let { text -> ReportSheet(text, a.clearReport) }
}

// ------------------------------------------------------------------------------------------
// Header: wide at the top of a tab; when scrolling down the app name is pulled up and the tab name takes its place.
// The lower edge is pulled and pushed by the scroll movement.
// ------------------------------------------------------------------------------------------

@Composable
private fun TabTitle(title: String, style: TextStyle, color: Color, reduceMotion: Boolean) {
    AnimatedContent(
        targetState = title,
        transitionSpec = {
            if (reduceMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 3 }) togetherWith
                (fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 3 })
        },
        label = "title"
    ) { t ->
        Text(t, style = style, color = color)
    }
}

@Composable
private fun Header(
    title: String,
    collapse: () -> Float,
    reduceMotion: Boolean,
    onRestart: () -> Unit,
    onExpandedHeight: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val statusPx = WindowInsets.statusBars.getTop(density).toFloat()
    val collapsedBandPx = with(density) { 56.dp.toPx() }
    val bigTitle = MaterialTheme.typography.displaySmall
    // In the narrow view the tab name uses the same typeface and weight as the app name, only a bit smaller
    val compactTitle = bigTitle.copy(fontSize = 30.sp, lineHeight = 36.sp)

    Box(
        Modifier
            .fillMaxWidth()
            // The reported height changes between wide ↔ narrow (only the layout phase reads it)
            // The background must come BEFORE the height-changing layout: otherwise it is drawn at the old (wide) size
            // and the header does not look smaller even when its content shrinks.
            .background(
                cs.surfaceContainer,
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val c = collapse().coerceIn(0f, 1f)
                val collapsedH = statusPx + collapsedBandPx
                val h = (placeable.height + (collapsedH - placeable.height) * c).roundToInt()
                layout(placeable.width, h) { placeable.place(0, 0) }
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { onExpandedHeight(it.height) }
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Wide view: large app name with the small tab name below; fades out as it is pulled up while scrolling
            Column(
                Modifier
                    .padding(top = 20.dp, bottom = 14.dp, end = 56.dp)
                    .graphicsLayer {
                        val c = collapse()
                        translationY = -c * 46.dp.toPx()
                        alpha = (1f - c * 1.7f).coerceIn(0f, 1f)
                    }
            ) {
                Text("SoundMod", style = bigTitle, color = cs.primary)
                TabTitle(title, MaterialTheme.typography.titleMedium, cs.onSurfaceVariant, reduceMotion)
            }
            // Narrow view: the tab name rises from below and takes the place of the app name
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 10.dp, end = 56.dp)
                    .graphicsLayer {
                        val c = collapse()
                        alpha = ((c - 0.45f) * 2f).coerceIn(0f, 1f)
                        translationY = (1f - c) * 26.dp.toPx()
                    }
            ) {
                TabTitle(title, compactTitle, cs.primary, reduceMotion)
            }
            FilledTonalIconButton(
                onClick = onRestart,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp)
                    .graphicsLayer { translationY = -collapse() * 16.dp.toPx() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh SystemUI")
            }
        }
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 22.dp, bottom = 8.dp)
    )
}

// ------------------------------------------------------------------------------------------
// Sesler sekmesi
// ------------------------------------------------------------------------------------------

@Composable
private fun SoundsTab(
    state: UiState,
    player: State<PlayerUi>,
    a: UiActions,
    listState: LazyListState
) {
    var sheetSlot by remember { mutableStateOf<SoundSlot?>(null) }
    var pickerSlot by remember { mutableStateOf<SoundSlot?>(null) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = LocalListTopPadding.current, bottom = LocalListBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (state.root == RootState.DENIED) {
            item(key = "rootwarn") {
                Card(
                    shape = segmentShape(0, 1),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No root access", style = MaterialTheme.typography.titleMedium)
                        Text("To change sounds, grant SoundMod access in your root manager.", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = a.refresh,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) { Text("Try again") }
                    }
                }
            }
        }
        var running = 0
        SoundCatalog.groups.forEach { group ->
            val rows = state.slots.filter { it.slot.group == group }
            if (rows.isEmpty()) return@forEach
            val base = running
            running += rows.size
            item(key = "h_$group") { GroupHeader(group) }
            itemsIndexed(rows, key = { _, r -> r.slot.id }) { i, ss ->
                SlotRow(
                    shape = segmentShape(i, rows.size),
                    ss = ss,
                    player = player,
                    onClick = { sheetSlot = ss.slot },
                    onPlay = { a.playSlot(ss.slot) },
                    modifier = Modifier.animateItem().staggerIn(base + i, listState)
                )
            }
        }
    }

    sheetSlot?.let { slot ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { sheetSlot = null }, sheetState = sheetState) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(slot.title, style = MaterialTheme.typography.headlineSmall)
                Text(slot.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (slot.sysUiReload) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Android loads this sound once when SystemUI starts; refreshing is recommended after a change. " +
                            "To hear it in silent mode too, go to Settings → Advanced → “Force-play lock sounds”.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { pickerSlot = slot; sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Choose from system sounds") }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { a.pickFile(slot); sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Choose my own file") }
                slot.fx?.let { fx ->
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { a.testFx(fx) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Try through the system")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { a.applySilent(slot); sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Mute this sound") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { a.reset(slot); sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to default")
                }
            }
        }
    }

    pickerSlot?.let { slot ->
        LaunchedEffect(slot) { a.ensureLibrary() }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { pickerSlot = null }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
                Text(
                    "Choose a sound for ${slot.title}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                CompositionLocalProvider(
                    LocalListBottomPadding provides 24.dp,
                    LocalListTopPadding provides 0.dp,
                    LocalHeaderHeight provides { 0 }
                ) {
                    SoundList(
                        sounds = state.library,
                        loading = state.libraryLoading,
                        player = player,
                        onPlay = a.playLibrary,
                        onPick = { a.pickSystem(slot, it); pickerSlot = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotRow(
    shape: Shape,
    ss: SlotState,
    player: State<PlayerUi>,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier
) {
    val src = remember { MutableInteractionSource() }
    val key = "slot:${ss.slot.id}"
    val active = player.value.key == key
    val playing = active && player.value.playing
    val custom = ss.label != null

    Card(
        onClick = onClick,
        shape = shape,
        interactionSource = src,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxWidth().pressScale(src)
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ss.slot.title, style = MaterialTheme.typography.titleMedium)
                    AnimatedVisibility(visible = custom) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    ss.label ?: "Default",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (custom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PlayPauseButton(playing = playing, onClick = onPlay)
        }
        AnimatedVisibility(
            visible = active,
            enter = fadeIn(tween(120)) + expandVertically(tween(160)),
            exit = fadeOut(tween(80)) + shrinkVertically(tween(110))
        ) {
            PlayProgress(player, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp))
        }
    }
}

// ------------------------------------------------------------------------------------------
// Library
// ------------------------------------------------------------------------------------------

@Composable
private fun LibraryTab(
    state: UiState,
    player: State<PlayerUi>,
    a: UiActions,
    listState: LazyListState
) {
    SoundList(
        sounds = state.library,
        loading = state.libraryLoading,
        player = player,
        onPlay = a.playLibrary,
        onPick = null,
        listState = listState,
        actions = {
            IconButton(onClick = a.reloadLibrary) { Icon(Icons.Default.Refresh, "Rescan") }
        }
    )
}

/**
 * The sound list. The screen does not open with a search bar: there is just a search icon on top; tapping it opens the search field,
 * and closing it clears the query.
 */
@Composable
private fun SoundList(
    sounds: List<SystemSound>,
    loading: Boolean,
    player: State<PlayerUi>,
    onPlay: (String) -> Unit,
    onPick: ((SystemSound) -> Unit)?,
    listState: LazyListState = rememberLazyListState(),
    actions: @Composable () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<String?>(null) }
    val categories = remember(sounds) { sounds.map { it.category }.distinct() }
    val filtered = remember(sounds, query, category) {
        sounds.filter {
            (category == null || it.category == category) &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
    }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(searching) {
        if (searching) {
            // The search field is created with an opening animation; wait for it to settle before requesting focus
            delay(80)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
    }

    val headerHeight = LocalHeaderHeight.current
    // The search row and filter chips are attached to the header: they slide up together as it shrinks and the list grows with them
    Column(
        Modifier
            .layout { measurable, constraints ->
                val top = headerHeight().coerceIn(0, constraints.maxHeight)
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = constraints.maxHeight - top))
                layout(placeable.width, constraints.maxHeight) { placeable.place(0, top) }
            }
            .fillMaxSize()
    ) {
        Row(
            Modifier.padding(start = 28.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (filtered.size != sounds.size) "${filtered.size} / ${sounds.size} sounds" else "${sounds.size} sounds found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                searching = !searching
                if (!searching) query = ""
            }) {
                Icon(
                    if (searching) Icons.Default.Close else Icons.Default.Search,
                    if (searching) "Close search" else "Search"
                )
            }
            actions()
        }
        AnimatedVisibility(
            visible = searching,
            enter = fadeIn(tween(120)) + expandVertically(tween(180)),
            exit = fadeOut(tween(90)) + shrinkVertically(tween(140))
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                shape = CircleShape,
                placeholder = { Text("Search sounds") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .focusRequester(focus)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text("Hepsi") },
                    shape = CircleShape
                )
            }
            itemsIndexed(categories) { _, c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = if (category == c) null else c },
                    label = { Text(c) },
                    shape = CircleShape
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (loading) {
                LoadingIndicator(Modifier.align(Alignment.Center))
            } else if (filtered.isEmpty()) {
                Text(
                    "No sounds found",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = LocalListBottomPadding.current),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(filtered, key = { _, s -> s.path }) { i, s ->
                        SoundRow(
                            shape = segmentShape(i, filtered.size),
                            sound = s,
                            player = player,
                            onPlay = { onPlay(s.path) },
                            onPick = onPick?.let { pick -> { pick(s) } },
                            modifier = Modifier.animateItem().staggerIn(i, listState)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundRow(
    shape: Shape,
    sound: SystemSound,
    player: State<PlayerUi>,
    onPlay: () -> Unit,
    onPick: (() -> Unit)?,
    modifier: Modifier
) {
    val src = remember { MutableInteractionSource() }
    val key = "lib:${sound.path}"
    val active = player.value.key == key
    val playing = active && player.value.playing

    val cardModifier = modifier.fillMaxWidth().pressScale(src)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    val content: @Composable () -> Unit = {
        Column {
            Row(
                Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sound.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${sound.category} · ${sound.partition}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PlayPauseButton(playing = playing, onClick = onPlay)
                if (onPick != null) {
                    Spacer(Modifier.width(4.dp))
                    FilledTonalIconButton(onClick = onPick) { Icon(Icons.Default.Check, "Select") }
                }
            }
            AnimatedVisibility(
                visible = active,
                enter = fadeIn(tween(120)) + expandVertically(tween(160)),
                exit = fadeOut(tween(80)) + shrinkVertically(tween(110))
            ) {
                PlayProgress(player, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp))
            }
        }
    }

    if (onPick != null) {
        Card(onClick = onPick, shape = shape, interactionSource = src, colors = colors, modifier = cardModifier) { content() }
    } else {
        Card(shape = shape, colors = colors, modifier = cardModifier) { content() }
    }
}

// ------------------------------------------------------------------------------------------
// Ayarlar
// ------------------------------------------------------------------------------------------

// ------------------------------------------------------------------------------------------
// Advanced tab: system sound switches, audio processing, lock sounds
// ------------------------------------------------------------------------------------------

@Composable
private fun AdvancedTab(state: UiState, a: UiActions, listState: LazyListState) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val changed = state.slots.filter { it.label != null }
    val globalText = if (state.normalize) "${state.targetDb} dBFS" else "off"

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = LocalListTopPadding.current, bottom = LocalListBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // ---- System sound switches ----
        item { GroupHeader("System sound switches") }
        item {
            val s = state.sys
            val ringerOk = s.ringer == "Normal" && !s.systemMuted
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(0, listState)) {
                SettingsCard(segmentShape(0, 2), container) {
                    Text("Android's own sound settings", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "If one of these is off, the related sounds never play, including the ones you picked. " +
                            "The switches change the system setting directly; they are the same as the ones in Settings → Sound.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SwitchRow("Touch sounds", "Keyboard keys and screen touch sounds", s.touchSounds, true) {
                        a.setSystemToggle(SysToggle.TOUCH, it)
                    }
                    SwitchRow("Lock screen sounds", "Lock, unlock and trusted-device sounds", s.lockSounds, true) {
                        a.setSystemToggle(SysToggle.LOCK, it)
                    }
                    SwitchRow("Charging sounds", "Sound played when a charging cable is plugged in", s.chargingSounds, true) {
                        a.setSystemToggle(SysToggle.CHARGING, it)
                    }
                }
                SettingsCard(segmentShape(1, 2), container) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (ringerOk) Icons.Default.Check else Icons.Default.Warning, null,
                            tint = if (ringerOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("The phone is currently in ${s.ringer} mode", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "System volume ${s.systemVol}/${s.systemMax}" + (if (s.systemMuted) " (muted)" else "") +
                                    " · Media volume ${s.mediaVol}/${s.mediaMax}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Text(
                        if (ringerOk) "Sounds are able to play."
                        else "While the phone is silent or vibrating, or the system volume is muted, lock and touch sounds do not play. " +
                            "Turn on the ringer mode or raise the volume.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Audio processing ----
        item { GroupHeader("Audio processing") }
        item {
            Column(modifier = Modifier.staggerIn(1, listState)) {
                SettingsCard(segmentShape(0, 1), container) {
                    SwitchRow(
                        "Genel normalize",
                        "Unless set per sound, the sounds you pick are brought to this average level (peaks are softly limited)",
                        state.normalize, true, a.setNormalize
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Genel hedef seviye", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${state.targetDb} dBFS (ortalama)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { a.setTargetDb(state.targetDb - 1) }, enabled = state.normalize
                        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(
                            onClick = { a.setTargetDb(state.targetDb + 1) }, enabled = state.normalize
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                    Text(
                        "The setting only applies to sounds you pick from now on. To update the existing ones:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = a.reapplyAll,
                        enabled = state.root == RootState.GRANTED,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reprocess all selected sounds") }
                }
            }
        }

        item { GroupHeader("Per-sound level") }
        if (changed.isEmpty()) {
            item(key = "norm_empty") {
                SettingsCard(segmentShape(0, 1), container) {
                    Text(
                        "You have not changed any sound yet. Once you pick a sound in the Sounds tab you can set its own " +
                            "normalization here.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            itemsIndexed(changed, key = { _, ss -> "norm_${ss.slot.id}" }) { i, ss ->
                SlotNormRow(
                    shape = segmentShape(i, changed.size),
                    container = container,
                    ss = ss,
                    norm = state.slotNorm[ss.slot.id] ?: SlotNorm(0, state.targetDb),
                    globalText = globalText,
                    enabled = state.root == RootState.GRANTED,
                    onChange = { mode, db -> a.setSlotNorm(ss.slot.id, mode, db) },
                    onApply = { a.reapplySlot(ss.slot) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // ---- Lock sounds ----
        item { GroupHeader("Lock sounds") }
        item {
            Column(modifier = Modifier.staggerIn(2, listState)) {
                SettingsCard(segmentShape(0, 1), container) {
                    SwitchRow(
                        "Force-play lock sounds",
                        "SoundMod plays the lock, unlock and trusted-device sounds at the media volume; " +
                            "they are audible even when the phone is silent or vibrating.",
                        state.forceLock, state.root == RootState.GRANTED, a.setForceLock
                    )
                    Text(
                        "When off (default) Android's own mechanism is used and nothing is touched. " +
                            "When on, a hook is added to SystemUI; the sound is played with SoundPool (the engine the " +
                            "stock lock sound also uses). If the “Lock screen sounds” switch is off or the media volume is 0, " +
                            "the stock behavior is used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Headphones ----
        item { GroupHeader("Headphones") }
        item {
            Column(modifier = Modifier.staggerIn(3, listState)) {
                SettingsCard(segmentShape(0, 1), container) {
                    SwitchRow(
                        "Turn off the headphone volume warning",
                        "The “loud volume can damage your hearing” warning shown after a reboot will not appear.",
                        state.noHpWarning, state.root == RootState.GRANTED, a.setNoHpWarning
                    )
                    Text(
                        "The system re-enables this warning at every boot. While this is on, the three audio properties that " +
                            "enable it are turned off at every boot by a tiny script in /data/adb/soundmod; no hook is added to the system. " +
                            "If you turn it off the script is deleted and the warning returns from the next boot. Listening " +
                            "at high volume for a long time can damage your hearing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotNormRow(
    shape: Shape,
    container: Color,
    ss: SlotState,
    norm: SlotNorm,
    globalText: String,
    enabled: Boolean,
    onChange: (Int, Int) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by rememberSaveable(ss.slot.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "normchevron"
    )
    val summary = when (norm.mode) {
        1 -> "Normalization off"
        2 -> "Custom · ${norm.db} dBFS"
        else -> "Genel · $globalText"
    }
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { open = !open }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(ss.slot.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (open) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }
            AnimatedVisibility(
                visible = open,
                enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
                exit = shrinkVertically(tween(170, easing = FastOutSlowInEasing)) + fadeOut(tween(90))
            ) {
                Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Global", "Off", "Custom").forEachIndexed { i, t ->
                            FilterChip(
                                selected = norm.mode == i,
                                onClick = { onChange(i, norm.db) },
                                label = { Text(t) },
                                shape = CircleShape
                            )
                        }
                    }
                    if (norm.mode == 2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${norm.db} dBFS (ortalama)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalIconButton(onClick = { onChange(2, norm.db - 1) }) {
                                Text("−", style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalIconButton(onClick = { onChange(2, norm.db + 1) }) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    FilledTonalButton(
                        onClick = onApply,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Apply to this sound") }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Settings tab: status, appearance, tools
// ------------------------------------------------------------------------------------------

@Composable
private fun SettingsTab(state: UiState, a: UiActions, listState: LazyListState, onRestartClick: () -> Unit) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = LocalListTopPadding.current, bottom = LocalListBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // ---- Durum ----
        item { GroupHeader("Durum") }
        item {
            val info = state.rootInfo
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(0, listState)) {
                StatusCard(
                    shape = segmentShape(0, 2), container = container,
                    title = "Root", ok = state.root == RootState.GRANTED,
                    summary = when (state.root) {
                        RootState.GRANTED -> "Aktif · ${info.manager}" +
                            if (info.selinux.isNotBlank()) " · SELinux ${info.selinux}" else ""
                        RootState.DENIED -> "Not granted"
                        RootState.UNKNOWN -> "Kontrol ediliyor…"
                    },
                    text = when (state.root) {
                        RootState.GRANTED -> "Root izni verildi"
                        RootState.DENIED -> "No root access. Grant SoundMod access in your root manager."
                        RootState.UNKNOWN -> "Kontrol ediliyor…"
                    },
                    details = rootDetails(info)
                ) { OutlinedButton(onClick = a.refresh) { Text("Check again") } }
                StatusCard(
                    shape = segmentShape(1, 2), container = container,
                    title = "Vector module", ok = state.hookStatus != null,
                    summary = hookSummary(state.hookStatus),
                    text = state.hookStatus?.let {
                        if (it.contains("ERROR")) "Some hooks are not working" else "Hooks loaded"
                    } ?: ("The hooks have not run yet. Enable SoundMod in Vector, select “System Framework” and " +
                        "“System UI” in its scope, then reboot."),
                    details = hookDetails(state.hookStatus),
                    labelWidth = 120.dp
                ) {}
            }
        }

        // ---- Appearance ----
        item { GroupHeader("Appearance") }
        item {
            val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(1, listState)) {
                SettingsCard(segmentShape(0, 4), container) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                    val modes = listOf("System", "Light", "Dark")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        modes.forEachIndexed { i, t ->
                            FilterChip(
                                selected = state.themeMode == i,
                                onClick = { a.setThemeMode(i) },
                                label = { Text(t, maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    SwitchRow(
                        "OLED (saf siyah)",
                        "Makes the background black in the dark theme; also applied when the system switches to dark",
                        state.oled, true, a.setOled
                    )
                }
                SettingsCard(segmentShape(1, 4), container) {
                    SwitchRow(
                        "Use system colors",
                        if (dynamicOk) "Follows the wallpaper and system theme (Material You)" else "Not supported on this Android version",
                        state.dynamicColor && dynamicOk, dynamicOk, a.setDynamic
                    )
                    val paletteActive = !(state.dynamicColor && dynamicOk)
                    Text(
                        "Renk paleti",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.alpha(if (paletteActive) 1f else 0.5f)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(Palettes.list) { i, p ->
                            Swatch(
                                color = Palettes.swatch(p),
                                name = p.name,
                                selected = paletteActive && state.palette == i,
                                onClick = { a.setPalette(i) }
                            )
                        }
                    }
                    if (!paletteActive) {
                        Text(
                            "Picking a palette turns the system colors off automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SettingsCard(segmentShape(2, 4), container) {
                    SwitchRow("Reduce animations", "Calmer transitions", state.reduceMotion, true, a.setReduceMotion)
                }
                SettingsCard(segmentShape(3, 4), container) {
                    Text("Bottom navigation bar", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The list order is the left-to-right order in the bar. Press and hold the ≡ icon on the left and drag to " +
                            "reorder, and hide the ones you do not want. The Settings tab is always visible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Wide", "Compact").forEachIndexed { i, t ->
                            FilterChip(
                                selected = state.navStyle == i,
                                onClick = { a.setNavStyle(i) },
                                label = { Text(t) },
                                shape = CircleShape
                            )
                        }
                    }
                    TabOrderEditor(state, a)
                }
            }
        }

        // ---- Tools ----
        item { GroupHeader("Tools") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(2, listState)) {
                SettingsCard(segmentShape(0, 2), container) {
                    Text("SystemUI", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "If you just enabled the module or something gets stuck you can refresh SystemUI " +
                            "(the button at the top right does the same). The screen redraws for a few seconds; open " +
                            "apps are not closed. No sound setting does this on its own.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onRestartClick,
                        enabled = state.root == RootState.GRANTED,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Refresh SystemUI") }
                }
                SettingsCard(segmentShape(1, 2), container) {
                    Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Collects file permissions, settings, hook status, the last SystemUI event and SELinux denials into one report.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = a.buildReport,
                        enabled = state.root == RootState.GRANTED,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Generate diagnostic report") }
                    if (BuildConfig.VERBOSE_LOG) {
                        SwitchRow(
                            "Verbose event log (pre-release)",
                            "Writes lock and screenshot events to the report. Only available in pre-release builds.",
                            state.debugLog, state.root == RootState.GRANTED, a.setDebugLog
                        )
                    }
                }
            }
        }

        item {
            Text(
                "SoundMod 0.12.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 16.dp)
            )
        }
    }
}

/**
 * A collapsible status card. When collapsed it shows a short summary; tapping it reveals details and actions.
 * If there is a problem (ok = false) it opens by itself; once the user taps it, their choice is remembered.
 */
@Composable
private fun StatusCard(
    shape: Shape,
    container: Color,
    title: String,
    ok: Boolean,
    summary: String,
    text: String,
    details: List<Pair<String, String>> = emptyList(),
    labelWidth: Dp = 92.dp,
    actions: @Composable () -> Unit
) {
    // 0 = automatic (open if there is a problem), 1 = opened by the user, 2 = closed by the user
    var choice by rememberSaveable(title) { mutableStateOf(0) }
    val expanded = when (choice) {
        1 -> true
        2 -> false
        else -> !ok
    }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "chevron"
    )
    Card(
        onClick = { choice = if (expanded) 2 else 1 },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (ok) Icons.Default.Check else Icons.Default.Warning, null,
                    tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    // The summary row is always visible: the header height stays constant and nothing jumps when collapsing
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
                exit = shrinkVertically(tween(170, easing = FastOutSlowInEasing)) + fadeOut(tween(90))
            ) {
                Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                    if (details.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            details.forEach { (k, v) ->
                                Row {
                                    Text(
                                        k,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(labelWidth)
                                    )
                                    Text(v, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    actions()
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(shape: Shape, container: Color, content: @Composable () -> Unit) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun Swatch(color: Color, name: String, selected: Boolean, onClick: () -> Unit) {
    val size by animateDpAsState(
        targetValue = if (selected) 52.dp else 44.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "swatch"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(size).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Default.Check, null, tint = Color.White)
            }
        }
        Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

// ------------------------------------------------------------------------------------------
// Diagnostic report
// ------------------------------------------------------------------------------------------

@Composable
private fun ReportSheet(text: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Diagnostic report", style = MaterialTheme.typography.titleLarge)
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                        SelectionContainer {
                            Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("SoundMod raporu", text))
                }) { Text("Kopyala") }
                OutlinedButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}


// ------------------------------------------------------------------------------------------
// Playback progress, root info, first-run info
// ------------------------------------------------------------------------------------------

/**
 * A wavy bar that advances over the duration of the sound. The position is computed frame by frame from the
 * playback start time without querying the player (no polling), so the bar flows smoothly. The wave is visible from start to end.
 */
@Composable
private fun PlayProgress(player: State<PlayerUi>, modifier: Modifier = Modifier) {
    val p = player.value
    val progress = remember { mutableFloatStateOf(0f) }
    val shownMs = remember { mutableIntStateOf(0) }

    LaunchedEffect(p.key, p.playing, p.anchorPosMs, p.anchorTimeMs, p.durationMs) {
        val dur = p.durationMs
        fun posNow(): Int =
            if (p.playing) (p.anchorPosMs + (SystemClock.elapsedRealtime() - p.anchorTimeMs)).toInt() else p.anchorPosMs
        fun apply(pos: Int) {
            val c = if (dur > 0) pos.coerceIn(0, dur) else 0
            progress.floatValue = if (dur > 0) c.toFloat() / dur else 0f
            // The duration text updates with tenth-of-a-second precision (so it does not redraw on every frame)
            if (!p.playing || kotlin.math.abs(c - shownMs.intValue) >= 100) shownMs.intValue = c
        }
        apply(posNow())
        while (p.playing) {
            withFrameNanos { }
            apply(posNow())
        }
    }

    Column(modifier) {
        LinearWavyProgressIndicator(
            progress = { progress.floatValue },
            amplitude = { 1f },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            timeText(shownMs.intValue, p.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
        )
    }
}

private fun timeText(pos: Int, dur: Int): String {
    fun f(ms: Int): String =
        if (dur < 10_000) String.format(java.util.Locale.US, "%.1f sn", ms / 1000f)
        else String.format(java.util.Locale.US, "%d:%02d", ms / 60000, (ms / 1000) % 60)
    return "${f(pos)} / ${f(dur)}"
}

private fun rootDetails(i: RootInfo): List<Pair<String, String>> = buildList {
    if (i.manager.isNotBlank()) {
        add("Manager" to (i.manager + if (i.managerVersion.isNotBlank()) " · ${i.managerVersion}" else ""))
    }
    if (i.suVersion.isNotBlank()) add("su version" to i.suVersion)
    if (i.kernel.isNotBlank()) add("Kernel" to i.kernel)
    if (i.kernelMode.isNotBlank()) add("Mod" to i.kernelMode)
    if (i.selinux.isNotBlank()) add("SELinux" to i.selinux)
    if (i.context.isNotBlank()) add("Context" to i.context)
}

@Composable
private fun IntroDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Before you start") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IntroItem("Root access", "Sounds are deployed through root. Your root manager will ask for permission.")
                IntroItem(
                    "Enable in Vector",
                    "Turn SoundMod on; make sure System Framework and System UI are selected in its scope, then reboot the device."
                )
                IntroItem(
                    "Refresh SystemUI (Settings → Tools)",
                    "If you just enabled the module or the lock/screenshot sounds do not play, this restarts SystemUI " +
                        "without rebooting the device. The screen redraws for a few seconds; open " +
                        "apps are not closed. No setting does this on its own; you are asked first."
                )
                IntroItem("If something is stuck", "Settings → Tools → Generate diagnostic report shows what is not working.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
private fun IntroItem(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}


/**
 * Splits the hook status text (“system_server: A OK, B ERROR: …” / “SystemUI: … [Vector 2.2, API 102]”) into the same
 * “name → value” rows as the Root card. Commas inside parentheses do not break the split.
 */
private fun hookDetails(status: String?): List<Pair<String, String>> {
    if (status.isNullOrBlank()) return emptyList()
    val names = mapOf(
        "SoundEffectsHelper" to "Keyboard and touch",
        "Screenshot" to "Screenshot",
        "Keyguard" to "Lock screen"
    )
    var framework = ""
    val rows = ArrayList<Pair<String, String>>()
    for (line in status.lines()) {
        var body = line.substringAfter(':', "").trim()
        if (body.isEmpty()) continue
        val br = Regex("""\s*\[([^\]]*)]\s*$""").find(body)
        if (br != null) {
            framework = br.groupValues[1].replace(", API", " · framework API")
            body = body.substring(0, br.range.first)
        }
        val parts = ArrayList<String>()
        var depth = 0
        val cur = StringBuilder()
        var i = 0
        while (i < body.length) {
            val c = body[i]
            if (c == '(') depth++
            if (c == ')') depth--
            if (c == ',' && depth == 0) {
                parts += cur.toString().trim(); cur.clear()
            } else cur.append(c)
            i++
        }
        if (cur.isNotBlank()) parts += cur.toString().trim()
        for (part in parts) {
            val name = part.substringBefore(' ')
            val value = part.substringAfter(' ', "").trim()
            val shown = when {
                value == "OK" -> "Working"
                value.startsWith("ERROR") -> "Error" + value.removePrefix("ERROR").ifBlank { "" }
                value.startsWith("stock") -> "Stock behavior" + value.removePrefix("stock")
                else -> value.ifBlank { "—" }
            }
            rows += (names[name] ?: name) to shown
        }
    }
    if (framework.isNotBlank()) {
        rows.add(0, "Framework" to framework)
        rows.add(1, "Module API" to SoundPaths.MODULE_API.toString())
    }
    return rows
}

private fun hookSummary(status: String?): String = when {
    status == null -> "Not working"
    status.contains("ERROR") -> "Some hooks are not working"
    else -> "Kancalar aktif"
}

@Composable
private fun RestartPromptDialog(p: RestartPrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.title) },
        text = { Text(p.text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Yenile") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } }
    )
}


@Composable
private fun RestartConfirmDialog(onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var dontShowAgain by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Refresh SystemUI?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The status bar, notification shade and lock screen restart for a few seconds; the screen may " +
                        "flicker briefly. Open apps and your data are not affected. It helps if you just enabled the module or " +
                        "a sound is not playing."
                )
                // The row and the checkbox stay separately tappable; no gray ripple is shown
                CompositionLocalProvider(LocalRippleConfiguration provides null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { dontShowAgain = !dontShowAgain },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                        Text("Do not show again", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(dontShowAgain) }) { Text("Yenile") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


/**
 * Drag-and-drop ordering for the bottom bar tabs. Press and hold the ≡ icon on the left and drag; the other rows
 * slide to make room, and the new order is saved on release.
 */
@Composable
private fun TabOrderEditor(state: UiState, a: UiActions) {
    val haptic = LocalHapticFeedback.current
    val rowPx = with(LocalDensity.current) { 56.dp.toPx() }
    var dragging by remember { mutableIntStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    val order = state.tabOrder
    val target = if (dragging >= 0) {
        (dragging + (offset / rowPx).roundToInt()).coerceIn(0, order.lastIndex)
    } else -1
    val visibleOthers = order.count { it != 3 && it !in state.hiddenTabs }

    Column(Modifier.fillMaxWidth()) {
        order.forEachIndexed { index, id ->
            val isDragged = index == dragging
            val shift = when {
                dragging < 0 || isDragged -> 0f
                dragging < index && index <= target -> -rowPx
                target <= index && index < dragging -> rowPx
                else -> 0f
            }
            // On release the order changes instantly; the slide animation only runs while dragging
            val animShift by animateFloatAsState(
                targetValue = shift,
                animationSpec = if (dragging >= 0) spring(dampingRatio = 0.8f, stiffness = 500f) else snap(),
                label = "tabshift"
            )
            val shown = id == 3 || id !in state.hiddenTabs
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) offset else animShift
                        val sc = if (isDragged) 1.03f else 1f
                        scaleX = sc
                        scaleY = sc
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Press and hold, then drag to move",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(44.dp)
                        .pointerInput(id, order) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragging = index
                                    offset = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offset += dragAmount.y
                                },
                                onDragEnd = {
                                    val to = (index + (offset / rowPx).roundToInt()).coerceIn(0, order.lastIndex)
                                    if (to != index) a.moveTabTo(id, to)
                                    dragging = -1
                                    offset = 0f
                                },
                                onDragCancel = {
                                    dragging = -1
                                    offset = 0f
                                }
                            )
                        }
                        .padding(10.dp)
                )
                Spacer(Modifier.width(8.dp))
                Icon(tabIcon(id), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(TAB_TITLES[id], style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = shown,
                    onCheckedChange = { a.setTabHidden(id, !it) },
                    enabled = id != 3 && !(shown && visibleOthers <= 1)
                )
            }
        }
    }
}
