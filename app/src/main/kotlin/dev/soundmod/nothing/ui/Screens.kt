@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.soundmod.nothing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.soundmod.nothing.core.SlotType
import dev.soundmod.nothing.core.SoundCatalog
import dev.soundmod.nothing.core.SoundSlot
import dev.soundmod.nothing.core.SystemSound

@Composable
fun SoundModScreen(
    state: UiState,
    snackbar: SnackbarHostState,
    onRefresh: () -> Unit,
    onReloadLibrary: () -> Unit,
    onPreviewPath: (String) -> Unit,
    onTestFx: (Int) -> Unit,
    onPickSystem: (SoundSlot, SystemSound) -> Unit,
    onPickFile: (SoundSlot) -> Unit,
    onReset: (SoundSlot) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onNormalize: (Boolean) -> Unit,
    onTargetDb: (Int) -> Unit,
    onInstallModule: () -> Unit,
    onReboot: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Sesler", "Kütüphane", "Ayarlar")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(titles[tab]) })
                if (state.busy) {
                    if (state.reduceMotion) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.Notifications, null) }, label = { Text("Sesler") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.Search, null) }, label = { Text("Kütüphane") })
                NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.Settings, null) }, label = { Text("Ayarlar") })
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> SoundsTab(state, onTestFx, onPickSystem, onPickFile, onReset, onPreviewPath)
                1 -> LibraryTab(state, onReloadLibrary, onPreviewPath)
                else -> SettingsTab(
                    state, onRefresh, onReduceMotion, onNormalize, onTargetDb, onInstallModule, onReboot
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Sesler sekmesi
// ------------------------------------------------------------------------------------------

@Composable
private fun SoundsTab(
    state: UiState,
    onTestFx: (Int) -> Unit,
    onPickSystem: (SoundSlot, SystemSound) -> Unit,
    onPickFile: (SoundSlot) -> Unit,
    onReset: (SoundSlot) -> Unit,
    onPreviewPath: (String) -> Unit
) {
    var sheetSlot by remember { mutableStateOf<SoundSlot?>(null) }
    var pickerSlot by remember { mutableStateOf<SoundSlot?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(state.note, style = MaterialTheme.typography.titleMedium)
                    if (state.root == RootState.DENIED) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Root izni alınamadı. Ayarlar sekmesinden yeniden dene.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        SoundCatalog.slots.map { it.group }.distinct().forEach { group ->
            item {
                Text(
                    group,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            items(state.slots.filter { it.slot.group == group }, key = { it.slot.id }) { ss ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { sheetSlot = ss.slot },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ss.slot.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                ss.label ?: "Varsayılan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ss.label != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ss.slot.fx?.let { fx ->
                            IconButton(onClick = { onTestFx(fx) }) { Icon(Icons.Default.PlayArrow, "Dene") }
                        }
                    }
                }
            }
        }
    }

    sheetSlot?.let { slot ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { sheetSlot = null }, sheetState = sheetState) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text(slot.title, style = MaterialTheme.typography.headlineSmall)
                Text(slot.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { pickerSlot = slot; sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sistem seslerinden seç") }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { onPickFile(slot); sheetSlot = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kendi dosyamı seç") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onReset(slot); sheetSlot = null },
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
        LibraryPicker(
            title = slot.title,
            sounds = state.library,
            loading = state.libraryLoading,
            onPreview = onPreviewPath,
            onPick = { onPickSystem(slot, it); pickerSlot = null },
            onDismiss = { pickerSlot = null }
        )
    }
}

// ------------------------------------------------------------------------------------------
// Kütüphane
// ------------------------------------------------------------------------------------------

@Composable
private fun LibraryTab(state: UiState, onReload: () -> Unit, onPreview: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${state.library.size} ses bulundu",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onReload) { Icon(Icons.Default.Refresh, "Yeniden tara") }
        }
        SoundList(state.library, state.libraryLoading, onPreview, null)
    }
}

