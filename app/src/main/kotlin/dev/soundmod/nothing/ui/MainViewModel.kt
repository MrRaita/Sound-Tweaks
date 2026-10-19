package dev.soundmod.nothing.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Handler
import android.os.SystemClock
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.soundmod.nothing.core.ApplyInfo
import dev.soundmod.nothing.core.AudioConverter
import dev.soundmod.nothing.core.Root
import dev.soundmod.nothing.core.RootInfo
import dev.soundmod.nothing.core.SoundCatalog
import dev.soundmod.nothing.core.SoundPaths
import dev.soundmod.nothing.core.SoundRepository
import dev.soundmod.nothing.core.SoundScanner
import dev.soundmod.nothing.core.SoundSlot
import dev.soundmod.nothing.core.SystemSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RootState { UNKNOWN, GRANTED, DENIED }

enum class SysToggle { TOUCH, LOCK, CHARGING }

data class SlotState(val slot: SoundSlot, val label: String?)

/** Sistem ses ayarlarının canlı durumu (sesleri bastırabilen şeyler) */
data class SysState(
    val touchSounds: Boolean = true,
    val lockSounds: Boolean = true,
    val chargingSounds: Boolean = true,
    val ringer: String = "",
    val systemMuted: Boolean = false,
    val systemVol: Int = 0,
    val systemMax: Int = 0,
    val mediaVol: Int = 0,
    val mediaMax: Int = 0
)

/** Ses başına normalize seçimi: mode 0 = genel ayar, 1 = kapalı, 2 = özel hedef (db) */
data class SlotNorm(val mode: Int = 0, val db: Int = -30)

data class UiState(
    val root: RootState = RootState.UNKNOWN,
    val hookStatus: String? = null,
    val slots: List<SlotState> = emptyList(),
    val library: List<SystemSound> = emptyList(),
    val libraryLoading: Boolean = false,
    val busy: Boolean = false,
    val reduceMotion: Boolean = false,
    val normalize: Boolean = true,
    val targetDb: Int = -30,
    val themeMode: Int = 0,
    val dynamicColor: Boolean = true,
    val palette: Int = 0,
    val sys: SysState = SysState(),
    val report: String? = null,
    val rootInfo: RootInfo = RootInfo(),
    val debugLog: Boolean = false,
    val showIntro: Boolean = false,
    val forceLock: Boolean = false,
    val restartPrompt: RestartPrompt? = null,
    val skipRestartWarning: Boolean = false,
    val slotNorm: Map<String, SlotNorm> = emptyMap(),
    /** Alt çubuktaki sekmelerin sırası (0 Sesler, 1 Kütüphane, 2 Gelişmiş, 3 Ayarlar) */
    val tabOrder: List<Int> = listOf(0, 1, 2, 3),
    val hiddenTabs: Set<Int> = emptySet(),
    /** Alt çubuk stili: 0 = gömülü (genişleyen hap), 1 = yüzen */
    val navStyle: Int = 0,
    val noHpWarning: Boolean = false
)

