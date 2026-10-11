package dev.soundmod.nothing.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.soundmod.nothing.core.AudioConverter
import dev.soundmod.nothing.core.ModuleInstaller
import dev.soundmod.nothing.core.Root
import dev.soundmod.nothing.core.SoundCatalog
import dev.soundmod.nothing.core.SoundRepository
import dev.soundmod.nothing.core.SoundScanner
import dev.soundmod.nothing.core.SoundSlot
import dev.soundmod.nothing.core.SystemSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RootState { UNKNOWN, GRANTED, DENIED }

data class SlotState(val slot: SoundSlot, val label: String?)

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
    val targetDb: Int = -24,
    val note: String = ""
)

object Notes {
    private val list = listOf(
        "Telefonun sesi senin elinde 🎧",
        "Her tuşun bir sesi var, hadi kişiselleştir ✨",
        "Küçük sesler, büyük fark 🔊",
        "Sessiz kalan ayrıntıları konuştur 😌",
        "Bugün hangi sesi değiştiriyoruz? 🎶"
    )
    fun random(): String = list.random()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SoundRepository(app)
    private val prefs = app.getSharedPreferences("soundmod_ui", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        UiState(
            slots = readSlots(),
            reduceMotion = prefs.getBoolean("reduce_motion", false),
            normalize = prefs.getBoolean("normalize", true),
            targetDb = prefs.getInt("target_db", -24),
            note = Notes.random()
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var player: MediaPlayer? = null

    init {
        refreshAll()
        loadLibrary()
    }

    private fun readSlots() = SoundCatalog.slots.map { SlotState(it, repo.label(it)) }
    private fun msg(s: String) { _messages.tryEmit(s) }
    private fun app() = getApplication<Application>()

    // ---- Genel durum ----

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val granted = Root.available()
            if (granted) repo.ensureStore()
            _state.update {
                it.copy(
                    root = if (granted) RootState.GRANTED else RootState.DENIED,
                    hookStatus = if (granted) repo.readStatus() else null,
                    moduleInstalled = if (granted) ModuleInstaller.isInstalled() else false,
                    slots = readSlots()
                )
            }
        }
    }

    fun loadLibrary() {
        _state.update { it.copy(libraryLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val list = SoundScanner.scan()
            _state.update { it.copy(library = list, libraryLoading = false) }
        }
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

    // ---- Ses değiştirme ----

    fun applySystem(slot: SoundSlot, sound: SystemSound) = work("Uygulanıyor…") {
        repo.applySystem(slot, sound).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” için ${sound.name} seçildi") },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    fun applyCustom(slot: SoundSlot, uri: Uri) = work("Dönüştürülüyor…") {
        val name = displayName(uri)
        val s = _state.value
        val opt = AudioConverter.Options(s.normalize, s.targetDb)
        repo.applyCustom(slot, uri, name, opt).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” için $name uygulandı") },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    fun reset(slot: SoundSlot) = work("Sıfırlanıyor…") {
        repo.reset(slot).fold(
            onSuccess = { afterChange(slot, "“${slot.title}” varsayılana döndü") },
            onFailure = { msg("Hata: ${it.message}") }
        )
    }

    private fun afterChange(slot: SoundSlot, ok: String) {
        if (slot.reload) reloadEffects()
        _state.update { it.copy(slots = readSlots()) }
        msg(ok)
    }

    private fun work(@Suppress("UNUSED_PARAMETER") what: String, block: () -> Unit) {
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

    // ---- Önizleme / test ----

    fun preview(path: String) {
        stopPlayer()
        try {
            val mp = MediaPlayer()
            mp.setDataSource(path)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { it.release(); if (player === it) player = null }
            mp.setOnErrorListener { p, _, _ -> p.release(); if (player === p) player = null; true }
            mp.prepareAsync()
            player = mp
        } catch (e: Exception) {
            msg("Çalınamadı: ${e.message}")
        }
    }

    fun testEffect(fx: Int) {
        try {
            val am = app().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.playSoundEffect(fx)
        } catch (e: Exception) {
            msg("Test başarısız: ${e.message}")
        }
    }

    private fun stopPlayer() {
        player?.let { runCatching { it.release() } }
        player = null
    }

    // ---- Yardımcı modül ----

    fun installModule() = work("Modül kuruluyor…") {
        val (ok, out) = ModuleInstaller.install(app())
        if (ok) {
            msg("Modül kuruldu. Etkinleşmesi için cihazı yeniden başlat.")
            _state.update { it.copy(moduleInstalled = ModuleInstaller.isInstalled()) }
        } else {
            msg("Otomatik kurulamadı. Kök yöneticisinden /Download/SoundMod-SELinux.zip dosyasını elle kur. ${out.take(120)}")
        }
    }

    fun reboot() {
        viewModelScope.launch(Dispatchers.IO) { ModuleInstaller.reboot() }
    }

    override fun onCleared() {
        stopPlayer()
        super.onCleared()
    }
}