@Composable
private fun LibraryPicker(
    title: String,
    sounds: List<SystemSound>,
    loading: Boolean,
    onPreview: (String) -> Unit,
    onPick: (SystemSound) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("$title için ses seç") },
                    actions = { TextButton(onClick = onDismiss) { Text("Kapat") } }
                )
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                SoundList(sounds, loading, onPreview, onPick)
            }
        }
    }
}

@Composable
private fun SoundList(
    sounds: List<SystemSound>,
    loading: Boolean,
    onPreview: (String) -> Unit,
    onPick: ((SystemSound) -> Unit)?
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
            label = { Text("Ara") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Hepsi") }) }
            items(categories) { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
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
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(filtered, key = { it.path }) { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = onPick != null) { onPick?.invoke(s) }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${s.category} · ${s.partition}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onPreview(s.path) }) { Icon(Icons.Default.PlayArrow, "Dinle") }
                            if (onPick != null) {
                                IconButton(onClick = { onPick(s) }) { Icon(Icons.Default.Check, "Seç") }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Ayarlar
// ------------------------------------------------------------------------------------------

@Composable
private fun SettingsTab(
    state: UiState,
    onRefresh: () -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onNormalize: (Boolean) -> Unit,
    onTargetDb: (Int) -> Unit,
    onInstallModule: () -> Unit,
    onReboot: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatusCard(
                title = "Root",
                ok = state.root == RootState.GRANTED,
                text = when (state.root) {
                    RootState.GRANTED -> "Root izni verildi"
                    RootState.DENIED -> "Root izni yok. Kök yöneticisinde SoundMod'a izin ver."
                    RootState.UNKNOWN -> "Kontrol ediliyor…"
                }
            ) { TextButton(onClick = onRefresh) { Text("Yeniden kontrol et") } }
        }
        item {
            StatusCard(
                title = "Vector modülü",
                ok = state.hookStatus != null,
                text = state.hookStatus
                    ?: "Hook henüz çalışmamış. Vector'da SoundMod'u etkinleştir, kapsamda " +
                    "“Sistem Çerçevesi” ve “Sistem Arayüzü”nü seç, sonra yeniden başlat."
            ) {}
        }
        item {
            StatusCard(
                title = "SELinux yardımcı modülü (isteğe bağlı)",
                ok = state.moduleInstalled,
                text = if (state.moduleInstalled) "Kurulu. Sistem Arayüzü sesleri okunabilsin diye kural ekler."
                else "Kilit/ekran görüntüsü gibi SystemUI sesleri özel dosyada çalmazsa kur. Kurduktan sonra yeniden başlat."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onInstallModule, enabled = state.root == RootState.GRANTED) {
                        Text(if (state.moduleInstalled) "Yeniden kur" else "Kur")
                    }
                    OutlinedButton(onClick = onReboot, enabled = state.root == RootState.GRANTED) { Text("Yeniden başlat") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ses işleme", style = MaterialTheme.typography.titleMedium)
                    SwitchRow("Seviyeyi normalize et", "Kendi dosyaların ortak bir seviyeye getirilir", state.normalize, onNormalize)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hedef seviye", style = MaterialTheme.typography.bodyLarge)
                            Text("${state.targetDb} dBFS (ort.)", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { onTargetDb(state.targetDb - 2) }, enabled = state.normalize) { Text("−") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { onTargetDb(state.targetDb + 2) }, enabled = state.normalize) { Text("+") }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Görünüm", style = MaterialTheme.typography.titleMedium)
                    SwitchRow("Animasyonları azalt", "Daha sakin geçişler", state.reduceMotion, onReduceMotion)
                }
            }
        }
        item {
            Text(
                "SoundMod 0.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
private fun StatusCard(title: String, ok: Boolean, text: String, actions: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (ok) Icons.Default.Check else Icons.Default.Warning, null)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
