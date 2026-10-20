package dev.soundmod.nothing.hook

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import dev.soundmod.nothing.BuildConfig
import dev.soundmod.nothing.core.SoundPaths
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.io.File

/**
 * libxposed API 101 giriş sınıfı.
 *
 *  - system_server ("system" kapsamı): SoundEffectsHelper.getResourceFilePath çıktısını,
 *    /data/system/soundmod/override/<stokAdı>.* varsa onunla değiştirir (klavye/dokunma sesleri).
 *  - SystemUI:
 *      · ScreenshotSoundProviderImpl.getScreenshotSound → özel dosyayla MediaPlayer
 *      · KeyguardViewMediator.playSounds / playTrustedSound → kilit, kilit açma, güvenilir cihaz
 *        sesleri; özel dosya varsa onu biz çalarız, yoksa stok davranış sürer.
 *
 * Her hook ayrı try/catch içinde; hata olursa stok davranışa düşer.
 */
class SoundModule : XposedModule() {

    @Volatile private var sysUiHooked = false

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        val parts = ArrayList<String>()
        try {
            hookSoundEffects(param.classLoader)
            parts += "SoundEffectsHelper OK"
        } catch (t: Throwable) {
            parts += "SoundEffectsHelper HATA: ${t.javaClass.simpleName}: ${t.message}"
            log(Log.ERROR, TAG, "SoundEffectsHelper hook failed", t)
        }
        writeStatus(SoundPaths.STATUS_SYSTEM, "system_server: ${parts.joinToString(", ")}")
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != "com.android.systemui" || sysUiHooked) return
        sysUiHooked = true
        val parts = ArrayList<String>()
        try {
            hookScreenshot(param.classLoader)
            parts += "Screenshot OK"
        } catch (t: Throwable) {
            parts += "Screenshot HATA: ${t.javaClass.simpleName}: ${t.message}"
            log(Log.ERROR, TAG, "Screenshot hook failed", t)
        }
        try {
            parts += "Keyguard ${hookKeyguard(param.classLoader)}"
        } catch (t: Throwable) {
            parts += "Keyguard HATA: ${t.javaClass.simpleName}: ${t.message}"
            log(Log.ERROR, TAG, "Keyguard hook failed", t)
        }
        putGlobal(SoundPaths.SYSUI_STATUS_KEY, "SystemUI: ${parts.joinToString(", ")} [$frameworkName $frameworkVersion, API $apiVersion]")
    }

    // ---------------------------------------------------------------------------------------
    // system_server: klavye ve dokunma sesleri
    // ---------------------------------------------------------------------------------------

    private fun hookSoundEffects(cl: ClassLoader) {
        val clazz = Class.forName("com.android.server.audio.SoundEffectsHelper", false, cl)
        val method = clazz.declaredMethods.firstOrNull {
            it.name == "getResourceFilePath" && it.parameterTypes.size == 1
        } ?: error("getResourceFilePath bulunamadı")

        // Satır içine alınmış (inline) çağrıları engellemek için sınıftaki diğer metotları deopt et
        clazz.declaredMethods.forEach { m ->
            if (m != method) runCatching { deoptimize(m) }
        }

        hook(method).intercept { chain ->
            val orig = chain.proceed()
            try {
                val name = fileNameOf(chain.getArg(0)) ?: (orig as? String)?.substringAfterLast('/')
                val ov = name?.let { findOverride(it.substringBeforeLast('.')) }
                ov ?: orig
            } catch (t: Throwable) {
                orig
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // SystemUI: ekran görüntüsü
    // ---------------------------------------------------------------------------------------

    private fun hookScreenshot(cl: ClassLoader) {
        val clazz = Class.forName("com.android.systemui.screenshot.ScreenshotSoundProviderImpl", false, cl)
        val method = clazz.declaredMethods.firstOrNull {
            it.name == "getScreenshotSound" && it.parameterTypes.isEmpty()
        } ?: error("getScreenshotSound bulunamadı")

        hook(method).intercept { chain ->
            val orig = chain.proceed()
            val path = findOverride("screenshot")
            if (path == null || orig !is MediaPlayer) {
                orig
            } else {
                try {
                    val mp = MediaPlayer()
                    mp.setAudioAttributes(sonification())
                    mp.setDataSource(path)
                    mp.prepare()
                    orig.release()
                    mp
                } catch (t: Throwable) {
                    orig
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // SystemUI: kilit, kilit açma, güvenilir cihaz  (YALNIZCA "zorla çal" modu açıkken)
    // ---------------------------------------------------------------------------------------

    /**
     * Varsayılan: hiçbir şeye dokunulmaz; stok mekanizma Settings.Global yolundaki sesi (lock_sound vb.)
     * kendi kuralları ve ses akışıyla çalar.
     *
     * Kullanıcı "zorla çal" modunu açtıysa (bayrak dosyası) SystemUI açılırken burada keyguard çağrıları
     * yakalanır: özel ses SoundPool ile medya ses seviyesinden çalınır, böylece sessiz/titreşim modu ve
     * sistem sesi 0 iken de duyulur. "Kilit ekranı sesleri" ayarı kapalıysa stok davranış korunur.
     */
    private fun hookKeyguard(cl: ClassLoader): String {
        if (!File(SoundPaths.FORCE_FLAG).exists()) return "stok (zorla çalma kapalı)"

        val clazz = Class.forName("com.android.systemui.keyguard.KeyguardViewMediator", false, cl)
        val boolType = java.lang.Boolean.TYPE
        val playSounds = clazz.declaredMethods.firstOrNull {
            it.name == "playSounds" && it.parameterTypes.size == 1 && it.parameterTypes[0] == boolType
        }
        val playTrusted = clazz.declaredMethods.firstOrNull {
            it.name == "playTrustedSound" && it.parameterTypes.isEmpty()
        }
        if (playSounds == null && playTrusted == null) error("playSounds/playTrustedSound bulunamadı")

        // Küçük özel metotlar çağıranlarına satır içi derlenmiş olabilir. Sistemi az kurcalamak için yalnızca
        // ses akışındaki çağıranları (çıkış bitişi, uykuya geçiş, *Sound*, erişim köprüleri) deopt ederiz;
        // hiçbiri bulunamazsa (farklı ROM) sınıftaki tüm metotlara düşeriz.
        val others = clazz.declaredMethods.filter { it != playSounds && it != playTrusted }
        val callers = others.filter {
            it.name.contains("ExitFinished") || it.name.contains("GoingToSleep") || it.name.contains("Sound")
        }
        (if (callers.isEmpty()) others else callers).forEach { runCatching { deoptimize(it) } }

        val hooked = ArrayList<String>()
        if (playSounds != null) {
            hook(playSounds).intercept { chain ->
                val locked = (chain.getArg(0) as? Boolean) ?: true
                val handled = if (locked) forcePlay("lock_sound", "lock") else forcePlay("unlock_sound", "unlock")
                if (handled) null else chain.proceed()
            }
            hooked += "playSounds"
        }
        if (playTrusted != null) {
            hook(playTrusted).intercept { chain ->
                if (forcePlay("trusted_sound", "trusted")) null else chain.proceed()
            }
            hooked += "playTrustedSound"
        }

        // SoundPool, uygulama bağlamı (kimlik/paket bilgisi) hazır olmadan kurulursa ses çıkmayabilir;
        // bu yüzden havuz erken değil, Sistem Arayüzü açıldıktan sonra kurulur ve sesler önden yüklenir.
        preloadLater(0)
        return "zorla çalma: ${hooked.joinToString("+")}"
    }

    private fun preloadLater(tries: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentApp() != null) {
                for (key in listOf("lock_sound", "unlock_sound", "trusted_sound")) {
                    FilesIdx.find(key)?.let { ForcePlayer.preload(it) }
                }
            } else if (tries < 5) {
                preloadLater(tries + 1)
            }
        }, 4000)
    }

    /**
     * @return true = çağrıyı biz karşıladık ve ses gerçekten çalmaya başladı (stok çalma atlanır);
     *         false = stok davranış sürsün. Biz çalamıyorsak (henüz yüklenmedi, hata, medya sesi 0...)
     *         sessiz kalmamak için her zaman stok mekanizmaya bırakırız.
     */
    private fun forcePlay(key: String, kind: String): Boolean {
        return try {
            if (!File(SoundPaths.FORCE_FLAG).exists()) return false // mod kapatıldı: stok davranış
            val path = FilesIdx.find(key) ?: return false
            val app = currentApp() ?: return false
            if (Settings.System.getInt(app.contentResolver, "lockscreen_sounds_enabled", 1) == 0) {
                return false // kullanıcı kilit seslerini kapattı: stok davranış (çalmaz)
            }
            val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return false // medya sesi kapalı: duyulmaz
            val ok = ForcePlayer.play(path)
            if (BuildConfig.VERBOSE_LOG && File(SoundPaths.DEBUG_FLAG).exists()) {
                putGlobal(
                    SoundPaths.SYSUI_LAST_KEY,
                    "$kind: ${if (ok) "SoundPool çalındı" else "stok davranışa düşüldü"} [${ForcePlayer.lastInfo}] (${System.currentTimeMillis()})"
                )
            }
            ok
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "forcePlay failed", t)
            false
        }
    }

    private fun sonification(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    // ---------------------------------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------------------------------

    @Volatile private var nameField: java.lang.reflect.Field? = null

    private fun fileNameOf(resource: Any?): String? {
        if (resource == null) return null
        return try {
            var f = nameField
            if (f == null) {
                f = resource.javaClass.getDeclaredField("mFileName")
                f.isAccessible = true
                nameField = f
            }
            f!!.get(resource) as? String
        } catch (t: Throwable) {
            null
        }
    }

    private fun findOverride(base: String): String? = OverrideIdx.find(base)

    private fun currentApp(): Application? = try {
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? Application
    } catch (t: Throwable) {
        null
    }

    /**
     * SystemUI /data/system'e yazamayabilir; durum ve olaylar Settings.Global'a yazılır (SystemUI'nin
     * yetkisi var). Uygulama bağlamı henüz oluşmamış olabileceğinden hazır olana kadar bekler.
     */
    private fun putGlobal(key: String, value: String) {
        Thread {
            repeat(120) {
                try {
                    val app = currentApp()
                    if (app != null) {
                        Settings.Global.putString(app.contentResolver, key, value)
                        return@Thread
                    }
                } catch (t: Throwable) {
                    // tekrar dene
                }
                Thread.sleep(500)
            }
        }.start()
    }

    // Durum dosyasını SoundMod uygulaması (başka uid) okur; bu yüzden herkese okunabilir yapılır
    @Suppress("SetWorldReadable")
    private fun writeStatus(path: String, text: String) {
        try {
            val f = File(path)
            f.parentFile?.mkdirs()
            f.writeText("$text [$frameworkName $frameworkVersion, API $apiVersion]")
            f.setReadable(true, false)
        } catch (t: Throwable) {
            // durum dosyası yazılamazsa sorun değil
        }
    }

    private companion object {
        const val TAG = "SoundMod"
    }
}

/**
 * Bir dizinin içeriğini bellekte tutar. Her sorguda yalnızca dizinin değişim zamanına bakılır (tek stat);
 * içerik değiştiyse dizin bir kez okunur. Böylece olaylarda uzantı başına dosya sorgusu yapılmaz.
 */
private class DirIndex(path: String) {
    private val dir = File(path)
    @Volatile private var stamp = Long.MIN_VALUE
    @Volatile private var map: Map<String, String> = emptyMap()

    fun find(base: String): String? {
        val m = dir.lastModified()
        if (m != stamp) {
            rebuild()
            stamp = m
        }
        return map[base]
    }

    private fun rebuild() {
        val files = dir.listFiles()
        if (files == null) {
            map = emptyMap()
            return
        }
        val result = HashMap<String, String>()
        for (ext in SoundPaths.EXTENSIONS) { // uzantı öncelik sırası
            for (f in files) {
                if (f.extension.lowercase() == ext && f.length() > 0) {
                    result.putIfAbsent(f.nameWithoutExtension, f.absolutePath)
                }
            }
        }
        map = result
    }
}

private val OverrideIdx = DirIndex(SoundPaths.OVERRIDE_DIR)
private val FilesIdx = DirIndex(SoundPaths.FILES_DIR)

/**
 * "Zorla çal" modu için tek bir SoundPool (stok kilit sesinin de kullandığı motor), medya ses akışı.
 * Sesler yüklenip bellekte tutulur; olayda anında çalar. Dosya değişirse yeniden yüklenir.
 */
private object ForcePlayer {
    private class Entry(val id: Int, val stamp: Long, var loaded: Boolean, var status: Int = -1)

    private var pool: SoundPool? = null
    private val entries = HashMap<String, Entry>()

    /** Tanılama: son yükleme/çalma sonucu */
    @Volatile var lastInfo: String = "-"

    private fun ensurePool(): SoundPool {
        pool?.let { return it }
        val p = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        p.setOnLoadCompleteListener { _, id, status ->
            synchronized(this) {
                val e = entries.values.firstOrNull { it.id == id }
                if (e != null) {
                    e.status = status
                    e.loaded = status == 0
                }
                lastInfo = "yükleme=$status"
            }
        }
        pool = p
        return p
    }

    @Synchronized
    fun preload(path: String) {
        ensureLoaded(path)
    }

    /** Yüklü değilse yükletir ve false döner (bu olayda stok çalsın, sonrakinde biz çalarız). */
    @Synchronized
    fun play(path: String): Boolean {
        val e = ensureLoaded(path) ?: return false
        if (!e.loaded) return false
        val stream = ensurePool().play(e.id, 1f, 1f, 1, 0, 1f)
        lastInfo = "akış=$stream"
        return stream != 0
    }

    private fun ensureLoaded(path: String): Entry? {
        val p = ensurePool()
        val stamp = File(path).lastModified()
        val cur = entries[path]
        if (cur != null && cur.stamp == stamp) return cur
        if (cur != null) p.unload(cur.id)
        val id = p.load(path, 1)
        if (id == 0) return null
        val e = Entry(id, stamp, false)
        entries[path] = e
        return e
    }
}