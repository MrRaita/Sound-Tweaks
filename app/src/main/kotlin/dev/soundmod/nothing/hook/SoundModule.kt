package dev.soundmod.nothing.hook

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.provider.Settings
import android.util.Log
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
        val status = StringBuilder("system_server: ")
        try {
            hookSoundEffects(param.classLoader)
            status.append("SoundEffectsHelper OK")
        } catch (t: Throwable) {
            status.append("SoundEffectsHelper HATA: ${t.javaClass.simpleName}: ${t.message}")
            log(Log.ERROR, TAG, "SoundEffectsHelper hook failed", t)
        }
        writeStatus(SoundPaths.STATUS_SYSTEM, status.toString())
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
            parts += "Keyguard OK (${hookKeyguard(param.classLoader)})"
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
    // SystemUI: kilit, kilit açma, güvenilir cihaz
    // ---------------------------------------------------------------------------------------

    /**
     * Stok akış: ekran kapanırken playSounds(true), kilit açılınca playSounds(false), güvenilir cihazda
     * playTrustedSound(). Özel dosya varsa orijinal çağrı atlanır ve ses biz çalarız; böylece Settings
     * yoluna, SoundPool'un açılışta bir kez yüklenmesine ya da eksik stok dosyaya bağımlı kalmayız.
     */
    private fun hookKeyguard(cl: ClassLoader): String {
        val clazz = Class.forName("com.android.systemui.keyguard.KeyguardViewMediator", false, cl)
        val boolType = java.lang.Boolean.TYPE
        val playSounds = clazz.declaredMethods.firstOrNull {
            it.name == "playSounds" && it.parameterTypes.size == 1 && it.parameterTypes[0] == boolType
        }
        val playTrusted = clazz.declaredMethods.firstOrNull {
            it.name == "playTrustedSound" && it.parameterTypes.isEmpty()
        }
        if (playSounds == null && playTrusted == null) error("playSounds/playTrustedSound bulunamadı")

        // Küçük özel metotlar çağıranlarına satır içi derlenmiş olabilir; çağıranları deopt et
        clazz.declaredMethods.forEach { m ->
            if (m != playSounds && m != playTrusted) runCatching { deoptimize(m) }
        }

        val hooked = ArrayList<String>()
        if (playSounds != null) {
            hook(playSounds).intercept { chain ->
                val locked = (chain.getArg(0) as? Boolean) ?: true
                val kind = if (locked) "lock" else "unlock"
                val path = findOverride(kind)
                if (path != null && playOwn(path, kind)) null else chain.proceed()
            }
            hooked += "playSounds"
        }
        if (playTrusted != null) {
            hook(playTrusted).intercept { chain ->
                val path = findOverride("trusted")
                if (path != null && playOwn(path, "trusted")) null else chain.proceed()
            }
            hooked += "playTrustedSound"
        }
        return hooked.joinToString("+")
    }

    /**
     * Özel sesi çalar. Kilit ekranı sesleri sistem ayarında kapalıysa stok davranışı korur:
     * hiçbir şey çalmaz (orijinali de atlar). @return true = çağrı biz tarafından karşılandı
     */
    private fun playOwn(path: String, kind: String): Boolean {
        return try {
            val app = currentApp()
            val enabled = app == null ||
                Settings.System.getInt(app.contentResolver, "lockscreen_sounds_enabled", 1) != 0
            if (enabled) {
                val mp = MediaPlayer()
                mp.setAudioAttributes(sonification())
                mp.setDataSource(path)
                mp.setOnPreparedListener { it.start() }
                mp.setOnCompletionListener { it.release() }
                mp.setOnErrorListener { p, _, _ ->
                    p.release()
                    true
                }
                mp.prepareAsync()
            }
            putGlobal(SoundPaths.SYSUI_LAST_KEY, "$kind: ${if (enabled) "özel ses çalındı" else "kilit sesleri kapalı"} (${System.currentTimeMillis()})")
            true
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "playOwn failed", t)
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

    private fun fileNameOf(resource: Any?): String? {
        if (resource == null) return null
        return try {
            val f = resource.javaClass.getDeclaredField("mFileName")
            f.isAccessible = true
            f.get(resource) as? String
        } catch (t: Throwable) {
            null
        }
    }

    private fun findOverride(base: String): String? {
        for (ext in SoundPaths.EXTENSIONS) {
            val f = File(SoundPaths.OVERRIDE_DIR, "$base.$ext")
            if (f.isFile && f.length() > 0 && f.canRead()) return f.absolutePath
        }
        return null
    }

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
