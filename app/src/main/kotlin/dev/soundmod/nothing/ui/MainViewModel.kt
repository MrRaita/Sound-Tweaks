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
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.soundmod.nothing.core.ApplyInfo
import dev.soundmod.nothing.core.AudioConverter
import dev.soundmod.nothing.core.ModuleInstaller
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
    val systemMax: Int = 0
)

data class UiState(
    val root: RootState = RootState.UNKNOWN,
    val hookStatus: String? = null,
    val moduleInstalled: Boolean = false,
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
    val showIntro: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SoundRepository(app)
    private val prefs = app.getSharedPreferences("soundmod_ui", Context.MODE_PRIVATE)

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
            showIntro = !prefs.getBoolean("intro_seen", false)
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _player = MutableStateFlow(PlayerUi())
    val player: StateFlow<PlayerUi> = _player.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var mp: MediaPlayer? = null
    private var ticker: Job? = null

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
    private fun opts() = _state.value.let { AudioConverter.Options(it.normalize, it.targetDb) }

    // ---- Genel durum ----

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repo.probe()
            _state.update {
                it.copy(
                    root = if (p.granted) RootState.GRANTED else RootState.DENIED,
                    hookStatus = p.hookStatus,
                    moduleInstalled = p.moduleInstalled,
                    rootInfo = p.info,
                    debugLog = p.debugLog,
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
                systemMax = am.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
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
        repo.applySystem(slot, sound, opts()).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” → ${sound.name}", it) },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    fun applyCustom(slot: SoundSlot, uri: Uri) = work {
        val name = displayName(uri)
        repo.applyCustom(slot, uri, name, opts()).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” → $name", it) },
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
        var last: ApplyInfo? = null
        for (ss in readSlots()) {
            if (!repo.hasSource(ss.slot)) continue
            repo.reprocess(ss.slot, opts()).fold(
                onSuccess = {
                    ok++
                    last = it
                    if (ss.slot.reload) needReload = true
                },
                onFailure = { fail++ }
            )
        }
        if (needReload) reloadEffects()
        _state.update { it.copy(slots = readSlots()) }
        msg(
            when {
                ok == 0 && fail == 0 -> "Yeniden işlenecek seçili ses yok"
                else -> "$ok ses yeniden işlendi" + (if (fail > 0) ", $fail hata" else "") +
                    (last?.summary()?.let { if (ok == 1) " · $it" else "" } ?: "")
            }
        )
    }

    private fun afterChange(slot: SoundSlot, text: String, info: ApplyInfo?) {
        if (slot.reload) reloadEffects()
        _state.update { it.copy(slots = readSlots()) }
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
                runCatching { p.pause() }
                ticker?.cancel()
                _player.value = cur.copy(playing = false)
            } else {
                runCatching { p.start() }
                _player.value = cur.copy(playing = true)
                startTicker()
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
                _player.value = PlayerUi(key, true, 0f, 0, runCatching { it.duration }.getOrDefault(0))
                startTicker()
            }
            np.setOnCompletionListener { release() }
            np.setOnErrorListener { _, _, _ ->
                release()
                msg("Çalınamadı")
                true
            }
            mp = np
            _player.value = PlayerUi(key, true, 0f)
            np.prepareAsync()
        } catch (e: Exception) {
            release()
            msg("Çalınamadı: ${e.message}")
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                val p = mp ?: break
                val d = runCatching { p.duration }.getOrDefault(0)
                val pos = runCatching { p.currentPosition }.getOrDefault(0)
                val frac = if (d > 0) (pos.toFloat() / d).coerceIn(0f, 1f) else 0f
                _player.update { it.copy(progress = frac, positionMs = pos, durationMs = d) }
                delay(50)
            }
        }
    }

    private fun release() {
        ticker?.cancel()
        mp?.let { runCatching { it.release() } }
        mp = null
        _player.value = PlayerUi()
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

    fun installModule() = work {
        val (ok, out) = ModuleInstaller.install(app())
        if (ok) {
            msg("Modül kuruldu. Etkinleşmesi için cihazı yeniden başlat.")
            _state.update { it.copy(moduleInstalled = ModuleInstaller.isInstalled()) }
        } else {
            msg("Otomatik kurulamadı. Kök yöneticisinden /Download/SoundMod-SELinux.zip dosyasını elle kur. ${out.take(120)}")
        }
    }

    fun restartSysUiNow() = work {
        repo.restartSystemUi()
        msg("Sistem Arayüzü yenilendi")
    }

    fun reboot() {
        viewModelScope.launch(Dispatchers.IO) { ModuleInstaller.reboot() }
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
