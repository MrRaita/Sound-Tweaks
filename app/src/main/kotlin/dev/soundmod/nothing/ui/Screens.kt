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

/** Arayüzün çağırdığı tüm eylemler (tek yerde toplandı). */
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

private val TAB_TITLES = listOf("Sesler", "Kütüphane", "Gelişmiş", "Ayarlar")

private fun tabIcon(id: Int): ImageVector = when (id) {
    0 -> Icons.Default.Notifications
    1 -> Icons.Default.Search
    2 -> Icons.Default.WrenchIcon
    else -> Icons.Default.Settings
}

@Composable
fun SoundModScreen(
    state: UiState,
    player: State<PlayerUi>,
    snackbar: SnackbarHostState,
    a: UiActions
) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = TAB_TITLES
    // Ayarlar (3) her zaman görünür; diğerleri kullanıcı sırasına göre, gizlenmemişse
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

    // Üst başlık iki durumludur (geniş / dar): kaydırma eşiği aşılınca yaylı bir geçişle daralır, başa dönülünce açılır.
    // Eşikler arasında (histerezis) durum değişmez; başlık yarı daralmış bir ara halde kalmaz.
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

    // Çubuklar listenin üstünde yüzer; listeler ekranın altına kadar uzanır, son satırlar çubuğun altında kalmaz
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottom = 96.dp + navInset
    // Başlık listenin üstüne biner (köşelerin ardında liste görünür); listeler genişlemiş başlık kadar boşlukla başlar
    var headerPx by remember { mutableIntStateOf(0) }
    val headerInset = with(density) { headerPx.toDp() }
    // Başlığın anlık yüksekliği (Kütüphane'nin sabit arama/filtre alanı başlığa bağlı kalsın diye)
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

                // İki stil de yüzer; fark yalnızca genişliktir (geniş çubukta seçili sekme yazıyla genişler)
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
// Başlık: sekme başında geniş; aşağı kaydırınca uygulama adı yukarı çekilir, yerine sekme adı geçer.
// Alt sınır kaydırma hareketine göre çekilip itilir.
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
    // Dar görünümde sekme adı, uygulama adıyla aynı yazı tipi ve kalınlıkta, yalnızca biraz küçük
    val compactTitle = bigTitle.copy(fontSize = 30.sp, lineHeight = 36.sp)

    Box(
        Modifier
            .fillMaxWidth()
            // Bildirilen yükseklik geniş ↔ dar arasında değişir (yalnızca yerleşim aşaması okur)
            // Arka plan, yükseklik değiştiren layout'tan ÖNCE verilmeli: aksi halde arka plan eski (geniş) boyutta
            // çizilir ve başlık içerik daralsa da küçülmüş görünmez.
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
            // Geniş görünüm: uygulama adı büyük, altında sekme adı küçük; kaydırınca yukarı çekilip solar
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
            // Dar görünüm: sekme adı, uygulama adının yerine aşağıdan yükselerek geçer
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
                Icon(Icons.Default.Refresh, contentDescription = "Sistem Arayüzünü yenile")
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
                        Text("Root izni yok", style = MaterialTheme.typography.titleMedium)
                        Text("Ses değiştirmek için kök yöneticisinde SoundMod'a izin ver.", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = a.refresh,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) { Text("Yeniden dene") }
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
                        "Android bu sesi Sistem Arayüzü açılırken bir kez yükler; değişince yenileme önerilir. " +
                            "Sessiz modda da duyulmasını istersen Ayarlar → Gelişmiş → “Kilit seslerini zorla çal”.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { pickerSlot = slot; sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sistem seslerinden seç") }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { a.pickFile(slot); sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kendi dosyamı seç") }
                slot.fx?.let { fx ->
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { a.testFx(fx) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sistem üzerinden dene")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { a.applySilent(slot); sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Bu sesi sustur") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { a.reset(slot); sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Varsayılana dön")
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
                    "${slot.title} için ses seç",
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
                                "Özel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    ss.label ?: "Varsayılan",
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
// Kütüphane
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
            IconButton(onClick = a.reloadLibrary) { Icon(Icons.Default.Refresh, "Yeniden tara") }
        }
    )
}