/** Kullanıcıya sorulan Sistem Arayüzü yenileme önerisi */
data class RestartPrompt(val title: String, val text: String)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SoundRepository(app)
    private val prefs = app.getSharedPreferences("soundmod_ui", Context.MODE_PRIVATE)

    init {
        // Sürüm 0.5: kilit yuvaları yeniden stok mekanizmaya (Settings yolu) döndü; eski kayıtları unut
        if (prefs.getInt("schema", 0) < 2) {
            SoundCatalog.slots.filter { it.sysUiReload }.forEach { repo.forget(it) }
            prefs.edit().putInt("schema", 2).apply()
        }
    }

    private val _state = MutableStateFlow(
        UiState(
            slots = readSlots(),
            reduceMotion = prefs.getBoolean("reduce_motion", false),
            normalize = prefs.getBoolean("normalize", true),
            targetDb = prefs.getInt("target_db", -30),
            themeMode = prefs.getInt("theme_mode", 0),
            dynamicColor = prefs.getBoolean("dynamic", true),
            palette = prefs.getInt("palette", 0),
            sys = readSys(),
            showIntro = !prefs.getBoolean("intro_seen", false),
            skipRestartWarning = prefs.getBoolean("skip_restart_warning", false),
            slotNorm = readNorms(),
            tabOrder = readTabOrder(),
            hiddenTabs = readHiddenTabs(),
            navStyle = prefs.getInt("nav_style", 0)
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _player = MutableStateFlow(PlayerUi())
    val player: StateFlow<PlayerUi> = _player.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var mp: MediaPlayer? = null

    private var libraryLoaded = false
    private var watching = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private val debouncedSysRefresh = Runnable { refreshSys() }

    private fun scheduleSysRefresh() {
        uiHandler.removeCallbacks(debouncedSysRefresh)
        uiHandler.postDelayed(debouncedSysRefresh, 150)
    }

    /** Zil modu / ses seviyesi değişince durum kartı anında güncellensin (yalnızca uygulama görünürken). */
    private val sysReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scheduleSysRefresh()
        }
    }
    private val sysObserver = object : ContentObserver(uiHandler) {
        override fun onChange(selfChange: Boolean) {
            scheduleSysRefresh()
        }
    }

    /** Uygulama ön plana gelince çağrılır; dinleyiciler yalnızca görünürken açıktır. */
    fun startSysWatch() {
        if (watching) return
        watching = true
        try {
            val f = IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            }
            ContextCompat.registerReceiver(app(), sysReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
            val cr = app().contentResolver
            cr.registerContentObserver(Settings.System.getUriFor(Settings.System.SOUND_EFFECTS_ENABLED), false, sysObserver)
            cr.registerContentObserver(Settings.System.getUriFor("lockscreen_sounds_enabled"), false, sysObserver)
            cr.registerContentObserver(Settings.Global.getUriFor("charging_sounds_enabled"), false, sysObserver)
        } catch (e: Exception) {
            // dinleyici açılamazsa sekmeye girince yine yenilenir
        }
        refreshSys()
    }

    /** Uygulama arka plana geçince tüm dinleyiciler kapanır: arka planda hiçbir şey çalışmaz. */
    fun stopSysWatch() {
        if (!watching) return
        watching = false
        uiHandler.removeCallbacks(debouncedSysRefresh)
        runCatching { app().unregisterReceiver(sysReceiver) }
        runCatching { app().contentResolver.unregisterContentObserver(sysObserver) }
    }

    init {
        refreshAll()
    }

    private fun readSlots() = SoundCatalog.slots.map { SlotState(it, repo.label(it)) }
    private fun msg(s: String) { _messages.tryEmit(s) }
    private fun app() = getApplication<Application>()
    private fun readNorms(): Map<String, SlotNorm> {
        val globalDb = prefs.getInt("target_db", -30)
        return SoundCatalog.slots.associate { sl ->
            val v = prefs.getString("norm_${sl.id}", "g") ?: "g"
            sl.id to when {
                v == "g" -> SlotNorm(0, globalDb)
                v == "off" -> SlotNorm(1, globalDb)
                else -> SlotNorm(2, v.toIntOrNull() ?: globalDb)
            }
        }
    }

    /** Bir yuva için geçerli ses işleme seçenekleri: önce ses başına seçim, yoksa genel ayar */
    private fun optsFor(slot: SoundSlot): AudioConverter.Options {
        val st = _state.value
        val n = st.slotNorm[slot.id] ?: SlotNorm(0, st.targetDb)
        return when (n.mode) {
            1 -> AudioConverter.Options(false, st.targetDb)
            2 -> AudioConverter.Options(true, n.db)
            else -> AudioConverter.Options(st.normalize, st.targetDb)
        }
    }

    fun setSlotNorm(slotId: String, mode: Int, db: Int) {
        val c = db.coerceIn(-45, -10)
        prefs.edit().putString(
            "norm_$slotId",
            when (mode) { 1 -> "off"; 2 -> c.toString(); else -> "g" }
        ).apply()
        _state.update { it.copy(slotNorm = it.slotNorm + (slotId to SlotNorm(mode, c))) }
    }

    private fun readTabOrder(): List<Int> {
        val parsed = prefs.getString("tab_order", null)?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
        return if (parsed != null && parsed.sorted() == listOf(0, 1, 2, 3)) parsed else listOf(0, 1, 2, 3)
    }

    private fun readHiddenTabs(): Set<Int> {
        val parsed = prefs.getString("tab_hidden", "")!!.split(",").mapNotNull { it.trim().toIntOrNull() }
        // Ayarlar (3) hiçbir zaman gizlenemez; en az bir başka sekme görünür kalmalı
        val hidden = parsed.filter { it in 0..2 }.toSet()
        return if (hidden.size >= 3) hidden - hidden.first() else hidden
    }

    /** Sekmeyi sıradaki yeni konumuna taşır (sürükle-bırak sonrası). */
    fun moveTabTo(id: Int, newIndex: Int) {
        val order = _state.value.tabOrder.toMutableList()
        val i = order.indexOf(id)
        if (i < 0) return
        order.removeAt(i)
        order.add(newIndex.coerceIn(0, order.size), id)
        prefs.edit().putString("tab_order", order.joinToString(",")).apply()
        _state.update { it.copy(tabOrder = order) }
    }

    fun setNavStyle(v: Int) {
        prefs.edit().putInt("nav_style", v).apply()
        _state.update { it.copy(navStyle = v) }
    }

    fun setTabHidden(id: Int, hidden: Boolean) {
        if (id !in 0..2) return // Ayarlar her zaman görünür
        val cur = _state.value.hiddenTabs
        val next = if (hidden) cur + id else cur - id
        if (next.size >= 3) return // en az bir sekme (Ayarlar dışında) görünür kalmalı
        prefs.edit().putString("tab_hidden", next.joinToString(",")).apply()
        _state.update { it.copy(hiddenTabs = next) }
    }

    fun setSkipRestartWarning(v: Boolean) {
        prefs.edit().putBoolean("skip_restart_warning", v).apply()
        _state.update { it.copy(skipRestartWarning = v) }
    }

    // ---- Genel durum ----

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repo.probe()
            _state.update {
                it.copy(
                    root = if (p.granted) RootState.GRANTED else RootState.DENIED,
                    hookStatus = p.hookStatus,
                    rootInfo = p.info,
                    debugLog = p.debugLog,
                    forceLock = p.forceLock,
                    noHpWarning = p.noHpWarning,
                    slots = readSlots(),
                    sys = readSys()
                )
            }
        }
    }

    fun refreshSys() {
        _state.update { it.copy(sys = readSys()) }
    }

    private fun readSys(): SysState {
        return try {
            val cr = app().contentResolver
            val am = app().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            SysState(
                touchSounds = Settings.System.getInt(cr, Settings.System.SOUND_EFFECTS_ENABLED, 1) == 1,
                lockSounds = Settings.System.getInt(cr, "lockscreen_sounds_enabled", 1) == 1,
                chargingSounds = Settings.Global.getInt(cr, "charging_sounds_enabled", 1) == 1,
                ringer = when (am.ringerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> "Normal"
                    AudioManager.RINGER_MODE_VIBRATE -> "Titreşim"
                    else -> "Sessiz"
                },
                systemMuted = am.isStreamMute(AudioManager.STREAM_SYSTEM),
                systemVol = am.getStreamVolume(AudioManager.STREAM_SYSTEM),
                systemMax = am.getStreamMaxVolume(AudioManager.STREAM_SYSTEM),
                mediaVol = am.getStreamVolume(AudioManager.STREAM_MUSIC),
                mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            )
        } catch (e: Exception) {
            SysState()
        }
    }

    /** Kütüphane yalnızca ilk gerektiğinde (sekme/seçici açılınca) taranır. */
    fun ensureLibrary() {
        if (!libraryLoaded) loadLibrary()
    }

    fun loadLibrary() {
        libraryLoaded = true
        _state.update { it.copy(libraryLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val list = SoundScanner.scan()
            _state.update { it.copy(library = list, libraryLoading = false) }
        }
    }

    /** IO iş parçacığında: kütüphane henüz taranmadıysa şimdi tara. */
    private fun libraryNow(): List<SystemSound> {
        val cur = _state.value.library
        if (libraryLoaded && cur.isNotEmpty()) return cur
        val l = SoundScanner.scan()
        libraryLoaded = true
        _state.update { it.copy(library = l, libraryLoading = false) }
        return l
    }

    // ---- Ayarlar ----

    fun setReduceMotion(v: Boolean) {
        prefs.edit().putBoolean("reduce_motion", v).apply()
        _state.update { it.copy(reduceMotion = v) }
    }

    fun setNormalize(v: Boolean) {
        prefs.edit().putBoolean("normalize", v).apply()
        _state.update { it.copy(normalize = v) }
    }

    fun setTargetDb(v: Int) {
        val c = v.coerceIn(-45, -10)
        prefs.edit().putInt("target_db", c).apply()
        _state.update { it.copy(targetDb = c) }
    }

    fun setThemeMode(v: Int) {
        prefs.edit().putInt("theme_mode", v).apply()
        _state.update { it.copy(themeMode = v) }
    }

    fun setDynamic(v: Boolean) {
        prefs.edit().putBoolean("dynamic", v).apply()
        _state.update { it.copy(dynamicColor = v) }
    }

    fun setPalette(v: Int) {
        prefs.edit().putInt("palette", v).putBoolean("dynamic", false).apply()
        _state.update { it.copy(palette = v, dynamicColor = false) }
    }

    fun setSystemToggle(kind: SysToggle, on: Boolean) = work {
        val v = if (on) 1 else 0
        val cmd = when (kind) {
            SysToggle.TOUCH -> "settings put system sound_effects_enabled $v"
            SysToggle.LOCK -> "settings put system lockscreen_sounds_enabled $v"
            SysToggle.CHARGING -> "settings put global charging_sounds_enabled $v"
        }
        val r = Root.run(cmd)
        if (!r.ok) msg("Ayar değiştirilemedi: ${r.out}")
        _state.update { it.copy(sys = readSys()) }
    }

    // ---- Ses değiştirme ----

    fun applySystem(slot: SoundSlot, sound: SystemSound) = work {
        repo.applySystem(slot, sound, optsFor(slot)).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” → ${sound.name}", it) },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    fun applyCustom(slot: SoundSlot, uri: Uri) = work {
        val name = displayName(uri)
        repo.applyCustom(slot, uri, name, optsFor(slot)).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” → $name", it) },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    /** Seçilen sesi susturur (kısa sessiz dosya). */
    fun applySilent(slot: SoundSlot) = work {
        repo.applySilence(slot).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” susturuldu", it) },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    /** Tek bir sesi, kendi normalize seçimiyle yeniden işler. */
    fun reapplySlot(slot: SoundSlot) = work {
        repo.reprocess(slot, optsFor(slot)).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” yeniden işlendi", it) },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    fun reset(slot: SoundSlot) = work {
        repo.reset(slot).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” varsayılana döndü", null) },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    /** Seçilmiş tüm sesleri güncel normalize ayarıyla yeniden işler */
    fun reapplyAll() = work {
        var ok = 0
        var fail = 0
        var needReload = false
        var needSysUi = false
        var lastSummary = ""
        for (ss in readSlots()) {
            if (!repo.hasSource(ss.slot)) continue
            val r = repo.reprocess(ss.slot, optsFor(ss.slot))
            val info = r.getOrNull()
            if (info == null) {
                fail++
                continue
            }
            ok++
            lastSummary = info.summary()
            if (ss.slot.reload) needReload = true
            if (ss.slot.sysUiReload) needSysUi = true
        }
        if (needReload) reloadEffects()
        if (needSysUi && !_state.value.forceLock) askSysUiReload()
        _state.update { it.copy(slots = readSlots()) }
        val text = if (ok == 0 && fail == 0) {
            "Yeniden işlenecek seçili ses yok"
        } else {
            "$ok ses yeniden işlendi" + (if (fail > 0) ", $fail hata" else "") +
                (if (ok == 1 && lastSummary.isNotBlank()) " · $lastSummary" else "")
        }
        msg(text)
    }

    private fun afterChange(slot: SoundSlot, text: String, info: ApplyInfo?) {
        if (slot.reload) reloadEffects()
        _state.update { it.copy(slots = readSlots()) }
        if (slot.sysUiReload && !_state.value.forceLock) askSysUiReload()
        val level = info?.summary()?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: ""
        msg(text + level)
    }

    private fun work(block: () -> Unit) {
        if (_state.value.root != RootState.GRANTED) {
            msg("Önce root izni gerekli (Ayarlar sekmesi)")
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun displayName(uri: Uri): String = try {
        app().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment ?: "ses"
    } catch (e: Exception) {
        "ses"
    }

    /** system_server'daki SoundEffectsHelper'ı yeniden yükletir; yeni yollar getResourceFilePath'ten geçer. */
    private fun reloadEffects() {
        try {
            val am = app().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // SDK saplarında görünürlüğü sürüme göre değiştiği için yansıma ile çağrılır
            AudioManager::class.java.getMethod("unloadSoundEffects").invoke(am)
            AudioManager::class.java.getMethod("loadSoundEffects").invoke(am)
        } catch (e: Exception) {
            msg("Sesler yeniden yüklenemedi, değişiklik için yeniden başlatmak gerekebilir")
        }
    }

    // ---- Çalma (çal / duraklat / devam) ----

    fun playSlot(slot: SoundSlot) {
        val key = "slot:${slot.id}"
        viewModelScope.launch(Dispatchers.IO) {
            if (_player.value.key == key && mp != null) {
                withContext(Dispatchers.Main) { toggle(key, "") }
                return@launch
            }
            val path = repo.previewPath(slot) ?: repo.stockPath(slot, libraryNow())
            if (path == null) {
                msg("Bu ses için önizleme dosyası bulunamadı")
                return@launch
            }
            withContext(Dispatchers.Main) { toggle(key, path) }
        }
    }

    fun playLibrary(path: String) {
        viewModelScope.launch(Dispatchers.Main) { toggle("lib:$path", path) }
    }

    /** Aynı anahtar çalıyorsa duraklatır, duraklatılmışsa devam ettirir; farklıysa yenisini başlatır. */
    private fun toggle(key: String, path: String) {
        val cur = _player.value
        val p = mp
        if (cur.key == key && p != null) {
            if (cur.playing) {
                val pos = runCatching { p.currentPosition }.getOrDefault(cur.anchorPosMs)
                runCatching { p.pause() }
                _player.value = cur.copy(playing = false, anchorPosMs = pos)
            } else {
                runCatching { p.start() }
                _player.value = cur.copy(playing = true, anchorTimeMs = SystemClock.elapsedRealtime())
            }
            return
        }
        release()
        try {
            val np = MediaPlayer()
            np.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            np.setDataSource(path)
            np.setOnPreparedListener {
                it.start()
                _player.value = PlayerUi(
                    key = key,
                    playing = true,
                    durationMs = runCatching { it.duration }.getOrDefault(0),
                    anchorPosMs = 0,
                    anchorTimeMs = SystemClock.elapsedRealtime()
                )
            }
            np.setOnCompletionListener { finishPlayback(key) }
            np.setOnErrorListener { _, _, _ ->
                release()
                msg("Çalınamadı")
                true
            }
            mp = np
            playStartedAt = SystemClock.elapsedRealtime()
            _player.value = PlayerUi(key = key, playing = true, anchorTimeMs = playStartedAt)
            np.prepareAsync()
        } catch (e: Exception) {
            release()
            msg("Çalınamadı: ${e.message}")
        }
    }

    private var hideJob: Job? = null
    private var playStartedAt = 0L

    private fun release() {
        hideJob?.cancel()
        mp?.let { runCatching { it.release() } }
        mp = null
        _player.value = PlayerUi()
    }

    /**
     * Çalma bitince oynatıcıyı bırakır ama ilerleme çubuğu dolu ve dalgalı hâlde bir süre görünür kalır;
     * böylece çok kısa seslerde de çubuk ve dalga efekti fark edilir.
     */
    private fun finishPlayback(key: String) {
        val cur = _player.value
        mp?.let { runCatching { it.release() } }
        mp = null
        if (cur.key != key) return
        _player.value = cur.copy(playing = false, anchorPosMs = cur.durationMs)
        hideJob?.cancel()
        // Uzun seslerde çubuk bitişte hemen kaybolur; çok kısa seslerde efekt görünsün diye çalmanın başından
        // itibaren toplam en az ~0.5 sn görünür kalır.
        val hold = maxOf(100L, 500L - (SystemClock.elapsedRealtime() - playStartedAt))
        hideJob = viewModelScope.launch(Dispatchers.Main) {
            delay(hold)
            if (_player.value.key == key && !_player.value.playing) _player.value = PlayerUi()
        }
    }

    /** Sistem ses efektini sistem yoluyla çalar (dokunma sesleri açıksa duyulur). */
    fun testEffect(fx: Int) {
        try {
            val am = app().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.playSoundEffect(fx)
            if (!_state.value.sys.touchSounds) msg("Dokunma sesleri kapalı, Ayarlar sekmesinden açabilirsin")
        } catch (e: Exception) {
            msg("Test başarısız: ${e.message}")
        }
    }

    // ---- Yardımcı modül / Sistem Arayüzü / rapor ----

    fun restartSysUiNow() = work {
        repo.restartSystemUi()
        msg("Sistem Arayüzü yenilendi")
    }

    fun buildReport() = work {
        val keys = "lock_sound unlock_sound trusted_sound low_battery_sound charging_started_sound " +
            "wireless_charging_started_sound desk_dock_sound car_dock_sound"
        val cmd = """
            echo "== build =="; getprop ro.build.display.id
            echo "== depo =="; ls -laZ ${SoundPaths.BASE} ${SoundPaths.OVERRIDE_DIR} ${SoundPaths.FILES_DIR} 2>&1
            echo "== ayarlar =="
            for k in $keys; do echo "${'$'}k=${'$'}(settings get global ${'$'}k)"; done
            echo "lockscreen_sounds_enabled=${'$'}(settings get system lockscreen_sounds_enabled)"
            echo "sound_effects_enabled=${'$'}(settings get system sound_effects_enabled)"
            echo "charging_sounds_enabled=${'$'}(settings get global charging_sounds_enabled)"
            echo "== varsayilan dosyalar =="
            for k in $keys; do v=${'$'}(settings get global ${'$'}k); echo "${'$'}k -> ${'$'}(ls -lZ ${'$'}v 2>&1)"; done
            echo "lock/unlock/trusted stok dosyalar:"; ls -lZ /product/media/audio/ui/ 2>&1 | grep -i -E "lock|trusted"
            echo "== hook durumu =="; cat ${SoundPaths.STATUS_SYSTEM} 2>&1; echo
            settings get global ${SoundPaths.SYSUI_STATUS_KEY}
            echo "kilit sesi zorla cal: ${'$'}([ -e ${SoundPaths.FORCE_FLAG} ] && echo ACIK || echo kapali)"
            echo "ayrintili olay kaydi: ${'$'}([ -e ${SoundPaths.DEBUG_FLAG} ] && echo ACIK || echo kapali)"
            echo "son SystemUI olayi:"; settings get global ${SoundPaths.SYSUI_LAST_KEY}
            echo "== sistem arayuzu =="; pidof com.android.systemui
            echo "== selinux (son reddedilenler) =="
            logcat -d -b all 2>/dev/null | grep -i "avc:.*denied" | grep -i -E "scontext=u:r:(systemui_app|system_server|system_app|platform_app)" | tail -15
            echo "== bitti =="
        """.trimIndent()
        val r = Root.run(cmd)
        _state.update { it.copy(report = r.out.ifBlank { "Rapor boş (kod ${r.code})" }) }
    }

    private fun askSysUiReload() {
        _state.update {
            it.copy(
                restartPrompt = RestartPrompt(
                    "Sistem Arayüzü yenilensin mi?",
                    "Kilitleme, kilit açma ve güvenilir cihaz sesleri Sistem Arayüzü açılırken bir kez yüklenir. " +
                        "Yeni sesin hemen geçerli olması için Sistem Arayüzü yeniden başlatılabilir. Ekran birkaç saniye " +
                        "yeniden çizilir, açık uygulamalar kapanmaz. İstersen sonra da yenileyebilirsin " +
                        "(Ayarlar → Gelişmiş)."
                )
            )
        }
    }

    /** Kilit sesleri "zorla çal" modu: medya ses seviyesiyle çal, sessiz/titreşim kısıtlamalarını aş. */
    fun setForceLock(on: Boolean) = work {
        val r = repo.setForceLock(on)
        if (!r.ok) {
            msg("Ayar değiştirilemedi: ${r.out}")
        } else {
            _state.update { it.copy(forceLock = on) }
            if (on) {
                _state.update {
                    it.copy(
                        restartPrompt = RestartPrompt(
                            "Zorla çalma için yenile",
                            "Zorla çalma, Sistem Arayüzü açılırken kurulur. Etkinleşmesi için Sistem Arayüzü şimdi " +
                                "yeniden başlatılsın mı? Ekran birkaç saniye yeniden çizilir, açık uygulamalar kapanmaz."
                        )
                    )
                }
            } else {
                msg("Zorla çalma kapatıldı, stok kilit sesi mekanizması kullanılacak")
            }
        }
    }

    /** Kulaklık ses uyarısını kapat/aç (açılış betiği; kapatma sonraki açılışta etkili olur). */
    fun setNoHpWarning(on: Boolean) = work {
        val r = repo.setNoHpWarning(on)
        if (r.ok) {
            _state.update { it.copy(noHpWarning = on) }
            msg(if (on) "Kulaklık ses uyarısı kapatıldı" else "Betik silindi, uyarı bir sonraki açılışta geri gelir")
        } else {
            msg("Ayar değiştirilemedi: ${r.out}")
        }
    }

    fun confirmRestartPrompt() {
        _state.update { it.copy(restartPrompt = null) }
        restartSysUiNow()
    }

    fun dismissRestartPrompt() {
        _state.update { it.copy(restartPrompt = null) }
    }

    fun dismissIntro() {
        prefs.edit().putBoolean("intro_seen", true).apply()
        _state.update { it.copy(showIntro = false) }
    }

    fun showIntroAgain() {
        _state.update { it.copy(showIntro = true) }
    }

    fun setDebugLog(on: Boolean) = work {
        val r = repo.setDebug(on)
        if (r.ok) _state.update { it.copy(debugLog = on) } else msg("Ayar değiştirilemedi: ${r.out}")
    }

    fun clearReport() {
        _state.update { it.copy(report = null) }
    }

    override fun onCleared() {
        release()
        stopSysWatch()
        super.onCleared()
    }
}
