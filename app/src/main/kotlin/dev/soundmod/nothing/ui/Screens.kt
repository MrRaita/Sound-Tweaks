@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.soundmod.nothing.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val setDynamic: (Boolean) -> Unit,
    val setPalette: (Int) -> Unit,
    val restartSysUiNow: () -> Unit,
    val setSystemToggle: (SysToggle, Boolean) -> Unit,
    val installModule: () -> Unit,
    val reboot: () -> Unit,
    val buildReport: () -> Unit,
    val clearReport: () -> Unit,
    val dismissIntro: () -> Unit,
    val showIntro: () -> Unit,
    val setDebugLog: (Boolean) -> Unit,
    val setForceLock: (Boolean) -> Unit,
    val confirmRestartPrompt: () -> Unit,
    val dismissRestartPrompt: () -> Unit
)

@Composable
fun SoundModScreen(
    state: UiState,
    player: State<PlayerUi>,
    snackbar: SnackbarHostState,
    a: UiActions
) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Sesler", "Kütüphane", "Ayarlar")
    val listStates = listOf(rememberLazyListState(), rememberLazyListState(), rememberLazyListState())

    LaunchedEffect(tab) {
        if (tab == 1) a.ensureLibrary()
        if (tab == 2) a.refreshSys()
    }

    CompositionLocalProvider(LocalReduceMotion provides state.reduceMotion) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                Column {
                    Header(titles[tab], state.reduceMotion)
                    if (state.busy) {
                        if (state.reduceMotion) LinearProgressIndicator(Modifier.fillMaxWidth())
                        else LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    tonalElevation = 3.dp
                ) {
                    NavigationBar(containerColor = Color.Transparent) {
                        NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.Notifications, null) }, label = { Text("Sesler") })
                        NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.Search, null) }, label = { Text("Kütüphane") })
                        NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.Settings, null) }, label = { Text("Ayarlar") })
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        if (state.reduceMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = 300f)) { it * dir / 3 } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(180)) { -it * dir / 3 } + fadeOut(tween(120)))
                        }
                    },
                    label = "tabs"
                ) { t ->
                    when (t) {
                        0 -> SoundsTab(state, player, a, listStates[0])
                        1 -> LibraryTab(state, player, a, listStates[1])
                        else -> SettingsTab(state, a, listStates[2])
                    }
                }
                if (state.busy && !state.reduceMotion) {
                    LoadingIndicator(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(48.dp))
                }
            }
        }
    }

    state.restartPrompt?.let { RestartPromptDialog(it, a.confirmRestartPrompt, a.dismissRestartPrompt) }
    if (state.showIntro) IntroDialog(a.dismissIntro)
    state.report?.let { text -> ReportSheet(text, a.clearReport) }
}

// ------------------------------------------------------------------------------------------
// Başlık: üstte uygulama adı, altında bulunulan sekme
// ------------------------------------------------------------------------------------------

@Composable
private fun Header(title: String, reduceMotion: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 14.dp)
    ) {
        Text(
            "SoundMod",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                if (reduceMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 3 }) togetherWith
                    (fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 3 })
            },
            label = "title"
        ) { t ->
            Text(
                t,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
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
                        OutlinedButton(onClick = a.refresh) { Text("Yeniden dene") }
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
        AnimatedVisibility(visible = active) {
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
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 28.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${state.library.size} ses bulundu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = a.reloadLibrary) { Icon(Icons.Default.Refresh, "Yeniden tara") }
        }
        SoundList(
            sounds = state.library,
            loading = state.libraryLoading,
            player = player,
            onPlay = a.playLibrary,
            onPick = null,
            listState = listState
        )
    }
}