/**
 * Ses listesi. Ekran arama çubuğuyla açılmaz: üstte yalnızca bir arama simgesi vardır; dokununca arama alanı
 * açılır, kapatılınca temizlenir.
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
            // Arama alanı açılış animasyonuyla oluşur; odak istemeden önce yerleşmesi beklenir
            delay(80)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
    }

    val headerHeight = LocalHeaderHeight.current
    // Arama satırı ve filtre çipleri başlığa bağlıdır: başlık daralırken birlikte yukarı kayar, liste onlarla büyür
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
                if (filtered.size != sounds.size) "${filtered.size} / ${sounds.size} ses" else "${sounds.size} ses bulundu",
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
                    if (searching) "Aramayı kapat" else "Ara"
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
                placeholder = { Text("Ses ara") },
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
                    "Ses bulunamadı",
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
                    FilledTonalIconButton(onClick = onPick) { Icon(Icons.Default.Check, "Seç") }
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
// Gelişmiş sekmesi: sistem ses anahtarları, ses işleme, kilit sesleri
// ------------------------------------------------------------------------------------------

@Composable
private fun AdvancedTab(state: UiState, a: UiActions, listState: LazyListState) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val changed = state.slots.filter { it.label != null }
    val globalText = if (state.normalize) "${state.targetDb} dBFS" else "kapalı"

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = LocalListTopPadding.current, bottom = LocalListBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // ---- Sistem ses anahtarları ----
        item { GroupHeader("Sistem ses anahtarları") }
        item {
            val s = state.sys
            val ringerOk = s.ringer == "Normal" && !s.systemMuted
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(0, listState)) {
                SettingsCard(segmentShape(0, 2), container) {
                    Text("Android'in kendi ses ayarları", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Bunlardan biri kapalıysa ilgili sesler, senin seçtiklerin dahil, hiç çalmaz. " +
                            "Anahtarlar doğrudan sistem ayarını değiştirir; Ayarlar → Ses bölümündekilerle aynıdır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SwitchRow("Dokunma sesleri", "Klavye tuşları ve ekran dokunma sesleri", s.touchSounds, true) {
                        a.setSystemToggle(SysToggle.TOUCH, it)
                    }
                    SwitchRow("Kilit ekranı sesleri", "Kilitleme, kilit açma ve güvenilir cihaz sesleri", s.lockSounds, true) {
                        a.setSystemToggle(SysToggle.LOCK, it)
                    }
                    SwitchRow("Şarj sesleri", "Şarj kablosu takılınca çalan ses", s.chargingSounds, true) {
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
                            Text("Şu an telefon: ${s.ringer} modda", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Sistem sesi ${s.systemVol}/${s.systemMax}" + (if (s.systemMuted) " (kapalı)" else "") +
                                    " · Medya sesi ${s.mediaVol}/${s.mediaMax}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Text(
                        if (ringerOk) "Sesler çalabilir durumda."
                        else "Telefon sessiz ya da titreşimdeyken, ya da sistem sesi kapalıyken kilit ve dokunma sesleri çalmaz. " +
                            "Zil modunu ya da ses seviyesini aç.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Ses işleme ----
        item { GroupHeader("Ses işleme") }
        item {
            Column(modifier = Modifier.staggerIn(1, listState)) {
                SettingsCard(segmentShape(0, 1), container) {
                    SwitchRow(
                        "Genel normalize",
                        "Ses başına ayar yapılmadıkça seçtiğin sesler bu ortalama seviyeye getirilir (tepeler yumuşak sınırlanır)",
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
                        "Ayar yalnızca bundan sonra seçtiklerine uygulanır. Mevcut sesleri güncellemek için:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = a.reapplyAll,
                        enabled = state.root == RootState.GRANTED,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Seçili tüm sesleri yeniden işle") }
                }
            }
        }

        item { GroupHeader("Ses başına seviye") }
        if (changed.isEmpty()) {
            item(key = "norm_empty") {
                SettingsCard(segmentShape(0, 1), container) {
                    Text(
                        "Henüz değiştirdiğin bir ses yok. Sesler sekmesinden bir ses seçtiğinde burada kendi normalize " +
                            "ayarını yapabilirsin.",
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

        // ---- Kilit sesleri ----
        item { GroupHeader("Kilit sesleri") }
        item {
            Column(modifier = Modifier.staggerIn(2, listState)) {
                SettingsCard(segmentShape(0, 1), container) {
                    SwitchRow(
                        "Kilit seslerini zorla çal",
                        "Kilitleme, kilit açma ve güvenilir cihaz seslerini SoundMod medya ses seviyesiyle çalar; " +
                            "telefon sessiz ya da titreşimdeyken de duyulur.",
                        state.forceLock, state.root == RootState.GRANTED, a.setForceLock
                    )
                    Text(
                        "Kapalıyken (varsayılan) Android'in kendi mekanizması kullanılır ve hiçbir şeye dokunulmaz. " +
                            "Açıkken Sistem Arayüzüne bir kanca eklenir; ses SoundPool ile (stok kilit sesinin de " +
                            "kullandığı motor) çalınır. “Kilit ekranı sesleri” anahtarı kapalıysa ya da medya sesi 0 ise " +
                            "stok davranışa dönülür.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Kulaklık ----
        item { GroupHeader("Kulaklık") }
        item {
            Column(modifier = Modifier.staggerIn(3, listState)) {
                SettingsCard(segmentShape(0, 1), container) {
                    SwitchRow(
                        "Kulaklık ses uyarısını kapat",
                        "Yeniden başlatmadan sonra çıkan “yüksek ses kulaklarına zarar verebilir” uyarısı gösterilmez.",
                        state.noHpWarning, state.root == RootState.GRANTED, a.setNoHpWarning
                    )
                    Text(
                        "Sistem her açılışta bu uyarıyı yeniden etkinleştirir. Açıkken, her açılışta uyarıyı açan üç ses " +
                            "ayarı kapatılır; bunu /data/adb/soundmod içindeki küçük bir betik yapar, sisteme kanca eklenmez. " +
                            "Kapatırsanız betik silinir, uyarı sonraki açılıştan itibaren geri gelir. Uzun süre yüksek sesle " +
                            "dinlemek işitmeye zarar verebilir.",
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
        1 -> "Normalize kapalı"
        2 -> "Özel · ${norm.db} dBFS"
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
                    contentDescription = if (open) "Daralt" else "Genişlet",
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
                        listOf("Genel", "Kapalı", "Özel").forEachIndexed { i, t ->
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
                    ) { Text("Bu sese uygula") }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Ayarlar sekmesi: durum, görünüm, araçlar
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
                        RootState.DENIED -> "İzin yok"
                        RootState.UNKNOWN -> "Kontrol ediliyor…"
                    },
                    text = when (state.root) {
                        RootState.GRANTED -> "Root izni verildi"
                        RootState.DENIED -> "Root izni yok. Kök yöneticisinde SoundMod'a izin ver."
                        RootState.UNKNOWN -> "Kontrol ediliyor…"
                    },
                    details = rootDetails(info)
                ) { OutlinedButton(onClick = a.refresh) { Text("Yeniden kontrol et") } }
                StatusCard(
                    shape = segmentShape(1, 2), container = container,
                    title = "Vector modülü", ok = state.hookStatus != null,
                    summary = hookSummary(state.hookStatus),
                    text = state.hookStatus?.let {
                        if (it.contains("HATA")) "Bazı kancalar çalışmıyor" else "Kancalar yüklendi"
                    } ?: ("Hook henüz çalışmamış. Vector'da SoundMod'u etkinleştir, kapsamda " +
                        "“Sistem Çerçevesi” ve “Sistem Arayüzü”nü seç, sonra yeniden başlat."),
                    details = hookDetails(state.hookStatus),
                    labelWidth = 120.dp
                ) {}
            }
        }

        // ---- Görünüm ----
        item { GroupHeader("Görünüm") }
        item {
            val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(1, listState)) {
                SettingsCard(segmentShape(0, 4), container) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                    val modes = listOf("Sistem", "Açık", "Koyu")
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
                        "Koyu temada arka planı siyah yapar; sistem koyuya geçince de uygulanır",
                        state.oled, true, a.setOled
                    )
                }
                SettingsCard(segmentShape(1, 4), container) {
                    SwitchRow(
                        "Sistem renklerini kullan",
                        if (dynamicOk) "Duvar kağıdı ve sistem temasına uyar (Material You)" else "Bu Android sürümünde desteklenmiyor",
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
                            "Bir palet seçersen sistem renkleri otomatik kapanır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SettingsCard(segmentShape(2, 4), container) {
                    SwitchRow("Animasyonları azalt", "Daha sakin geçişler", state.reduceMotion, true, a.setReduceMotion)
                }
                SettingsCard(segmentShape(3, 4), container) {
                    Text("Alt gezinti çubuğu", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Liste sırası çubuktaki soldan sağa sıradır. Soldaki ≡ simgesine basılı tutup sürükleyerek sırayı " +
                            "değiştir, istemediklerini gizle. Ayarlar sekmesi her zaman görünür.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Geniş", "Kompakt").forEachIndexed { i, t ->
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

        // ---- Araçlar ----
        item { GroupHeader("Araçlar") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(2, listState)) {
                SettingsCard(segmentShape(0, 2), container) {
                    Text("Sistem Arayüzü", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Modülü yeni etkinleştirdiysen ya da bir şey takılırsa Sistem Arayüzünü yenileyebilirsin " +
                            "(sağ üstteki düğmeyle de aynısı yapılır). Ekran birkaç saniye yeniden çizilir; açık " +
                            "uygulamalar kapanmaz. Hiçbir ses ayarı bunu kendiliğinden yapmaz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onRestartClick,
                        enabled = state.root == RootState.GRANTED,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sistem Arayüzünü yenile") }
                }
                SettingsCard(segmentShape(1, 2), container) {
                    Text("Tanılama", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Dosya izinleri, ayarlar, hook durumu, son SystemUI olayı ve SELinux reddlerini tek raporda toplar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = a.buildReport,
                        enabled = state.root == RootState.GRANTED,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Tanı raporu oluştur") }
                    if (BuildConfig.VERBOSE_LOG) {
                        SwitchRow(
                            "Ayrıntılı olay kaydı (ön sürüm)",
                            "Kilit ve ekran görüntüsü olaylarını rapora yazar. Yalnızca ön sürümlerde bulunur.",
                            state.debugLog, state.root == RootState.GRANTED, a.setDebugLog
                        )
                    }
                }
            }
        }

        item {
            Text(
                "SoundMod 0.11.8",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 16.dp)
            )
        }
    }
}

/**
 * Daralıp açılır durum kartı. Daraltılmışken kısa bir özet gösterir; dokununca ayrıntılar ve eylemler açılır.
 * Sorun varsa (ok = false) kendiliğinden açık gelir; kullanıcı dokunursa seçimi hatırlanır.
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
    // 0 = otomatik (sorun varsa açık), 1 = kullanıcı açtı, 2 = kullanıcı kapattı
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
                    // Özet satırı her durumda görünür: başlık yüksekliği sabit kalır, kapanırken sıçrama olmaz
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
                    contentDescription = if (expanded) "Daralt" else "Genişlet",
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
// Tanı raporu
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
            Text("Tanı raporu", style = MaterialTheme.typography.titleLarge)
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
                OutlinedButton(onClick = onDismiss) { Text("Kapat") }
            }
        }
    }
}


// ------------------------------------------------------------------------------------------
// Çalma ilerlemesi, root bilgisi, ilk açılış bilgisi
// ------------------------------------------------------------------------------------------

/**
 * Ses süresi boyunca ilerleyen dalgalı çubuk. Konum, oynatıcı sorgulanmadan (yoklama yok) çalma başlangıç
 * anından kare kare hesaplanır; böylece çubuk takılmadan akar. Dalga baştan sona görünür.
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
            // Süre yazısı saniyenin onda biri hassasiyetinde güncellenir (her karede yeniden çizim olmasın)
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
        add("Yönetici" to (i.manager + if (i.managerVersion.isNotBlank()) " · ${i.managerVersion}" else ""))
    }
    if (i.suVersion.isNotBlank()) add("su sürümü" to i.suVersion)
    if (i.kernel.isNotBlank()) add("Çekirdek" to i.kernel)
    if (i.kernelMode.isNotBlank()) add("Mod" to i.kernelMode)
    if (i.selinux.isNotBlank()) add("SELinux" to i.selinux)
    if (i.context.isNotBlank()) add("Bağlam" to i.context)
}

@Composable
private fun IntroDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Başlamadan önce") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IntroItem("Root izni", "Sesler root ile dağıtılır. Kök yöneticin izin isteyecek.")
                IntroItem(
                    "Vector'da etkinleştir",
                    "SoundMod'u aç; kapsamda Sistem Çerçevesi ve Sistem Arayüzü seçili olsun, sonra cihazı yeniden başlat."
                )
                IntroItem(
                    "Sistem Arayüzünü yenile (Ayarlar → Gelişmiş)",
                    "Modülü yeni etkinleştirdiysen ya da kilit/ekran görüntüsü sesleri çalmıyorsa, cihazı yeniden " +
                        "başlatmadan Sistem Arayüzünü yeniden başlatır. Ekran birkaç saniye yeniden çizilir, açık " +
                        "uygulamalar kapanmaz. Hiçbir ayar bunu kendiliğinden yapmaz; önce sana sorulur."
                )
                IntroItem("Takılırsa", "Ayarlar → Araçlar → Tanı raporu oluştur neyin çalışmadığını gösterir.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Anladım") } }
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
 * Hook durum metnini (“system_server: A OK, B HATA: …” / “SystemUI: … [Vector 2.2, API 102]”) Root kartındaki gibi
 * “ad → değer” satırlarına ayırır. Parantez içindeki virgüller bölmeyi bozmaz.
 */
