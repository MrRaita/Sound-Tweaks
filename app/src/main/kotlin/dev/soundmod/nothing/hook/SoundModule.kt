package dev.soundmod.nothing.hook

import android.media.AudioAttributes
import android.app.Application
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
 *  - SystemUI: ScreenshotSoundProviderImpl.getScreenshotSound'un döndürdüğü MediaPlayer'ı
 *    override/screenshot.* varsa özel dosyayla değiştirir.
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
        val status = StringBuilder("SystemUI: ")
        try {
            hookScreenshot(param.classLoader)
            status.append("Screenshot OK")
        } catch (t: Throwable) {
            status.append("Screenshot HATA: ${t.javaClass.simpleName}: ${t.message}")
            log(Log.ERROR, TAG, "Screenshot hook failed", t)
        }
        reportSysUi(status.toString())
    }

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
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
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

    /**
     * SystemUI /data/system'e yazamayabilir; durumu Settings.Global'a yazarız (SystemUI'nin yetkisi var).
     * Uygulama bağlamı henüz oluşmamış olabileceğinden hazır olana kadar bekler.
     */
    private fun reportSysUi(text: String) {
        val full = "$text [$frameworkName $frameworkVersion, API $apiVersion]"
        Thread {
            repeat(120) {
                try {
                    val app = Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication").invoke(null) as? Application
                    if (app != null) {
                        Settings.Global.putString(app.contentResolver, SoundPaths.SYSUI_STATUS_KEY, full)
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