@Composable
private fun SoundList(
    sounds: List<SystemSound>,
    loading: Boolean,
    player: State<PlayerUi>,
    onPlay: (String) -> Unit,
    onPick: ((SystemSound) -> Unit)?,
    listState: LazyListState = rememberLazyListState()
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val categories = remember(sounds) { sounds.map { it.category }.distinct() }
    val filtered = remember(sounds, query, category) {
        sounds.filter {
            (category == null || it.category == category) &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            shape = CircleShape,
            label = { Text("Ara") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
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
            AnimatedVisibility(visible = active) {
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

@Composable
private fun SettingsTab(state: UiState, a: UiActions, listState: LazyListState) {
    var confirmRestart by remember { mutableStateOf(false) }
    val container = MaterialTheme.colorScheme.surfaceContainerHigh

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // ---- Durum ----
        item { GroupHeader("Durum") }
        item {
            val info = state.rootInfo
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(0, listState)) {
                StatusCard(
                    shape = segmentShape(0, 3), container = container,
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
                    shape = segmentShape(1, 3), container = container,
                    title = "Vector modülü", ok = state.hookStatus != null,
                    summary = hookSummary(state.hookStatus),
                    text = state.hookStatus
                        ?: "Hook henüz çalışmamış. Vector'da SoundMod'u etkinleştir, kapsamda " +
                        "“Sistem Çerçevesi” ve “Sistem Arayüzü”nü seç, sonra yeniden başlat."
                ) {}
                StatusCard(
                    shape = segmentShape(2, 3), container = container,
                    title = "SELinux yardımcı modülü (isteğe bağlı)", ok = state.moduleInstalled,
                    summary = if (state.moduleInstalled) "Kurulu" else "Kurulu değil",
                    text = if (state.moduleInstalled)
                        "Kurulu. Açılışta ses deposunun SELinux bağlamını onarır."
                    else "Açılışta ses deposunun bağlamını onarır. Kilit/ekran görüntüsü sesleri özel dosyada çalmazsa kur."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = a.installModule, enabled = state.root == RootState.GRANTED) {
                            Text(if (state.moduleInstalled) "Yeniden kur" else "Kur")
                        }
                        OutlinedButton(onClick = a.reboot, enabled = state.root == RootState.GRANTED) { Text("Yeniden başlat") }
                    }
                }
            }
        }

        // ---- Sistem ses durumu ----
        item { GroupHeader("Sistem ses anahtarları") }
        item {
            val s = state.sys
            val ringerOk = s.ringer == "Normal" && !s.systemMuted
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(1, listState)) {
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
                                "Sistem ses seviyesi ${s.systemVol}/${s.systemMax}" + if (s.systemMuted) " (kapalı)" else "",
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
            Column(modifier = Modifier.staggerIn(2, listState)) {
                SettingsCard(segmentShape(0, 1), container) {
                    SwitchRow(
                        "Seviyeyi normalize et",
                        "Seçtiğin sesler ortak bir ortalama seviyeye getirilir (tepeler yumuşak sınırlanır)",
                        state.normalize, true, a.setNormalize
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hedef seviye", style = MaterialTheme.typography.bodyLarge)
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
                    ) { Text("Seçili sesleri yeniden işle") }
                }
            }
        }

        // ---- Görünüm ----
        item { GroupHeader("Görünüm") }
        item {
            val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(3, listState)) {
                SettingsCard(segmentShape(0, 3), container) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Sistemi izle", "Açık", "Koyu").forEachIndexed { i, t ->
                            FilterChip(
                                selected = state.themeMode == i,
                                onClick = { a.setThemeMode(i) },
                                label = { Text(t) },
                                shape = CircleShape
                            )
                        }
                    }
                }
                SettingsCard(segmentShape(1, 3), container) {
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
                SettingsCard(segmentShape(2, 3), container) {
                    SwitchRow("Animasyonları azalt", "Daha sakin geçişler", state.reduceMotion, true, a.setReduceMotion)
                }
            }
        }

        // ---- Gelişmiş ----
        item { GroupHeader("Gelişmiş") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.staggerIn(4, listState)) {
                SettingsCard(segmentShape(0, 3), container) {
                    SwitchRow(
                        "Kilit seslerini zorla çal",
                        "Kilitleme, kilit açma ve güvenilir cihaz seslerini SoundMod medya ses seviyesiyle çalar; " +
                            "telefon sessiz ya da titreşimdeyken de duyulur.",
                        state.forceLock, state.root == RootState.GRANTED, a.setForceLock
                    )
                    Text(
                        "Kapalıyken (varsayılan) Android'in kendi mekanizması kullanılır ve hiçbir şeye dokunulmaz. " +
                            "Açıkken Sistem Arayüzüne bir kanca eklenir; ses SoundPool ile (stok kilit sesinin de " +
                            "kullandığı motor) çalınır. “Kilit ekranı sesleri” anahtarı kapalıysa yine çalmaz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SettingsCard(segmentShape(1, 3), container) {
                    Text("Sistem Arayüzü", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Modülü yeni etkinleştirdiysen ya da bir şey takılırsa Sistem Arayüzünü yenileyebilirsin. " +
                            "Ekran birkaç saniye yeniden çizilir; açık uygulamalar kapanmaz. Hiçbir ses ayarı bunu kendiliğinden yapmaz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { confirmRestart = true },
                        enabled = state.root == RootState.GRANTED,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sistem Arayüzünü yenile") }
                }
                SettingsCard(segmentShape(2, 3), container) {
                    Text("Tanılama", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Dosya izinleri, ayarlar, hook durumu, son SystemUI olayı ve SELinux reddlerini tek raporda toplar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = a.buildReport,
                        enabled = state.root == RootState.GRANTED,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Tanı raporu oluştur") }
                    SwitchRow(
                        "Ayrıntılı olay kaydı",
                        "Kilit ve ekran görüntüsü olaylarını rapora yazar. Kapalıyken arka planda hiçbir şey yazılmaz.",
                        state.debugLog, state.root == RootState.GRANTED, a.setDebugLog
                    )
                    OutlinedButton(onClick = a.showIntro, modifier = Modifier.fillMaxWidth()) {
                        Text("Başlangıç bilgisini yeniden göster")
                    }
                }
            }
        }

        item {
            Text(
                "SoundMod 0.5.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 16.dp)
            )
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Sistem Arayüzü yenilensin mi?") },
            text = {
                Text(
                    "Durum çubuğu, bildirim paneli ve kilit ekranı birkaç saniye yeniden başlatılır; ekran kısa süre " +
                        "titreyebilir. Açık uygulamalar ve verilerin etkilenmez."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmRestart = false; a.restartSysUiNow() }) { Text("Yenile") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text("Vazgeç") }
            }
        )
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
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { choice = if (expanded) 2 else 1 }
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
                    if (!expanded) {
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Daralt" else "Genişlet",
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }
            AnimatedVisibility(visible = expanded) {
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
                                        modifier = Modifier.width(92.dp)
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

/** Ses süresi boyunca saniye saniye ilerleyen dalgalı çubuk; dalga baştan sona görünür. */
@Composable
private fun PlayProgress(player: State<PlayerUi>, modifier: Modifier = Modifier) {
    Column(modifier) {
        LinearWavyProgressIndicator(
            progress = { player.value.progress },
            amplitude = { 1f },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            timeText(player.value.positionMs, player.value.durationMs),
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
                IntroItem("Takılırsa", "Ayarlar → Gelişmiş → Tanı raporu neyin çalışmadığını gösterir.")
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
