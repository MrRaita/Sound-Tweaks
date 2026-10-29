package com.mrraita.soundtwks.hook

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
import com.mrraita.soundtwks.BuildConfig
import com.mrraita.soundtwks.core.SoundPaths
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.io.File

/**
 * libxposed API 101 entry class.
 *
 *  - system_server ("system" scope): replaces the output of SoundEffectsHelper.getResourceFilePath
 *    with /data/system/soundtweaks/override/<stockName>.* when present (keyboard/touch sounds).
 *  - SystemUI:
 *      · ScreenshotSoundProviderImpl.getScreenshotSound → MediaPlayer with the custom file
 *      · KeyguardViewMediator.playSounds / playTrustedSound → lock, unlock and trusted-device
 *        sounds; we play the custom file if there is one, otherwise stock behavior continues.
 *
 * Every hook sits in its own try/catch; on failure the stock behavior is used.
 */
class SoundTweaksModule : XposedModule() {

    @Volatile private var sysUiHooked = false

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        val parts = ArrayList<String>()
        try {
            hookSoundEffects(param.classLoader)
            parts += "SoundEffectsHelper OK"
        } catch (t: Throwable) {
            parts += "SoundEffectsHelper ERROR: ${t.javaClass.simpleName}: ${t.message}"
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
            parts += "Screenshot ERROR: ${t.javaClass.simpleName}: ${t.message}"
            log(Log.ERROR, TAG, "Screenshot hook failed", t)
        }
        try {
            parts += "Keyguard ${hookKeyguard(param.classLoader)}"
        } catch (t: Throwable) {
            parts += "Keyguard ERROR: ${t.javaClass.simpleName}: ${t.message}"
            log(Log.ERROR, TAG, "Keyguard hook failed", t)
        }
        putGlobal(SoundPaths.SYSUI_STATUS_KEY, "SystemUI: ${parts.joinToString(", ")} [$frameworkName $frameworkVersion, API $apiVersion]")
    }

    // ---------------------------------------------------------------------------------------
    // system_server: keyboard and touch sounds
    // ---------------------------------------------------------------------------------------

    private fun hookSoundEffects(cl: ClassLoader) {
        val clazz = Class.forName("com.android.server.audio.SoundEffectsHelper", false, cl)
        val method = clazz.declaredMethods.firstOrNull {
            it.name == "getResourceFilePath" && it.parameterTypes.size == 1
        } ?: error("getResourceFilePath not found")

        // The JIT may inline getResourceFilePath into any caller in this class (including private lambdas and
        // synthetic accessors), which would silently bypass the hook. The set of callers cannot be known reliably
        // across OEM builds, so every other method of this one small helper class is deoptimized. Scope is limited to
        // this class only; the target method itself is hooked, not deoptimized.
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
    // SystemUI: screenshot
    // ---------------------------------------------------------------------------------------

    private fun hookScreenshot(cl: ClassLoader) {
        val clazz = Class.forName("com.android.systemui.screenshot.ScreenshotSoundProviderImpl", false, cl)
        val method = clazz.declaredMethods.firstOrNull {
            it.name == "getScreenshotSound" && it.parameterTypes.isEmpty()
        } ?: error("getScreenshotSound not found")

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
    // SystemUI: lock, unlock, trusted device  (ONLY while "force play" mode is on)
    // ---------------------------------------------------------------------------------------

    /**
     * Default: nothing is touched; the stock mechanism plays the sound at the Settings.Global path (lock_sound etc.)
     * with its own rules and audio stream.
     *
     * If the user turned on "force play" mode (flag file), the keyguard calls are intercepted here when SystemUI
     * starts: the custom sound is played with SoundPool at the media volume, so it is audible in silent/vibrate
     * mode and with the system volume at 0. If the "lock screen sounds" setting is off, stock behavior is kept.
     */
    private fun hookKeyguard(cl: ClassLoader): String {
        if (!File(SoundPaths.FORCE_FLAG).exists()) return "stock (force play off)"

        val clazz = Class.forName("com.android.systemui.keyguard.KeyguardViewMediator", false, cl)
        val boolType = java.lang.Boolean.TYPE
        val playSounds = clazz.declaredMethods.firstOrNull {
            it.name == "playSounds" && it.parameterTypes.size == 1 && it.parameterTypes[0] == boolType
        }
        val playTrusted = clazz.declaredMethods.firstOrNull {
            it.name == "playTrustedSound" && it.parameterTypes.isEmpty()
        }
        if (playSounds == null && playTrusted == null) error("playSounds/playTrustedSound not found")

        // Small private methods may be inlined into their callers. To disturb the system as little as possible we
        // only deoptimize callers in the sound flow (exit finished, going to sleep, *Sound*, access bridges); if none
        // is found (a different ROM) we fall back to every method of the class.
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

        // A SoundPool created before the application context (identity/package info) is ready may stay silent, so
        // the pool is built only after SystemUI has started and the sounds are preloaded.
        preloadLater(0)
        return "force play: ${hooked.joinToString("+")}"
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
     * @return true = we handled the call and the sound really started (stock playback is skipped);
     *         false = let the stock behavior continue. If we cannot play (not loaded yet, error, media volume 0...)
     *         we always leave it to the stock mechanism so the event is never silent.
     */
    private fun forcePlay(key: String, kind: String): Boolean {
        return try {
            if (!File(SoundPaths.FORCE_FLAG).exists()) return false // mode turned off: stock behavior
            val path = FilesIdx.find(key) ?: return false
            val app = currentApp() ?: return false
            if (Settings.System.getInt(app.contentResolver, "lockscreen_sounds_enabled", 1) == 0) {
                return false // user turned lock sounds off: stock behavior (silent)
            }
            val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return false // media volume is zero: it would be inaudible
            val ok = ForcePlayer.play(path)
            if (BuildConfig.VERBOSE_LOG && File(SoundPaths.DEBUG_FLAG).exists()) {
                putGlobal(
                    SoundPaths.SYSUI_LAST_KEY,
                    "$kind: ${if (ok) "played with SoundPool" else "fell back to stock behavior"} [${ForcePlayer.lastInfo}] (${System.currentTimeMillis()})"
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
    // Helpers
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
     * SystemUI may not be able to write to /data/system; status and events are written to Settings.Global (which
     * SystemUI is allowed to do). The application context may not exist yet, so this waits until it is ready.
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

    // The Sound Tweaks app (a different uid) reads the status file, so it is made world-readable
    @Suppress("SetWorldReadable")
    private fun writeStatus(path: String, text: String) {
        try {
            val f = File(path)
            f.parentFile?.mkdirs()
            f.writeText("$text [$frameworkName $frameworkVersion, API $apiVersion]")
            f.setReadable(true, false)
        } catch (t: Throwable) {
            // it does not matter if the status file cannot be written
        }
    }

    private companion object {
        const val TAG = "SoundTweaks"
    }
}

/**
 * Keeps a directory listing in memory. Each lookup only checks the directory's modification time (a single stat);
 * the directory is read once when it changed, so events never cost one file lookup per extension.
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
        for (ext in SoundPaths.EXTENSIONS) { // extension priority order
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
 * A single SoundPool for "force play" mode (the same engine the stock lock sound uses), on the media stream.
 * Sounds are loaded and kept in memory so they play instantly on the event. A changed file is reloaded.
 */
private object ForcePlayer {
    private class Entry(val id: Int, val stamp: Long, var loaded: Boolean, var status: Int = -1)

    private var pool: SoundPool? = null
    private val entries = HashMap<String, Entry>()

    /** Diagnostics: result of the last load/play */
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
                lastInfo = "load=$status"
            }
        }
        pool = p
        return p
    }

    @Synchronized
    fun preload(path: String) {
        ensureLoaded(path)
    }

    /** Starts loading if not loaded yet and returns false (stock plays this time, we play next time). */
    @Synchronized
    fun play(path: String): Boolean {
        val e = ensureLoaded(path) ?: return false
        if (!e.loaded) return false
        val stream = ensurePool().play(e.id, 1f, 1f, 1, 0, 1f)
        lastInfo = "stream=$stream"
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