private fun hookDetails(status: String?): List<Pair<String, String>> {
    if (status.isNullOrBlank()) return emptyList()
    val names = mapOf(
        "SoundEffectsHelper" to "Klavye ve dokunma",
        "Screenshot" to "Ekran görüntüsü",
        "Keyguard" to "Kilit ekranı"
    )
    var framework = ""
    val rows = ArrayList<Pair<String, String>>()
    for (line in status.lines()) {
        var body = line.substringAfter(':', "").trim()
        if (body.isEmpty()) continue
        val br = Regex("""\s*\[([^\]]*)]\s*$""").find(body)
        if (br != null) {
            framework = br.groupValues[1].replace(", API", " · çerçeve API")
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
                value == "OK" -> "Çalışıyor"
                value.startsWith("HATA") -> "Hata" + value.removePrefix("HATA").ifBlank { "" }
                value.startsWith("stok") -> "Stok davranış" + value.removePrefix("stok")
                else -> value.ifBlank { "—" }
            }
            rows += (names[name] ?: name) to shown
        }
    }
    if (framework.isNotBlank()) {
        rows.add(0, "Çerçeve" to framework)
        rows.add(1, "Modül API" to SoundPaths.MODULE_API.toString())
    }
    return rows
}

private fun hookSummary(status: String?): String = when {
    status == null -> "Çalışmıyor"
    status.contains("HATA") -> "Bazı kancalar çalışmıyor"
    else -> "Kancalar aktif"
}

@Composable
private fun RestartPromptDialog(p: RestartPrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.title) },
        text = { Text(p.text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Yenile") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Sonra") } }
    )
}


@Composable
private fun RestartConfirmDialog(onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var dontShowAgain by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sistem Arayüzü yenilensin mi?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Durum çubuğu, bildirim paneli ve kilit ekranı birkaç saniye yeniden başlatılır; ekran kısa süre " +
                        "titreyebilir. Açık uygulamalar ve verilerin etkilenmez. Modülü yeni etkinleştirdiysen ya da " +
                        "bir ses çalmıyorsa işe yarar."
                )
                // Satır ve kutucuk ayrı ayrı dokunulabilir kalır; gri dalga (ripple) izi gösterilmez
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
                        Text("Bir daha gösterme", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(dontShowAgain) }) { Text("Yenile") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}


/**
 * Alt çubuk sekmeleri için sürükle-bırak sıralama. Soldaki ≡ simgesine basılı tutup sürüklenir; diğer satırlar
 * yer açmak için kayar, bırakınca yeni sıra kaydedilir.
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
            // Bırakınca sıra anında değişir; kayma animasyonu yalnızca sürüklerken çalışır
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
                    contentDescription = "Basılı tutup sürükleyerek taşı",
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
